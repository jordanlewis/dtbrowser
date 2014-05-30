(ns dtbrowser.core
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [servant.macros :refer [defservantfn]])
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [clojure.string :refer [split join capitalize]]
            [cljs.core.async :refer [put! chan <! close!]]
            [servant.core :as servant]
            [servant.worker :as worker]
            [ajax.core :refer [GET]])
  (:import [goog.net XhrIo]
           goog.object
           goog.net.EventType
           [goog.events EventType]))

(def worker-count 2)
(def worker-script "dtbrowser.js")

(enable-console-print!)

;; the song data will go in here after fetching
(def data {})
(def all-items #{})

(def app-state (atom {:items #{} :selected {} :loading true}))

;; our lunr.js index
(def index (js/lunr #(this-as this
                              (. this field "title")
                              (. this ref "id"))))

;; gets called when the user changes the filter text
;; searches the index and sets :selected to the result
(defn handle-filter-change [e app owner {:keys [filter-text]}]
  (let [new-filter-text (.. e -target -value)
        found-items (if (empty? new-filter-text)
                      all-items
                      (into #{} (map #(.-ref %) (. index search new-filter-text))))]
    (prn new-filter-text)
    (om/set-state! owner :filter-text new-filter-text)
    (swap! app-state assoc :items found-items)))


(defn display [show]
  (if show
    #js {}
    #js {:display "none"}))

;; a component for a single song in the list
(defn song-view [songid owner]
  (reify
    om/IRender
    (render [this]
      (dom/li nil;;#js {:style (display (contains? (:items @app-state) songid))}
        (dom/a #js {:onClick (fn [e] (swap! app-state assoc :selected songid))}
               (aget (aget data songid) "title"))))))

;; a component for a list of songs
(defn song-list-view [app owner]
  (reify
    om/IShouldUpdate
    (should-update [this next-props next-state]
      (not= (:items app) (:items next-props)))
    om/IRender
    (render [this]
      (prn "rerendering")
      (apply dom/ul nil
        (map #(om/build song-view % {:react-key (str "result-" %)}) (:items app))))))

;; a component for a filterable list of songs
(defn filterable-song-list-view [app owner]
  (reify
    om/IInitState
    (init-state [this]
      {:filter-text ""})
    om/IRenderState
    (render-state [this state]
      (dom/div #js {:id "song-list-container"}
        (dom/h2 nil "Song list")
          (if (:loading app)
            (dom/span #js {:id "hodor"} "Please wait while the index is populated...")
            (dom/input #js {:type "text" :ref "filter-text" :value (:filter-text state)
                            :onChange #(handle-filter-change % app owner state)}))
        (om/build song-list-view app)))))

(defn app [app owner]
  (reify
    om/IRender
    (render [this]
      (dom/div nil
        (om/build filterable-song-list-view app)
        (dom/div #js {:id "song-view-container"}
          (dom/h2 nil "Song text")
            (if-let [song (aget data (:selected app))]
              (dom/pre nil (join "\n" (aget song "txt")))))))))

(defservantfn build-index [data]
  (prn "Building lunr.js index...")
  (def index (js/lunr #(this-as this
                                (. this field "title")
                                (. this ref "id"))))

  (dorun (map (fn [k i]
                (. index add #js {"id" k "title" (aget (aget data k) "title")}))
              (js/Object.keys data)
              (range)))
  index)

(defn handler [response]
  (prn "Setting up state...")
  (set! data (.getResponseJson (.-target response)))
  (set! all-items (js/Object.keys data))
  (swap! app-state assoc :items all-items)
  (def servant-channel (servant/spawn-servants 1 worker-script))
  (def index-channel (servant/servant-thread servant-channel servant/standard-message build-index data))
  (go
   (set! index (<! index-channel))
     (println "done building index")
   (servant/kill-servants servant-channel 1))

  (swap! app-state assoc :loading false)
  (prn "Done"))

(if (servant/webworker?)
  (worker/bootstrap)
  (do
    (om/root app app-state {:target (. js/document (getElementById "app"))})
    (.send XhrIo "data.json" handler)))

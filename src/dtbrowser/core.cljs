(ns dtbrowser.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [domina :refer [by-id set-text!]]
            [clojure.string :refer [split join capitalize]]
            [cljs.core.async :refer [put! chan <! >! timeout alts!]]
            [goog.events :as events]
            [ajax.core :refer [GET]])
  (:import [goog.net XhrIo]
           goog.object
           goog.net.EventType
           [goog.events EventType]))

(enable-console-print!)

;; the song data will go in here after fetching
(def data {})
(def all-items [])

(def app-state (atom {:items [] :selected {} :loading true}))

;; our lunr.js index
(def index (js/lunr #(this-as this
                              (. this field "title")
                              (. this field "tags")
                              (. this ref "id"))))

(defn search-handler [response]
  (swap! app-state assoc :items
         (map #(% "id")
              (get-in (js->clj (.getResponseJson (.-target response)))
                      ["response" "docs"]))))

(defn search [query]
  (.send XhrIo (str "/solr/collection1/select?q=" query "&rows=1000&fl=id&wt=json&indent=true") search-handler))

;; gets called when the user changes the filter text
;; searches the index and sets :selected to the result
(defn handle-filter-change [e app owner {:keys [filter-text]}]
  (let [new-filter-text (.. e -target -value)]
        ;found-items (if (empty? new-filter-text)
        ;              all-items
        ;              (into [] (map #(.-ref %) (. index search new-filter-text))))]
    (search new-filter-text)
    (prn new-filter-text)
    (om/set-state! owner :filter-text new-filter-text)))

;; a component for a single song in the list
(defn song-view [songid owner]
  (reify
    om/IRender
    (render [this]
      (dom/li nil
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
        (map #(om/build song-view % {:react-key %})
             (if-let [items (:items app)] items (js/Object.keys data)))))))

;; a component for a filterable list of songs
(defn filterable-song-list-view [app owner]
  (reify
    om/IInitState
    (init-state [this]
      {:filter-text ""})
    om/IRenderState
    (render-state [this state]
      (dom/div #js {:id "song-list-container"}
        (dom/div #js {:id "song-list-header"}
          (dom/h2 nil "Song list")
          (if (:loading app)
            (dom/span #js {:id "hodor"} "Please wait while the index is populated...")
            (dom/div nil
              (dom/input #js {:type "text" :ref "filter-text" :value (:filter-text state)
                              :onChange #(handle-filter-change % app owner state)}))))
        (dom/div #js {:id "song-list"}
          (om/build song-list-view app))))))

(defn app [app owner]
  (reify
    om/IRender
    (render [this]
      (dom/div #js {:id "container"}
        (om/build filterable-song-list-view app)
        (dom/div #js {:id "song-view-container"}
          (dom/h2 nil "Song text")
            (if-let [song (aget data (:selected app))]
              (dom/div nil
                       (dom/pre nil (aget song "txt"))
                       (dom/span nil (aget song "auth"))
                       (dom/span nil (aget song "tags")))))))))

(om/root app app-state {:target (. js/document (getElementById "app"))})

(defn add-to-index [queue percent]
  (doseq [k queue]
    (. index add (clj->js {"id" k
                           "title" (aget (aget data k) "title")
                           "body" (aget (aget data k) "txt")
                           "tags" (aget (aget data k) "tags")})))
  (set-text! (by-id "counter") (str percent "%")))

(defn index-loop []
  (let [in (chan)]
    (go (loop [refresh (timeout 40) queue [] counter 0]
          (let [[v c] (alts! [refresh in] :priority true)
                percent (Math/floor (* 100 (/ counter (count all-items))))]
            (condp = c
              refresh (do (add-to-index queue percent)
                          (<! (timeout 0))
                          (recur (timeout 40) [] counter))
              in (if (= v :done)
                   (add-to-index queue percent)
                   (recur refresh (conj queue v) (inc counter)))))))
    in))

(defn index-loop-2 []
  (let [in (chan 500)]
    (go (loop [counter 0 percent 0]
          (let [k (<! in)
                new-percent (Math/floor (* 100 (/ counter (count all-items))))]
            (if k
              (do
                (if-let [v (aget data k)]
                  (. index add
                     (clj->js {"id" k
                               "title" (aget v "title")
                               "tags"  (aget v "tags")})))
                (if-not (= percent new-percent)
                  (set-text! (by-id "counter") (str new-percent "%")))
                (<! (timeout 0))
                (recur (inc counter) new-percent))
              (prn "Done indexing")))))
    in))


(defn handler [response]
  (prn "Setting up state...")
  (set! data (.getResponseJson (.-target response)))
  (set! all-items (js/Object.keys data))
  (swap! app-state assoc :items all-items)

  (comment
  (prn "Adding to index queue...")
  (def ms (. (js/Date.) (getTime)))
  (let [index-chan (index-loop-2)]
    (go
      (doseq [k (js/Object.keys data)]
        (>! index-chan k))
      (>! index-chan :done)))

  )
  (swap! app-state assoc :loading false)
  (prn (str "Done in " (- (. (js/Date.)(getTime)) ms) "ms")))

(.send XhrIo "data.json" handler)

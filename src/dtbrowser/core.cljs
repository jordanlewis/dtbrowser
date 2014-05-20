(ns dtbrowser.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [clojure.string :refer [split join capitalize]]
            [cljs.core.async :refer [put! chan <!]]
            [ajax.core :refer [GET]]))

(enable-console-print!)

(def data {})

(def app-state (atom {:items [] :selected {}}))

(def index (js/lunr #(this-as this (. this field "title") (. this ref "id"))))

(defn handle-filter-change [e app owner {:keys [filter-text]}]
  (let [new-filter-text (.. e -target -value)
        found-items (map #(.-ref %) (. index search new-filter-text))]
    (om/set-state! owner :filter-text new-filter-text)
    (swap! app-state assoc :items found-items)))

(defn song-list-view [app owner]
  (reify
    om/IInitState
    (init-state [this]
      {:filter-text ""})
    om/IRenderState
    (render-state [this state]
      (dom/div #js {:id "song-list-container"}
        (dom/h2 nil "Song list")
        (dom/input #js {:type "text" :ref "filter-text" :value (:filter-text state)
                        :onChange #(handle-filter-change % app owner state)})
               (prn "rerendering")
        (apply dom/ul nil
          (map (fn [[k v]]
                 (dom/li nil
                   (dom/a #js {:onClick (fn [e] (om/update! app :selected k))}
                          (v "title"))))
               (select-keys data (:items app))))))))

(defn app [app owner]
  (reify
    om/IRender
    (render [this]
      (dom/div nil
        (om/build song-list-view app)
        (dom/div #js {:id "song-view-container"}
          (dom/h2 nil "Song text")
          (dom/pre nil (join "\n" (get (get data (:selected app) {}) "txt" ""))))))))

(defn titleize [item]
  (->> "title" item (#(split % " ")) (map capitalize) (join " ") (assoc item "title")))

(defn handler [response]
  (prn "Building lunr.js index...")
  (dorun (map (fn [[k v]] (. index add #js {"id" k "title" (v "title") })) response))
  (prn "Setting up state...")
  (set! data (into {} (for [[k v] response] [k (titleize v)])))
  (swap! app-state assoc :items (keys data))
  (prn "Done"))

(GET "data.json" {:handler handler :response-format :json});

(om/root app app-state {:target (. js/document (getElementById "app"))})

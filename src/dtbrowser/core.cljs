(ns dtbrowser.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [clojure.string :refer [split join capitalize]]
            [cljs.core.async :refer [put! chan <!]]
            [ajax.core :refer [GET]]))

(enable-console-print!)


(def app-state (atom {:text ""}))
(def song-state (atom {:song {}}))

(defn song-view [[n song] owner]
  (reify
    om/IRender
    (render [this]
      (dom/li nil
        (dom/span nil (:name song))))))

(defn main-app [app owner]
  (reify
    om/IRender
    (render [this]
       (dom/div nil
          (dom/h2 nil "Song list")
          (apply dom/ul nil
            (om/build-all song-view (seq {:text app})))))))

(defn simple-app [app owner]
  (apply dom/ul nil
         (map (fn [[a b]]
                (dom/li nil
                  (dom/a #js {:onClick (fn [e] (.log js/console "hi")(swap! song-state assoc :song @b))}
                         (join " " (map capitalize (split (b "title") " "))))))
              (:text app))))

(defn song-view [app owner]
  (dom/div nil
    (dom/h2 nil "Song text")
           (dom/pre nil (join "\n" ((:song app) "txt")))))

(defn handler [response]
  (swap! app-state assoc :text response))
(om/root simple-app app-state {:target (. js/document (getElementById "song-list-container"))})
(om/root song-view song-state {:target (. js/document (getElementById "song-view-container"))})
(GET "../data.json" {:handler handler :response-format :json});

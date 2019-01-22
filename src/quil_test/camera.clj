(ns quil-test.camera
  (:require [quil.core :as q]))

(defn behavior-follow-fn
  [camera entity-to-follow]
  (if-let [player-transform (get-in entity-to-follow [:components :transform])]
    (-> camera
        (assoc-in [:components :transform :x] (+ (:x player-transform) (- (/ (q/width) 2))))
        (assoc-in [:components :transform :y] (+ (:y player-transform) (- (/ (q/height) 2)))))
    camera))

(defn camera
  [entities]
  (for [entity entities]
    (let [camera (get-in entity [:components :camera])
          behaviour (get camera :behavior)]
      (if (and camera behaviour)
        (case (:type behaviour)
          :follow (if-let [entity-to-follow (first (filter #(= (:id %) (get behaviour :entity-to-follow)) entities))]
                    (behavior-follow-fn entity entity-to-follow)
                    entity)
          entity)
        entity))))
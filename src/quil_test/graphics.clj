(ns quil-test.graphics
  (:require [quil.core :as q]))

(defn draw-entity
  [entity]
  (let [drawable (get-in entity [:components :drawable])]
    ((:draw-fn drawable))))

(defn on-screen? [x y]
  (and (<= x (q/width))
       (<= y (q/height))))

(defn graphics
  [entities]
  (if-let [camera (first (filter #(get-in % [:components :camera]) entities))]
    (doseq [entity entities]
      (let [entity-drawable (get-in entity [:components :drawable])
            entity-transform (get-in entity [:components :transform])
            camera-transform (get-in camera [:components :transform])]
        (when (and entity-drawable entity-transform)
          (let [screen-x (- (:x entity-transform) (:x camera-transform))
                screen-y (- (:y entity-transform) (:y camera-transform))]
            (when (on-screen? screen-x screen-y)
              (q/push-matrix)
              (q/translate screen-x screen-y)
              (draw-entity entity)
              (q/pop-matrix))))))))

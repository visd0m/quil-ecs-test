(ns quil-test.movement
  (:require [quil-test.fsm :as fsm]))

(defn handle-transitions
  [entity]
  (let [dx (get-in entity [:components :motion :dx])
        dy (get-in entity [:components :motion :dy])]
    ;(println (:tag entity) " dx:" dx " dy:" dy)
    (if (and (not= dx 0) (not= dy 0))
      (fsm/trigger-transition entity [:jumping])
      (if (not= dx 0)
        (fsm/trigger-transition entity [:moving])
        (if (not= dy 0)
          (fsm/trigger-transition entity [:jumping])
          (fsm/trigger-transition entity [:idle]))))))

(defn movement
  [entities]
  (for [entity entities]
    (let [transform (get-in entity [:components :transform])
          motion (get-in entity [:components :motion])]
      (if (and transform motion)
        (-> entity
            (update-in [:components :transform :x] #(+ (:dx motion) %))
            (update-in [:components :transform :y] #(+ (:dy motion) %))
            (handle-transitions))
        entity))))
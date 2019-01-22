(ns quil-test.movement)

(defn movement

  [entities]
  (for [entity entities]
    (let [transform (get-in entity [:components :transform])
          motion (get-in entity [:components :motion])]
      (if (and transform motion)
        (-> entity
            (update-in [:components :transform :x] #(+ (:dx motion) %))
            (update-in [:components :transform :y] #(+ (:dy motion) %)))
        entity))))
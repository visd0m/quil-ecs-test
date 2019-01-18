(ns quil-test.graphics)

(defn draw-entity
  [entity]
  (let [transform (get-in entity [:components :transform])
        drawable (get-in entity [:components :drawable])]
    ((:draw-fn drawable) transform)))

(defn graphics
  [entities]
  (doseq [entity entities]
    (when (and (get-in entity [:components :drawable]) (get-in entity [:components :transform]))
      (draw-entity entity))))

(ns quil-test.input)

(defn input
  [entities key-event]
  (for [entity entities]
    (let [input (get-in entity [:components :input])
          motion (get-in entity [:components :motion])]
      (if (and input motion)
        (assoc-in entity [:components :motion] ((:input-handler-fn input) entity motion key-event))
        entity))))

(defn handle-movement-wasd-jump
  [entity motion key-event]
  (let [fsm (get-in entity [:components :fsm])]
    (case (:type key-event)
      :down (case (:key key-event)
              :w (if (and fsm (= (:current-state fsm) :jumping))
                   motion
                   (assoc motion :dy (* -1 10)))
              :a (assoc motion :dx (* -1 (:velocity motion)))
              :d (assoc motion :dx (* 1 (:velocity motion)))
              motion)
      :released (case (:key key-event)
                  :a (assoc motion :dx 0)
                  :d (assoc motion :dx 0)
                  motion)
      motion)))
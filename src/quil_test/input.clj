(ns quil-test.input)

(defn
  input
  [entities key-event]
  (for [entity entities]
    (let [input (get-in entity [:components :input])
          motion (get-in entity [:components :motion])]
      (if (and input motion)
        (assoc-in entity [:components :motion] ((:input-handler-fn input) motion key-event))
        entity))))

(defn handle-movement-wasd-jump
  [motion key-event]
  (case (:type key-event)
    :down (case (:key key-event)
            :w (assoc motion :dy (* -1 (:velocity motion)))
            :a (assoc motion :dx (* -1 (:velocity motion)))
            :s (assoc motion :dy (* 1 (:velocity motion)))
            :d (assoc motion :dx (* 1 (:velocity motion)))
            motion)
    :released (case (:key key-event)
                :w (assoc motion :dy 0)
                :a (assoc motion :dx 0)
                :s (assoc motion :dy 0)
                :d (assoc motion :dx 0)
                motion)
    motion))
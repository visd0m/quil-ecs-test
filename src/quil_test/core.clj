(ns quil-test.core
  (:require [quil.core :as q]
            [quil.middleware :as m]))

; graphics

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

; movement

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

; physics

(defn is-physics-eligible?
  [entity]
  (and (get-in entity [:components :collider])
       (get-in entity [:components :transform])))

(defn get-min-aab
  [entity]
  (let [transform (get-in entity [:components :transform])
        motion (get-in entity [:components :motion] {:dx 0 :dy 0})]
    {:x (+ (:x transform) (:dx motion))
     :y (+ (:y transform) (:dy motion))}))

(defn get-max-aab
  [entity]
  (let [transform (get-in entity [:components :transform])
        collider (get-in entity [:components :collider])
        motion (get-in entity [:components :motion] {:dx 0 :dy 0})]
    {:x (+ (+ (:x transform) (:dx motion)) (:width collider))
     :y (+ (+ (:y transform) (:dy motion) (:height collider)))}))

(defn get-aab
  [entity]
  {:min-aab (get-min-aab entity)
   :max-aab (get-max-aab entity)})

(defn aabs-overlapping?
  [{min-aab1 :min-aab max-aab1 :max-aab}
   {min-aab2 :min-aab max-aab2 :max-aab}]
  (if (or (< (:x max-aab1) (:x min-aab2)) (> (:x min-aab1) (:x max-aab2)))
    false
    (if (or (< (:y max-aab1) (:y min-aab2)) (> (:y min-aab1) (:y max-aab2)))
      false
      true)))

(defn is-colliding?
  [e1 e2]
  (aabs-overlapping? (get-aab e1) (get-aab e2)))

(defn physics
  [entities]
  (for [entity entities]
    (if (and (is-physics-eligible? entity) (not (empty? (->> entities
                                                             (filter #(not (= (:id %) (:id entity))))
                                                             (filter is-physics-eligible?)
                                                             (filter (fn [e2] (is-colliding? entity e2)))))))
      (-> entity
          (assoc-in [:components :motion :dx] 0)
          (assoc-in [:components :motion :dy] 0))
      entity)))

; input

(defn input
  [entities key-event]
  (for [entity entities]
    (let [input (get-in entity [:components :input])
          motion (get-in entity [:components :motion])]
      (if (and input motion)
        (assoc-in entity [:components :motion] ((:input-handler-fn input) motion key-event))
        entity))))

(defn handle-movement-wasd
  [motion key-event]
  (println "key-event=" key-event)
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

; setting up

(defn create-entity
  []
  {:id (java.util.UUID/randomUUID)})

(defn register-entity
  [state entity]
  (assoc state :entities (conj (:entities state) entity)))

(defn attach-component-on-entity
  [entity component]
  (assoc-in entity [:components (:type component)]
            (merge (:component component) {:id (java.util.UUID/randomUUID)})))

(defn create-world
  []
  (let [state {:systems  {:graphics graphics
                          :movement movement
                          :physics  physics}
               :entities []}
        player (-> (create-entity)
                   (attach-component-on-entity {:type      :transform
                                                :component {:x 100 :y 100}})
                   (attach-component-on-entity {:type      :input
                                                :component {:input-handler-fn handle-movement-wasd}})
                   (attach-component-on-entity {:type      :motion
                                                :component {:velocity 2
                                                            :dx       0
                                                            :dy       0}})
                   (attach-component-on-entity {:type      :drawable
                                                :component {:draw-fn (fn [transform]
                                                                       (q/fill 0 0 0)
                                                                       (q/rect (:x transform) (:y transform) 30 30))}})
                   (attach-component-on-entity {:type      :collider
                                                :component {:width 30 :height 30}}))
        wall1 (-> (create-entity)
                  (attach-component-on-entity {:type      :transform
                                               :component {:x 0 :y 0}})
                  (attach-component-on-entity {:type      :drawable
                                               :component {:draw-fn (fn [transform]
                                                                      (q/fill 100 100 100)
                                                                      (q/rect (:x transform) (:y transform) 20 500))}})
                  (attach-component-on-entity {:type      :collider
                                               :component {:width 20 :height 500}}))
        wall2 (-> (create-entity)
                  (attach-component-on-entity {:type      :transform
                                               :component {:x 480 :y 0}})
                  (attach-component-on-entity {:type      :drawable
                                               :component {:draw-fn (fn [transform]
                                                                      (q/fill 100 100 100)
                                                                      (q/rect (:x transform) (:y transform) 20 450))}})
                  (attach-component-on-entity {:type      :collider
                                               :component {:width 20 :height 450}}))
        wall3 (-> (create-entity)
                  (attach-component-on-entity {:type      :transform
                                               :component {:x 300 :y 300}})
                  (attach-component-on-entity {:type      :drawable
                                               :component {:draw-fn (fn [transform]
                                                                      (q/fill 100 100 100)
                                                                      (q/rect (:x transform) (:y transform) 20 20))}})
                  (attach-component-on-entity {:type      :collider
                                               :component {:width 20 :height 20}}))]
    (-> state
        (register-entity player)
        (register-entity wall1)
        (register-entity wall2)
        (register-entity wall3))))

(defn setup []
  (q/frame-rate 60)
  (q/color-mode :rgb)
  (create-world))

; update

(defn update-state
  [state]
  (assoc state :entities (-> (:entities state)
                             (physics)
                             (movement))))

(defn key-down
  [state key-event]
  (assoc state :entities (-> (:entities state)
                             (input (assoc key-event :type :down)))))

(defn key-released
  [state key-event]
  (assoc state :entities (-> (:entities state)
                             (input (assoc key-event :type :released)))))

; draw

(defn draw
  [state]
  (q/background 255)
  (graphics (:entities state)))

; sketch

(q/defsketch quil-test
             :title "ecs test"
             :size [500 500]
             :setup setup
             :key-pressed key-down
             :key-released key-released
             :update update-state
             :draw draw
             :features [:keep-on-top]
             :middleware [m/fun-mode])
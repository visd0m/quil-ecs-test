(ns quil-test.core
  (:require [quil.core :as q]
            [quil.middleware :as m]
            [quil-test.entity :as entity]
            [quil-test.physics :as physics]
            [quil-test.graphics :as graphics]
            [quil-test.movement :as movement]
            [quil-test.input :as input]))

; setting up

(defn create-world
  []
  (let [state {:systems  {:graphics graphics
                          :movement movement
                          :physics  physics}
               :entities []}
        player (-> (entity/create-entity)
                   (entity/attach-component-on-entity {:type      :transform
                                                       :component {:x 100 :y 100}})
                   (entity/attach-component-on-entity {:type      :input
                                                       :component {:input-handler-fn handle-movement-wasd}})
                   (entity/attach-component-on-entity {:type      :motion
                                                       :component {:velocity 2
                                                                   :dx       0
                                                                   :dy       0}})
                   (entity/attach-component-on-entity {:type      :drawable
                                                       :component {:draw-fn (fn [transform]
                                                                              (q/fill 0 0 0)
                                                                              (q/rect (:x transform) (:y transform) 30 30))}})
                   (entity/attach-component-on-entity {:type      :collider
                                                       :component {:width 30 :height 30}}))
        wall1 (-> (entity/create-entity)
                  (entity/attach-component-on-entity {:type      :transform
                                                      :component {:x 0 :y 0}})
                  (entity/attach-component-on-entity {:type      :drawable
                                                      :component {:draw-fn (fn [transform]
                                                                             (q/fill 100 100 100)
                                                                             (q/rect (:x transform) (:y transform) 20 500))}})
                  (entity/attach-component-on-entity {:type      :collider
                                                      :component {:width 20 :height 500}}))
        wall2 (-> (entity/create-entity)
                  (entity/attach-component-on-entity {:type      :transform
                                                      :component {:x 480 :y 0}})
                  (entity/attach-component-on-entity {:type      :drawable
                                                      :component {:draw-fn (fn [transform]
                                                                             (q/fill 100 100 100)
                                                                             (q/rect (:x transform) (:y transform) 20 450))}})
                  (entity/attach-component-on-entity {:type      :collider
                                                      :component {:width 20 :height 450}}))
        wall3 (-> (entity/create-entity)
                  (entity/attach-component-on-entity {:type      :transform
                                                      :component {:x 300 :y 300}})
                  (entity/attach-component-on-entity {:type      :drawable
                                                      :component {:draw-fn (fn [transform]
                                                                             (q/fill 100 100 100)
                                                                             (q/rect (:x transform) (:y transform) 20 20))}})
                  (entity/attach-component-on-entity {:type      :collider
                                                      :component {:width 20 :height 20}}))]
    (-> state
        (entity/register-entity player)
        (entity/register-entity wall1)
        (entity/register-entity wall2)
        (entity/register-entity wall3))))

(defn setup []
  (q/frame-rate 60)
  (q/color-mode :rgb)
  (create-world))

; update

(defn update-state
  [state]
  (assoc state :entities (-> (:entities state)
                             (physics/physics)
                             (movement/movement))))

(defn key-down
  [state key-event]
  (assoc state :entities (-> (:entities state)
                             (input/input (assoc key-event :type :down)))))

(defn key-released
  [state key-event]
  (assoc state :entities (-> (:entities state)
                             (input/input (assoc key-event :type :released)))))

; draw

(defn draw
  [state]
  (q/background 255)
  (graphics/graphics (:entities state)))

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
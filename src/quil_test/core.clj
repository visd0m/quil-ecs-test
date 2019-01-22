(ns quil-test.core

  (:require [quil.core :as q]
            [quil.middleware :as m]
            [quil-test.entity :as entity]
            [quil-test.physics :as physics]
            [quil-test.graphics :as graphics]
            [quil-test.movement :as movement]
            [quil-test.camera :as camera]
            [quil-test.input :as input]))

; setting up

(defn create-world
  []
  (let [state {:entities []}
        player (-> (entity/create-entity "player")
                   (entity/attach-component-on-entity {:type      :transform
                                                       :component {:x 0
                                                                   :y 0}})
                   (entity/attach-component-on-entity {:type      :input
                                                       :component {:input-handler-fn input/handle-movement-wasd}})
                   (entity/attach-component-on-entity {:type      :motion
                                                       :component {:velocity 5
                                                                   :dx       0
                                                                   :dy       0}})
                   (entity/attach-component-on-entity {:type      :drawable
                                                       :component {:draw-fn (fn []
                                                                              (q/fill 0 0 0)
                                                                              (q/rect 0 0 30 30))}})
                   (entity/attach-component-on-entity {:type      :collider
                                                       :component {:width          30
                                                                   :height         30
                                                                   :is-rigid-body? true}}))

        camera (-> (entity/create-entity "camera")
                   (entity/attach-component-on-entity {:type      :transform
                                                       :component {:x 0
                                                                   :y 0}})
                   (entity/attach-component-on-entity {:type      :camera
                                                       :component {:behavior {:type             :follow
                                                                              :entity-to-follow (:id player)}}}))
        wall1 (-> (entity/create-entity "wall1")
                  (entity/attach-component-on-entity {:type      :transform
                                                      :component {:x -50 :y -50}})
                  (entity/attach-component-on-entity {:type      :drawable
                                                      :component {:draw-fn (fn []
                                                                             (q/fill 100 100 100)
                                                                             (q/rect 0 0 20 500))}})
                  (entity/attach-component-on-entity {:type      :collider
                                                      :component {:width          20
                                                                  :height         500
                                                                  :is-rigid-body? true}}))

        wall2 (-> (entity/create-entity "wall2")
                  (entity/attach-component-on-entity {:type      :transform
                                                      :component {:x -100 :y -100}})
                  (entity/attach-component-on-entity {:type      :drawable
                                                      :component {:draw-fn (fn []
                                                                             (q/fill 100 100 100)
                                                                             (q/rect 0 0 20 450))}})
                  (entity/attach-component-on-entity {:type      :collider
                                                      :component {:width          20
                                                                  :height         450
                                                                  :is-rigid-body? true}}))

        wall3 (-> (entity/create-entity "wall3")
                  (entity/attach-component-on-entity {:type      :transform
                                                      :component {:x 20 :y 20}})
                  (entity/attach-component-on-entity {:type      :drawable
                                                      :component {:draw-fn (fn []
                                                                             (q/fill 100 100 100)
                                                                             (q/rect 0 0 20 20))}})
                  (entity/attach-component-on-entity {:type      :collider
                                                      :component {:width              20
                                                                  :height             20
                                                                  :is-rigid-body?     false
                                                                  :is-colliding?      false
                                                                  :on-collision-enter (fn [this-entity]
                                                                                        (println "[on collision ENTER] entity=" (:tag this-entity))
                                                                                        this-entity)
                                                                  :on-collision-exit  (fn [this-entity]
                                                                                        (println "[on collision EXIT] entity=" (:tag this-entity))
                                                                                        this-entity)}}))]
    (-> state
        (entity/register-entity player)
        (entity/register-entity camera)
        (entity/register-entity wall1)
        (entity/register-entity wall2)
        (entity/register-entity wall3))))

(defn setup []
  (q/frame-rate 60)
  (create-world))

; update

(defn update-state
  [state]
  (assoc state :entities (-> (:entities state)
                             (physics/physics)
                             (movement/movement)
                             (camera/camera))))

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
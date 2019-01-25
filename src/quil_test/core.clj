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

(defn- create-player
  [x y]
  (-> (entity/create-entity "player")
      (entity/attach-component-on-entity {:type      :transform
                                          :component {:x x
                                                      :y y}})
      (entity/attach-component-on-entity {:type      :input
                                          :component {:input-handler-fn input/handle-movement-wasd-jump}})
      (entity/attach-component-on-entity {:type      :motion
                                          :component {:velocity 2
                                                      :dx       0
                                                      :dy       0}})
      (entity/attach-component-on-entity {:type      :drawable
                                          :component {:draw-fn (fn []
                                                                 (q/fill 255 0 255)
                                                                 (q/rect 0 0 30 30))}})
      (entity/attach-component-on-entity {:type      :collider
                                          :component {:width          30
                                                      :height         30
                                                      :is-rigid-body? true
                                                      :is-kinematic?  false}})))

(defn- create-camera
  [entity-to-follow]
  (-> (entity/create-entity "camera")
      (entity/attach-component-on-entity {:type      :transform
                                          :component {:x (get-in entity-to-follow [:components :transform :x])
                                                      :y (get-in entity-to-follow [:components :transform :y])}})
      (entity/attach-component-on-entity {:type      :camera
                                          :component {:behavior {:type             :follow
                                                                 :entity-to-follow (:id entity-to-follow)}}})))

(defn- create-wall
  [{x              :x
    y              :y
    width          :width
    height         :height
    is-rigid-body? :is-rigid-body?, :or {is-rigid-body? true}}]
  (-> (entity/create-entity "wall")
      (entity/attach-component-on-entity {:type      :transform
                                          :component {:x x
                                                      :y y}})
      (entity/attach-component-on-entity {:type      :drawable
                                          :component {:draw-fn (fn []
                                                                 (q/fill 100 100 100)
                                                                 (q/rect 0 0 width height))}})
      (entity/attach-component-on-entity {:type      :collider
                                          :component {:width          width
                                                      :height         height
                                                      :is-rigid-body? is-rigid-body?
                                                      :is-kinematic?  true}})))

(defn create-world
  []
  (let [state {:entities []}
        player (create-player 0 0)
        camera (create-camera player)
        wall1 (create-wall {:x -30 :y -380 :width 20 :height 500})
        ground1 (create-wall {:x -10 :y 100 :width 500 :height 20})
        ground2 (create-wall {:x 300 :y 300 :width 500 :height 20})
        wall2 (create-wall {:x -110 :y -100 :width 20 :height 450})
        wall3 (create-wall {:x 20 :y 20 :width 20 :height 20 :is-rigid-body? false})]
    (-> state
        (entity/register-entity player)
        (entity/register-entity camera)
        (entity/register-entity wall1)
        (entity/register-entity wall2)
        (entity/register-entity ground1)
        (entity/register-entity ground2)
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
             :size [400 400]
             :setup setup
             :key-pressed key-down
             :key-released key-released
             :update update-state
             :draw draw
             :features [:keep-on-top]
             :middleware [m/fun-mode])
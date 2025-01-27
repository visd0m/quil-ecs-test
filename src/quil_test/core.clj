(ns quil-test.core
  (:require [quil.core :as q]
            [quil.middleware :as m]
            [quil-test.physics :as physics]
            [quil-test.graphics :as graphics]
            [quil-test.movement :as movement]
            [quil-test.camera :as camera]
            [quil-test.input :as input]
            [quil-test.entity :as entity]
            [quil-test.fsm :as fsm]
            [quil-test.script :as script]))

; setting up

(defn- create-player
  [x y width height]
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
                                          :component {:width   width
                                                      :height  height
                                                      :draw-fn (fn []
                                                                 (q/fill 255 0 255)
                                                                 (q/rect 0 0 width height))}})
      (entity/attach-component-on-entity {:type      :collider
                                          :component {:width          width
                                                      :height         height
                                                      :is-rigid-body? true
                                                      :is-kinematic?  false}})
      (entity/attach-component-on-entity {:type      :script
                                          :component {:fn script/player-script}})
      (entity/attach-component-on-entity {:type      :fsm
                                          :component {:current-state         :idle
                                                      :triggered-transitions []
                                                      :states                [{:id          :idle
                                                                               :components  [{:type      :drawable
                                                                                              :component {:width   width
                                                                                                          :height  height
                                                                                                          :draw-fn (fn []
                                                                                                                     (q/fill 255 0 255)
                                                                                                                     (q/rect 0 0 width height))}}
                                                                                             {:type      :input
                                                                                              :component {:input-handler-fn input/handle-movement-wasd-jump}}]
                                                                               :transitions [:moving :jumping]}
                                                                              {:id          :moving
                                                                               :components  [{:type      :drawable
                                                                                              :component {:width   width
                                                                                                          :height  height
                                                                                                          :draw-fn (fn []
                                                                                                                     (q/fill 255 255 255)
                                                                                                                     (q/rect 0 0 width height))}}
                                                                                             {:type      :input
                                                                                              :component {:input-handler-fn input/handle-movement-wasd-jump}}]
                                                                               :transitions [:idle :jumping]}
                                                                              {:id          :jumping
                                                                               :components  [{:type      :drawable
                                                                                              :component {:width   width
                                                                                                          :height  height
                                                                                                          :draw-fn (fn []
                                                                                                                     (q/fill 0 0 0)
                                                                                                                     (q/rect 0 0 width height))}}
                                                                                             {:type      :input
                                                                                              :component {:input-handler-fn input/handle-movement-wasd}}]
                                                                               :transitions [:idle :moving]}]}})))

(defn- create-camera
  [entity-to-follow]
  (-> (entity/create-entity "camera")
      (entity/attach-component-on-entity {:type      :transform
                                          :component {:x (get-in entity-to-follow [:components :transform :x])
                                                      :y (get-in entity-to-follow [:components :transform :y])}})
      (entity/attach-component-on-entity {:type      :camera
                                          :component {:behavior {:type             :follow
                                                                 :entity-to-follow (:id entity-to-follow)}}})))

(defn- create-rigid-body
  [{x              :x
    y              :y
    tag            :tag
    width          :width
    height         :height
    is-rigid-body? :is-rigid-body?, :or {is-rigid-body? true}}]
  (-> (entity/create-entity tag)
      (entity/attach-component-on-entity {:type      :transform
                                          :component {:x x
                                                      :y y}})
      (entity/attach-component-on-entity {:type      :drawable
                                          :component {:width   width
                                                      :height  height
                                                      :draw-fn (fn []
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
        player (create-player 0 0 30 30)
        camera (create-camera player)
        wall1 (create-rigid-body {:x -30 :y -380 :tag "wall" :width 20 :height 500})
        ground1 (create-rigid-body {:x -10 :y 100 :tag "ground" :width 500 :height 20})
        ground2 (create-rigid-body {:x 300 :y 300 :tag "ground" :width 500 :height 20})
        wall2 (create-rigid-body {:x -110 :y -100 :tag "wall" :width 20 :height 450})
        wall3 (create-rigid-body {:x 20 :y 20 :tag "wall" :width 20 :height 20 :is-rigid-body? false})]
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
                             (camera/camera)
                             (fsm/fsm)
                             (script/script))))

; input

(defn key-down
  [state key-event]
  (assoc state :entities (-> (:entities state)
                             (input/input (assoc key-event :type :down)))))

(defn key-released
  [state key-event]
  (assoc state :entities (-> (:entities state)
                             (input/input (assoc key-event :type :released)))))

(defn is-mouse-over-entity?
  [mouse-x mouse-y entity]
  (let [entity-transform (get-in entity [:components :transform])
        entity-drawable (get-in entity [:components :drawable])
        entity-min-x (:x entity-transform)
        entity-min-y (:y entity-transform)
        entity-max-x (+ (:x entity-transform) (:width entity-drawable))
        entity-max-y (+ (:y entity-transform) (:height entity-drawable))]
    (let [result (and (>= mouse-x entity-min-x) (<= mouse-x entity-max-x)
                      (>= mouse-y entity-min-y) (<= mouse-y entity-max-y))]
      (if result
        (do
          (println "tag=" (:tag entity) " mouse-x=" mouse-x " entity-min-x=" entity-min-x " entity-max-x=" entity-max-x)
          (println "tag=" (:tag entity) " mouse-y=" mouse-y " entity-min-y=" entity-min-y " entity-max-y=" entity-max-y)))
      result)))

(defn is-entity-draggable?
  [entity]
  (and (get-in entity [:components :transform]) (get-in entity [:components :drawable])))

(defn mouse-dragged
  [state key-event]
  (assoc state :entities (for [entity (:entities state)]
                           (let [mouse-x (- (:x key-event) (/ (q/width) 2))
                                 mouse-y (- (:y key-event) (/ (q/height) 2))]
                             (if (and (is-entity-draggable? entity)
                                      (is-mouse-over-entity? mouse-x mouse-y entity))
                               (-> entity
                                   (assoc-in [:components :transform :x] (+ (get-in entity [:components :transform :x]) (- mouse-x (get-in entity [:components :transform :x]))))
                                   (assoc-in [:components :transform :y] (+ (get-in entity [:components :transform :y]) (- mouse-y (get-in entity [:components :transform :y])))))
                               entity)))))

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
  :mouse-dragged mouse-dragged
  :key-pressed key-down
  :key-released key-released
  :update update-state
  :draw draw
  :features [:keep-on-top]
  :middleware [m/fun-mode])

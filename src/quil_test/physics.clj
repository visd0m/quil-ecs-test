(ns quil-test.physics)

(defn is-physics-eligible?
  [entity]
  (and (get-in entity [:components :collider])
       (get-in entity [:components :transform])))

(defn get-min-aab
  [x y]
  {:x x
   :y y})

(defn get-max-aab
  [x y width height]
  {:x (+ x width)
   :y (+ y height)})

(defn get-aab
  [x y width height]
  {:min-aab (get-min-aab x y)
   :max-aab (get-max-aab x y width height)})

(defn aabs-overlapping?
  [{min-aab1 :min-aab max-aab1 :max-aab}
   {min-aab2 :min-aab max-aab2 :max-aab}]
  (if (or (< (:x max-aab1) (:x min-aab2)) (> (:x min-aab1) (:x max-aab2)))
    false
    (if (or (< (:y max-aab1) (:y min-aab2)) (> (:y min-aab1) (:y max-aab2)))
      false
      true)))

(defn- collision-dx-dy
  [e1-transform e1-collider dx dy
   e2-transform e2-collider]
  (let [x-collision? (aabs-overlapping? (get-aab (+ (:x e1-transform) dx)
                                                 (:y e1-transform)
                                                 (:width e1-collider)
                                                 (:height e1-collider))
                                        (get-aab (:x e2-transform)
                                                 (:y e2-transform)
                                                 (:width e2-collider)
                                                 (:height e2-collider)))
        y-collision? (aabs-overlapping? (get-aab (:x e1-transform)
                                                 (+ (:y e1-transform) dy)
                                                 (:width e1-collider)
                                                 (:height e1-collider))
                                        (get-aab (:x e2-transform)
                                                 (:y e2-transform)
                                                 (:width e2-collider)
                                                 (:height e2-collider)))]
    {:collision? (or x-collision? y-collision?) :x-collision? x-collision? :y-collision? y-collision?}))

(defn- collision-dx
  [e1-transform e1-collider dx
   e2-transform e2-collider]
  (let [x-collision? (aabs-overlapping? (get-aab (+ (:x e1-transform) dx)
                                                 (:y e1-transform)
                                                 (:width e1-collider)
                                                 (:height e1-collider))
                                        (get-aab (:x e2-transform)
                                                 (:y e2-transform)
                                                 (:width e2-collider)
                                                 (:height e2-collider)))]
    {:collision? x-collision? :x-collision? x-collision? :y-collision? false}))

(defn- collision-dy
  [e1-transform e1-collider dy
   e2-transform e2-collider]
  (let [y-collision? (aabs-overlapping? (get-aab (:x e1-transform)
                                                 (+ (:y e1-transform) dy)
                                                 (:width e1-collider)
                                                 (:height e1-collider))
                                        (get-aab (:x e2-transform)
                                                 (:y e2-transform)
                                                 (:width e2-collider)
                                                 (:height e2-collider)))]
    {:collision? y-collision? :x-collision? false :y-collision? y-collision?}))

(defn get-collision-with-motion
  [e1-transform e1-collider e1-motion
   e2-transform e2-collider]
  (let [dx (* (:dx e1-motion) (:velocity e1-motion))
        dy (* (:dy e1-motion) (:velocity e1-motion))]
    (if (and (not= dx 0) (not= dy 0))
      (collision-dx-dy e1-transform e1-collider dx dy
                       e2-transform e2-collider)
      (if (not= dx 0)
        (collision-dx e1-transform e1-collider dx
                      e2-transform e2-collider)
        (if (not= dy 0)
          (collision-dy e1-transform e1-collider dy
                        e2-transform e2-collider)
          {:collision? false :x-collision? false :y-collision? false})))))

(defn get-collision
  [e1 e2]
  (let [e1-transform (get-in e1 [:components :transform])
        e1-collider (get-in e1 [:components :collider])
        e2-transform (get-in e2 [:components :transform])
        e2-collider (get-in e2 [:components :collider])]
    (merge {:entity e2} (if (aabs-overlapping? (get-aab (:x e1-transform)
                                                        (:y e1-transform)
                                                        (:width e1-collider)
                                                        (:height e1-collider))
                                               (get-aab (:x e2-transform)
                                                        (:y e2-transform)
                                                        (:width e2-collider)
                                                        (:height e2-collider)))
                          {:collision? true :x-collision? true :y-collision? true}
                          (if-let [e1-motion (get-in e1 [:components :motion])]
                            (get-collision-with-motion e1-transform e1-collider e1-motion
                                                       e2-transform e2-collider)
                            {:collision? false :x-collision? false :y-collision? false})))))

(defn get-collisions
  [entity all-entities]
  (->> all-entities
       (filter #(not (= (:id %) (:id entity))))
       (filter is-physics-eligible?)
       (map (fn [e2] (get-collision entity e2)))
       (filter :collision?)))

(defn update-x
  [entity collisions]
  (let [x-collision? (not-empty (filter :x-collision? collisions))]
    (if x-collision?
      (assoc-in entity [:components :motion :dx] 0)
      entity)))

(defn update-y
  [entity collisions]
  (let [y-collision? (not-empty (filter :y-collision? collisions))]
    (if y-collision?
      (assoc-in entity [:components :motion :dy] 0)
      entity)))

(defn update-motion
  [entity collisions]
  (if (get-in entity [:components :motion])
    (-> entity
        (update-x collisions)
        (update-y collisions))
    entity))

(defn update-entity-motion
  [entity collisions]
  (if (and (not-empty (->> collisions
                           (map :entity)
                           (filter #(get-in % [:components :collider :is-rigid-body?])))))
    (update-motion entity collisions)
    entity))

(defn update-entity-on-collision-enter
  [entity]
  (if-not (get-in entity [:components :collider :is-colliding?])
    (if-let [on-collision-enter-fn (get-in entity [:components :collider :on-collision-enter])]
      (on-collision-enter-fn entity)
      entity)
    entity))

(defn update-entity-on-collision-exit
  [entity]
  (if (get-in entity [:components :collider :is-colliding?])
    (if-let [on-collision-exit-fn (get-in entity [:components :collider :on-collision-exit])]
      (on-collision-exit-fn entity)
      entity)
    entity))

(defn update-collisions
  [entity all-entities]
  (let [collisions (get-collisions entity all-entities)]
    (if (not-empty collisions)
      (-> entity
          (update-entity-motion collisions)
          (update-entity-on-collision-enter)
          (assoc-in [:components :collider :is-colliding?] true)
          (assoc-in [:components :collider :collisions] collisions))
      (-> entity
          (update-entity-on-collision-exit)
          (assoc-in [:components :collider :is-colliding?] false)
          (assoc-in [:components :collider :collisions] [])))))

(defn update-gravity
  [entity]
  (if-not (get-in entity [:components :collider :is-kinematic?])
    (if (get-in entity [:components :motion])
      (update-in entity [:components :motion :dy] #(if (< % 10) (+ % 0.5) %))
      entity)
    entity))

(defn handle-physics
  [entity all-entities]
  (-> entity
      (update-gravity)
      (update-collisions all-entities)))

(defn physics
  [entities]
  (for [entity entities]
    (if (is-physics-eligible? entity)
      (handle-physics entity entities)
      entity)))
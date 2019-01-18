(ns quil-test.physics)

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

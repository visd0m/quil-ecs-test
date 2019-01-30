(ns quil-test.entity)

(defn create-entity
  [tag]
  {:id  (java.util.UUID/randomUUID)
   :tag tag})

(defn register-entity
  [state entity]
  (assoc state :entities (conj (:entities state) entity)))

(defn attach-component-on-entity
  [entity component]
  ;(println "attaching component=" component " to entity=" entity)
  (let [component-with-id (merge (:component component) {:id (java.util.UUID/randomUUID)})]
    (assoc-in entity [:components (:type component)] component-with-id)))
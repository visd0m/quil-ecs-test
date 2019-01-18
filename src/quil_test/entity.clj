(ns quil-test.entity)

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

(ns quil-test.fsm
  (:require [quil-test.entity :as entity]))

(defn- get-current-state
  [fsm]
  (let [current-state-id (get fsm :current-state)
        states (:states fsm)]
    (->> states
         (filter #(= (:id %) current-state-id))
         (first))))

(defn- is-valid-transition?
  [new-state-id current-state]
  (let [allowed-transitions (get current-state :transitions)]
    (some #(= new-state-id %) allowed-transitions)))

(defn- get-state
  [fsm state-id]
  (let [states (:states fsm)]
    (->> states
         (filter #(= (:id %) state-id))
         (first))))

(defn set-state
  [entity state]
  (if (get-in entity [:components :fsm])
    (let [components (get state :components [])
          updated-entity (assoc-in entity [:components :fsm :current-state] (:id state))]
      (reduce #(entity/attach-component-on-entity %1 %2) updated-entity components))
    entity))

(defn- handle-transitions
  [entity transitions fsm]
  (let [current-state (get-current-state fsm)
        state (->> transitions
                   (filter #(is-valid-transition? % current-state))
                   (map #(get-state fsm %))
                   (first))]
    (if state
      (set-state entity state)
      entity)))

(defn fsm
  [entities]
  (for [entity entities]
    (if-let [fsm (get-in entity [:components :fsm])]
      (let [triggered-transitions (get fsm :triggered-transitions [])]
        (if (seq triggered-transitions)
          (-> entity
              (handle-transitions triggered-transitions fsm)
              (assoc-in [:components :fsm :triggered-transitions] []))
          entity))
      entity)))

(defn trigger-transition
  [entity transition-ids]
  (println "triggering transition" transition-ids)
  (if (get-in entity [:components :fsm])
    (-> entity
        (update-in [:components :fsm :triggered-transitions] #(distinct (concat % transition-ids))))
    entity))
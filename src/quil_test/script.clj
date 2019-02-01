(ns quil-test.script
  (:require [quil-test.fsm :as fsm]))

; helpers

(defn is-grounded?
  [entity]
  (let [entity-transform (get-in entity [:components :transform])
        collisions (get-in entity [:components :collider :collisions])]
    (if (and entity-transform (seq collisions))
      (if-let [ground-transform (first (->> collisions
                                            (filter :y-collision?)
                                            (filter #(and (= (get-in % [:entity :tag]) "ground")))
                                            (map #(get-in % [:entity :components :transform]))))]
        (<= (:y entity-transform) (:y ground-transform))
        false)
      false)))

(defn player-script
  [entity]
  (let [dx (get-in entity [:components :motion :dx])
        dy (get-in entity [:components :motion :dy])]
    (if (and (not= dx 0) (not= dy 0))
      (fsm/trigger-transition entity [:jumping])
      (if (not= dx 0)
        (fsm/trigger-transition entity [:moving])
        (if (not= dy 0)
          (fsm/trigger-transition entity [:jumping])
          (if (is-grounded? entity)
            (fsm/trigger-transition entity [:idle])
            entity))))))

; system

(defn script
  [entities]
  (for [entity entities]
    (if-let [script (get-in entity [:components :script])]
      (let [script-fn (:fn script)]
        (script-fn entity))
      entity)))
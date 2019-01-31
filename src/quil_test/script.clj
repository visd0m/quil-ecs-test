(ns quil-test.script)

(defn script
  [entities]
  (for [entity entities]
    (if-let [script (get-in entity [:components :script])]
      (let [script-fn (:fn script)]
        (script-fn entity))
      entity)))
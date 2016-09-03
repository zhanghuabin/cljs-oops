(ns oops.schema
  (:require [clojure.spec :as s]
            [clojure.walk :as walk]))

; --- specs -----------------------------------------------------------------------------------------------------------------

(s/def ::obj-key (s/or :string string? :keyword keyword?))
(s/def ::obj-selector (s/or :key ::obj-key :selector (s/* ::obj-selector)))
(s/def ::obj-path-segment string?)
(s/def ::obj-path (s/or :invalid #{::invalid-path} :path (s/* ::obj-path-segment)))

; --- path utils ------------------------------------------------------------------------------------------------------------

(defn coerce-key [destructured-key]
  (let [value (second destructured-key)]
    (case (first destructured-key)
      :string value
      :keyword (name value))))

(defn coerce-key-node [node]
  (if (and (sequential? node)
           (= (first node) :key))
    [(coerce-key (second node))]
    node))

(defn coerce-selector-keys [destured-selector]
  (walk/postwalk coerce-key-node destured-selector))

(defn coerce-selector-node [node]
  (if (and (sequential? node)
           (= (first node) :selector))
    (vector (second node))
    node))

(defn coerce-nested-selectors [destured-selector]
  (walk/postwalk coerce-selector-node destured-selector))

(defn build-selector-path [destructured-selector]
  {:post [(s/valid? ::obj-path %)]}
  (if (= destructured-selector ::s/invalid)
    ::invalid-path
    (-> destructured-selector
        (coerce-selector-keys)
        (coerce-nested-selectors)
        (flatten))))

(defn selector->path [selector]
  (->> selector
       (s/conform ::obj-selector)
       (build-selector-path)))

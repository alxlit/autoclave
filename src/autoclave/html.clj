(ns autoclave.html
  "Wraps the OWASP HTML Sanitizer library."
  (:require [clojure.string :as string])
  (:import [org.owasp.html AttributePolicy
                           ElementPolicy
                           HtmlPolicyBuilder
                           HtmlSanitizer
                           HtmlSanitizer$Policy
                           PolicyFactory
                           Sanitizers]))

(defn- attr-policy [f]
  (proxy [AttributePolicy] []
    (apply [element-name attr-name value] (f element-name attr-name value))))

(def attr-options-map
  {:globally
   (fn [builder _] (.globally builder))
   :matching
   (fn [builder [f]]
     (.matching builder (if (fn? f) (attr-policy f) f)))
   :on-elements
   (fn [builder args]
     (.onElements builder (into-array String args)))})

(defn- ensure-args
  [options]
  (->> options
       (partition 2 1 [])
       (map (fn [[a b]]
              (if (keyword? a) (list a (if-not (keyword? b) b)))))
       (mapcat identity)
       (partition 2)))

(defn- apply-attr-options
  "Apply a sequence of attribute options to an AttributeBuilder."
  [builder options]
  (let [options (ensure-args options)]
    (doseq [[kw args] options]
      ((attr-options-map kw) builder args))
    builder))

(defn- element-policy [f]
  (proxy [ElementPolicy] []
    (apply [element-name attrs] (f element-name attrs))))

(def options-map
  {:allow-attributes
   (fn [builder args]
     (let [[names options] (split-with #(not (keyword? %)) args)]
       (-> builder
           (.allowAttributes (into-array String names))
           (apply-attr-options options))))
   :allow-common-block-elements
   (fn [builder _] (.allowCommonBlockElements builder))
   :allow-common-inline-formatting-elements
   (fn [builder _] (.allowCommonInlineFormattingElements builder))
   :allow-elements
   (fn [builder args]
     (let [[[f] names] (split-with #(fn? %) args)
           names (into-array String names)
           policy (if f (element-policy f))]
       (if policy
         (.allowElements builder policy names)
         (.allowElements builder names))))
   :allow-standard-url-protocols
   (fn [builder _] (.allowStandardUrlProtocols builder))
   :allow-styling
   (fn [builder _] (.allowStyling builder))
   :allow-text-in
   (fn [builder args]
     (.allowTextIn builder (into-array String args)))
   :allow-url-protocols
   (fn [builder args]
     (.allowUrlProtocols builder (into-array String args)))
   :allow-without-attributes
   (fn [builder args]
     (.allowWithoutAttributes builder (into-array String args)))
   :disallow-attributes
   (fn [builder args]
     (let [[names options] (split-with #(not (keyword? %)) args)]
       (-> builder
           (.disallowAttributes (into-array String names))
           (apply-attr-options options))))
   :disallow-elements
   (fn [builder args]
     (.disallowElements builder (into-array String args)))
   :disallow-text-in
   (fn [builder args]
     (.disallowTextIn builder (into-array String args)))
   :disallow-url-protocols
   (fn [builder args]
     (.disallowUrlProtocols builder (into-array String args)))
   :disallow-without-attributes
   (fn [builder args]
     (.disallowWithoutAttributes builder (into-array String args)))
   :require-rel-nofollow-on-links
   (fn [builder _]
     (.requireRelNofollowOnLinks builder))})

(defn apply-options
  [builder options]
  (let [options (ensure-args options)]
    (doseq [[kw args] options]
      ((options-map kw) builder args))
    builder))

(def policies-map
  "Predefined policies."
  {:BLOCKS      Sanitizers/BLOCKS
   :FORMATTING  Sanitizers/FORMATTING
   :IMAGES      Sanitizers/IMAGES
   :LINKS       Sanitizers/LINKS
   :STYLES      Sanitizers/STYLES})

(defn policy
  "Access a predefined policy or create one from a sequence of options."
  [& options]
  (or (policies-map (first options))
      (.toFactory (apply-options (HtmlPolicyBuilder.) options))))

(defn- ensure-policy
  "Ensure that the argument is an instance of PolicyFactory, which supports
   merging (and doesn't have a copy constructor)."
  [p]
  (if (instance? HtmlSanitizer$Policy p)
    (if (not (instance? PolicyFactory p))
      (throw (Exception. (str (class p) " doesn't support merging."))) p)
    (apply policy (conj [] p))))

(defn merge-policies
  "Merge multiple PolicyFactory and/or option sequences together."
  [& policies]
  (->> policies (map ensure-policy) (reduce #(.and % %))))

(defn sanitize
  "Apply an HTML sanitization policy (a PolicyFactory object rather than a
   sequence of options) to a string of HTML."
  ([html] (sanitize (policy) html))
  ([policy html] (.sanitize policy html)))

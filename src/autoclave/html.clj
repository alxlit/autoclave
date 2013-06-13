(ns autoclave.html
  "Wraps the OWASP HTML Sanitizer."
  (:require [clojure.string :as string])
  (:import [org.owasp.html AttributePolicy ElementPolicy HtmlPolicyBuilder
                           HtmlSanitizer HtmlSanitizer$Policy PolicyFactory
                           Sanitizers]))

(defn- ensure-args
  "Utility for handling policy option sequences; not all options take
   arguments, and this allows omitting empty argument vectors."
  [options]
  (->> options
       (partition 2 1 [])
       (map (fn [[a b]] (if (keyword? a) (list a (if-not (keyword? b) b)))))
       (mapcat identity)
       (partition 2)))

(defn- attribute-policy
  [f]
  (proxy [AttributePolicy] []
    (apply [element-name attr-name value] (f element-name attr-name value))))

(defn- element-policy
  [f]
  (proxy [ElementPolicy] []
    (apply [element-name attrs] (f element-name attrs))))

(defn- apply-attribute-options
  "Apply options to an HtmlPolicyBuilder.AttributeBuilder."
  [builder options]
  (let [options (ensure-args options)]
    (doseq [[kw args] options]
      (case kw
        :globally (.globally builder)
        :matching
        (let [[match] args]
          (if (fn? match)
            (.matching builder (attribute-policy match))
            (.matching builder match)))
        :on-elements (.onElements builder (into-array String args)))))
  builder)

(defn- apply-options
  "Apply options to an HtmlPolicyBuilder."
  [builder options]
  (let [options (ensure-args options)]
    (doseq [[kw args] options]
      (case kw
        :allow-attributes
        (let [[names options] (split-with #(not (keyword? %)) args)
              names (into-array String names)
              builder (.allowAttributes builder names)]
          (apply-attribute-options builder options))
        :allow-common-block-elements (.allowCommonBlockElements builder)
        :allow-common-inline-formatting-elements
        (.allowCommonInlineFormattingElements builder)
        :allow-elements
        (let [policy (if (fn? (first args)) (element-policy (first args)))
              names (into-array String (if (nil? policy) args (rest args)))]
          (if (nil? policy)
            (.allowElements builder names)
            (.allowElements builder policy names)))
        :allow-standard-url-protocols (.allowStandardUrlProtocols builder)
        :allow-styling (.allowStyling builder)
        :allow-text-in (.allowTextIn builder (into-array String args))
        :allow-url-protocols
        (.allowUrlProtocols builder (into-array String args))
        :allow-without-attributes
        (.allowWithoutAttributes builder (into-array String args))
        :disallow-attributes
        (let [[names options] (split-with #(not (keyword? %)) args)
              names (into-array String names)
              builder (.disallowAttributes builder names)]
          (apply-attribute-options builder options))
        :disallow-elements (.disallowElements builder (into-array String args))
        :disallow-text-in (.disallowTextIn builder (into-array String args))
        :disallow-url-protocols
        (.disallowUrlProtocols builder (into-array String args))
        :disallow-without-attributes
        (.disallowWithoutAttributes builder (into-array String args))
        :require-rel-nofollow-on-links (.requireRelNofollowOnLinks builder))))
  builder)

(def prepackaged-policies
  {:BLOCKS      Sanitizers/BLOCKS
   :FORMATTING  Sanitizers/FORMATTING
   :IMAGES      Sanitizers/IMAGES
   :LINKS       Sanitizers/LINKS
   :STYLES      Sanitizers/STYLES})

(defn policy
  "Build an HTML sanitization policy."
  [& options]
  (if-let [policy (prepackaged-policies (first options))]
    policy
    (.toFactory (apply-options (HtmlPolicyBuilder.) options))))

(defn- ensure-policy
  [p]
  (if (instance? HtmlSanitizer$Policy p)
    (if-not (instance? PolicyFactory p)
      (throw (Exception. (str "Please use PolicyFactory, as it supports "
                              "merging (" (class p) " does not).")))
      p)
    (policy p)))

(defn merge-policies
  "Merge multiple sanitization policies (PolicyFactory objects or options
   sequences) together."
  [& policies]
  (->> policies (map ensure-policy) (reduce #(.and % %))))

(defn sanitize
  "Apply a sanitization policy to a string of HTML."
  ([html] (.sanitize (policy) html))
  ([policy html] (.sanitize policy html)))

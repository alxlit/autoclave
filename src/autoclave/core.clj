(ns autoclave.core
  "Provides aliases to the important bits."
  (:require [autoclave.html :as html]
            [autoclave.json :as json]
            [autoclave.markdown :as markdown]))

(def html-sanitize
  "Alias for html/sanitize."
  html/sanitize)

(def html-policy
  "Alias for html/policy."
  html/policy)

(def html-merge-policies
  "Alias for html/merge-policies."
  html/merge-policies)

(def json-sanitize
  "Alias for json/sanitize."
  json/sanitize)

(def markdown-link-renderer
  "Alias for markdown/link-renderer."
  markdown/link-renderer)

(def markdown-processor
  "Alias for markdown/processor."
  markdown/processor)

(def markdown-to-html
  "Alias for markdown/to-html"
  markdown/to-html)

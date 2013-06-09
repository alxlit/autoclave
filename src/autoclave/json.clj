(ns autoclave.json
  "Wraps the OWASP JSON Sanitizer project."
  (:import [com.google.json JsonSanitizer]))

(defn sanitize
  "Sanitize a string containing JSON-like content."
  [json]
  (JsonSanitizer/sanitize json))

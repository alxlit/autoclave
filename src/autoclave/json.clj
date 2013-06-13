(ns autoclave.json
  "Wraps the OWASP JSON Sanitizer."
  (:import [com.google.json JsonSanitizer]))

(defn sanitize
  "Sanitize a string containing JSON-like content."
  [json]
  (JsonSanitizer/sanitize json))

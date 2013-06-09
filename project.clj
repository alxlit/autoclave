(defproject autoclave "0.1.0"
  :description "A library for sanitizing various kinds of user input."
  :url "http://github.com/alxlit/autoclave-clj"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :codox {:output-dir "doc/codox"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [com.googlecode.owasp-java-html-sanitizer/owasp-java-html-sanitizer "r173"]]

  ; JSON Sanitizer is included locally as it isn't available from any
  ; repositories, as far as I know.
  :resource-paths ["resources/json-sanitizer-2012-10-17.jar"])


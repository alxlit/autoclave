(defproject alxlit/autoclave "0.2.0"
  :description "A library for safely handling various kinds of user input."
  :url "http://github.com/alxlit/autoclave"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :codox {:output-path "doc/codox"
          :source-uri "http://github.com/alxlit/autoclave/blob/{version}/{filepath}#L{line}"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.pegdown/pegdown "1.6.0"]
                 [commons-codec "1.4"]
                 [com.google.guava/guava "11.0"]
                 [com.mikesamuel/json-sanitizer "1.2.2"
                  :exclusions [com.google.guava/guava
                               com.google.code.findbugs/jsr305]]
                 [com.googlecode.owasp-java-html-sanitizer/owasp-java-html-sanitizer "20190325.1"
                  :exclusions [commons-codec
                               com.google.guava/guava
                               com.google.code.findbugs/jsr305]]])



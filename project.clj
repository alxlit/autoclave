(defproject autoclave "0.1.2"
  :description "A library for sanitizing various kinds of user input."
  :url "http://github.com/alxlit/autoclave"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :codox {:output-dir "doc/codox"
          :src-dir-uri "http://github.com/alxlit/autoclave/blob/master"
          :src-linenum-anchor-prefix "L"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [com.googlecode.owasp-java-html-sanitizer/owasp-java-html-sanitizer "r173"]]
  :java-source-paths ["src/java"])


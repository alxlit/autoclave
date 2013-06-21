(ns autoclave.markdown_test
  (:require [autoclave.core :refer :all]
            [clojure.string :refer [capitalize]]
            [clojure.test :refer [deftest is]]))

(def example
  (str "# Header 1\n"
       "## Header 2\n"
       "Lorem ipsum dolor sit amet, consectetur adipiscing elit.\n"
       "<span style=\"color: red\">Some HTML.</span>\n"
       "http://google.com.\n"
       "And here is a [[wiki]] link."))

(deftest test-none
  (is (= (markdown-to-html example)
         (str "<h1>Header 1</h1>"
              "<h2>Header 2</h2>"
              "<p>Lorem ipsum dolor sit amet, consectetur adipiscing elit. "
              "<span style=\"color: red\">Some HTML.</span> "
              "http://google.com. "
              "And here is a [[wiki]] link.</p>"))))

(deftest test-supress-all-html
  (let [processor (markdown-processor :suppress-all-html)]
    (is (= (markdown-to-html processor example)
           (str "<h1>Header 1</h1>"
                "<h2>Header 2</h2>"
                "<p>Lorem ipsum dolor sit amet, consectetur adipiscing elit. "
                "Some HTML. "
                "http://google.com. "
                "And here is a [[wiki]] link.</p>")))))

(deftest test-links
  (let [processor (markdown-processor :autolinks :wikilinks)]
    (is (= (markdown-to-html processor example)
           (str "<h1>Header 1</h1>"
                "<h2>Header 2</h2>"
                "<p>Lorem ipsum dolor sit amet, consectetur adipiscing elit. "
                "<span style=\"color: red\">Some HTML.</span> "
                "<a href=\"http://google.com\">http://google.com</a>. "
                "And here is a <a href=\"./wiki.html\">wiki</a> link.</p>")))))

(deftest test-link-renderer
  (let [link-renderer (markdown-link-renderer
                        {:auto (fn [node]
                                 {:text (->> (.getText node)
                                             (re-find #"://\.?(\w+).")
                                             second
                                             capitalize)
                                  :href (.getText node)
                                  :attributes ["class" "autolink"]})})
        processor (markdown-processor :autolinks)]
    (is (= (markdown-to-html processor link-renderer example)
           (str "<h1>Header 1</h1>"
                "<h2>Header 2</h2>"
                "<p>Lorem ipsum dolor sit amet, consectetur adipiscing elit. "
                "<span style=\"color: red\">Some HTML.</span> "
                "<a href=\"http://google.com\" class=\"autolink\">Google</a>. "
                "And here is a [[wiki]] link.</p>")))))


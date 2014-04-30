(ns autoclave.html_test
  "Adapted from the OWASP HTML Sanitizer test suite (HtmlPolicyBuilderTest.java)."
  (:require [autoclave.core :refer :all]
            [clojure.test :refer [deftest is]]
            [clojure.string :as string]))

(def example
  (str "<h1 id='foo'>Header</h1>\n"
       "<p onclick='alert(42)'>Paragraph 1<script>evil()</script></p>\n"
       "<p><a href='javascript:bad()'>Click</a> <a href='foo.html'>me</a>"
       " <a href='http://outside.org/'>out</a></p>\n"
       "<p><img src=canary.png alt=local-canary>"
       "<img src='http://canaries.org/canary.png'></p>\n"
       "<p><b style=font-size:bigger>Fancy</b> with <i><b>soupy</i> tags</b>.\n"
       "<p style='color: expression(foo()); text-align: center;\n"
       "        /* direction: ltr */; font-weight: bold'>Stylish Para 1</p>\n"
       "<p style='color: red; font-weight; expression(foo());\n"
       "          direction: rtl; font-weight: bold'>Stylish Para 2</p>\n"))

(deftest test-text-filter
  (let [policy (html-policy)]
    (is (= (html-sanitize policy example)
           (str "Header\n"
                "Paragraph 1\n"
                "Click me out\n"
                "\n"
                "Fancy with soupy tags.\n"
                "Stylish Para 1\n"
                "Stylish Para 2\n")))))

(deftest test-canned-formatting-tag-filter
  (let [policy (html-policy :allow-common-inline-formatting-elements)]
    (is (= (html-sanitize policy example)
           (str "Header\n"
                "Paragraph 1\n"
                "Click me out\n"
                "\n"
                "<b>Fancy</b> with <i><b>soupy</b></i><b> tags</b>.\n"
                "Stylish Para 1\n"
                "Stylish Para 2\n")))))

(deftest test-canned-formatting-tag-filter-no-italics
  (let [policy (html-policy :allow-common-inline-formatting-elements
                            :disallow-elements ["i"])]
    (is (= (html-sanitize policy example)
           (str "Header\n"
                "Paragraph 1\n"
                "Click me out\n"
                "\n"
                "<b>Fancy</b> with <b>soupy</b><b> tags</b>.\n"
                "Stylish Para 1\n"
                "Stylish Para 2\n")))))

(deftest test-simple-tag-filter
  (let [policy (html-policy :allow-elements ["h1" "i"])]
    (is (= (html-sanitize policy example)
           (str "<h1>Header</h1>\n"
                "Paragraph 1\n"
                "Click me out\n"
                "\n"
                "Fancy with <i>soupy</i> tags.\n"
                "Stylish Para 1\n"
                "Stylish Para 2\n")))))

(deftest test-links-allowed
  (let [policy (html-policy :allow-elements ["a"]
                            :allow-attributes ["href" :on-elements ["a"]])]
    (is (= (html-sanitize policy example)
           (str "Header\n"
                "Paragraph 1\n"
                "Click <a href=\"foo.html\">me</a> out\n"
                "\n"
                "Fancy with soupy tags.\n"
                "Stylish Para 1\n"
                "Stylish Para 2\n")))))

(deftest test-external-links-allowed
  (let [policy (html-policy :allow-elements ["a"]
                            :allow-standard-url-protocols
                            :allow-attributes ["href" :on-elements ["a"]])]
    (is (= (html-sanitize policy example)
           (str "Header\n"
                "Paragraph 1\n"
                "Click <a href=\"foo.html\">me</a>"
                " <a href=\"http://outside.org/\">out</a>\n"
                "\n"
                "Fancy with soupy tags.\n"
                "Stylish Para 1\n"
                "Stylish Para 2\n")))))

(deftest test-links-with-nofollow
  (let [policy (html-policy :allow-elements ["a"]
                            :allow-attributes ["href" :on-elements ["a"]]
                            :require-rel-nofollow-on-links)]
    (is (= (html-sanitize policy example)
           (str "Header\n"
                "Paragraph 1\n"
                "Click <a href=\"foo.html\" rel=\"nofollow\">me</a> out\n"
                "\n"
                "Fancy with soupy tags.\n"
                "Stylish Para 1\n"
                "Stylish Para 2\n")))))

(deftest test-images-allowed
  (let [policy (html-policy :allow-elements ["img"]
                            :allow-attributes ["src" "alt" :on-elements ["img"]]
                            :allow-url-protocols ["https"])]
    (is (= (html-sanitize policy example)
           (str "Header\n"
                "Paragraph 1\n"
                "Click me out\n"
                "<img src=\"canary.png\" alt=\"local-canary\" />\n"
                "Fancy with soupy tags.\n"
                "Stylish Para 1\n"
                "Stylish Para 2\n")))))

(deftest test-style-filtering
  (let [policy (html-policy :allow-common-inline-formatting-elements
                            :allow-common-block-elements
                            :allow-styling
                            :allow-standard-url-protocols)]
    (is (= (html-sanitize policy example)
           (str "<h1>Header</h1>\n"
                "<p>Paragraph 1</p>\n"
                "<p>Click me out</p>\n"
                "<p></p>\n"
                "<p><b>Fancy</b> with <i><b>soupy</b></i><b> tags</b>.\n"
                "</p><p style=\"text-align:center;font-weight:bold\">"
                "Stylish Para 1</p>\n"
                "<p style=\"color:red;direction:rtl;font-weight:bold\">"
                "Stylish Para 2</p>\n")))))

(deftest test-element-transforming
  (let [policy (html-policy :allow-elements ["h1" "p" "div"]
                            :allow-elements
                            [(fn [element-name attrs]
                               (doto attrs
                                 (.add "class")
                                 (.add (str "header-" element-name)))
                               "div")
                            "h1"])]

    (is (= (html-sanitize policy example)
           (str "<div class=\"header-h1\">Header</div>\n"
                "<p>Paragraph 1</p>\n"
                "<p>Click me out</p>\n"
                 "<p></p>\n"
                 "<p>Fancy with soupy tags.\n"
                 "</p><p>Stylish Para 1</p>\n"
                 "<p>Stylish Para 2</p>\n")))))

(deftest test-allow-url-protocols
  (let [policy (html-policy :allow-elements ["img"]
                            :allow-attributes ["src" "alt" :on-elements ["img"]]
                            :allow-url-protocols ["http"])]
    (is (= (html-sanitize policy example)
           (str "Header\n"
                "Paragraph 1\n"
                "Click me out\n"
                "<img src=\"canary.png\" alt=\"local-canary\" />"
                "<img src=\"http://canaries.org/canary.png\" />\n"
                "Fancy with soupy tags.\n"
                "Stylish Para 1\n"
                "Stylish Para 2\n")))))

(deftest test-merge-policies
  (let [policy (html-policy :BLOCKS :LINKS)]
    nil))


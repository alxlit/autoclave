(ns autoclave.markdown
  "Wraps the PegDown library."
  (:require [autoclave.html :as html])
  (:import [org.pegdown Extensions
                        LinkRenderer
                        LinkRenderer$Attribute
                        LinkRenderer$Rendering
                        PegDownProcessor]
           [org.pegdown.ast AutoLinkNode
                            ExpLinkNode
                            MailLinkNode
                            RefLinkNode
                            WikiLinkNode]))

(def options-map
  {:abbreviations         Extensions/ABBREVIATIONS
   :all                   Extensions/ALL
   :autolinks             Extensions/AUTOLINKS
   :definitions           Extensions/DEFINITIONS
   :fenced-code-blocks    Extensions/FENCED_CODE_BLOCKS
   :hardwraps             Extensions/HARDWRAPS
   :none                  Extensions/NONE
   :quotes                Extensions/QUOTES
   :smarts                Extensions/SMARTS
   :smartypants           Extensions/SMARTYPANTS
   :suppress-all-html     Extensions/SUPPRESS_ALL_HTML
   :suppress-html-blocks  Extensions/SUPPRESS_HTML_BLOCKS
   :suppress-inline-html  Extensions/SUPPRESS_INLINE_HTML
   :tables                Extensions/TABLES
   :wikilinks             Extensions/WIKILINKS})

(defn- link-rendering
  "Helper for turning a hash-map into a LinkRenderer$Rendering."
  [rendering]
  (if (map? rendering)
    (let [{:keys [href text attributes]} rendering
          rendering (LinkRenderer$Rendering. href text)]
      (doseq [[name value] (partition 2 attributes)]
        (.withAttribute rendering name value))
      rendering)
    rendering))

(defn link-renderer
  "Helper for proxying the LinkRenderer class."
  [handlers]
  (proxy [LinkRenderer] []
    (render [node & args]
      (let [handler (condp instance? node
                      AutoLinkNode (:auto handlers)
                      ExpLinkNode
                      (or (:explicit handlers)
                          (fn [node text] (proxy-super render node text)))
                      MailLinkNode (:mail handlers)
                      RefLinkNode
                      (or (:reference handlers)
                          (fn [node url title text]
                            (proxy-super render node url title text)))
                      WikiLinkNode (:wiki handlers))
            handler (or handler (fn [node] (proxy-super render node)))]
        (link-rendering (apply handler node args))))))

(defn processor
  "Creates a function that produces PegDownProcessor instances with the given
   extensions enabled."
  ([] (processor :none))
  ([& options]
   (let [options (map options-map options)
         options (if (seq options) (reduce (comp int bit-or) options))]
     (fn [] (PegDownProcessor. options)))))

(defn to-html
  "Render a string of Markdown to HTML, optionally using the provided
   PegDownProcessor and LinkRenderer."
  ([md]
   (to-html (processor) md))
  ([processor md]
   (to-html processor (link-renderer {}) md))
  ([processor link-renderer md]
   (.markdownToHtml (processor) md link-renderer)))

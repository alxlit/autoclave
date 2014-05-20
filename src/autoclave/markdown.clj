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
                            ExpImageNode
                            MailLinkNode
                            RefLinkNode
                            RefImageNode
                            WikiLinkNode]))

(def extensions
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
   :strikethrough         Extensions/STRIKETHROUGH
   :suppress-all-html     Extensions/SUPPRESS_ALL_HTML
   :suppress-html-blocks  Extensions/SUPPRESS_HTML_BLOCKS
   :suppress-inline-html  Extensions/SUPPRESS_INLINE_HTML
   :tables                Extensions/TABLES
   :wikilinks             Extensions/WIKILINKS})

(defn- link-rendering
  "Turns a map into a LinkRendering$Rendering."
  [rendering]
  (if (map? rendering)
    (let [{:keys [href text attributes]} rendering
          rendering (LinkRenderer$Rendering. href text)]
      (doseq [[name value] (partition 2 attributes)]
        (.withAttribute rendering name value))
      rendering)
    rendering))

(defn link-renderer
  "Create a LinkRenderer."
  [handlers]
  (proxy [LinkRenderer] []
    (render [node & args]
      (let [handler (condp instance? node
                      AutoLinkNode (:auto handlers)
                      ExpLinkNode
                      (or (:explicit handlers)
                          (fn [node text] 
                            (proxy-super render node text)))
                      ExpImageNode
                      (or (:explicit-image handlers)
                          (fn [node text] 
                            (proxy-super render node text)))
                      MailLinkNode (:mail handlers)
                      RefLinkNode
                      (or (:reference handlers)
                          (fn [node url title text]
                            (proxy-super render node url title text)))
                      WikiLinkNode (:wiki handlers)
                      RefImageNode 
                      (or (:reference-image handlers)
                          (fn [node url title text]
                            (proxy-super render node url title text))))
            handler (or handler (fn [node] (proxy-super render node)))]
        (link-rendering (apply handler node args))))))

(defn processor
  "Returns a function that produeces PegDownProcessor instances with the given
  extensions enabled."
  ([]
     (processor :none))
  ([& exts]
     (let [flags (reduce (comp int bit-or) (map extensions exts))]
       #(PegDownProcessor. flags))))

(defn to-html
  "Render a string of Markdown to HTML, optionally using the provided
   PegDownProcessor and LinkRenderer."
  ([md]
   (to-html (processor) md))
  ([processor md]
   (to-html processor (link-renderer {}) md))
  ([processor link-renderer md]
   (.markdownToHtml (processor) md link-renderer)))


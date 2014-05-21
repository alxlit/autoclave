(ns autoclave.markdown
  "Wraps the PegDown library."
  (:require [autoclave.html :as html])
  (:import [org.pegdown Extensions
                        LinkRenderer
                        LinkRenderer$Attribute
                        LinkRenderer$Rendering
                        PegDownProcessor
                        FastEncoder] 
           [org.pegdown.ast AutoLinkNode
                            ExpLinkNode
                            ExpImageNode
                            MailLinkNode
                            RefLinkNode
                            RefImageNode
                            WikiLinkNode]
           [java.lang IllegalStateException]
           [java.io UnsupportedEncodingException]))

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


;;
;; 2014-05-21 Reworked markdown.clj, espeed (James Thornton, http://jamesthornton.com)
;;
;; Implemented most of Pegdown's LinkRenderer class in Clojure
;; https://github.com/sirthias/pegdown/blob/master/src/main/java/org/pegdown/LinkRenderer.java
;; Notably the default-handlers format is the same as the custom handlers format used for tests,
;; and most of the proxy/proxy-super code is gone. Trying to ensure perfect parity with the 
;; Java class to ensure output is the same for benchmark and comparision purposes.
;; If/when the Pegdown devs create an interface for LinkRenderer, 
;; it would enable better/faster Clojure-Java interop, e.g. reify could be used instead of proxy
;; See https://github.com/sirthias/pegdown/issues/132
;;
;; Description from Pegdown's LinkedRenderer class:
;;
;; A LinkRenderer is responsible for turning an AST node representing a link into a {@link LinkRenderer.Rendering}
;; instance, which hold the actual properties of the link as it is going to be rendered.
;; If you'd like to apply custom logic to link rendering (e.g. for selectively adding "nofollow" attributes) you
;; should derive a custom LinkRenderer from this class and override the respective methods.
;; https://github.com/sirthias/pegdown/blob/master/src/main/java/org/pegdown/LinkRenderer.java
;;

;; Utils and Helper funcs used by the Pegdown LinkRenderer class

(defn encode-url
  "Returns an encoded URL, as ued by Pegdown."
  [^String url]
  (java.net.URLEncoder/encode url "UTF-8"))

(defn encode
  "Returns an encoded attribute value, as used by Pegdown."
  [^String value] 
  (FastEncoder/encode value))

(defn obfuscate
  "Returns an obfuscated email address, as used by Pegdown."
  [^String email] 
  (FastEncoder/obfuscate email))

(defn attribute
  "Attribute constructor."
  [name value] 
  (LinkRenderer$Attribute. name (encode value)))

(defn with-attribute 
  "Adds attribute to the LinkRenderer$Rendering instance."
  ([rendering name value] 
     (.withAttribute rendering (attribute name value)))
  ([rendering attribute]
     (.withAttribute rendering attribute))) 

(defn add-attribs
  "Adds multiple attributes to- and returns the LinkRenderer$Rendering instance."
  [rendering attributes]
  (doseq [[name value] (partition 2 attributes)] 
    (with-attribute rendering name value))
  rendering)

(defn wiki-url
  [node]
  "Returns the default Pegdown-formatted wiki URL."
  (let [dashed-url (.replace (.getText node) " " "-")]
    (str "./" dashed-url ".html")))


;; NOTE: Not presently catching and throwing the wiki encoding exception; 
;; however, the Java pegdown wiki-link renderer does do this
;;
;; (defn- wiki-link-node
;;   [node]
;;   (let [dashed-url (.replace (.getText node) " " "-")
;;         wiki-url (str "./" dashed-url ".html")]
;;     (try 
;;       (rendering (wiki-url node) (.getText node))
;;       (catch UnsupportedEncodingException e
;;         (throw IllegalStateException.)))))


;; TODO: Be consistent with handler names, e.g. auto to auto-link
(def default-handlers
  "Default LinkRenderer handlers map; Custom handlers can be passed into the 
   link-renderer func where they will be merged with these."
  {:auto       (fn [node]      
                {:href (.getText node)
                 :text (.getText node)
                 :attributes nil})
   :exp-link   (fn [node text]  
                 {:href (.url node)
                  :text text
                  :attributes ["title" (.title node)]})
   :exp-image  (fn [node text] 
                 {:href (.url node)
                  :text text
                  :attributes ["title" (.title node)]})
   :mail-link  (fn [node]      
                 {:href (str "mailto:" (obfuscate (.getText node)))
                  :text (.getText (obfuscate (.getText node)))
                  :attributes nil})
   :ref-link   (fn [node url title text]  
                 {:href url
                  :text text
                  :attributes ["title" title]})
   :ref-image  (fn [node url title alt]  
                 {:href url
                  :text alt
                  :attributes ["title" title]})
   :wiki-link  (fn [node]      
                 {:href (wiki-url node)
                  :text (.getText node)
                  :attributes nil})})

(defn select-handler
  "Returns the default handler func for the node type."
  [node handlers]
  (condp instance? node
    AutoLinkNode   (:auto handlers)
    ExpLinkNode    (:exp-link handlers)
    ExpImageNode   (:exp-image handlers) 
    MailLinkNode   (:mail-link handlers)
    RefLinkNode    (:ref-link handlers)
    RefImageNode   (:ref-image handlers) 
    WikiLinkNode   (:wiki-link handlers)))

(defn rendering 
  "Builds and returns Pegdown LinkRenderer. Rendering instance for the node type; 
   args and attributes are set in the node-type's handler. Sets multiple link tag 
   attributes if name/value pairs are supplied in the node handler's attributes vector."
  ([handler node args]
     (let [{:keys [href text attributes]} (apply handler node args)]
       (-> (LinkRenderer$Rendering. href text) (add-attribs attributes)))))

(defn link-renderer
  "Returns a Pegdown LinkRenderer$Rendering instance, 
   which holds the actual properties of the link as it is going to be rendered.
   To use custom link renderers, pass in a custom handlers map formatted like 
   like the default-handlers map (the two maps will be merged together)."
  [handlers]
  (proxy [LinkRenderer] []
    (render [node & args]
      (let [handlers (merge default-handlers handlers)
            handler  (select-handler node handlers)]
        (rendering handler node args)))))
        

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
   PegDownProcessor and LinkRenderer.
   Using Pegdown's default LinkRenderer unless custom handlers are provided.
   To use the Clojure default link-renderer instead, 
   change (LinkedRender.) to (link-renderer {})"
  ([md]
   (to-html (processor) md))
  ([processor md]
   (to-html processor (LinkRenderer.) md))
  ([processor link-renderer md]
   (.markdownToHtml (processor) md link-renderer)))


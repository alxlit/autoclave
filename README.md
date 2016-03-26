# autoclave

A library for safely handling various kinds of user input. The idea is
to provide a simple, convenient API that builds upon existing, proven
libraries such as [OWASP JSON Sanitizer][owasp-json], [OWASP HTML
Sanitizer][owasp-html], and [PegDown][pegdown]

[![Clojars Project](https://img.shields.io/clojars/v/alxlit/autoclave.svg)](https://clojars.org/alxlit/autoclave)

## Usage

```clj
(require '[autoclave.core :refer :all])
```

### JSON

The `json-sanitize` function takes a string containing JSON-like
content and produces well-formed JSON. It corrects minor mistakes in
encoding and makes it easier to embed in HTML and XML documents.

```clj
(json-sanitize "{some: 'derpy json' n: +123}")
; "{\"some\": \"derpy json\" ,\"\":\"n\" ,\"\":123}"
```

More information, quoted from [here][owasp-json-gh]:

> The output is well-formed JSON as defined by [RFC 4627][rfc-4627].
The output satisfies (four) additional properties:

> 1. The output will not contain the substring (case-insensitively)
     `</script` so can be embedded inside an HTML script element without
     further encoding.
> 2. The output will not contain the substring `]]>` so can be embedded inside
     an XML CDATA section without further encoding.
> 3. The output is a valid Javascript expression, so can be parsed by
     Javascript's `eval` builtin (after being wrapped in parentheses) or by
     `JSON.parse`. Specifically, the output will not contain any string
     literals with embedded JS newlines (U+2028 Paragraph separator or U+2029
     Line separator).
> 4. The output contains only valid Unicode [scalar values][unicode-scalars]
     (no isolated [UTF-16 surrogates][unicode-surrogates]) that are
     [allowed in XML][xml-charsets] unescaped.

### HTML

By default, the `html-sanitize` function strips all HTML from a string.

```clj
(html-sanitize "Hello, <script>alert(\"0wn3d\");</script>world!")
; "Hello, world!"
```

#### Policies

You can create policies using `html-policy` to whitelist certain HTML
elements and attributes with fine-grained control.

```clj
(def policy (html-policy :allow-elements ["a"]
                         :allow-attributes ["href" :on-elements ["a"]]
                         :allow-standard-url-protocols
                         :require-rel-nofollow-on-links))

(html-sanitize policy "<a href=\"http://github.com/\">GitHub</a>")
; "<a href=\"http://github.com\" rel=\"nofollow\">GitHub</a>"
```

Here are the available options (adapted from [here][html-policy-builder]):

  * <strong>`:allow-attributes [& attr-names attr-options]`</strong> <br/>
    Allow specific attributes. The following options are available:
    * `:globally` <br/>
      Allow the specified attributes to appear on all elements.
    * `:matching [pattern]` <br/>
      Allow only values that match the provided regular expression
      (java.util.regex.Pattern).
    * `:matching [f]` <br/>
      Allow the named attributes for which `(f element-name attr-name value)`
      returns a non-nil, possibly adjusted `value`. [Here][html-test-L197]
      is an example.
    * `:on-elements [& element-names]` <br/>
      Allow the named attributes only on the named elements.
  * <strong>`:allow-common-block-elements`</strong> <br/>
    Allows `p`, `div`, `h[1-6]`, `ul`, `ol`, `li`, and `blockquote`.
  * <strong>`:allow-common-inline-formatting-elements`</strong> <br/>
    Allows `b`, `i`, `font`, `s`, `u`, `o`, `sup`, `sub`, `ins`, `del`,
    `strong`, `strike`, `tt`, `code`, `big`, `small`, `br`, and `span`
    elements.
  * <strong>`:allow-elements [f & element-names]`</strong> <br />
    Allow the named elements for which `(f element-name ^java.util.List attrs)`
    returns a non-nil, possibly adjusted `element-name`. [Here][html-test-L138]
    is an example.
  * <strong>`:allow-elements [& element-names]`</strong> <br/>
    Allow the named elements.
  * <strong>`:allow-standard-url-protocols`</strong> <br/>
    Allows `http`, `https`, and `mailto` to appear in URL attributes.
  * <strong>`:allow-styling`</strong> <br/>
    Convert `style` attributes to simple `font` tags to allow color, size,
    typeface, and other styling.
  * <strong>`:allow-text-in [& element-names]`</strong> <br/>
    Allow text in the named elements.
  * <strong>`:allow-url-protocols [& url-protocols]`</strong> <br/>
    Allow the given URL protocols.
  * <strong>`:allow-without-attributes [& element-names]`</strong> <br/>
    Allow the named elements to appear without any attributes.
  * <strong>`:disallow-attributes [& attr-names attr-options]`</strong> <br/>
    Disallow the named attributes. See `:allow-attributes` for available
    options.
  * <strong>`:disallow-elements [& element-names]`</strong> <br/>
    Disallow the named elements.
  * <strong>`:disallow-text-in [& element-names]`</strong> <br/>
    Disallow text to appear in the named elements.
  * <strong>`:disallow-url-protocols [& url-protocols]`</strong> <br/>
    Disallow the given URL protocols.
  * <strong>`:disallow-without-attributes [& element-names]`</strong> <br/>
    Disallow the named elements to appear without any attributes.
  * <strong>`:require-rel-nofollow-on-links`</strong> <br/>
    Require `rel="nofollow"` in links (adding it if not present).

#### Predefined policies

Several policies come predefined for convenience. You can access them
using the `html-policy` or `html-merge-policies` functions (see
below).

```clj
(def policy (html-policy :BLOCKS))
```

  * <strong>`:BLOCKS`</strong> <br/>
    Allows common block elements, as in `:allow-common-block-elements`.
  * <strong>`:FORMATTING`</strong> <br/>
    Allows common inline formatting elements as in
    `:allow-common-inline-formatting-elements`.
  * <strong>`:IMAGES`</strong> <br/>
    Allows `img` tags with `alt`, `src`, `border`, `height`, and `width`
    attributes, with appropriate restrictions.
  * <strong>`:LINKS`</strong> <br/>
    Allows `a` tags with standard URL protocols and `rel="nofollow"`.
  * <strong>`:STYLES`</strong> <br/>
    Allows simple styling as in `:allow-styling`.

#### Merging policies

You can merge policies using `html-merge-policies`. Provide it with a
sequence of option sequences or `PolicyFactory` objects (such as those
returned by `html-policy`).

```clj
(def policy (html-merge-policies :BLOCKS :FORMATTING :LINKS))
```

### Markdown

There's already a PegDown wrapper for Clojure (called
[cegdown][cegdown]).  But this one's got a few more features and I'm
including it for the sake of completeness.

By default the `markdown-to-html` function simply adheres to the
original Markdown specification.

```clj
(markdown-to-html "# Hello, \"<em>world</em>\"")
; "<h1>Hello, \"<em>world</em>\"</h1>"
```

#### Processors

The `markdown-processor` function returns a processor factory with the
specified behavior. Suppose, for example, you wanted to suppress all
user-supplied HTML:

```clj
(def processor (markdown-processor :quotes
                                   :suppress-all-html))

(markdown-to-html processor "# Hello, \"<em>world</em>\"")
; "<h1>Hello, &ldquo;world&rdquo;</h1>"
```

It's also thread-safe.

Here are the available options (adapted from [here][markdown-extensions]):

  * <strong>`:abbreviations`</strong> <br/>
    Enable [abbreviations][markdown-abbr].
  * <strong>`:all`</strong> <br/>
    Enable all extensions, excluding the `:suppress-*` ones.
  * <strong>`:auto-links`</strong> <br/>
    Enable automatic linking of URLs.
  * <strong>`:definitions`</strong> <br/>
    Enable [definition lists][markdown-def-lists].
  * <strong>`:fenced-code-blocks`</strong> <br/>
    Enable fenced code blocks via different syntaxes, [one][markdown-code-1]
    and [two][markdown-code-2].
  * <strong>`:hardwraps`</strong> <br/>
    Enable interpretation of single newlines as [hardwraps][markdown-hardwraps].
  * <strong>`:none`</strong> <br/>
    Don't enable any extensions (default).
  * <strong>`:quotes`</strong> <br/>
    Turn single and double quotes and angle quotes into fancy entities.
  * <strong>`:smarts`</strong> <br/>
    Turn ellipses, dashes, and apostrophes into fancy entities.
  * <strong>`:smartypants`</strong> <br/>
    Enable `:quotes` and `:smarts`.
  * <strong>`:strikethrough`</strong> <br/>
    Enable ~~strikethrough~~.
  * <strong>`:suppress-all-html`</strong> <br/>
    Enable both `:suppress-html-blocks` and `:suppress-inline-html`.
  * <strong>`:suppress-html-blocks`</strong> <br/>
    Suppress user-supplied block HTML tags.
  * <strong>`:suppress-inline-html`</strong> <br/>
    Suppress user-supplied inline HTML tags.
  * <strong>`:tables`</strong> <br/>
    Enable [tables][markdown-tables].
  * <strong>`:wiki-links`</strong> <br/>
    Enable `[[wiki-style links]]` (see below for more information).

#### Link renderers

You can customize how automatic, explicit (or inline), mail,
reference, and wiki links are rendered by supplying your own
LinkRenderer. The `markdown-link-renderer` function provides a nicer
way to proxy it.

```clj
(def link-renderer (markdown-link-renderer
                     {:auto-link (fn [node]
                                   {:text (->> (.getText node)
                                               (re-find #"://(\w+).")
                                               second
                                               capitalize)
                                    :href (.getText node)
                                    :attributes ["class" "auto-link"]})})
  
(def processor (markdown-processor :auto-links))

(markdown-to-html processor link-renderer "http://google.com")
; "<a href=\"http://google.com\" class=\"auto-link\">Google</a>"
```

The available overrides are (adapted from [here][markdown-link-renderer]):

  * <strong>`:auto-link [^AutoLinkNode node]`</strong> <br/>
  * <strong>`:exp-link [^ExpLinkNode node ^String text]`</strong> <br/>
  * <strong>`:exp-image [^ExpImageNode node ^String text]`</strong> <br/>
  * <strong>`:mail-link [^MailLinkNode node]`</strong> <br/>
  * <strong>`:ref-link [^RefLinkNode node ^String url ^String title ^String text]`</strong> <br/>
  * <strong>`:ref-image [^RefImageNode node ^String url ^String title ^String text]`</strong> <br/>
  * <strong>`:wiki-link [^WikiLinkNode node]`</strong> <br/>

They should return a map containing the link's `:text`, `:href`, and any other
`:attributes` (as a flat sequence of strings) as in the example above.

## Other

  * [API](http://alxlit.github.io/autoclave/codox)

## License

Copyright © 2013-2016 Alex Little

Distributed under the Eclipse Public License, the same as Clojure.

[cegdown]: https://github.com/Raynes/cegdown
[json-sanitizer-source]: https://github.com/alxlit/autoclave/tree/master/src/java/com/google/json
[html-policy-builder]: https://owasp-java-html-sanitizer.googlecode.com/svn/trunk/distrib/javadoc/org/owasp/html/HtmlPolicyBuilder.html
[html-test-L138]: https://github.com/alxlit/autoclave/blob/master/test/autoclave/html_test.clj#L138
[html-test-L197]: https://github.com/alxlit/autoclave/blob/master/test/autoclave/html_test.clj#L197
[markdown-abbr]: http://michelf.ca/projects/php-markdown/extra/#abbr
[markdown-code-1]: http://michelf.ca/projects/php-markdown/extra/#fenced-code-blocks
[markdown-code-2]: https://github.com/adam-p/markdown-here/wiki/Markdown-Cheatsheet#code-and-syntax-highlighting
[markdown-def-lists]: http://michelf.ca/projects/php-markdown/extra/#def-list
[markdown-extensions]: http://www.decodified.com/pegdown/api/org/pegdown/Extensions.html
[markdown-hardwraps]: https://github.com/adam-p/markdown-here/wiki/Markdown-Cheatsheet#line-breaks
[markdown-tables]: http://fletcher.github.io/peg-multimarkdown/#tables
[markdown-link-renderer]: http://www.decodified.com/pegdown/api/org/pegdown/LinkRenderer.html
[owasp]: https://www.owasp.org/
[owasp-json]: https://www.owasp.org/index.php/OWASP_JSON_Sanitizer
[owasp-json-gh]: https://github.com/owasp/json-sanitizer#output
[owasp-html]: https://www.owasp.org/index.php/OWASP_Java_HTML_Sanitizer
[pegdown]: https://github.com/sirthias/pegdown
[rfc-4627]: http://www.ietf.org/rfc/rfc4627.txt
[unicode-scalars]: http://www.unicode.org/glossary/#unicode_scalar_value
[unicode-surrogates]: http://www.unicode.org/glossary/#surrogate_pair
[xml-charsets]: http://www.w3.org/TR/xml/#charsets



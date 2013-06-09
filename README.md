# autoclave-clj

A library for sanitizing various kinds of user input. The idea is to provide a
simple, convenient API that is built upon existing, proven libraries.

  * JSON, using [OWASP JSON Sanitizer][owasp-json] (included locally, as it
    isn't available from any repositories afaik)
  * HTML, using [OWASP HTML Sanitizer][owasp-html]

Feel free to contribute.

## Installation

```clj
:dependencies [[autoclave "0.1.0"]]
```

## Usage

### JSON

The `json-sanitize` function takes JSON-like content and produces well-formed
JSON, and even deals with XSS vulnerabilities. Read [this][owasp-json] for more
information.

```clj
(require '[autoclave.core :refer :all])

(json-sanitize "{some: 'derpy json' n: +123}")
; "{\"some\": \"derpy json\" ,\"\":\"n\" ,\"\":123}"
```

### HTML

By default the `html-sanitize` function strips all HTML from a string, encodes
entities, and deals with XSS vulnerabilities.

```clj
(require '[autoclave.core :refer :all])

(html-sanitize "Hello, <script>alert(\"0wn3d\");</script> world!")
; "Hello, world!"
```

You can also create policies using `html-policy` to allow certain HTML elements
and attributes with fine-grained control.

```clj
(def policy (html-policy :allow-elements ["a"]
                         :allow-attributes ["href" :on-elements ["a"]]
                         :allow-standard-url-protocols
                         :require-rel-nofollow-on-links))

(html-sanitize policy "<a href=\"http://github.com/\">GitHub</a>")
; "<a href=\"http://github.com\" rel=\"nofollow\">GitHub</a>"
```

See the [documentation][docs] for more details.

## License

Copyright © 2013 alxlit

Distributed under the Eclipse Public License, the same as Clojure.

[docs]: TODO
[owasp-json]: https://www.owasp.org/index.php/OWASP_JSON_Sanitizer
[owasp-json-gc]: https://code.google.com/p/json-sanitizer/
[owasp-html]: https://www.owasp.org/index.php/OWASP_Java_HTML_Sanitizer

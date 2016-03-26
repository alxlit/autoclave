(ns autoclave.json-test
  "Adapted from the OWASP JSON Sanitizer test suite."
  (:require [autoclave.core :refer :all]
            [clojure.test :refer [deftest is]]))

(defmacro assert-sanitized [output & [input]]
  `(is (= (json-sanitize (or ~input ~output)) ~output)))

(deftest test-sanitize
  ;; On the left is the sanitized output, and on the right the input.
  ;; If there is a single string, then the input is fine as-is.
  (assert-sanitized "null", nil)
  (assert-sanitized "null" "")
  (assert-sanitized "null")
  (assert-sanitized "false")
  (assert-sanitized "true")
  (assert-sanitized " false ")
  (assert-sanitized "  false")
  (assert-sanitized "false\n")
  (assert-sanitized "false" "false,true")
  (assert-sanitized "\"foo\"")
  (assert-sanitized "\"foo\"" "'foo'")
  (assert-sanitized 
   "\"<script>foo()<\\/script>\"" "\"<script>foo()</script>\"")
  (assert-sanitized 
   "\"<script>foo()<\\/script>\"" "\"<script>foo()</script>\"")
  (assert-sanitized "\"<\\/SCRIPT\\n>\"" "\"</SCRIPT\n>\"")
  (assert-sanitized "\"<\\/ScRIpT\"" "\"</ScRIpT\"")
  
  ;; \u0130 is a Turkish dotted upper-case 'I' so the lower case version of
  ;; the tag name is "script".
  (assert-sanitized "\"<\\/ScR\u0130pT\"" "\"</ScR\u0130pT\"")
  (assert-sanitized "\"<b>Hello</b>\"")
  (assert-sanitized "\"<s>Hello</s>\"")
  (assert-sanitized "\"<[[\\u005d]>\"" "'<[[]]>'")
  (assert-sanitized "\"\\u005d]>\"" "']]>'")
  (assert-sanitized "[[0]]" "[[0]]>")
  (assert-sanitized "[1,-1,0.0,-0.5,1e2]" "[1,-1,0.0,-0.5,1e2,")
  (assert-sanitized "[1,2,3]" "[1,2,3,]")
  (assert-sanitized "[1,null,3]" "[1,,3,]")
  (assert-sanitized "[1 ,2 ,3]" "[1 2 3]")
  (assert-sanitized "{ \"foo\": \"bar\" }")
  (assert-sanitized "{ \"foo\": \"bar\" }" "{ \"foo\": \"bar\", }")
  (assert-sanitized "{\"foo\":\"bar\"}" "{\"foo\",\"bar\"}")
  (assert-sanitized "{ \"foo\": \"bar\" }" "{ foo: \"bar\" }")
  (assert-sanitized "{ \"foo\": \"bar\"}" "{ foo: 'bar")
  (assert-sanitized "{ \"foo\": [\"bar\"]}" "{ foo: ['bar")
  (assert-sanitized "false" "// comment\nfalse")
  (assert-sanitized "false" "false// comment")
  (assert-sanitized "false" "false// comment\n")
  (assert-sanitized "false" "false/* comment */")
  (assert-sanitized "false" "false/* comment *")
  (assert-sanitized "false" "false/* comment ")
  (assert-sanitized "false" "/*/true**/false")
  (assert-sanitized "1")
  (assert-sanitized "-1")
  (assert-sanitized "1.0")
  (assert-sanitized "-1.0")
  (assert-sanitized "1.05")
  (assert-sanitized "427.0953333")
  (assert-sanitized "6.0221412927e+23")
  (assert-sanitized "6.0221412927e23")
  (assert-sanitized "6.0221412927e0" "6.0221412927e")
  (assert-sanitized "6.0221412927e-0" "6.0221412927e-")
  (assert-sanitized "6.0221412927e+0" "6.0221412927e+")
  (assert-sanitized "1.660538920287695E-24")
  (assert-sanitized "-6.02e-23")
  (assert-sanitized "1.0" "1.")
  (assert-sanitized "0.5" ".5")
  (assert-sanitized "-0.5" "-.5")
  (assert-sanitized "0.5" "+.5")
  (assert-sanitized "0.5e2" "+.5e2")
  (assert-sanitized "1.5e+2" "+1.5e+2")
  (assert-sanitized "0.5e-2" "+.5e-2")
  (assert-sanitized "{\"0\":0}" "{0:0}")
  (assert-sanitized "{\"0\":0}" "{-0:0}")
  (assert-sanitized "{\"0\":0}" "{+0:0}")
  (assert-sanitized "{\"1\":0}" "{1.0:0}")
  (assert-sanitized "{\"1\":0}" "{1.:0}")
  (assert-sanitized "{\"0.5\":0}" "{.5:0}")
  (assert-sanitized "{\"-0.5\":0}" "{-.5:0}")
  (assert-sanitized "{\"0.5\":0}" "{+.5:0}")
  (assert-sanitized "{\"50\":0}" "{+.5e2:0}")
  (assert-sanitized "{\"150\":0}" "{+1.5e+2:0}")
  (assert-sanitized "{\"0.1\":0}" "{+.1:0}")
  (assert-sanitized "{\"0.01\":0}" "{+.01:0}")
  (assert-sanitized "{\"0.005\":0}" "{+.5e-2:0}")
  (assert-sanitized "{\"1e+101\":0}" "{10e100:0}")
  (assert-sanitized "{\"1e-99\":0}" "{10e-100:0}")
  (assert-sanitized "{\"1.05e-99\":0}" "{10.5e-100:0}")
  (assert-sanitized "{\"1.05e-99\":0}" "{10.500e-100:0}")
  (assert-sanitized "{\"1.234e+101\":0}" "{12.34e100:0}")
  (assert-sanitized "{\"1.234e-102\":0}" "{.01234e-100:0}")
  (assert-sanitized "{\"1.234e-102\":0}" "{.01234e-100:0}")
  (assert-sanitized "{}")
  
  ;; Remove grouping parentheses.
  (assert-sanitized "{}" "({})")
  
  ;; Escape code-points and isolated surrogates which are not XML embeddable.
  (assert-sanitized "\"\\u0000\\u0008\\u001f\"" "'\u0000\u0008\u001f'")
  (assert-sanitized "\"\ud800\udc00\\udc00\\ud800\"",
                    "'\ud800\udc00\udc00\ud800'")
  (assert-sanitized "\"\ufffd\\ufffe\\uffff\"" "'\ufffd\ufffe\uffff'")
  
  ;; These control characters should be elided if they appear outside a string
  ;; literal.
  (assert-sanitized "42" "\uffef\u000042\u0008\ud800\uffff\udc00")
  (assert-sanitized "null" "\uffef\u0000\u0008\ud800\uffff\udc00")
  (assert-sanitized "[null]" "[,]")
  (assert-sanitized "[null]" "[null,]")
  (assert-sanitized "{\"a\":0,\"false\":\"x\",\"\":{\"\":-1}}",
                    "{\"a\":0,false\"x\":{\"\":-1}}")
  (assert-sanitized "[true ,false]" "[true false]")
  (assert-sanitized "[\"\\u00a0\\u1234\"]")
  (assert-sanitized "{\"a\\b\":\"c\"}" "{a\\b\"c")
  (assert-sanitized "{\"a\":\"b\",\"c\":null}" "{\"a\":\"b\",\"c\":")
  (assert-sanitized 
   "{\"1e0001234567890123456789123456789123456789\":0}",
   ;; Exponent way out of representable range in a JS double.
   "{1e0001234567890123456789123456789123456789:0}"
   )
  ;; This is an odd consequence of the way we recode octal literals.
  ;; Our octal recoder does not fail on digits '8' or '9'.
  (assert-sanitized "-2035208041" "-016923547559"))

(deftest test-issue-3
  ;; These triggered index out of bounds and assertion errors.
  (assert-sanitized "[{\"\":{}}]" "[{{},\u00E4")
  (assert-sanitized "[{\"\":{}}]" "[{{\u00E4\u00E4},\u00E4"))

(deftest test-issue-4
  ;;Make sure that bare words are quoted.
  (assert-sanitized "\"dev\"" "dev")
  (assert-sanitized "\"eval\"" "eval")
  (assert-sanitized "\"comment\"" "comment")
  (assert-sanitized "\"fasle\"" "fasle")
  (assert-sanitized "\"FALSE\"" "FALSE")
  (assert-sanitized "\"dev/comment\"" "dev/comment")
  (assert-sanitized "\"devcomment\"" "dev\\comment")
  (assert-sanitized "\"dev\\ncomment\"" "dev\\ncomment")
  (assert-sanitized "[\"dev\", \"comment\"]" "[dev\\, comment]"))



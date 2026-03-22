# ZIL Source — Original Zork Trilogy

This directory contains the original ZIL (Zork Implementation Language) source code for
all three Zork games, as released by Microsoft on GitHub under the `historicalsource`
organisation.

## Sources

| Game   | Repository                                      | License |
|--------|-------------------------------------------------|---------|
| Zork I | https://github.com/historicalsource/zork1       | MIT     |
| Zork 2 | https://github.com/historicalsource/zork2       | MIT     |
| Zork 3 | https://github.com/historicalsource/zork3       | MIT     |

All three repos fall under the `historicalsource` umbrella:
https://github.com/historicalsource

## About the Source Code

The code was contributed anonymously and represents a snapshot of the Infocom development
system at the time of the company's shutdown. It was written in ZIL, a refactoring of MDL
(Muddle), itself a LISP dialect created by MIT students and staff.

There is no known official Infocom compiler (ZILCH, which ran on a TOPS20 mainframe) still
available. A community-maintained compiler called [ZILF](http://zilf.io) can compile these
files with minor issues.

## Why It's Here

These files are the reference source for our Clojure translation project. They live in
`src/zork.zil/` as read-only reference material — all active development happens in
`src/zork.clojure/`.

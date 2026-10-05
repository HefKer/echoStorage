# An automated check of glossary casing

No test or lint checks that docs and comments write glossary terms with the casing `CONTEXT.md` gives them. Review is how a lower-case "quick-stack" or "a strict chest" gets caught.

## Why this is out of scope

Most glossary terms are also ordinary English words. "strict", "link", "category", "stray" and "vacuum" all turn up in prose in their plain sense, and a check cannot tell "a Strict chest" from "a strict rule about nesting". So a check has two shapes, and neither is worth having:

- Cover every term, with an allow-list of the plain uses. The allow-list grows with every new ADR and becomes the thing that needs maintaining.
- Cover only the terms with no plain use ("Quick-stack", "Echo Chest"). Those are the ones nobody gets wrong.

It would also have to skip code spans, code blocks, `_Avoid_` lists and quoted issue titles, which means parsing Markdown, Javadoc and line comments.

The cost of a miss is a lower-case word in a document. The casing was brought into line by hand twice (#52, #53), each as one sweep, and a sweep stays cheap.

## Prior requests

- #61: "Nothing checks glossary casing, so it can drift again"
- #103: "Review judgement calls from #97: config comment wording and PlayerTextTest" (point 3: widen `PlayerTextTest` from Link to every glossary term that is not shown as a name)

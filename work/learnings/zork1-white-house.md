# Learnings: Zork I — White House and Interior

## Trap Door: v-open must NOT set TOUCHBIT on doorbit objects
*From #1 — House interior rooms · 2026-04-12*

Verified in DOSBox r88/840726: `move rug`, `open trap door`, `down` causes the trap door to crash shut on cellar entry ("The trap door crashes shut, and you hear someone barring it."). This means ZIL's V-OPEN does NOT set TOUCHBIT on the trap-door — TOUCHBIT is the guard in CELLAR-FCN M-ENTER that decides whether to fire the auto-close sequence. When TRAP-DOOR-FCN is implemented as a Clojure object action handler, it must not set `:touchbit` during open. The `:touchbit` on the trap-door is exclusively managed by CELLAR-FCN M-ENTER.

## TRAP-DOOR-EXIT is a PER Routine, Not an IF-OPEN Gate
*From #1 — House interior rooms · 2026-04-12*

In ZIL, the living-room `:down` exit is `(DOWN PER TRAP-DOOR-EXIT)` — a dedicated routine at `1actions.zil:567-577`, not a simple `(IF obj IS OPEN)` conditional. `TRAP-DOOR-EXIT` checks both `RUG-MOVED` and `TRAP-DOOR OPENBIT` itself. The Clojure `:if-open` exit map only checks OPENBIT; the RUG-MOVED guard is a known deferral pending rug puzzle implementation. When implementing the rug puzzle, use a pre-walk hook or `:m-beg` dispatch in `living-room-fcn`, not an `:if` flag on the exit map — the ZIL uses a full routine for this.

## Dead-State Atoms Need TODO Comments
*From #1 — House interior rooms · 2026-04-12*

The `rug-moved` atom was added as part of the house interior PR, but the rug puzzle (MOVE RUG verb handler) isn't implemented yet. The atom is read but never written in production — only tests set it directly. Any time a game-state atom is introduced without the corresponding write path, it must have a `;;; TODO: not written until {feature} is implemented` comment at the definition site. Without this comment, the atom appears to be working code when it is actually dead state.

## CELLAR-FCN M-ENTER Does NOT Clear :invisible on Trap-Door
*From #1 — House interior rooms · 2026-04-12*

ZIL CELLAR-FCN M-ENTER only touches two flags: `FCLEAR TRAP-DOOR OPENBIT` and `FSET TRAP-DOOR TOUCHBIT` (1actions.zil:540-541). It does NOT clear `:invisible`. The `:invisible` flag is cleared by the rug puzzle handler `RUG-FCN MOVE` (1actions.zil:597) when the player moves the rug. In normal gameplay the rug is always moved before the player can descend, so `:invisible` is already cleared by the time M-ENTER fires. Tests that simulate a player going from living-room to cellar must first clear `:invisible` to represent the rug having been moved.

## MAZE-11-FCN M-ENTER Clears :invisible on the Grate
*From #2 — Underground dungeon rooms · 2026-04-12*

ZIL MAZE-11-FCN M-ENTER (1actions.zil:833-835) calls `FCLEAR GRATE INVISIBLE` when the player enters the grating-room. Tests that bypass `arrive!` (direct `reset! here`) must manually clear `:invisible` on `:grate` to simulate this — same pattern as other arrive!-dependent state in `house_interior_test.clj`. The `:grate` object is defined with `#{:doorbit :ndescbit :invisible}` flags; its `:location :local-globals` key is new in the EDN objects section and will need to be handled by room-global lookup logic when the grate puzzle is implemented.

## ZIL GOTO Score-Obj Order: Between M-ENTER and Room Description
*From #1 — House interior rooms · 2026-04-12*

In ZIL's GOTO routine (gverbs.zil:2121-2136), the ordering is: M-ENTER action → SCORE-OBJ → V-FIRST-LOOK (room description). The endgame whisper from SCORE-OBJ therefore prints BEFORE the room description but AFTER M-ENTER side effects. Our Clojure `arrive!` combines M-ENTER and room description in one call, so `score-obj` currently fires after both. To match ZIL exactly, `arrive!` would need to be split into separate M-ENTER and description phases with `score-obj` in between. Noted as a known deviation — addressing it requires restructuring `arrive!`.

## describe-objects: :fdesc Priority and :ndescbit Suppression
*From #3 — World objects · 2026-04-13*

ZIL's DESCRIBE-OBJECTS routine shows an object's FDESC (the "first description") when the object is in its initial room position. Our `describe-objects` was missing this — it jumped straight to "There is a X here." for objects with `:fdesc` but no `:ldesc`. Fixed in #3 by checking `:fdesc` before the generic fallback. Additionally, `:ndescbit` must always suppress description — an object with both `:ndescbit` and `:ldesc` should never auto-describe when looking (LDESC is for examine, not room look). Any future changes to `describe-objects` must preserve: ndescbit suppresses always → ldesc → fdesc → generic.

## Container Contents Display is a Known Deviation (tracked as #46)
*From #3 — World objects · 2026-04-13*

The original Zork I shows items inside/on open containers when looking at a room (e.g., the bottle and sack on the kitchen table, the egg through the open nest in Up a Tree). Our `describe-objects` only calls `objects-in @here` — it does not recurse into open containers. This is a tracked deviation: see issue #46. Integration tests for rooms with container-held items should use `includes?` checks rather than exact string matching to avoid false failures when this is eventually implemented.

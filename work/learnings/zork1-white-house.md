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

## describe-objects: FDESC/LDESC Priority and NDESCBIT Semantics
*From #3 — World objects · 2026-04-13*

ZIL's DESCRIBE-OBJECT priority (gverbs.zil:1693-1707):
1. `:invisible` → suppress everything (object not visible)
2. `:fdesc` → show if object is untouched (no `:touchbit`) AND not `:ndescbit`
3. `:ldesc` → show if present (NDESCBIT does NOT suppress LDESC)
4. generic "There is a X here." → show if not `:ndescbit`

Key implication: NDESCBIT suppresses FDESC and the generic fallback, but NOT LDESC. An object with `:ndescbit` + `:ldesc` WILL auto-describe via its LDESC. TOUCHBIT is set when the player handles an object; once set it causes FDESC to be skipped permanently (LDESC takes over). Any future changes to `describe-objects` must preserve this exact priority.

## Darkness System: lit?, update-lit!, and load-world! Ordering
*From #8 — Darkness and light source mechanic · 2026-04-14*

`lit?` (gparser.zil:1333): room has `:onbit` OR winner carries object with `:lightbit`+`:onbit`. The `@lit` atom caches the current lighting state and must be explicitly updated via `update-lit!` after resetting `@here` directly (e.g., in tests). `load-world!` now resets `@here`/`@winner` to starting values before calling `update-lit!`, making it a complete game-state reset. Tests that set `@here` directly (bypassing `arrive!`) must call `z/update-lit!` before testing light-dependent behavior — otherwise `@lit` will reflect the previous room's lighting.

## JIGS-UP: Score Penalty and Deferred Elements
*From #8 — Darkness and light source mechanic · 2026-04-14*

ZIL JIGS-UP (1actions.zil:4046): prints death message, deducts 10 score, prints death block, then handles resurrection (DEATHS counter, LUCKY flag, "Bad luck, huh?"). Our `jigs-up` implements the message + score deduction + death block. Resurrection, LUCKY flag, and "Bad luck, huh?" are deferred. The death block format is `\n    ****  You have died  ****\n` (from ZIL `|    ****  You have died  ****\n|\n|`). Returns `:quit` to signal game end.

## Known Deviation: "You have moved into a dark place." Ordering
*From #8 — Darkness and light source mechanic · 2026-04-14*

ZIL GOTO prints "You have moved into a dark place." BEFORE V-FIRST-LOOK (before room description). Our implementation prints it AFTER `arrive!` (which includes V-FIRST-LOOK). This means on a lit→dark move, you see "It is pitch black. You are likely to be eaten by a grue." before "You have moved into a dark place." — the opposite of ZIL order. Fixing this requires splitting `arrive!` into separate M-ENTER and display phases. Accepted as a known deviation.

## Object Action Handlers: Write Them as object-action Methods
*From #14 — light / extinguish · 2026-04-14 · updated #48 · 2026-09-25*

ZIL objects with an `ACTION` property have that routine called before the generic verb handler fires (RTRUE skips the verb handler; RFALSE lets it run). Since #48, object ACTION logic goes in an `object-action` defmethod keyed on the object's `:action` value in `1dungeon.edn`. Verb handlers call `(dispatch-action obj-key verb)` first and stop on a truthy result. Do not fold ACTION logic into verb handlers, and do not leave `:action` values as documentation-only dead weight.

*Superseded "fold the target object's ACTION logic into the verb handler until #48 is implemented" on 2026-09-25 — #48 (PR #51) implemented dispatch.*

## Failed Actions DO Consume a Turn in the Original (verified)
*From #14 — light / extinguish · 2026-04-14 · verified during #48 audit · 2026-09-25*

Verified against the original r88/840726 story file (dfrotz, `score` move counts): once a command parses and reaches its verb/ACTION handler, it costs a turn **whether or not the action succeeds**. This holds for all six failed V-LAMP-ON/OFF outcomes ("It is already on.", "It is already off.", "You can't turn that on.", "You can't turn that off.", "A burned-out lamp won't light.", "The lamp has already burned out."). It also holds for "It is securely anchored." (take mailbox), "It is already closed.", a second `open mailbox`, "You already have that.", and "You can't see any foobar here!". Failures inside the parser do NOT cost a turn: `drop leaflet` when not holding it, and `turn off rug` ("You don't have the carpet."), both stay at 0 moves. The rule lives in ZIL MAIN-LOOP-1 (gmain.zil:169-172): CLOCKER runs whenever P-WON, except for meta-verbs. Fixed in 96d8d67: `perform` now runs `clocker!` for every parsed command unless the handler returns `:parse-fail` (orphan or HAVE check) or `:clock-wait` (V-WAIT runs the clock 3× itself), or the verb is a meta-verb. Handler `:turn` returns no longer drive the clock. New handlers only need to return `:parse-fail` for failures the ZIL parser would have caught.

*Superseded "Current assumption: [failed actions] do not [consume a turn]" on 2026-09-25.*

## Object ACTION Dispatch Mirrors room-action: Keyed on :action Value, Truthy = RTRUE
*From #48 — Object action handler dispatch · 2026-06-27*

`dispatch-action obj-key verb` looks up `:action` on the object and calls the `object-action` multimethod, which dispatches on the `:action` **value** (`:lantern`, `:mailbox-f`) — exactly the `room-action` pattern (which dispatches on the room's `:action` value). A truthy handler return pre-empts the verb handler (ZIL RTRUE); `nil` falls through (RFALSE). Handlers receive the parse state as a map, `[action {:prsa :prso :prsi}]`, named after ZIL's PRS globals. This matters because ZIL PERFORM applies **PRSI's ACTION before PRSO's** (gmain.zil:214, 222), so a handler can run when its object is not the direct object. MAILBOX-F's `<EQUAL? ,PRSO ,MAILBOX>` guard exists for TAKE LEAFLET FROM MAILBOX. Passing only the dispatched obj-key can't express that guard. Handlers for objects ZIL names directly (LANTERN uses `,LAMP`) keep referencing them by key. Blocking branches use `(handled "msg")`, which prints and returns truthy in one call, so a print-then-nil mistake can't double-output. A unit test checks every handler key against the EDN `:action` values to catch typos that would silently hit `:default`. Most `:action` values still have no handler → `:default` returns nil → verb runs. Decision (2026-09-25): keep this argument-passing style rather than PRS atoms.

*Superseded "the multimethod signature is `[action verb]` (no obj-key passed)" on 2026-09-25 — changed in fbe4f32 (#48 review S-2).*

## #48 Boundary: lamp-power Reset Stays in v-lamp-on, Not the :lantern Handler
*From #48 — Object action handler dispatch · 2026-06-27*

When folding LANTERN into the dispatch system, only the burned-out (RMUNGBIT) checks moved to the `:lantern` handler — those return RTRUE to pre-empt the verb. The `lamp-power` reset to 185 stayed in `v-lamp-on`'s success branch, even though ZIL's `<ENABLE <INT I-LANTERN>>` (the fuel-countdown enable) technically lives in LANTERN. Reason: that LANTERN branch returns RFALSE (`<>`), so `v-lamp-on` runs regardless — keeping the reset there is behaviorally identical and avoids expanding scope past the issue's AC. If the lamp's ACTION ever needs to short-circuit on turn-on, revisit this.

*Update 2026-09-25:* the reset itself is a bug, wherever it lives. Verified in the original: fuel is initialized once (`GO`: `QUEUE I-LANTERN 200`) and carries over across off/on. Lit-turn thresholds are 200 / 300 / 370 / 385 (burn-out: "You'd better have more light than from the brass lantern."). The turn-on turn uses fuel; the turn-off turn doesn't. Tracked as #52.

## Container Contents Display is a Known Deviation (tracked as #46)
*From #3 — World objects · 2026-04-13*

The original Zork I shows items inside/on open containers when looking at a room (e.g., the bottle and sack on the kitchen table, the egg through the open nest in Up a Tree). Our `describe-objects` only calls `objects-in @here` — it does not recurse into open containers. This is a tracked deviation: see issue #46. Integration tests for rooms with container-held items should use `includes?` checks rather than exact string matching to avoid false failures when this is eventually implemented.

## Integration Tests Can Be Wrong: Re-verify Against the Original with dfrotz
*From #48 audit · 2026-09-25*

`turn_counter_test.clj` was believed verified, but 14 of 24 scenarios disagreed with the original r88/840726 when replayed in dfrotz, and 2 used inputs (`xyzzy`, `foobar`) that are real Zork vocabulary, so they didn't test what their names claimed. Before trusting or writing a turn/output assertion, replay it with dfrotz (see CLAUDE.md). When picking a "nonsense" word for an unknown-word test, confirm the original answers "I don't know the word".

## Open Questions

- **Known-but-absent objects.** The original distinguishes an unknown word ("I don't know the word "qwxzt"." — 0 moves) from a known word for an object that isn't here ("You can't see any foobar here!" — 1 move, via NOT-HERE-OBJECT in PERFORM). Our parser has no vocabulary, so `:not-found` covers both, with "I don't see that here." and 0 moves. Fixing this needs a parser vocabulary.
- **Message-text mismatches found in passing:** V-WAIT prints "Time passes..." in the original (ours: "Time passes."); the DROP HAVE-check says "You don't have that!" (ours: "You're not holding that.").
- **`again` in dfrotz.** `north` then `again` printed "Maximum verbosity." rather than replaying the move, while `wait; again` replayed correctly. Probably a dfrotz input-buffer quirk; `again` text assertions were left unchanged.

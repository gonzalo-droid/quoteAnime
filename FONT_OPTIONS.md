# Home quote text — font options

Context: the Home screen's featured quote (`QuoteDetailContent.kt`, shared with the
Catalog detail view) renders the quote body in `Georgia` (actually **Lora**, italic,
22sp) and the author in `Didot` (actually **Playfair Display**). Both are self-hosted
OFL fonts already bundled in `app/src/main/res/font/`. This doc evaluates alternatives
for the quote body specifically, sourced the same way the existing pair presumably
was: static per-weight instances extracted from Google Fonts' variable-font sources via
the `fonts.googleapis.com/css2` API (all confirmed **SIL Open Font License 1.1**, same
license as Lora/Playfair — safe to bundle).

All four candidates below were downloaded as real `.ttf` files into
`app/src/main/res/font/` and wired into `FontFamilies.kt` following the existing
`FontFamily(Font(...), ...)` pattern, so any of them can be swapped in with a
one-line change.

## Candidates

### 1. Fraunces — **recommended, implemented**
Soft, characterful old-style serif (from the same foundry lineage as Google's
"expressive serif" wave). It has a gentle "wonk" to its curves that reads as warm
and a little playful rather than stiff-formal — closer to a handwritten dedication
than a courtroom document. That fits the app's register well: dark/anime, but the
tagline ("frases que marcaron nuestra vida") is intimate and poetic, not severe.
It's also clearly differentiated from Playfair Display (used for the author line),
avoiding the "two very similar high-contrast serifs stacked on top of each other"
effect that Lora + Playfair currently risk.
- Weights bundled: Regular, Italic, SemiBold, Bold.
- Applied to the quote body's `fontFamily` in `QuoteDetailContent.kt` (replacing Georgia/Lora).

### 2. Cormorant Garamond
A delicate, refined old-style Garamond derivative — quieter and more intimate than
Lora, with elegant italics. Good alternative if the app wants a softer, more
"handwritten letter" feel than Fraunces' slightly more contemporary warmth. Its
lighter weight and higher x-height contrast can read a little thin at small sizes
on a busy photo background, so if picked, keep font-size ≥ 22sp and make sure the
gradient overlay stays dark enough for contrast.
- Weights bundled: Regular, Italic, SemiBold, Bold.

### 3. Bodoni Moda
A modern/didone serif with strong thick-thin contrast — dramatic, editorial,
"manga volume title" energy. The boldest personality swing of the four; best suited
if the app wants the quote to feel like a poster/graphic-novel splash page rather
than a quiet literary quote. Because of the high stroke contrast, hairlines can look
fragile in italic at small sizes — reserve for larger font sizes or non-italic use.
- Weights bundled: Regular, Italic, Bold.

### 4. Abril Fatface
Single-weight ultra-bold display serif — very high impact, works best for a short
hero word or a 1-2 line pull-quote, not longer quote bodies (no italic or secondary
weights exist for this face, by design — it's a display-only face). Good candidate
for a future "quote of the day" hero treatment or a big pull-quote moment elsewhere
in the app, but too heavy/loud for the current 22sp multi-line quote body.
- Weight bundled: Regular only.

## Recommendation

**Fraunces**, applied as the new default for the Home quote body (`QuoteDetailContent.kt`).
It keeps the literary/editorial tone the app already has, but adds warmth and a bit of
personality that fits "anime quotes that marked our lives" better than a fully formal
serif, while staying easily legible at 22sp italic over a photo background. The other
three fonts remain wired into `FontFamilies.kt` (`CormorantGaramond`, `BodoniModa`,
`AbrilFatface`) so they can be tried by swapping the `fontFamily` argument if the
direction doesn't land.

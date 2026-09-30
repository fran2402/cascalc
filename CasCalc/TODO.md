# Still to do

From the list of 28 September, in the order I'd tackle them.

- Tablets: a crash was reported with the complex, trig and log keys. I couldn't find the cause
  from the code (no device here); the app now shows the stack trace after a crash, with Copy.
  Send it to fix this.

## Done
- Round of 30 September: legend and line names, data tables with column roles and error
  bars, SciencePlots export colours, theme line colours, first line on top everywhere, cursor
  scrolling and tap-at-end, product keys take the operand before, limits with fractional
  powers, tablet settings and acknowledgements, crash report.
- Round of 29 September (sixth): tablet layout with the keyboard on a chosen side (calculator
  and all three graph modes), scrolling data tables, taps beside constructions, sideways
  scrolling lines with edge swipes, first line on top, italic z and i in complex exports,
  delete confirmation for lines, custom symbols in lines and sliders, bottom bar order and an
  unsquashed graph keyboard.
- Keypad: one size in all four modes; the extended keypad is three rows tall. Earlier: mod
  takes the whole product after it; √ with an empty index means 2; the power key with nothing
  before it adds a bracketed base box; backspace removes an empty construction; every key has
  a long-press explanation; ≤ and ≥ combine as the second key is typed.
- Matrices and complex: Aᴴ, ⁻¹/ᵀ/ᴴ on whole matrices, products drawn between their boxes,
  eigenvalue keys, ∮ and Res keys, i, Lambert W and Bessel J and Y in the complex group.
- Colors: constants list key, symbol builder and := in the Ans color; your symbols in the
  operator color; "Remove symbol" on a saved symbol's card.
- Graphs: drag to reorder; "+ Add" always visible; lists of points and Fit; saved graphs
  (projects); a colormap picker for the complex plane (matplotlib's maps), with a plain color
  for ∮ loops and curves; export as PDF (default), PNG, JPG or SVG with the limits and light
  or dark chosen and a live preview (2D, complex and 3D).
- Second round: long-press crashes on the complex keys and symbols fixed; pinning; undefine and
  tinted defined letters; no long-press on digits; immediate ≤ ≥; free reordering with a
  handle; swipe to delete lines; Fit in the row; + Add and export on the graph's bottom bar;
  saved line colors; faster inequalities; settings reordered; fuller acknowledgements.
- Settings: the ⋮ menu in all four modes; custom app colors when Material You is off.
- Third round: ∂ key crash; Bessel J_a(z)/Y_a(z) drawing and complex plotting; loop tool
  removed; square pgfplots-style exports; the full matplotlib colormap library with your own
  list, pins, reordering and inverting; CSV import; draggable points, labels, joined points,
  fill opacity and graph settings.

- Fourth round: colormap popup with drag, swipe and one-word names; swipe to delete lines;
  point shapes and sizes; cylindrical and spherical 3D coordinates; pgfplots-style 3D export;
  graph settings in 3D and complex; pin card bug; one-line formulas; Σ/Π with x; lists in
  lines, polygons, tables, notes and folders.

- Fifth round: list ranges ([1...10], [1, 3, ...11]) and comprehensions
  ([n² for n = [1...5]], also for points); folders inside folders; the ∇ crash note closed (everything
  was reported working after its fix).

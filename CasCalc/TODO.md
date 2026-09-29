# Still to do

From the list of 28 September, in the order I'd tackle them.

## Bugs
- The gradient key crashed when drawn. It now has its own drawing (∇ with the order box raised,
  the argument in brackets) and the key label uses it, instead of a lone power after ∇. The
  crash couldn't be reproduced without a phone, so check it there; if it still crashes, the
  line from `adb logcat -b crash -d | grep -A25 "Process: com.example.cas"` will say where.

## Done
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

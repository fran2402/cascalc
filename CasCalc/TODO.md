# Still to do

From the list of 28 September, in the order I'd tackle them.

## Bugs
- The gradient key crashes when drawn (it evaluates correctly: ∇ with an empty order box gives
  the gradient, with 2 gives ∇²). Needs the crash line from
  `adb logcat -b crash -d | grep -A25 "Process: com.example.cas"`.

## Keypad and editing
- One keypad size in all four modes (calculator, 2D, 3D, complex); make the extended keypad shorter.
- Done: mod takes the whole product after it (7 mod 2π); √ with an empty index means 2; the
  power key with nothing before it adds a bracketed base box; backspace removes a construction
  whose main box is empty (fractions only when their bottom is empty too); every key has a
  long-press explanation; ≤ and ≥ combine as the second key is typed.

## Matrices and complex (done)
- Aᴴ is a raised H (conjugate transpose); ⁻¹, ᵀ and ᴴ pressed inside a matrix apply to the
  whole matrix; dot, cross, Hadamard and Kronecker are drawn between their two boxes; the
  eigenvalue keys have no matrix icon; the raised −1, T and H sit higher on the keys.
- The ∮ and Res keys show just ∮ f dz and Res f; i, Lambert W and Bessel J and Y are in the
  complex group.

## Colors
- Done: the constants list key, the symbol builder and := in the Ans color; your symbols in
  the operator color; a "Remove symbol" button on a saved symbol's long-press card.

## Graphs
- Done: drag a row (hold, then drag) to reorder the lines; "+ Add" always visible; hiding the
  keypad shows every line; lists of points with [ ] (the AC key becomes [ ] in 2D) and a Fit
  button under a function with unknowns once there's a list (Levenberg–Marquardt least squares,
  sliders set to the result).
- A colormap picker for the complex plane (matplotlib-style), keeping a plain color for ∮ loops.
- Done: saved graphs (projects) per mode, from the folder button at the top left: save under a
  name, open, rename, delete; each keeps its lines, colors, line styles and sliders.
- Export as PDF (default), PNG, JPG or SVG, with the limits and light or dark chosen.

## Settings
- Custom UI colors when Material You is off.
- Done: the ⋮ menu (settings, acknowledgements) is in all four modes.

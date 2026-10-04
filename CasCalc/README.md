# Calculator (CAS) for Android

A phone calculator that looks and feels like Google Calculator — Material You colors,
Google Sans Flex, pill keys that square up when pressed — with the abilities of a
CAS calculator: a real 2D math editor and exact answers.

## What it can do
It's a computer algebra system: letters stay symbolic, so 2x + 3x gives 5x, and
answers are exact (2√2, π/6, n(n+1)/2) with a "≈" chip for the decimal.

- **Algebra**: automatic simplification (like terms, powers, surds, i² = −1),
  expand, factor (rational roots, x²-substitution, common factors), simplify
  (cancels (x² − 1)/(x − 1) to x + 1).
- **Solve**: linear and quadratic equations with any coefficients (x = −b/a),
  polynomials by rational roots, xⁿ = c with all n exact roots, equations like
  2^(x+1) = 8 or sin x = ½ by undoing each operation, rational equations, and a
  numerical fallback (cos x = x → 0.739…).
- **Calculus**: symbolic d/dx (with or without a point); antiderivatives by table,
  linear substitution, integration by parts, partial fractions and
  derivative-divides substitution; definite integrals exact when possible and
  checked against a numerical result; Taylor polynomials; Σ closed forms for
  polynomial terms (Σk² = n(n+1)(2n+1)/6).
- **Linear algebra** with exact or symbolic entries: det, inverse, transpose, products,
  powers, rref, rank, trace, characteristic polynomial, eigenvalues (exact, including
  surds and complex ones) and eigenvectors (a basis for each eigenvalue, scaled to
  whole numbers), dot and cross products, vector length.
- **Limits** by substitution, cancelling and L'Hôpital's rule (lim sin x / x = 1), with
  a two-sided numerical check as the fallback.
- **Variables**: store values with a := 5; x, y… stay symbolic until given a value.
- Exact trig values at multiples of 30° and 45°, exact inverse trig at the standard
  values, exact logs (log₂ 8 = 3), complex numbers, physical constants.
- **Symbolic first, numerical as a fallback**: one ∫ key and one d/dx key. Leave the
  ∫ limits empty for an antiderivative, fill them for a definite integral (exact if
  possible, otherwise numerical); d/dx is symbolic, or evaluated at a point, falling
  back to a numerical derivative when there's no symbolic rule. Numerical results
  show "≈" instead of "=", and exact numbers have a "≈" chip for their decimal.

## What's new
- **Systems of equations**: type the equations separated by commas, and the unknowns in the
  second box: solve(x + y = 3, x − y = 1; x, y) → x = 2, y = 1. Works for linear systems
  (including infinitely many solutions, written with the free unknown) and nonlinear ones
  by elimination: (x = −2, y = −1) or (x = 1, y = 2).
- **Inequalities**: solve(x² < 4) → −2 < x < 2; x² ≥ 4 → x ≤ −2 or x ≥ 2; rational ones
  and chains like 0 < x − 1 ≤ 2. Endpoints stay exact (−√2 < x < √2).
- **Calculus**: limits at ±∞ (lim (2x + 1)/(x + 3) as x → ∞ = 2), one-sided limits (x → 0+),
  limits that are infinite, integrals with ∞ as a limit (∫₀^∞ e^(−x) dx = 1), higher
  derivatives dⁿ/dxⁿ, and **differential equations** with dsolve: first-order linear and
  separable, second-order linear with constant coefficients (polynomial, eᵐˣ, sin/cos
  right-hand sides, resonance included), and initial conditions: dsolve(y″ + y = 0,
  y(0) = 1, y′(0) = 0) → y = cos x.
- **Calculator ↔ graphs**: every answer that depends on x (or x and y) has a graph button
  that sends it to the 2D (or 3D) graph; solve(f(x) = g(x)) sends both sides so the
  solutions are the marked crossings. On the graph, a tapped point's bubble has
  "Use x" and "Use y", which put that number into the calculator.
- **Polish**: undo and redo (↶ ↷ above the tabs; undo also brings back a cleared input),
  Settings (significant digits 4–15, decimals first), share any answer as an image or as
  LaTeX (or copy the LaTeX), and long-press any key to see its name.
- **Keys**: x is on the number pad; a new abc tab holds the other letters; the algebra tab
  has comma and < > ≤ ≥; the calculus tab has dⁿ/dxⁿ, dsolve, y′ and ∞.

## Physical constants and unit systems
All constants from Wikipedia's *List of physical constants* (CODATA 2022 values), plus
standard gravity: c, h, ℏ, k_B, G, Λ, σ, c₁, c₁L, c₂, b, b′, b_entropy, e, G₀, G₀⁻¹, R_K,
K_J, Φ₀, α, α⁻¹, μ₀, Z₀, ε₀, mₑ, m_μ, m_τ, mₚ, mₙ, mₚ/mₑ, m_W/m_Z, sin²θ_W, gₑ, g_μ, gₚ,
h/2mₑ, μ_B, μ_N, rₑ, σₑ, a₀, R∞, Ry, E_h, G_F, N_A, R, F, N_A h, M(¹²C), m_u, M_u,
V_m(Si), Δν_Cs, g₀. Constants the SI defines exactly with terminating decimals (c, h, e,
k_B, N_A, R, F, N_A h, c₁L, Δν_Cs, g₀) stay exact; the rest are decimals.

On the constants tab the Rad/Deg switch becomes a unit-system switch:
- **SI**: the usual values.
- **Planck**: c = ℏ = G = k_B = 1 (and 4πε₀ = 1), so e² = α.
- **Atomic** (Hartree): ℏ = mₑ = e = a₀ = E_h = 1, so c = 1/α ≈ 137.036.
- **Natural** (particle physics): ℏ = c = ε₀ = k_B = 1 with energies in eV, so mₑ ≈ 510 999 eV
  and e = √(4πα).

Each constant stores its SI dimension (mass, length, time, current, temperature, amount),
and its value in another system is the SI value divided by that system's unit for the
dimension. Amounts of substance stay in moles in every system.

## Statistics
A statistics tab (bell-curve icon, before the letters): n!, n choose k, permutations P(n, k),
and, on lists typed with commas like mean(2, 4, 4, 5), the mean, median, sample standard
deviation s, population σ, sample variance s² and sum. Exact when the data are: the sample
standard deviation of 2, 4, 4, 4, 5, 5, 7, 9 is 4√14/7. Distributions: the normal density
φ(x, μ, σ) (a formula, so it can be graphed), Φ(x, μ, σ) = (1 + erf((x − μ)/(σ√2)))/2, its
inverse Φ⁻¹(p), binomial P(X = k) and P(X ≤ k), and Poisson P(X = k), exact when p is.

## Contour integrals and residues
On the ℂ tab: ∮ around a circle written underneath (|z − a| = r), and Res at a point (z = a).
∮ is computed by the trapezoid rule on the circle, which is exponentially accurate, and shown
exactly when ∮/2πi is a simple fraction: ∮_{|z|=1} e^z/z³ dz = πi, ∮_{|z−i|=1} dz/(z² + 1) = π.
Residues come from limits (any pole order up to 8), e.g. Res_{z=i} 1/(z² + 1) = −i/2.

## Help on every key
Long-press any key for a dialog over the darkened screen with its name, the formula, a line
of theory and how to use it; it stays until Dismiss (or Back). Formulas are written in LaTeX
in KeyHelp.kt (inline math as \( … \)) and turned into the calculator's own math tree by
engine/LatexParser.kt, so they're drawn in Computer Modern by the same renderer as answers:
fractions, roots, scripts, ∫ … dx, ∮, Σ, lim, binomials, matrices, Greek, and TeX's spacing
around operator names. A test checks every help formula uses only supported LaTeX. Constants
show their value, unit and whether they're exact.

## 3D graph additions
- Lines can be z = f(x, y) (or just f(x, y)) or **any equation in x, y and z**, drawn as an
  implicit surface by marching tetrahedra: x² + y² + z² = 4 is a sphere.
- **Limits:** − and + zoom; tapping the ranges opens a dialog for x, y and z (z can fit itself).
- **Tap the surface** to read (x, y, z) there (on z = f(x, y), z is computed exactly at x, y).
- Chips above the keypad type x, y and z; long-press a function's dot for its color.

## 2D area
Tap a curve, press the area button (∫ with shading) in the bubble, then tap a second point:
the region is shaded and a card shows ∫ₐᵇ f dx and the total area between curve and axis
(∫ₐᵇ |f| dx), e.g. 2 for sin x from 0 to π.

## Functions inside functions
∮ around a circle with letters in it (|z| = R) is kept as ∮ until they're known: a limit that
substitutes R = 3 works it out numerically, and a limit with R → ∞ turns it into 2πi times the
sum of all residues (the poles are the roots of f's denominator, found exactly), so
lim_{R→∞} ∮_{|z|=R} e^{iz}/(z² + 1)² dz = π/e. Residues at poles of rational denominators are
exact (the pole is divided out, no limits needed), and e^(ln a + qπi) simplifies
(e^(ln(−2)) = −2). CompatibilityTest checks limits of integrals, derivatives of integrals
with variable limits, integrals of derivatives, residues of derivatives, sums of binomial
probabilities, statistics of integrals and more.

## Constants in the help
Each constant's help shows its value as LaTeX: the symbol with upright labels (m_n), the digits
grouped in threes, a real power of ten, and upright SI units with negative exponents
(m³ kg⁻¹ s⁻²), plus a link to its NIST CODATA page (or NIST's constants index).

## Matrices that grow
The matrix key inserts a matrix with one empty row and column after the ones in use; typing
into the last row or column adds another. Trailing rows and columns that are still empty when
you press = aren't part of the matrix (empty cells inside count as 0), and the history keeps
only the part in use. The 2- and 3-vectors on the ∇ tab have a fixed size.

## Settings and acknowledgements
Settings (⋮ menu), in sections (AppSettings.kt keeps them):
- Numbers: significant digits, decimals first, digit grouping, number format (auto,
  scientific, engineering with exponents in threes), complex decimals as a + bi or r·e^{iθ}.
- Calculator: live answer, continue from the answer after =, explanations on long-press,
  history length (50, 100, 500, all), ask before clearing history.
- Appearance: theme (system, light, dark), dynamic color, app color (when dynamic color is
  off or unavailable), math size, keypad size, expressive or standard motion, keep the screen on.
- Feel: haptic feedback, key sounds.
- Graphs: grid lines, marking zeros/extrema/crossings, starting view (±5, ±10, ±20),
  complex plot quality (standard or full resolution), 3D surface detail.
- About: the acknowledgements (fonts, libraries, CODATA data, the published methods used,
  inspiration), each with a link, also reachable from the ⋮ menu.

## Compatibility sweep
preview-render/CompatibilitySweep.kt inserts every function key, fills it with sample
arguments until it works on its own, and nests it in a limit, a definite integral, a
derivative, a sum, a power and sin, then evaluates, formats and exports each result:
894 combinations work, 58 give a message (mathematically right, like ∫₀¹ Γ(x) dx diverging,
or operations not done symbolically, like d/dx Γ(x)), and none crash.

## Symbol builder
The first key on the letters tab opens the symbol builder, a full-screen page with a live
preview (and its LaTeX): pick a letter (Latin, Greek, calligraphic or Fraktur), an accent
(dot, double dot, hat, tilde, bar, arrow, check, breve, acute, grave; each chip shows it on
your letter), then tap the subscript or superscript box and fill it from the pad. "Save and
insert" types the symbol and puts it at the top of the letters tab; saved symbols can be
removed in the builder. A built symbol (x̂₁, θ̇², 𝔤ᵢ, v⃗) is a variable like any letter:
it can be solved for, differentiated by, graphed with sliders, and exports as \hat{x}_{1}.
LaTeX accents (\hat, \dot, \vec…) in formulas make the same symbols.

## Recent keypad and engine work
Trigonometry is four across and three down. Calculus holds the vector-calculus keys and the
coordinate switch; ∇ takes an optional order (blank is the gradient, 2 the Laplacian). The
limit's side (+ or −) is written above the point, and an empty box means an ordinary limit.
An empty root index means a square root. Backspace in the empty main field of a construction
removes the whole construction. ≤ and ≥ are typed as <= or >=. New in the engine: Lambert W,
Bessel J and Y (checked against published values), the Hadamard and Kronecker products and the
Hermitian conjugate. Settings and acknowledgements are in the ⋮ menu in every mode.

## The keypad
Above the number pad: the drag pill (swipe down to hide the keypad), a row with the collapse
button, the Rad/Deg (or coordinate, or unit) switch and the two cursor buttons, then the nine
groups as a connected button group of icons that fills the width, then one rounded surface of
keys, then a dot per group. Groups slide across as they change, by tapping an icon, swiping the
keys or the dots. Every group is the same height (four rows), so nothing shifts; groups with
three rows get taller keys. Grids are at most five across (three for trigonometry, six for
symbols). Constants and symbols scroll up and down in place.
Groups: Basic, Trigonometry, Calculus (with vector calculus and the coordinate switch),
Linear algebra, Statistics, Complex numbers, Physical constants, Symbols.

## Old keypad notes
Above the number pad: the drag pill (swipe it down to hide the keypad), then a row with the
collapse button, Rad/Deg and the move-left/right buttons, then the groups as Material 3
Expressive's connected button group (icon and name; the chosen one is filled and fully
rounded), then one rounded surface holding that group's keys, then a dot per group.
Swiping the keys sideways moves between groups. Grids are at most five across and four down,
and each group sets its own width (three for the wider trigonometry keys, six for symbols).
Constants and symbols are long lists and scroll up and down in place; the others don't scroll.
Groups: Basic, Trigonometry, Calculus, Vector calculus, Linear algebra, Statistics, Complex
numbers, Physical constants, Symbols.

## Building on GitHub
Every push to `main` (and to the development branch) builds the debug APK with GitHub Actions
(`.github/workflows/android-debug.yml`), then runs the unit tests. To get the APK: the
repository's **Actions** tab → the latest **Debug APK** run → **Artifacts** → `debug-apk` (a zip
with the .apk inside). Install it on a phone with "install unknown apps" allowed for your
browser or file manager. The run can also be started by hand (Run workflow). Debug builds are
signed with a throwaway debug key, so they install alongside nothing from the Play Store.

## Latest changes (4 October, symbolic calculator icons)
- **New calculator icons (beta) are now the keys' own symbols**, drawn with the app's round strokes
  as |x|, √ and Π were. Examples: π, e, n!, ln, log, ∂, ∫, lim, Σ, y′, ∇, ∇·, ∇×, x̄, x̃, σ, s²,
  (n k), ₙPₖ, φ, Φ, λ, Re, Im, z*, ∠z, e^{iθ}, Γ, ζ, W, J, Y, ∮ and Res. The operator or constant
  is in the accent color; the boxes and letters it works on are in ink.
  (`ui/KeyIcons.kt`; mockup: `preview-render/round49.png`.)

## Latest changes (4 October, new calculator icons, two-tone edit/copy/paste/share)
- **New calculator icons (beta, Settings › Calculator):** the function keys on the Basic,
  Calculus, Statistics and Complex pages show a two-tone picture of what they do instead of their
  label. Examples: π as a circle and its half-turn, e as its curve, ∫ as the shaded area, lim as a
  curve running into its asymptote, the distributions for the statistics keys, the complex plane
  for the complex keys. Trigonometry, matrices, letters and constants keep their labels.
  (`ui/KeyIcons.kt`.)
- **Edit, Copy, Paste and Share** are the app's own two-tone icons everywhere: menus, history,
  steps, the converter, exports and the data table. Every two-tone icon now also works as a plain
  one-color icon wherever one is drawn.
- **Data table:**
  - A bolder bold.
  - Exponent digits closer together.
  - Alignment stands as an upright connected group (Left / Center / Right) on tablets, with no
    empty space.
  - The status bar's zoom − and + show their signs again.
  - The arrow keys over the formula bar are gone.
- Mockups read the icons straight from the app's source (`preview-render/icons_svg.py`; mockup:
  `preview-render/round48.png`).

## Latest changes (4 October, typewriter superscripts, tablet + button)
- **Superscripts in the typewriter font:** ⁰ ⁴ ⁵ ⁶ ⁷ ⁸ ⁹ ⁺ ⁻ added to the cells' font, in roman,
  italic and bold. Each is made from the font's own digit, scaled and raised just as its ² is, so
  they match ¹ ² ³ and scientific formats (1.23 × 10⁻⁴) stay in one font.
  (`preview-render/mono_superscripts.py`.)
- **Tablet:** the + button sits higher, clear of the status bar's zoom slider.

## Latest changes (4 October, two-tone icons, Expressive ribbon, typewriter cells)
- **Two-tone icons:** every data-table icon has an ink layer (what a command acts on) and an
  accent layer (what it makes or does: a sort's arrow, an insert's plus, AutoSum's Σ), as the
  geometry tools' icons. They show two-tone in the ribbon, the phone's bottom bar, the ⌃ list,
  the selection toolbar and the column sheet.
- **Material 3 Expressive ribbon:**
  - Tabs with a sliding pill.
  - Each group in its own rounded card.
  - Large buttons are tonal shapes that square up when pressed or on; small ones are pills.
  - Bold | Italic and Left | Center | Right are connected button groups.
  - A menu's ▾ turns over while it's open, and the fold button is round and tonal.
- **Cells in Computer Modern's typewriter:** New Computer Modern Mono (the Unicode CMU
  Typewriter), with its true italic and its bold, in the cells and the formula bar.
  (`ui/TableIcons.kt`, `res/font/ncm_mono*.otf`; mockup: `preview-render/round47.png`.)

## Latest changes (4 October, per-cell styles and pinch to zoom)
- **Bold, Italic and Fill color per cell:** they style the selected cell or range, as in Excel and
  Sheets, and are saved with the table. Each cell keeps its look when rows are sorted, shuffled,
  inserted or deleted, when columns are inserted, moved or deleted, and when it's cut, copied,
  pasted or painted with Format Painter. Paste values leaves looks behind; Clear › formats removes
  them.
- **Pinch to zoom:** two fingers zoom the table between 60% and 160%; one finger still scrolls and
  taps. (`graph/DataTable.kt`, `CellStyle` in `graph/SheetTools.kt`, `SheetToolsTest`; mockup:
  `preview-render/round46.png`.)

## Latest changes (4 October, many more ribbon commands)
- **Home:**
  - Clipboard: Paste formats, Paste transposed and Format Painter.
  - A new Font group: Bold, Italic, Fill color ▾ and Change case ▾ (UPPERCASE, lowercase,
    Proper Case).
  - Number: Currency ▾ ($ € £ ¥).
  - Editing: Find & Select ▾ (Find and replace, Go to…, select all, a column or a row).
  - AutoSum ▾ also offers MEDIAN, PRODUCT and STDEV.
- **Insert:** Random ▾ (whole 1–100, 0 to 1, normal), Series, 1, 2, 3…, Today, Now and =TODAY().
- **Formulas:** the Distributions and Engineering categories, Fill formula down and Formulas to
  values.
- **Data:**
  - Randomize and Reverse the rows.
  - Fill blanks (each empty cell takes the one above).
  - To numbers (1,234.5, 12%, $40 and (3) become numbers).
  - Unique (each value once, in a new column).
  - New column ▾: a running total, the difference from the row before, % of the total, rank,
    z-score, or the numbers scaled 0 to 1.
- **View:** Hide column and Unhide (hidden columns are still saved and plotted), Go to, Select all.
- **Icons:** 19 more custom icons. (`graph/SheetTools.kt`, `SheetToolsTest`; mockup:
  `preview-render/round45.png`.)

## Latest changes (4 October, the data table on a phone)
- **No ribbon on a phone:** like Excel, Sheets and Numbers there, the commands sit in a bar
  along the bottom. Home ▾ picks the tab from a menu, its commands are a row of icons that scrolls
  sideways, and ⌃ opens them all as a list by group, where a menu opens in place with ← back.
  The tablet keeps the full ribbon.
- **Floating toolbar over a selection:** one tap selects a cell without the keyboard (a second tap,
  the formula bar or Edit types in it), and a Material 3 Expressive floating toolbar offers Edit,
  Cut, Copy, Paste, Clear, Insert ▾ (row above or below, column) and Delete ▾ (the selection's rows
  or columns).
- **123 / abc:** while typing on a phone, switch between the number pad and the whole keyboard.
  (Mockup: `preview-render/round44.png`.)

## Latest changes (4 October, Excel-style ribbon)
- **The ribbon, as in Excel:** text tabs (Home, Insert, Formulas, Data, View) with a sliding
  underline; groups of large buttons with small ones stacked beside them, ▾ split buttons,
  group names underneath (with a launcher arrow), lines between groups, and ⌃ to fold it away.
- **Home:** Clipboard (Paste ▾ with Paste values, Cut, Copy; pasted formulas shift their
  references), Alignment (left, center, right), Number (format ▾, percent, decimals),
  Styles (Conditional formatting ▾), Cells (Insert ▾, Delete ▾) and Editing (AutoSum ▾, Fill ▾,
  Clear ▾ contents / formats / all, Sort & Filter ▾, Find).
- **Formulas:** Insert function (search every function or browse by category, also from ƒx in
  the formula bar), and Show formulas.
- **Data:** Custom sort by up to four columns, Text to columns (comma, semicolon, space, tab,
  slash or any text, with a preview), plus the earlier tools.
- **View:** Show formulas, zoom out / 100% / zoom in, freeze the first column.
- **Formula bar:** a Name Box (type B12 or A1:C5 to go there), and ✕ / ✓ while typing.
- **Tablet status bar:** the mode (Ready, Enter, Edit, Select), the selection's average, count
  and sum, and a zoom slider.
- **Icons:** 22 more custom icons. (`graph/SheetTools.kt`, `SheetToolsTest`; mockup:
  `preview-render/round43.png`.)

## Latest changes (4 October, more spreadsheet power)
- **AutoSum (Home):** Σ puts =SUM, AVERAGE, COUNT, MIN or MAX of the numbers above (or to
  the left of) the selected cell into it, as Excel does.
- **Arrow keys** over the formula bar move the selected cell without closing the keyboard
  (down adds a row at the end).
- **Data bars and highlight rules (Format):** a bar in each cell as long as its number is far
  along the column's range. A rule tints the cells that are equal to, greater or less than a
  value, contain some text, or are empty, in one of five tints. Both are saved with the table.
- **Insights (Data, or a column's sheet):**
  - A histogram, with the mean, median and standard deviation.
  - The straight-line trend against the x column (or the row number), with slope, intercept,
    r and r², said in words.
  - Outliers by Tukey's fences, which Mark them outlines in the table.
- **Icons:** four more custom icons (AutoSum, Data bars, Highlight, Insights).
  (`graph/SheetTools.kt`, `SheetToolsTest`; mockup: `preview-render/round42.png`.)

## Latest changes (4 October, data table redesign)
- **The data table, organized like Sheets and Excel (Material 3 Expressive), with its rounded
  cells kept:**
  - **Formula bar:** tap a cell to select it, then type in the formula bar. On a phone it sits
    at the bottom, in reach of a thumb and just above the keyboard; on a tablet it's above the
    grid. It shows the cell's address in a pill, references in their colors, and a $ button
    that anchors the last reference.
  - **Toolbar in tabs:** Home, Insert, Data and Format, as an expressive connected button group,
    with big tonal buttons that work on the selected cell's column. On a phone they hide while
    the keyboard is up. On a tablet they're a ribbon of labelled groups.
  - **Column sheets:** a column's card opens a bottom sheet with its role as a segmented
    button and every action as a tile, instead of a long menu.
  - **Summary pill and inspector:** a floating pill sums the selected column or range (tap for
    the average, count, smallest and largest). A tablet's side panel shows the selected
    column's role, format, filter and statistics.
  - **Counts in the title:** the points and columns count moves into a line under the title.
  - **Icons:** 32 new icons for the tools, drawn to match the app's own (`ui/TableIcons.kt`).

## Latest changes (4 October, spreadsheet tools, complex constructions)
- **Data tables, more like a spreadsheet:**
  - Find and replace (matching cells outlined, step through them, replace one or all, match
    case or whole cells).
  - Filters by condition on any column (equal, greater, less, contains, empty, not empty),
    with a banner showing how many rows are shown; hidden rows are still plotted.
  - Hold a cell to select a range, then tap to stretch it. A bar shows its sum, average,
    count, smallest and largest, with Copy (tab-separated, pastes into Sheets or Excel) and
    Clear.
  - Number formats per column (fixed decimals, percent, scientific 1.23 × 10⁴, thousands
    grouped) and a red–yellow–green color scale. Only the look changes; both are saved with
    the table.
  - Freeze the first column. Remove duplicate rows, trim spaces, swap rows and columns.
  - The fill handle counts on from text, as Excel does: Week 1 → Week 2, Q09 → Q10,
    Jan → Feb, Monday → Tuesday.
  - The title is just "Data" on phones. (`graph/SheetTools.kt`, `SheetToolsTest`.)
- **Complex plane:** constructions are lines and points, not functions: their dot opens
  color, line style and options instead of a colormap. They show in the legend and are
  drawn in exports.
- **Geometry italics:** letters in construction values use Computer Modern's italic face,
  not the upright one slanted.
- **Geometry guide:** sits above the graph's buttons in all three graphs.
- **Languages removed** for now (they were a test). `tr()` stays, and `i18n/template.tsv`
  lists every string for translating; see `i18n/README.md`. Settings › Language appears once
  a translation is added.

## Latest changes (4 October, languages, speed, geometry guide)
- **Languages (test):** Settings › Language picks English, Frysk (West Frisian) or
  srpskohrvatski (Serbo-Croatian), or follows the phone. Serbo-Croatian has a Spelling choice,
  ijekavian (vrijeme) or ekavian (vreme); it starts as ekavian on a phone set to Serbian. The
  setting's title also says "Language" in English so it can always be found.
  - About 570 strings are translated: settings, menus, graphs, data tables, the converter and
    every geometry tool. Anything not translated yet, including the documentation and the key
    explanations, stays in English.
  - Translations are plain files, `assets/i18n/fy.tsv` and `sh.tsv`: the English, a tab, then
    the translation. Serbo-Croatian writes `{ijekavian|ekavian}` where the spellings differ.
    `TranslationsTest` checks each line matches a string the app shows, keeps its placeholders,
    and that both languages cover the same strings.
  - The strings are machine-written and need a native speaker's review.
- **Faster dragging:** moving a construction's point no longer compiles every function again,
  samples the 3D surfaces again, or redraws the complex colouring; only the constructions are
  worked out again. Numbers typed in constructions (Distance(A, B)/2) are compiled once and
  reused, which speeds up loci most.
- **Geometry guide:** the first time Construct opens in each graph, a card at the bottom walks
  through it: pick a tool, tap to build, drag a point, then what's special to that graph. Each
  step ticks itself off when done. Show me does it for you, Skip ends it, and the ? on the
  status card shows it again.

## Latest changes (4 October, dragging in 3D and the complex plane, typing fixes)
- **Deleting every line (phones):** the list no longer stays full screen with no keyboard.
  Deleting the line being typed in ends typing, and so does deleting the last line: the
  keyboard and the list step back and the graph returns.
- **Dragging points in 3D and on the complex plane:** free construction points drag, as on the
  2D graph. In 3D a point moves level (its height kept) and snaps to a fortieth of the box. On
  the complex plane a point snaps to a hundredth of the view, and a point typed as a number
  (A = 1 + 1.5i) is rewritten as one. Points on paths slide along them.
- **Complex plane drawn like the 2D graph:** lines, curves, loops and constructions have no dark
  outline any more. They use their own thickness and style (dashed, dotted…), points their mark
  and size, vectors an arrowhead, and names are set in math italic. Exports match.

## Latest changes (4 October, geometry in space and on the complex plane)
- **3D geometry:** with geometry mode on, the 3D graph builds in space (`graph/Geometry3D.kt`,
  with tests).
  - **Objects:** points (A = (1, 2, 3)), lines, segments, rays, vectors, planes, spheres,
    circles, polygons, and solids (pyramids, tetrahedra, prisms, cubes).
  - **Constructions:** intersections (two planes meet in a line, a plane and a sphere in a
    circle), perpendicular and parallel lines and planes, closest points.
  - **Measures:** distances (including between skew lines), angles (between lines, a line and a
    plane, or two planes), lengths, areas, volumes.
  - **Transforms:** reflect in a plane or point, rotate about a line, translate, dilate.
  - **Drawing:** planes are cut to the box; planes, spheres and solids are see-through and depth
    sorted with the surfaces.
  - **Construct mode:** a Planes & solids tool group, and a tap on empty space puts a point on the
    floor.
- **Complex-plane geometry:**
  - **Points are numbers:** A = 1 + 2i; A·B, A/B, powers, |A|, arg, √ and eᶻ work on them.
  - **New commands:** Conjugate, Modulus, Argument, RootsOfUnity and ComplexRoots.
  - **Functions of z:** with f(z) defined, Image(f, c) maps a point, line, circle or curve
    through f, and f(A) is a point.
  - **Construct mode:** the same tools as the 2D graph, plus a Complex group with its own icons.
- **Shared construct UI:** the tools and their icons now live in `ConstructUI.kt`, and each tool
  says which of the three graphs it belongs to.
- **Always on:** the unit converter and spreadsheet formulas, and their settings are gone.
- **Data table:** the ⋮ menu is after Redo.
- **Point card:** its buttons row now scrolls sideways (it didn't scroll at all before).

## Latest changes (4 October, point card, data tables)
- **Point card:** its buttons (area, tangent, normal, arc length…) scroll sideways again.
  A touch that starts on the card no longer pans the graph or closes the card, in 2D, 3D and
  the complex plane. Tangent, normal and arc length have their own icons, drawn like the area
  ones.
- **Out of beta:** the unit converter and spreadsheet formulas in data tables.
- **Data tables:**
  - **Column statistics:** count, sum, mean, median, standard deviation, smallest, largest
    and range; tap a value to copy it.
  - **Sorting and filling:** sort largest first, and fill a column with a series of any start
    and step.
  - **Columns and rows:** duplicate or move a column, and duplicate a row.
  - **Top ⋮ menu:** share or copy the table as CSV (formulas as their values), and remove
    empty rows.

## Latest changes (4 October, editing fixes)
- **Graph lines edit like the calculator:** the graph read a line by wrapping parts of it in a
  scratch row to copy them, and that took the parts out of the line being edited. After that,
  ⌫ inside an empty cos( ) (or any empty element) did nothing, and the cursor vanished when it
  stepped out of an element at the end. Parts are now copied without moving them
  (`MathCodec.copyOf`), with a test.
- **Keyboard comes back:** after pulling the keyboard down, tapping a line brings it (and the
  typing view) back.
- **Outline:** the line being edited has a border.

## Latest changes (4 October) 
- **Phones:** the keyboard button also hides the graph. While the graph is hidden, every line
  shows in a scrolling list, not just the one being edited.
- **Cursor:** the arrow keys stop at a line's start and end, and keep the cursor in view there.
  They pass over empty sub- and superscripts, which aren't drawn.
- **Backspace:** between an empty ( ) or [ ], both brackets go at once.
- **Data tables:** a $ button under a formula ending in a reference cycles it through A1, $A$1,
  A$1 and $A1, as Excel's F4 does. Filling keeps the $-fixed row or column. The docs explain
  mixed references.

## Latest changes (3 October, fixes)
- **Dragging points works again:** free points, points on paths, slider points and an area's
  edges can be dragged again (a check meant for the construct rail had stopped every drag).
- **Tablet construct rail:** scrolling through the tools is reliable. A touch that starts on
  the rail or the status card is left alone by the graph instead of fighting the scroll.
- **Phones:** editing a line also hides the graph while you type; Enter or the keyboard's handle
  brings it back. The extra pill above the list is gone.
- **Geometry options:** points get mark shapes (and no line style); lines get line styles.
- **Data tables:** cells open the full keyboard, not just a number pad.

## Latest changes (3 October, typing room and construct polish)
- **Room to type (phones, all three graph modes):** adding a line, note or folder hides the graph
  and its buttons so the list gets the screen. Enter, pulling down the keyboard's handle, the
  pill over the list, or closing a note's keyboard brings the graph back. A new note is focused
  straight away, and a new folder asks for its name.
- **Construct mode on phones:** the palette takes the list's place under the graph. The list comes
  back when construct mode closes, which now happens on its own when a line is edited or a line,
  note, folder or table is added. The palette no longer folds or shows recent tools. Every group's
  grid is the same height (it scrolls), and every tile is the same size, with short names
  ("Perp. bisector").
- **Tablets:** touches on the construct rail and status card no longer pan or tap the graph
  behind them.
- **＋ menu:** the geometry entry is gone. Every command, with what it does, is listed in
  Settings › Documentation › Geometry mode.
- **New geometry mode icon:** a compass drawing an arc.

## Latest changes (3 October, geometry fifth pass)
- **Tool palette under the graph:** on phones the construct palette now sits below the graph,
  which shrinks to make room and keeps its scale, so nothing is hidden behind it. Dragging over
  the palette no longer pans the graph. A group with many tools scrolls.
- **Values as math:** numbers in scientific notation under construction lines and beside points
  show as 2.5 × 10⁶, not 2.500e+06. Letters standing for quantities (the r of a circle) are
  italic.
- **More commands:** ClosestPoint, Inflection, CommonTangent, NinePointCircle, EulerLine,
  Excircle, TriangleCenter (Kimberling 1–8), MajorAxis, MinorAxis, UnitVector,
  PerpendicularVector, UnitPerpendicularVector, Direction, Circumference, Eccentricity,
  LinearEccentricity, SemiMajorAxisLength, SemiMinorAxisLength, Dot, Cross and AreCongruent.
  Angle(l, m) measures between two lines, and Segment(A, a) has length a.
- **More tools:** inflection points, closest point, common tangents and incircle.

## Latest changes (3 October, geometry fourth pass)
- **Fixed:** the app crashed on start, and kept crashing, once geometry mode was on. Lines
  were being built before the graph's view existed; they are now built once it does.
- **More tools:** a Transform group (mirror in a line or a point, invert in a circle, rotate,
  translate, dilate). Also new: polyline, best-fit line, compass, a segment with a set length,
  a circle with a set radius, an angle with a set size, a circumcircular sector, roots and
  extrema of a function, slope, and Relation. Tools that need a number ask for it after the
  taps. Tools that take any number of points finish with Finish.
- **New commands:** Polyline, FitLine, Root, Extremum, CircumcircularSector and Relation.
  Circle(A, segment) uses the segment's length as the radius, and Reflect in a circle
  inverts.
- **Naming:** it's called geometry mode everywhere; GeoGebra, whose tools it follows, is
  credited in Acknowledgements.

## Latest changes (3 October, geometry third pass)
- **Geometry mode (alpha), third pass**:
  - **Loci:** Locus(P, Q) draws the curve P traces as Q slides along its object, and there's
    a Locus tool. Other lines can use a locus, for example a point on it.
  - **New commands:** triangle centers (Circumcenter, Orthocenter, Incenter, and Centroid of
    points); a conic's parts (Foci, Vertex, Asymptote, Directrix) and Polar lines; and checks
    that show true or false (AreCollinear, AreConcyclic, AreParallel, ArePerpendicular,
    AreEqual).
  - **Line options:** construction lines now have their own options. Points have size, Show
    name and Show coordinates; filled shapes have fill opacity. A point on an object can Move
    along its path by itself, and everything built on it follows.

## Latest changes (3 October, geometry second pass)
- **Geometry mode (alpha), second pass**:
  - Arithmetic mixes with commands: Distance(A, B)/2, M = (A + B)/2, B = A + (2, 0), 3A,
    x(A) and y(A).
  - New objects: conics (Ellipse, Hyperbola, Parabola, Conic through five points), arcs and
    sectors (Semicircle, CircularArc, CircumcircularArc, CircularSector) and RegularPolygon.
  - Points on objects: P = Point(c, t) slides along a line, circle, arc, polygon, conic or
    function graph when dragged.
  - Functions f(x) from the graph's own lines work in Intersect, Tangent and Point.
  - The Construct tools pick lines, circles and curves as well as points (with what's picked
    highlighted), and there are many more of them, shown in two columns. New lines get
    automatic names (A, B… for points; f, g… for other objects; α, β… for angles).

## Latest changes (3 October, geometry alpha)
- **Geometry mode (alpha)**: constructions in the 2D graph, turned on in Settings ›
  Graphs. A = (1, 2) is a point you can drag, and commands build on it: Segment, Line, Ray,
  Vector, Circle (center and point, radius, or three points), Polygon, Midpoint, Intersect,
  PerpendicularLine, ParallelLine, PerpendicularBisector, AngleBisector, Tangent, Centroid,
  Incircle, Distance, Length, Perimeter, Area, Angle, Slope, Radius, Reflect, Rotate, Translate
  and Dilate. Lines can use names from any other line, and lowercase letters stay sliders.
  Construct (top right) has tap-to-build tools, and ＋ › Geometry lists the commands. The engine
  is `graph/Geometry.kt`, with `GeometryTest`. Still early: it will change a lot.

## Latest changes (3 October)
- **New icon**: a curve inside a hexagon over three axes, cream and sage on dark green. It is the
  adaptive launcher icon, with a one-color themed icon (axes at 40%) on Android 13+. The Play
  Store graphics in `store/` are `icon-512.png` and `feature-graphic.png` /
  `feature-graphic-plain.png`, and the home-screen shortcuts use the same colors. Everything
  comes from `preview-render/icon_artboard.py` (`render_store.py` makes the PNGs).

## Latest changes (1 October)
- **Folders**: every line has its own depth, so a folder holds only the lines inside it: +
  never drops a new line into a folder, and moving a (closed) folder carries its lines as a block
  without taking in others (graph/FolderTree.kt). Folders have a color and a name, set by
  long-pressing them (with "Into folder above" / "Out of folder"); × deletes one (its lines stay).
  Lines in a folder are marked with bars in the folder's color. Older saves keep their folders.
- **Add menu**: + is a Material 3 FAB menu (Line, Note, Folder, Table).
- **Backspace** in an empty graph line removes the line and continues in the one above (empty
  constructions inside a line already go as in the calculator).
- **Area**: the card sits under the legend; its value is recomputed live as sliders or the
  line change, and its edges can be dragged along the graph. At a crossing of two curves, a new
  button gives the **area between the curves**.
- **Export legend**: ∫ and ∮ are TeX's own large-operator glyphs (the Size2 font, embedded in
  SVG) with limits placed as LaTeX places them.
- **Line styles** like matplotlib's: solid, dashed, dotted, dash-dot, long dash, dash-dot-dot;
  the legend (on screen and exported) draws each as it is (dotted is no longer dashed).
- **Tracing**: drag along a curve in 2D to read it continuously; on the complex plane and in 3D,
  touch and hold, then drag.
- **Complex plane**: grid, axes and numbers in the theme's colors, as in 2D. Derivatives and
  integrals plot: ψ (digamma), ψ′, erf, ζ′ and ζ″ on complex arguments; Bessel derivatives by
  their recurrence; integrals with z in a limit by Gauss–Legendre along the straight path; an
  integral with no closed form (∫ Γ(z) dz) is drawn as ∫₁^z.
- **3D**: a Cartesian button beside cylindrical and spherical; exactly one is always chosen.
- **Tablets**: drag the pill between the list and the graph to resize them (double-tap resets).
- **2D fields**: an expression in x and y with no = (x² − y²) is drawn as a colored field with
  a colormap (viridis by default; long-press its dot for others), with its value on tap.

## Latest changes (30 September, ninth round)
- The app is now called **CAS Scientific Calculator** (app info, feature graphic, messages); the
  name under the icon is just **Calculator**.
  Graph files keep their format, so ones saved earlier still open.
- The saddle in the launcher icon is smaller, with thinner lines to match.

## Latest changes (30 September, eighth round)
- **New icon**: a saddle surface z = x² − y² with the curve that bends up (mint) and the one that
  bends down (gold), on deep blue-green. Launcher icon (adaptive, and a line-art themed icon on
  Android 13+) and the Play Store graphics in `store/`: `icon-512.png` (512 × 512) and
  `feature-graphic.png` / `feature-graphic-plain.png` (1024 × 500). All generated from one
  script, `preview-render/icon_saddle.py` (since replaced by `icon_artboard.py`).

## Latest changes (30 September, seventh round)
- **Complex plots**: the empty plane is white in light mode (black in dark mode), with dark
  axes and labels until something is plotted.
- **Cyrillic** removed from the symbol builder again (and its font from the app).

## Latest changes (30 September, sixth round)
- **Graph files**: .g2d (2D graphs), .g3d (3D graphs) and .gcp (complex plots) hold a whole
  graph (lines, colors, styles, notes, sliders, ranges). Save or share one from the export
  dialog or a saved graph's menu; import them on the Saved graphs page (the import button), or
  open one from a file manager to import it and switch to its mode. The format is plain text
  (graph/GraphFile.kt).
- **ħ on the key** is slanted (Google Sans Flex's own slant axis).
- **Point size** in tenths of a dp, shown to one decimal.

## Latest changes (30 September, fifth round)
- **Saved graphs, redone**: no longer draws data sets point by point (that crashed the app);
  they show as a "Data set" chip, and only short lines are drawn. The page has search, an
  order menu (newest, oldest, name), a connected button group for the modes with counts in
  brackets, date groups (Today, Yesterday, This week, Earlier), cards with each line's color,
  the mode's icon and a menu (rename, duplicate, delete), and a Save button at the bottom. It
  fills the screen and is a grid on tablets.
- **ħ key**: the Maltese ħ in the key font; math keeps the italic ħ.
- **Symbol builder**: with Italic off, the script buttons turn upright too; lowercase Greek
  written upright uses New Computer Modern's upright Greek; the Hebrew group is the Unicode
  letters only (ℵ ℶ ℷ ℸ removed); accents sit on Hebrew letters without the extra gap.

## Latest changes (30 September, fourth round)
- **Symbol builder**: the alphabet chooser is an M3 connected button group (Aa, Γγ, ℵב, 𝒜ℬ,
  𝔄𝔞, 𝔸𝔹); the script boxes are named left/right superscript and left/right subscript;
  double-tap a box to type text from the keyboard (upright, like \text), with an Italic toggle
  per box; the LaTeX field reads \text, \mathrm, \operatorname and \mathit; the triple dot
  is gone.
- **Fonts**: blackboard bold, Hebrew, ħ and the Greek letters Computer Modern lacks (ϰ, and
  capitals like Α that look Latin) come from New Computer Modern (GUST Font License), so they
  match the rest of the math; the ħ key and ħ in equations are drawn with the barred h.
- **3D coordinates** are chosen like the calculus variables: a segmented row of the letters
  and ✎ to pick your own.
- **Color sliders** leave the M3 Expressive gap either side of the thumb.
- **Folders**: the arrow follows the theme; lines in a folder get a bar in the primary color
  (the red swipe strip no longer shows in the indent).
- **Points**: eleven shapes and a Filled switch instead of fifteen shapes.
- **Data tables**: the brackets in σ(x) and σ(y) are upright.
- **Export**: preview beside the options on tablets; files are named with the date and time
  (graph-2026-09-30_14-05-12.pdf).
- **Defaults**: small math and a compact keypad on phones, medium on tablets.
- **Saved graphs**: one page for all graph modes, with filter chips and each mode's icon in the
  card's corner; a grid on tablets; opening one switches to its mode.

## Latest changes (30 September, third round)
- **3D inequalities**: x² + y² + z² ≤ 4, z < x² + y², ρ < 2… are drawn as solids: the boundary
  surface and, where the box cuts the solid, its walls (lighter and see-through).
- **STL export** from the 3D graph (the export dialog's new STL option): z = f(x, y) as the solid
  under it, inequalities as their solids, other surfaces as they are; 100 mm across.
- **Your own coordinate letters in 3D** (Graph settings › Letters): e.g. i, j, k for x, y, z, for
  each of Cartesian, cylindrical and spherical. They're read as those coordinates and shown on
  the axes and the letter chips.
- **Symbol builder**: every Greek letter (and the variants), Hebrew (ℵ ℶ ℷ ℸ and א–ת),
  blackboard bold (ℝ), bold, two more accents (ring), scripts before the letter
  (¹⁴₆C) as well as after, many more script characters, a LaTeX field that fills in the builder,
  the vector arrow lifted clear of the letter, and two columns on tablets.
- **Complex plots**: a new function takes the colormap at the top of your list.
- **Export legend**: ∮, ∫ and Σ set in LaTeX.
- Mock-ups: preview-render/round9.png.

## Latest changes (30 September, second round)
- **Export legend set in LaTeX**: fractions stacked, exponents raised, roots and |x| drawn,
  brackets as tall as their contents, letters in math italic, all in Computer Modern with the
  fonts' real widths (graph/MathScene.kt).
- **Export colors**: #165c99, #0bb04b, #f9950f, #ed310c, #7c5e8b, #484848 in turn (the gray
  lightened on dark exports); 3D surfaces are shaded dark to light in these, one per line.
- **Line colors on screen**: the theme's primary, secondary, tertiary, error and surface
  (inverse surface, so it shows) in turn, unless a color is picked.
- **Data sets aren't edited inline**: tapping a list of points does nothing; its table button edits it.
- **Cursor**: tapping past the end of the line (or before it, in the calculator) puts the cursor
  at the end (or start) in all four modes; in graphs the end of the line stays in view.
- **2 sin x**: a thin space before function names, as LaTeX sets it.
- **u·v, u×v, ∘, ⊗** before a matrix, letter or bracket take it as the second operand.
- **Color picker**: theme and standard colors first, then a saturation–brightness square with
  hue, exact values, the line and point options; two columns on tablets.
- **Colormap picker**: the chosen map large with a Reversed switch, your maps as cards (Edit to
  reorder or remove), then every map by kind or name with a star to add it; two panes on tablets.
- **Point shapes**: 15 (plus, star, pentagon, hexagon, down-triangle, asterisk, hollow square,
  diamond and triangle added).
- Mock-ups: preview-render/round8.png.

## Latest changes (30 September)
- **Legend**: each line has a name, shown top left on all three graphs (under the range control
  in 3D) and in exports, SciencePlots-style. Hold a line to rename it; names are text with
  LaTeX math between $ signs. The default is the line's math, "Data" for a list, the point
  itself for a point. Settings › Graphs › Legend turns it off.
- **Data tables**: a file imports as one line, its first column x and its second y. The table
  opens full screen from the table button on the line (before ✕); each column's role is x, y,
  σ(x), σ(y) or unused, and σ columns draw error bars (on screen and exported).
- **Exports** use SciencePlots' color cycle, one color per line in list order.
- **Line colors** come from the theme (Material You's when on): primary, tertiary, then the
  primary's hue turned round the wheel, unless a color is picked.
- **The first line is drawn over everything below it** (its points and region too), in 2D, 3D
  and on the complex plane.
- **Cursor**: long lines scroll to keep the cursor in view, the cursor at the end isn't cut
  off, and tapping after the end of a line puts the cursor there.
- **u·v, u×v, ∘, ⊗** after a matrix, a letter or a bracket take it as the first operand.
- **Limits** with fractional powers: lim x→0⁺ (x − sin x)/(x sin x)^{3/2} = 1/6 (was 0),
  one-sided limits of |B| and (B²)^{r}, and sin x/√x → 0.
- **Tablets**: Settings and Acknowledgements in two panes; "Keyboard side" only on tablets.
- **Crash report**: after a crash the app shows what went wrong, with Copy, for bug reports.
- Mock-ups: preview-render/round7.png (build_round7.py, shot_round7.py).

## Latest changes (29 September, sixth round)
- **Tablets** (screens at least 840 dp wide and 600 dp on the short side): the keyboard has a
  column of its own, always showing, with taller keys sitting at the bottom and no hide
  button. Settings › Keyboard side on tablets puts it on the left or right. The calculator's
  history and input fill the other side; the graph modes put the lines next to the keyboard
  and the plot beyond them (keyboard | lines | plot, or plot | lines | keyboard). Typing with
  no line open starts a new one. Mock-up: preview-render/tablet.png (build_tablet.py,
  shot_tablet.py).
- **Data tables** scroll, so they take any number of rows; cells are compact, numbered, and
  red when they aren't numbers.
- **Tapping beside a fraction, root, power…** puts the cursor just before or after it, so a
  bracket can go round it.
- **Long lines scroll sideways**; swiping to delete starts from the row's edges (the color
  dot, the handle, ✕), so it no longer fights the scrolling.
- **The first line is drawn on top**, so reordering lines changes which is in front.
- **Complex plot export**: z and i are italic in the axis names and labels.
- **Ask before deleting** (Settings) now also covers lines, notes and folders in the graphs.
- **Custom symbols** show as themselves in the lines, sliders and slider dialogs, and are one
  symbol everywhere.
- The show-keyboard button comes after the CSV button; the graph keyboard is measured first,
  so it's never squashed by a long list of lines.

## Latest changes (29 September, fifth round)
- **List ranges**: [1...10] counts in ones, [1, 3, ...11] in the step of its first two
  entries (type "..." as three points). They work anywhere a list does: y = [1...5]x.
- **List comprehensions**: [n² for n = [1...5]] (type "for" with the letters), so
  y = [nx for n = [1...3]] draws three lines and [(n, n²) for n = [1...10]] ten points.
  At most 100 entries.
- **Folders inside folders**: a folder's ⋮ menu puts it inside the folder above or takes it
  out; lines are indented by how deep they are, and closing or hiding a folder does the same
  to everything in it, subfolders included.

## Latest changes (29 September, fourth round)
- **Colormap picker** is a popup again: one-word names (Viridis, RdBu, Dusk…); your maps are
  dragged by their handle to reorder and swiped away to remove; More colormaps opens below
  with + on each; ⇄ runs a map backwards.
- **Swipe a line away** to delete it in the 2D, 3D and complex graphs (lines scroll sideways
  only while being edited, so the swipe isn't taken by the scroll).
- **Points**: shape (circle, ring, square, diamond, triangle, cross) and size, in the line's
  options; used in exports too.
- **3D coordinates**: r, θ, φ and ρ are always coordinates (ρ = 2 is a sphere, r = 1 a
  cylinder, φ = π/4 a cone). The cylinder and globe buttons switch to cylindrical or spherical
  coordinates: guides are drawn and a line without "=" means r = … or ρ = …. All seven letters
  are on the chips above the keypad.
- **3D view** now looks down on the graph (the camera looked up from below before), and the
  **3D export** is drawn like pgfplots: back walls with a grid, viridis-colored faceted
  surfaces, Computer Modern ticks and labels.
- **Graph settings** in 3D (limits, coordinates, surface detail) and on the complex plane
  (limits, plot quality).
- **Keys**: pinning no longer switches the open card to another key; formulas in the cards
  are one line that scrolls sideways; Σ and Π start with x.
- **From Desmos**: lists in lines (y = [1, 2, 3]x draws three lines); closed shapes (polygons)
  from lists of points; tables (edit a list of points row by row, or hold + for a new table);
  notes and folders (hold + ; a folder's arrow closes it and its eye hides everything in it).

## Latest changes (29 September, third round)
- **Fixed**: the ∂ key crashed (it asked the editor for half a path into the derivative);
  EveryKeyTest now presses every key, types, evaluates and backspaces, so no key can crash it.
- **Bessel functions** are drawn J_a(z) and Y_a(z), with a box for the order, and can be
  plotted on the complex plane: J by its power series for |z| < 20 and Hankel's expansion
  beyond (also used for real x ≥ 25 now), Y from J (checked against mpmath in BesselTest).
- **Complex plane**: the loop tool is gone; the + button has no label in any graph.
- **Export**: always square (and the preview too), drawn like pgfplots: a framed plot with
  inward ticks, Computer Modern tick labels and axis names (the fonts are embedded in SVGs),
  pgfplots' line colors unless you picked one, light or dark.
- **Colormaps**: all 86 on matplotlib's Choosing Colormaps page. Your list comes first (move
  entries up or down, remove them to More); More colormaps lists the rest by group, each with
  a pin; every row has an invert button for the reversed map (saved as name_r).
- **CSV import** (the file button on the 2D graph): commas, semicolons with decimal commas,
  tabs or spaces; headers skipped; the first column is x and each other column a list of points.
- **More like Desmos**: drag a point like (a, b) to move its sliders; show a point's
  coordinates; join a list's points with a line; fill opacity for inequalities; a graph
  settings button (exact limits, grid, axis numbers, degrees).

## Latest changes (29 September, second round)
- **Fixed a crash** on long-pressing the new complex keys (Lambert W, Bessel J and Y) and every
  letter and symbol: their explanations had doubled backslashes, which made an empty symbol the
  renderer couldn't draw. The parser no longer makes empty symbols, the renderer copes with one,
  and KeyHelpTest checks every key's explanation draws and has a description and an equation.
- **Keys**: digits have no long-press. Letters, built symbols and constants can be pinned from
  their card (Pin / Unpin): pinned keys move to the front of their group, after the special
  keys, with a small pin in the corner. A letter with a value (a := 5) or a function definition
  has a tinted key, and its card has Undefine.
- **≤ and ≥** show as soon as the second key is typed (the display now redraws on every edit,
  even one that doesn't move the cursor).
- **Graph lines**: drag the handle (or hold a row) to move a line any distance; swipe a line
  away to delete it; Fit sits at the end of its row. + Add is on the bar at the bottom left of
  the graph, the tools on the right, and export in its own circle at the far right.
- **Colors**: saved graphs keep each line's color slot as well as custom colors, dashes and
  thickness; points and lists of points have no dash or thickness options. Colormap names are
  single words (twilight shifted is Dusk).
- **Export** works for 3D graphs too (with z limits), and the dialog shows a live preview.
- **Inequalities** draw much faster: sliders are read once per redraw instead of at every
  point, and the shading is tested 4 × 4 cells at a time away from the boundary (RegionTest
  checks the picture is identical).
- **Settings** are ordered Appearance, Calculator, History, Numbers, Graphs, Touch and screen,
  About. The **acknowledgements** are grouped (fonts, libraries, data, numerical methods,
  graphics, inspiration), each entry with its authors, what it's used for here, a line on what
  it is and its license.

## Latest changes (29 September)
- **Export graphs**: the share button on the 2D and complex toolbars exports the graph as PDF
  (the default), PNG, JPG or SVG, with the limits you type (the current view to start with)
  and light or dark colors; then Save (the system's save dialog) or Share. The graph is
  rebuilt as a list of shapes (graph/Scene.kt) at the new limits, so SVG and PDF are real
  vector files; the complex plane's coloring is rendered afresh and embedded as a picture.
- **Colormaps on the complex plane**: long-press a function's dot to pick how arg f is
  colored: the classic wheel, matplotlib's cyclic twilight maps, or viridis, plasma, magma,
  cividis and turbo (the dot shows the map as a ring). ∮ loops and curves such as |z − 1| = 2
  keep a plain color (white unless you pick one). Saved with the graph and its projects.
- **App color**: with Material You off (or before Android 12), Settings → Appearance offers
  preset colors or any color from the picker; a full light and dark scheme is grown from it
  (TonalScheme.kt, Material's tonal-spot recipe in OKLab).
- **Keypad**: one size in all four modes, and the function panel is three rows tall.
- **∇** has its own drawing: the order box is raised on ∇ (∇ⁿ f), shown while editing or when
  filled, and exported to LaTeX as \nabla^{n}.

## Latest changes (earlier)
- Infinite sums: a sum with a symbolic upper limit and no closed form is kept as it is until
  a limit finishes it, so lim_{n→∞} Σ 1/k² = π²/6 (p-series through ζ), Σ 1/k³ = ζ(3),
  Σ 1/k = ∞, and geometric series work from any starting index.
- Limits inside sums and products, with the index still a letter:
  Σ_{k=1}^{3} lim_{x→0} sin(kx)/x = 6, Π_{k=1}^{3} lim_{x→∞} (1 + k/x)ˣ = e⁶.
- Indeterminate powers in limits (1^∞, 0⁰, ∞⁰) through exp(lim g·ln f): lim (1 + 1/x)ˣ = e.
- Vectors in the 2D graph: every letter in a vector gets a slider, and letters that already
  have sliders keep their values.
- Fixed the crash when Enter finished a line in the 2D or complex graph: axis labels are
  pinned inside the canvas, and while the keypad collapses the canvas is briefly a few pixels
  tall, so the allowed range turned inside out and coerceIn threw. pinInside handles that
  (PinInsideTest covers it), and the same guard is on every pinned bubble in all three graphs.
- (8, t): a pair is a parametric curve only when t runs through both coordinates; otherwise t
  is an ordinary slider and the pair is a point that moves with it.
- A 2-vector (or a matrix times one, like a rotation matrix with a slider angle) is drawn as
  an arrow from the origin in the 2D graph.
- Limits of sums and products: geometric sums have a closed form now
  (Σ c·rᵏ), so lim_{n→∞} Σ_{k=1}^{n} 1/2ᵏ = 1.
- Ans is the newest answer still in the history, so deleting a calculation moves it back.
- Settings: ask before deleting a single calculation (off by default).

## Latest changes
- The number pad's bottom-right key is Enter (↵); in the graphs, the variable key is a plain =
  sign (the letters are on the chips above the keypad).
- Graph lines can't crash the app while drawing: a function that fails at a point leaves a gap.
- Settings and acknowledgements are full-screen pages.
- The letters tab is drawn in LaTeX's fonts (italic Latin and small Greek, upright capital
  Greek) and adds \mathcal capitals and \mathfrak capitals and small letters (TeX's Caligraphic
  and Fraktur fonts), which work as variables and export to LaTeX.
- Slider values keep a fixed number of decimals for their range, in a fixed width.
- Loop integrals on the complex plane are shown in Google Sans Flex, to at most 4 decimals.
- Functions of several variables: f(x, y, z) = … (and f(x, y) = … draws z = f(x, y) in 3D).
- Matrices taken from the history can be extended again; a growable matrix always keeps exactly
  one spare row and column.
- Linear algebra and vector calculus share one tab (with the coordinate switch).

## Typing lines like Desmos
Graph lines have no "y =", "z =" or "f(z) =": type anything and the line is read from it.
2D: functions of x, y = …, r = f(θ), (x(t), y(t)), points, equations, inequalities, f(x) = …
definitions, conditions after commas. 3D: z = f(x, y) or just f(x, y), any equation in x, y, z,
points (a, b, c) and curves (x(t), y(t), z(t)). Complex plane: f(z), w = …, real equations,
∮ lines. Every letter that isn't one of the line's own coordinates gets a slider (y = 3t
has a slider for t). Double-tap a graph to reset its view.

## More like Desmos
- **Your own functions:** a line f(x) = x² (drawn, since its variable is x) lets every other
  line use f(x + 1), f(2) or f′(x); f(t) = … just defines f. In the calculator, f(x) := x²
  defines f for later calculations.
- **Restrictions:** conditions after commas limit where a line is drawn:
  y = x², 0 < x < 2 (Desmos writes them in braces; a comma is on the keypad).
- **Points:** (2, 3), or (a, b) with sliders.
- **Square zoom:** the toolbar button makes both axes the same scale.
- **Line style and thickness:** long-press a line's dot: solid, dashed or dotted, 1–8 dp.

## Material 3 Expressive
The app uses material3 1.4.0-alpha18 (stable 1.4.0 removed the expressive API, and the 1.5.0 alphas
need AGP 9.1 and compileSdk 37):
MaterialExpressiveTheme with the expressive motion scheme; the expressive slider (24 dp
track, 12 dp corners, 4 × 44 dp bar thumb) for parameters, colors, thickness and digits;
floating toolbars on the 2D and complex graphs; the loading indicator while "=" is working
and while the complex plot sharpens; segmented buttons for choices.

## Sliders and definitions
Any letter that isn't a coordinate gets a slider (−10 to 10 by default). Tap its value to type
a value and change the range; ▶ animates it across its range. A line of its own like a = 3
sets the letter instead (and hides its slider).

## More on the complex plane
x, y, r and θ can be used for ℜz, ℑz, |z| and arg z. An equation that's real on both sides
(|z − 1| = 2, x² + y² = 4) is drawn as a curve over the coloring, and a line with a contour
integral (∮ around |z − a| = r) draws its circle, with an arrow for its direction and its value.

## Exact complex values, and limits on size
ln, arg, asin, acos and √ give exact complex values where there's a closed form:
ln(−2) = ln 2 + πi, ln i = πi/2, arg(−1 + i) = 3π/4, asin 2 = π/2 + i ln(2 + √3) (the C99 /
Python convention), √(3 + 4i) = 2 + i. Exact powers stop at about 120 000 digits, answers over
1000 digits are shown in scientific notation, and running out of stack or memory gives a
message rather than a crash. −(a + b) keeps its brackets.

## Everyday details
- Graphs start empty. The hide-keyboard button above the keys (or pulling the handle down)
  hides the whole keypad; a keyboard button brings it back (in every mode).
- Delete a history item by swiping it sideways, or from its share menu.
- In graphs, long-press a function's color dot to pick any color, by sliders (HSV, RGB or OKLab) or typed as
  HSV, RGB, OKLab or hex, or one of eight standard ones; "Default" goes back to the theme's.
- Letters (i, e and π too) are italic in the math; capital Greek letters are upright.
- The letters tab has every Latin and Greek letter, including those with their own keys.
- Font fallback uses each font's real character table (FontCoverage.kt, regenerate with
  preview-render/font_coverage.py): Android's Paint.hasGlyph also counts system fonts, which
  made Greek letters silently come out in Roboto.

## 2D graph: kinds of line, polar coordinates, animation
Each line in the 2D graph's list is read by what you type (see graph/Curves.kt, PlotSpec):
- **y = f(x)**, or just f(x): a function, with zeros, extrema and crossings marked.
- **r = f(θ)**, or just f(θ): a polar curve, drawn over as many turns as it takes to close
  (up to 6; r = θ spirals keep going).
- **(f(t), g(t))**: a parametric curve; closed ones go round once, open ones use −10 ≤ t ≤ 10.
- **Any other equation in x and y** (x² + y² = 1, x = 2, y² = x³ − x): an implicit curve,
  by marching squares, skipping false crossings at poles.
- **An inequality** (y < x², x² + y² ≤ 9, 0 < x < 2): a shaded region; the edge is dashed
  for < and > and solid for ≤ and ≥.
The "+ y =" button opens templates for each kind, and while editing, chips above the keypad
type y, r, θ and t. Letters other than the plotting ones become sliders; **▶ on a slider
animates it**, bouncing between −10 and 10 (dragging it stops the animation).

**Polar coordinates** in both 2D views: the polar-grid toggle (bottom right) swaps the square
grid for circles and rays every 30° (labeled π/6, π/3… or degrees), and tapped points read
as r and θ with "Use r" / "Use θ". On the complex plane the same toggle overlays circles of
|z| and rays of arg z, readouts show both a + bi and |z|∠arg z (e.g. 1.4142∠π/4), and f can be
written with r and θ, which mean |z| and arg z.

## Vector calculus
A ∇ tab: ∂/∂x and ∂ⁿ/∂xⁿ (partial derivatives), ∬ and ∭ (nested integrals with the cursor
placed in the innermost one), ∇f (gradient), ∇·F (divergence), ∇×F (curl; the scalar version
in 2D), ∇²f (Laplacian), the Jacobian and Hessian matrices, and 2- and 3-component vectors.
Coordinates are x, y, z (those that appear). Multiple integrals are exact where possible
(∫₀¹∫₀ˣ xy dy dx = 1/8, the tetrahedron's volume 1/6); an inner integral with no closed form
is kept and the whole thing done numerically (∫₀¹∫₀¹ e^(−x²y²) dy dx ≈ 0.9059404763).

**Coordinate systems.** On the ∇ tab the Rad/Deg switch becomes a coordinate switch —
Cartesian (x, y, z), cylindrical (r, θ, z; plane polar when there's no z) and spherical
(r, θ, φ, with θ measured from the z-axis) — and ✎ picks the letters for the current system
(e.g. ρ, φ, z). ∇f, ∇·F, ∇×F and ∇²f use the general orthogonal-coordinate formulas with the
system's scale factors, and vector components are along its unit vectors, so in spherical
coordinates ∇(1/r) = (−1/r², 0, 0), ∇·(r², 0, 0) = 4r and ∇²(1/r) = 0. The Jacobian and
Hessian are the matrices of partial derivatives in the chosen letters.

**The ÷ key makes a fraction.** It never types a division sign. Whatever is just before the
cursor, back to the previous +, −, ×, comma, bracket or relation, moves up into the
numerator, so an implicit product like 2xy, 3x²y or 3(x + 1) sin x goes up whole, and the
cursor waits in the empty denominator. With nothing before it (at the start, or after an
operator) you get an empty fraction with the cursor on top; tap either box to move between
them. An explicit × still separates factors: a × 6 then ÷ gives a × 6/□.

**The previous answer.** The Ans key (end of the √x tab) inserts the last result. Pressing
+, −, ×, ÷, a power, !, % or mod straight after "=" continues from it (Ans + …), as on Google
Calculator, and tapping any answer in the history inserts that answer.

**Choosing the variable.** The letter in dx, ∂x and ∫ … dx is an editable box (x by
default): tap it, or press ← from the start of the body, delete it and type any single
letter, Greek included. Other letters are held constant, so ∫ 2xy dx = x²y while
∫ 2xy dy = xy², and ∂/∂y (x²y³) = 3x²y². While a derivative or integral is being edited, its
variable box is tinted to show it can be changed. In ∬ and ∭ each integral has its own
variable, so the order (dy dx or dx dy) is up to you.

## Complex plotting
A fourth mode (the Argand-diagram icon in the switcher) plots f(z) by domain coloring,
after samuelj.li's complex function plotter:
- **Color** is arg f(z) (red = positive real, then yellow, green, cyan, blue, magenta
  counterclockwise); **brightness** is log |f(z)|, so zeros are black and poles white. The
  number of times the colors cycle around a point is its order (a double zero cycles twice).
- A floating toolbar toggles **modulus bands** (a brightness step each time |f| doubles),
  **phase lines** (every 30° of arg f), and a **grid** where Re f and Im f are whole
  numbers (the image of the coordinate grid, which shows where f is conformal).
- **Loop tool**: draw a closed loop with a finger and it shows ∮ f(z) dz and the sum of
  residues inside (= ∮/2πi for a loop drawn counterclockwise). E.g. around i for 1/(z² + 1):
  ∮ = π, residues = −i/2.
- Tap a point to read z and f(z); drag and pinch to move and zoom; double-tap to reset.
- Letters other than z get **sliders**, e.g. f(z) = z − t.
- The number pad's x key types z in this mode. Rendering runs off the main thread: a
  coarse pass first so panning stays live, then a sharper one when the view settles.
- Every elementary function works on complex numbers (principal branches), plus Γ(z)
  (Lanczos) and ζ(s) (Borwein's algorithm with the functional equation), on the ℂ tab.
- In this mode the function keys start open on the ℂ tab, so ζ, Γ, Re, Im, conjugate and
  arg are one tap away. The CAS knows exact values: Γ(5/2) = 3√π/4,
  ζ(2) = π²/6, ζ(4) = π⁴/90, ζ(−1) = −1/12.
- "Graph this" on a calculator answer in z sends it here.

## Modes
A Material 3 Expressive button group at the top switches between them; the selected
button stretches into a wide filled pill.

- **Calculator** (calculator icon): everything below.
- **2D graphing** (line chart icon): y = f(x) for any number of functions, typed with the
  same keypad. Drag to move, pinch to zoom around your fingers, double-tap (or the
  focus button) to reset. Tap a curve to read a point; the tapped curve (or the one
  being edited) gets its zeros, maxima, minima, y-intercept and crossings with other
  curves marked, and tapping a mark shows its coordinates. Letters other than x become
  sliders (y = a·sin(bx) gets a and b). Lines break at jumps and gaps (tan x, √x), and
  odd roots of negatives stay real (∛−8 = −2). Tap a color dot to hide a curve.
- **3D graphing** (3D box icon): z = f(x, y) surfaces, shaded by height and lit, inside a
  box with labeled axes. Drag to turn, pinch to zoom, double-tap to reset; the range
  control sets x, y ∈ [−r, r]. Parameters get sliders here too.

Functions are symbolic first: they go through the CAS (so stored variables and exact
simplification apply), then get compiled to fast numeric code for drawing.

## Function tabs
Tabs are icons (TabIcons.kt: drawn on a 24×24 grid with 2 dp round strokes, to match
Material Symbols Rounded) and keys are math notation or icons. Every operation appears
once; long-press a key to see its name. Three rows show at a time; the "more" and
letters tabs scroll.

| # | Icon | Tab | Keys |
|---|---|---|---|
| 1 | √x | powers, roots, logs | π, e, eˣ, ×10ⁿ · x², xⁿ, √x, ⁿ√x · ln x, logₐ x, 10ⁿ, Ans |
| 2 | right triangle | trigonometry | sin, sin⁻¹, sinh, sinh⁻¹ (and cos, tan) |
| 3 | balance | algebra | x = ? (solve), =, comma, simplify · expand, factor, partial fractions, common denominator · <, >, ≤, ≥ |
| 4 | area under a curve | calculus | ∫f dx, d/dx, dⁿ/dxⁿ, lim · Taylor, y′ = f (dsolve), y′, ∞ · Σ, Π, n!, (n k) |
| 5 | ∇ | vector calculus | ∂/∂x, ∂ⁿ/∂xⁿ, ∬, ∭ · ∇f, ∇·F, ∇×F, ∇²f · J(F), H(f), 2- and 3-vectors |
| 6 | 2×2 matrix | linear algebra | matrix, A⁻¹, Aᵀ, \|A\| · λ, Av = λv, p(λ), tr A · rref, rk A, u·v, u×v |
| 7 | ℂ | complex numbers | i, ℜz, ℑz, z* · ∠z, e^(iθ), Γ(z), ζ(z) · z, w, ∮, Res |
| 8 | atom | physical constants | a list key with names, values and units, then all 55 constants below, four to a row (scrolls) |
| 9 | \|∨\| | more functions | \|x\|, ⌊x⌋, ⌈x⌉, ⌊x⌉ · gcd, lcm, a mod b, % · {x} (fractional part), sgn x, min, max |
| 10 | bell curve | statistics | n!, (n k), P(n, k), x̄ · med, s, σ, s² · φ, Φ, Φ⁻¹, Bin · comma, Σ, Pois, Bin≤ |
| 11 | Aα | letters | := then every Latin letter (lower and capital) and every Greek letter that looks different from Latin, six to a row |

x is on the number pad; e and π are on the √x tab; i, z and w are on the ℂ tab.

Limits and Taylor series are written x → a, and the display shows lim with x → a
underneath, as in a textbook.

Brackets are on the number pad's smart ( ) key.

## How it works
- `cas/` — the symbolic core: expressions (`Expr.kt`), automatic simplification
  (`Simplify.kt`), expand/factor/solve (`Algebra.kt`), calculus (`Calculus.kt`),
  matrices, numeric evaluation, printing.
- `editor/` — the 2D expression tree and cursor editing.
- `engine/` — turns the editor tree into expressions (`Evaluator.kt`) and results
  back into 2D rows (`Formatter.kt`).
- `graph/` — compiling expressions to numeric functions, 2D sampling with zeros and
  extrema, 3D surface sampling and projection (painter's algorithm).
- `ui/` — the math renderer (`MathView.kt`), keys, the calculator, the mode switcher
  and the two graph screens.

Everything except `ui/` is plain Kotlin; `./gradlew test` runs 562 tests covering the
CAS, the editor, the plotting engine, and that answers can be pasted back in and
re-evaluated.

Limits: no factoring of
polynomials with irreducible factors of degree 3 or more, and integrals the methods
above don't cover fall back to numbers (definite) or report "No antiderivative found".

## Build
Open the folder in Android Studio and run on a phone or emulator (Android 8+).
Colors follow the wallpaper on Android 12+, with an olive palette before that.

Fonts: the math display uses Computer Modern, LaTeX's typeface (MathJax's TeX fonts:
upright roman plus math italic for variables, and TeX's display-size ∫ and ∮). Everything else — keys, tabs, labels —
uses Google Sans Flex, always fully rounded (ROND 100), including Material dialogs, menus and
buttons (a rounded type scale in Theme.kt). Roboto covers any character neither has. All three are
under the SIL Open Font License (see the OFL files).
`preview-render/` draws the preview images outside Android. `examples.js` there is
generated by running the real engine on each example, so the pictured answers are the
engine's own output. `mathlayout.js` there is a
line-by-line port of `MathView.kt`'s layout rules, so the screenshots show exactly where
the app places exponents, limits, bars and brackets.

## Gestures and editing
- Pinch sideways on the 2D or complex graph to stretch only x, up and down for only y.
- Empty matrix cells count as 0.
- Backspace steps over the → of a limit and the circle under ∮ instead of deleting them.
- The plots' buttons (letter chips, Use x/y, Area, toolbar toggles, ±) tap like the keys.

## Building a release (.aab)
1. Choose the app's ID: in app/build.gradle.kts set `applicationId` to your own
   (e.g. com.yourname.cascalc). Play refuses com.example IDs, and it can't change later.
   Leave `namespace` as com.example.cas: it's the code's package, not the app's ID.
   If ANDROID_HOME and ANDROID_SDK_ROOT point to different places, build with
   `env -u ANDROID_SDK_ROOT ./gradlew bundleRelease`.
2. Make an upload key (once), in this folder:
   `keytool -genkeypair -v -keystore upload-keystore.jks -keyalg RSA -keysize 2048 -validity 10000 -alias upload`
3. Copy keystore.properties.example to keystore.properties and fill in the passwords.
   Back up the .jks file and the passwords: without them you can't publish updates.
4. Build: `./gradlew bundleRelease`
5. The bundle is app/build/outputs/bundle/release/app-release.aab.
For each new upload, raise `versionCode` in app/build.gradle.kts.

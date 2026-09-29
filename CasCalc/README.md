# Calculator (CAS) for Android

A phone calculator that looks and feels like Google Calculator — Material You colors,
Google Sans Flex, pill keys that square up when pressed — with the abilities of a
CAS calculator: a real 2D maths editor and exact answers.

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
in KeyHelp.kt (inline maths as \( … \)) and turned into the calculator's own maths tree by
engine/LatexParser.kt, so they're drawn in Computer Modern by the same renderer as answers:
fractions, roots, scripts, ∫ … dx, ∮, Σ, lim, binomials, matrices, Greek, and TeX's spacing
around operator names. A test checks every help formula uses only supported LaTeX. Constants
show their value, unit and whether they're exact.

## 3D graph additions
- Lines can be z = f(x, y) (or just f(x, y)) or **any equation in x, y and z**, drawn as an
  implicit surface by marching tetrahedra: x² + y² + z² = 4 is a sphere.
- **Limits:** − and + zoom; tapping the ranges opens a dialog for x, y and z (z can fit itself).
- **Tap the surface** to read (x, y, z) there (on z = f(x, y), z is computed exactly at x, y).
- Chips above the keypad type x, y and z; long-press a function's dot for its colour.

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
  off or unavailable), maths size, keypad size, expressive or standard motion, keep the screen on.
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
  **3D export** is drawn like pgfplots: back walls with a grid, viridis-coloured faceted
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
  it is and its licence.

## Latest changes (29 September)
- **Export graphs**: the share button on the 2D and complex toolbars exports the graph as PDF
  (the default), PNG, JPG or SVG, with the limits you type (the current view to start with)
  and light or dark colors; then Save (the system's save dialog) or Share. The graph is
  rebuilt as a list of shapes (graph/Scene.kt) at the new limits, so SVG and PDF are real
  vector files; the complex plane's colouring is rendered afresh and embedded as a picture.
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
- Letters (i, e and π too) are italic in the maths; capital Greek letters are upright.
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
grid for circles and rays every 30° (labelled π/6, π/3… or degrees), and tapped points read
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
A fourth mode (the Argand-diagram icon in the switcher) plots f(z) by domain colouring,
after samuelj.li's complex function plotter:
- **Colour** is arg f(z) (red = positive real, then yellow, green, cyan, blue, magenta
  anticlockwise); **brightness** is log |f(z)|, so zeros are black and poles white. The
  number of times the colours cycle around a point is its order (a double zero cycles twice).
- A floating toolbar toggles **modulus bands** (a brightness step each time |f| doubles),
  **phase lines** (every 30° of arg f), and a **grid** where Re f and Im f are whole
  numbers (the image of the coordinate grid, which shows where f is conformal).
- **Loop tool**: draw a closed loop with a finger and it shows ∮ f(z) dz and the sum of
  residues inside (= ∮/2πi for a loop drawn anticlockwise). E.g. around i for 1/(z² + 1):
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
  odd roots of negatives stay real (∛−8 = −2). Tap a colour dot to hide a curve.
- **3D graphing** (3D box icon): z = f(x, y) surfaces, shaded by height and lit, inside a
  box with labelled axes. Drag to turn, pinch to zoom, double-tap to reset; the range
  control sets x, y ∈ [−r, r]. Parameters get sliders here too.

Functions are symbolic first: they go through the CAS (so stored variables and exact
simplification apply), then get compiled to fast numeric code for drawing.

## Function tabs
Tabs are icons (TabIcons.kt: drawn on a 24×24 grid with 2 dp round strokes, to match
Material Symbols Rounded) and keys are maths notation or icons. Every operation appears
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
- `ui/` — the maths renderer (`MathView.kt`), keys, the calculator, the mode switcher
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

Fonts: the maths display uses Computer Modern, LaTeX's typeface (MathJax's TeX fonts:
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

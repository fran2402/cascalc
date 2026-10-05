package com.example.cas.ui

/**
 * What long-pressing a key shows: its name, the formula, a line of theory and
 * how to use it here. [formula] is LaTeX (a newline starts another line); [about] and [usage] are text with
 * inline LaTeX between \( and \). Both are drawn by the calculator's own
 * renderer via [com.example.cas.engine.LatexParser]. Keyed by spoken name (Keys.kt).
 */
data class KeyHelp(val title: String, val formula: String = "", val about: String = "", val usage: String = "", val link: String? = null)

object KeyHelps {
    private fun h(title: String, formula: String, about: String, usage: String) = KeyHelp(title, formula, about, usage)

    private val all: Map<String, KeyHelp> = mapOf(
        // ---- Powers, roots, logs
        "pi" to h("π", """\pi = 3.14159\,26535\ldots""", """The ratio of a circle's circumference to its diameter.""", """Kept exact: \(\sin\frac{\pi}{6} = \frac{1}{2}\). Tap ≈ on an answer for the decimal."""),
        "e" to h("Euler's number", """e = \lim_{n\to\infty} \left(1 + \frac{1}{n}\right)^n = 2.71828\ldots""", """The base of natural logarithms: \(\frac{d}{dx} e^x = e^x\).""", """Kept exact: \(\ln e = 1\)."""),
        "e to the power" to h("Exponential", """e^x = \sum_{n=0}^{\infty} \frac{x^n}{n!}""", """Grows at a rate equal to itself.""", """Type the exponent in the raised box, then → to leave it."""),
        "times ten to the power" to h("Scientific notation", """a \times 10^{n}""", """Writes very large or very small numbers.""", """For example \(6.02 \times 10^{23}\)."""),
        "squared" to h("Square", """x^2 = x \cdot x""", "", """Squares what's before the cursor."""),
        "power" to h("Power", """x^n""", """Repeated multiplication; fractional powers are roots: \(x^{1/2} = \sqrt{x}\).""", """Raises what's before the cursor; type the exponent, then →."""),
        "square root" to h("Square root", """\sqrt{x} = x^{1/2}""", """The non-negative number whose square is \(x\); \(\sqrt{-4} = 2i\).""", """Kept exact and simplified: \(\sqrt{8} = 2\sqrt{2}\)."""),
        "nth root" to h("nth root", """\sqrt[n]{x} = x^{1/n}""", """Odd roots of negatives are real: \(\sqrt[3]{-8} = -2\).""", """Fill the small box with \(n\), then the number under the root."""),
        "natural log" to h("Natural logarithm", """\ln x = \log_e x""", """The inverse of \(e^x\), with \(\ln(ab) = \ln a + \ln b\).""", """Logs of negatives are complex: \(\ln(-1) = \pi i\)."""),
        "log base a" to h("Logarithm", """\log_a x = \frac{\ln x}{\ln a}""", """The power of \(a\) that gives \(x\).""", """Fill the base in the small box: \(\log_2 8 = 3\)."""),
        "ten to the power" to h("Power of ten", """10^{n}""", "", """Type the exponent in the raised box."""),
        "previous answer" to h("Previous answer", """\text{Ans}""", """The last result.""", """An operator pressed straight after = also starts from it."""),
        // ---- Trigonometry
        "sin" to h("Sine", """\sin\theta = \frac{\text{opposite}}{\text{hypotenuse}}""", """Period \(2\pi\), and \(\sin^2\theta + \cos^2\theta = 1\).""", """Rad/Deg above the keys sets the angle unit. Exact at multiples of 30° and 45°."""),
        "cos" to h("Cosine", """\cos\theta = \frac{\text{adjacent}}{\text{hypotenuse}}""", """\(\cos\theta = \sin\left(\theta + \frac{\pi}{2}\right)\).""", """Rad/Deg sets the angle unit."""),
        "tan" to h("Tangent", """\tan\theta = \frac{\sin\theta}{\cos\theta}""", """Undefined where \(\cos\theta = 0\), at \(\theta = \frac{\pi}{2} + k\pi\).""", """Rad/Deg sets the angle unit."""),
        "inverse sin" to h("Arcsine", """\text{asin}\,x = \theta \quad \text{where} \quad \sin\theta = x
-\frac{\pi}{2} \le \theta \le \frac{\pi}{2}""", """Defined for \(-1 \le x \le 1\) (complex outside).""", """Answers in the current angle unit."""),
        "inverse cos" to h("Arccosine", """\text{acos}\,x = \theta \quad \text{where} \quad \cos\theta = x
0 \le \theta \le \pi""", """The angle whose cosine is \(x\), for \(-1 \le x \le 1\) (complex outside).""", """Answers in the current angle unit."""),
        "inverse tan" to h("Arctangent", """\text{atan}\,x = \theta \quad \text{where} \quad \tan\theta = x
-\frac{\pi}{2} < \theta < \frac{\pi}{2}""", """The angle whose tangent is \(x\); defined for every real \(x\).""", """Answers in the current angle unit."""),
        "hyperbolic sin" to h("Hyperbolic sine", """\sinh x = \frac{e^x - e^{-x}}{2}""", """\(\cosh^2 x - \sinh^2 x = 1\).""", ""),
        "hyperbolic cos" to h("Hyperbolic cosine", """\cosh x = \frac{e^x + e^{-x}}{2}""", """The shape of a hanging chain.""", ""),
        "hyperbolic tan" to h("Hyperbolic tangent", """\tanh x = \frac{\sinh x}{\cosh x}""", """Runs from \(-1\) to \(1\).""", ""),
        "inverse hyperbolic sin" to h("Inverse hyperbolic sine", """\text{asinh}\,x = \ln\left(x + \sqrt{x^2 + 1}\right)""", """Undoes \(\sinh\); defined for every real \(x\).""", ""),
        "inverse hyperbolic cos" to h("Inverse hyperbolic cosine", """\text{acosh}\,x = \ln\left(x + \sqrt{x^2 - 1}\right), \quad x \ge 1""", """Undoes \(\cosh\) on \(x \ge 1\), giving the value \(\ge 0\).""", ""),
        "inverse hyperbolic tan" to h("Inverse hyperbolic tangent", """\text{atanh}\,x = \frac{1}{2}\ln\frac{1 + x}{1 - x}, \quad |x| < 1""", """Undoes \(\tanh\) for \(-1 < x < 1\).""", ""),
        // ---- Algebra
        "solve" to h("Solve", """\text{solve}(x^2 = 2,\ x)
\to\ x = \pm\sqrt{2}""", """Exact where possible (roots, surds, complex numbers), numerical otherwise. Several equations separated by commas are solved together; an inequality gives intervals.""", """Equation in the first box, the unknown in the second."""),
        "equals sign" to h("Equals sign", """a = b""", """Makes an equation for solve, the graphs or dsolve.""", ""),
        "comma" to h("Comma", """a,\ b""", """Separates equations in a system, unknowns, list values and function arguments.""", ""),
        "simplify" to h("Simplify", """\frac{x^2 - 1}{x - 1} = x + 1""", """Cancels common factors and combines fractions.""", ""),
        "expand" to h("Expand", """(a + b)^2 = a^2 + 2ab + b^2""", """Multiplies out brackets.""", ""),
        "factor" to h("Factor", """x^2 - 1 = (x - 1)(x + 1)""", """Splits polynomials into factors with rational roots, and takes out common factors.""", ""),
        "partial fractions" to h("Partial fractions", """\frac{1}{x^2 - 1} = \frac{1}{2(x - 1)} - \frac{1}{2(x + 1)}""", """Splits a fraction into simpler ones, as used in integration.""", ""),
        "common denominator" to h("Common denominator", """\frac{1}{x} + \frac{1}{x + 1} = \frac{2x + 1}{x(x + 1)}""", """Combines fractions into one.""", ""),
        "less than" to h("Less than", """a < b""", """With solve: \(x^2 < 4\) gives \(-2 < x < 2\). In the 2D graph it shades a region, with a dashed edge.""", ""),
        "greater than" to h("Greater than", """a > b""", """With solve, or to shade a region in the 2D graph.""", ""),
        "less than or equal to" to h("Less than or equal to", """a \le b""", """Like \(<\) but includes the boundary, drawn solid in the graph.""", ""),
        "greater than or equal to" to h("Greater than or equal to", """a \ge b""", "", ""),
        // ---- Calculus
        "integral" to h("Integral", """\int_a^b f(x)\,dx = F(b) - F(a)
F' = f""", """The area under \(f\). Leave the limits empty for an antiderivative; \(\infty\) is allowed as a limit.""", """Tap the \(x\) in \(dx\) to change the variable. Exact when possible, numerical otherwise."""),
        "derivative" to h("Derivative", """\frac{d}{dx} f(x) = \lim_{h\to 0} \frac{f(x + h) - f(x)}{h}""", """The rate of change, or slope, of \(f\).""", """Fill the point box to evaluate at \(x = a\); tap \(dx\) to change the variable."""),
        "higher derivative" to h("Higher derivative", """\frac{d^n}{dx^n} f""", """The derivative taken \(n\) times.""", """Set \(n\) in the small box on \(d\)."""),
        "limit" to h("Limit", """\lim_{x\to a} f(x)""", """The value \(f\) approaches near \(a\). Tries substitution, cancelling and L'Hôpital's rule, then numbers.""", """Change \(a\) under lim; \(\infty\) is allowed."""),
        "Taylor series" to h("Taylor series", """f(x) \approx \sum_{n=0}^{N} \frac{f^{(n)}(a)}{n!}(x - a)^n""", """The polynomial that best matches \(f\) near \(a\).""", """taylor(f, x → a, order)."""),
        "solve differential equation" to h("Differential equation", """\text{dsolve}(y'' + y = 0,\ y(0) = 1,\ y'(0) = 0)
\to\ y = \cos x""", """First-order linear and separable equations; second-order ones with constant coefficients. Without conditions the answer has constants \(C_1, C_2\).""", """Use the \(y'\) and \(y''\) keys; add conditions after commas."""),
        "y prime" to h("First derivative of y", """y' = \frac{dy}{dx}""", """The derivative of the unknown function in dsolve.""", ""),
        "y double prime" to h("Second derivative of y", """y'' = \frac{d^2 y}{dx^2}""", """The second derivative in dsolve.""", ""),
        "infinity" to h("Infinity", """\infty""", """For limits and integrals: \(\lim_{x\to\infty}\), \(\int_0^\infty\).""", ""),
        "sum" to h("Sum", """\sum_{k=1}^{n} k^2 = \frac{n(n + 1)(2n + 1)}{6}""", """Adds \(f(k)\) for \(k = a, \ldots, b\), with closed forms for polynomial terms.""", ""),
        "product" to h("Product", """\prod_{k=1}^{n} k = n!""", """Multiplies \(f(k)\) for \(k = a, \ldots, b\).""", ""),
        "one-sided limit" to h("One-sided limit", """\lim_{x\to 0^+} \frac{1}{x} = \infty""", """The limit from the right (+) or, typing −, from the left.""", ""),
        // ---- Vector calculus
        "partial derivative" to h("Partial derivative", """\frac{\partial f}{\partial x}""", """Differentiates with the other letters held constant.""", """Tap the \(x\) in \(\partial x\) to change the variable."""),
        "higher partial derivative" to h("Higher partial derivative", """\frac{\partial^n f}{\partial x^n}""", "", """Set \(n\) in the small box."""),
        "double integral" to h("Double integral", """\int_0^1 \int_0^x f(x, y)\,dy\,dx""", """Integrates over a region; inner limits may depend on the outer variable.""", """Each integral has its own limits and variable."""),
        "triple integral" to h("Triple integral", """\iiint f\,dz\,dy\,dx""", """Integrates over a volume.""", ""),
        "gradient" to h("Gradient", """\nabla f = \begin{bmatrix} \partial f/\partial x \\ \partial f/\partial y \\ \partial f/\partial z \end{bmatrix}""", """Points the way \(f\) increases fastest.""", """The switch above the keys picks Cartesian, cylindrical or spherical coordinates."""),
        "divergence" to h("Divergence", """\nabla \cdot F = \frac{\partial P}{\partial x} + \frac{\partial Q}{\partial y} + \frac{\partial R}{\partial z}""", """How much a field spreads out from a point.""", """\(F\) is a vector of its components \(P, Q, R\)."""),
        "curl" to h("Curl", """\nabla \times F = \begin{bmatrix} R_y - Q_z \\ P_z - R_x \\ Q_x - P_y \end{bmatrix}""", """How much a field rotates around a point (a number in 2D).""", ""),
        "Laplacian" to h("Laplacian", """\nabla^2 f = \frac{\partial^2 f}{\partial x^2} + \frac{\partial^2 f}{\partial y^2} + \frac{\partial^2 f}{\partial z^2}""", """\(\nabla^2 f = 0\) means \(f\) is harmonic.""", ""),
        "Jacobian matrix" to h("Jacobian", """J = \left[\frac{\partial F_i}{\partial x_j}\right]""", """The matrix of partial derivatives of a vector function.""", ""),
        "Hessian matrix" to h("Hessian", """H = \left[\frac{\partial^2 f}{\partial x_i \partial x_j}\right]""", """Second derivatives; they tell maxima from minima.""", ""),
        "vector with 2 components" to h("Vector with 2 components", """\begin{bmatrix} a \\ b \end{bmatrix}""", "", """Fill the two boxes."""),
        "vector with 3 components" to h("Vector with 3 components", """\begin{bmatrix} a \\ b \\ c \end{bmatrix}""", "", """Fill the three boxes."""),
        // ---- Linear algebra
        "matrix" to h("Matrix", """A = \begin{bmatrix} a & b \\ c & d \end{bmatrix}""", """A grid of numbers or expressions.""", """Pick the size, then fill the boxes."""),
        "inverse" to h("Inverse", """A A^{-1} = I
\begin{bmatrix} a & b \\ c & d \end{bmatrix}^{-1} = \frac{1}{ad - bc}\begin{bmatrix} d & -b \\ -c & a \end{bmatrix}""", """Exists when \(\det A \ne 0\).""", """Put it after a matrix."""),
        "transpose" to h("Transpose", """(A^\top)_{ij} = A_{ji}""", """Swaps rows and columns.""", ""),
        "determinant" to h("Determinant", """\det \begin{bmatrix} a & b \\ c & d \end{bmatrix} = ad - bc""", """The factor by which \(A\) scales areas and volumes.""", ""),
        "eigenvalues" to h("Eigenvalues", """\det(A - \lambda I) = 0""", """The numbers \(\lambda\) with \(Av = \lambda v\) for some \(v \ne 0\).""", ""),
        "eigenvectors" to h("Eigenvectors", """A v = \lambda v""", """The directions \(A\) only stretches.""", ""),
        "characteristic polynomial" to h("Characteristic polynomial", """p(\lambda) = \det(\lambda I - A)""", """Its roots are the eigenvalues.""", ""),
        "trace" to h("Trace", """\text{tr}\,A = \sum_{i=1}^{n} a_{ii}""", """The sum of the diagonal, and also of the eigenvalues.""", ""),
        "reduced row echelon form" to h("Reduced row echelon form", """\text{rref}\begin{bmatrix} 1 & 2 \\ 3 & 4 \end{bmatrix} = \begin{bmatrix} 1 & 0 \\ 0 & 1 \end{bmatrix}""", """Gauss–Jordan elimination; shows the rank and the solutions of a system.""", ""),
        "rank" to h("Rank", """\text{rk}\,A""", """The number of independent rows.""", ""),
        "dot product" to h("Dot product", """u \cdot v = \sum_i u_i v_i = |u||v|\cos\theta""", """A number: the lengths times the cosine of the angle between them; zero when they're perpendicular.""", """dot(u, v)"""),
        "cross product" to h("Cross product", """u \times v = \begin{bmatrix} u_2 v_3 - u_3 v_2 \\ u_3 v_1 - u_1 v_3 \\ u_1 v_2 - u_2 v_1 \end{bmatrix}""", """Perpendicular to both, with length \(|u||v|\sin\theta\).""", """cross(u, v), for 3-vectors."""),
        // ---- Complex numbers
        "i, the imaginary unit" to h("Imaginary unit", """i^2 = -1""", """A square root of \(-1\); every complex number is \(a + bi\).""", ""),
        "real part" to h("Real part", """\Re(a + bi) = a""", """The part of a complex number without \(i\).""", ""),
        "imaginary part" to h("Imaginary part", """\Im(a + bi) = b""", """The number multiplying \(i\), itself real.""", ""),
        "conjugate" to h("Conjugate", """(a + bi)^* = a - bi""", """Reflects in the real axis, and \(z z^* = |z|^2\).""", ""),
        "argument" to h("Argument", """z = |z|\,e^{i \arg z}, \quad -\pi < \arg z \le \pi""", """The angle from the positive real axis.""", ""),
        "e to the i theta" to h("Euler's formula", """e^{i\theta} = \cos\theta + i\sin\theta""", """The point at angle \(\theta\) on the unit circle.""", ""),
        "gamma function" to h("Gamma function", """\Gamma(z) = \int_0^\infty t^{z - 1} e^{-t}\,dt
\Gamma(n) = (n - 1)!""", """Extends factorials to every complex number except \(0, -1, -2, \ldots\)""", """Exact at whole and half-whole numbers: \(\Gamma\left(\frac{1}{2}\right) = \sqrt{\pi}\)."""),
        "Riemann zeta function" to h("Riemann zeta function", """\zeta(s) = \sum_{n=1}^{\infty} \frac{1}{n^s}""", """Extended to every \(s \ne 1\); its non-trivial zeros are conjectured to lie on \(\Re s = \frac{1}{2}\).""", """Exact at even whole numbers: \(\zeta(2) = \frac{\pi^2}{6}\)."""),
        "z" to h("z", """z = x + iy""", """The complex variable; the number pad's variable key types \(z\) in the complex plotter.""", ""),
        "w" to h("w", """w = f(z)""", """A second complex variable.""", ""),
        "contour integral" to h("Contour integral", """\oint_{|z - a| = r} f(z)\,dz
= 2\pi i \sum_k \operatorname{Res}_{z = a_k} f""", """Around a circle, counterclockwise; equals \(2\pi i\) times the residues inside.""", """Edit the circle \(|z - a| = r\) underneath."""),
        "residue" to h("Residue", """\operatorname{Res}_{z=a} f = \frac{1}{2\pi i}\oint f(z)\,dz""", """The coefficient of \(\frac{1}{z - a}\) in the Laurent series of \(f\).""", """Set the point \(a\) underneath."""),
        // ---- More functions
        "absolute value" to h("Absolute value", """|x| = \sqrt{x^2}""", """The distance from 0; for vectors, the length.""", ""),
        "floor" to h("Floor", """\lfloor x \rfloor = \max\{n : n \le x\}""", """The largest whole number at most \(x\).""", ""),
        "ceiling" to h("Ceiling", """\lceil x \rceil = \min\{n : n \ge x\}""", """The smallest whole number at least \(x\).""", ""),
        "round" to h("Round", """\lfloor x \rceil = \lfloor x + \tfrac{1}{2} \rfloor""", """The nearest whole number.""", ""),
        "greatest common divisor" to h("Greatest common divisor", """\gcd(12, 18) = 6""", """The largest number dividing both.""", ""),
        "least common multiple" to h("Least common multiple", """\text{lcm}(4, 6) = 12""", """The smallest number both divide.""", ""),
        "mod" to h("Remainder", """a \bmod b = a - b\left\lfloor \frac{a}{b} \right\rfloor""", """The remainder after dividing \(a\) by \(b\), with the sign of \(b\): \(7 \bmod 3 = 1\).""", ""),
        "percent" to h("Percent", """x\% = \frac{x}{100}""", """Hundredths: \(50\% = 0.5\).""", ""),
        "fractional part" to h("Fractional part", """\{x\} = x - \lfloor x \rfloor""", """What's left after the whole part: \(\{2.75\} = 0.75\).""", ""),
        "sign" to h("Sign", """\text{sgn}\,x = \frac{x}{|x|}""", """\(-1\), \(0\) or \(1\).""", ""),
        "minimum" to h("Minimum", """\min(a, b)""", """The smallest of the values.""", """Any number of values, separated by commas."""),
        "maximum" to h("Maximum", """\max(a, b)""", """The largest of the values.""", """Any number of values, separated by commas."""),
        // ---- Statistics
        "factorial" to h("Factorial", """n! = 1 \cdot 2 \cdots n, \quad 0! = 1""", """The number of orderings of \(n\) things.""", """Put it after a number."""),
        "n choose k" to h("Combinations", """\binom{n}{k} = \frac{n!}{k!\,(n - k)!}""", """Ways to choose \(k\) of \(n\), order not mattering.""", ""),
        "permutations" to h("Permutations", """P(n, k) = \frac{n!}{(n - k)!}""", """Ways to choose \(k\) of \(n\) in order.""", ""),
        "mean" to h("Mean", """\bar{x} = \frac{1}{n}\sum_{i=1}^{n} x_i""", """The average.""", """Type the values with commas: mean(2, 4, 4, 5)."""),
        "median" to h("Median", """\text{med}(x_1, \ldots, x_n)""", """The middle value once sorted (the mean of the two middle ones for even \(n\)).""", ""),
        "sample standard deviation" to h("Sample standard deviation", """s = \sqrt{\frac{1}{n - 1}\sum_{i=1}^{n} (x_i - \bar{x})^2}""", """The spread of a sample.""", ""),
        "population standard deviation" to h("Population standard deviation", """\sigma = \sqrt{\frac{1}{n}\sum_{i=1}^{n} (x_i - \bar{x})^2}""", """The spread of a whole population.""", ""),
        "sample variance" to h("Sample variance", """s^2 = \frac{1}{n - 1}\sum_{i=1}^{n} (x_i - \bar{x})^2""", """How spread out a sample is: the mean squared distance from the mean, dividing by \(n - 1\).""", ""),
        "normal density" to h("Normal density", """\varphi(x;\ \mu, \sigma) = \frac{1}{\sigma\sqrt{2\pi}}\, e^{-\frac{(x - \mu)^2}{2\sigma^2}}""", """The bell curve.""", """normpdf(x, μ, σ). It's a formula, so it can be graphed."""),
        "normal distribution function" to h("Normal distribution", """\Phi(x;\ \mu, \sigma)
= \frac{1}{2}\left(1 + \text{erf}\,\frac{x - \mu}{\sigma\sqrt{2}}\right)""", """The probability \(P(X \le x)\).""", """normcdf(x, μ, σ)."""),
        "inverse normal" to h("Inverse normal", """\Phi^{-1}(p) = z, \quad \Phi(z) = p""", """Critical values: \(\Phi^{-1}(0.975) \approx 1.96\).""", ""),
        "binomial probability" to h("Binomial probability", """P(X = k) = \binom{n}{k} p^k (1 - p)^{n - k}""", """\(k\) successes in \(n\) independent trials.""", """Bin(n, p, k)."""),
        "cumulative binomial probability" to h("Cumulative binomial", """P(X \le k) = \sum_{i=0}^{k} \binom{n}{i} p^i (1 - p)^{n - i}""", """The chance of at most \(k\) successes in \(n\) trials, each with probability \(p\).""", ""),
        "Poisson probability" to h("Poisson probability", """P(X = k) = \frac{\lambda^k e^{-\lambda}}{k!}""", """Counts of rare events at rate \(\lambda\).""", ""),
        "sum of a list" to h("Sum of a list", """\sum_{i=1}^{n} x_i""", """Adds all the values.""", """Values separated by commas."""),
        "comma for lists" to h("Comma", """x_1,\ x_2,\ \ldots""", """Separates list values.""", ""),
        // ---- Number pad
        "all clear" to h("All clear", "", """Clears the input. Undo brings it back.""", ""),
        "brackets" to h("Brackets", """(\ )""", """Opens or closes a bracket, whichever makes sense here.""", ""),
        "divide" to h("Divide", """\frac{a}{b}""", """Makes a fraction; what's before the cursor moves up into the numerator.""", ""),
        "backspace" to h("Backspace", "", """Deletes; hold to clear everything.""", ""),
        "enter" to h("Enter", """2 + 3 = 5""", """Works out the answer; in the graphs, finishes the line.""", ""),
        "equals sign" to h("Equals sign", """a = b""", """In the graphs: makes an equation, like \(x^2 + y^2 = 4\) or \(f(x) = x^2\).""", ""),
        "x" to h("x", """2x + 3x = 5x""", """The main variable, for solving, calculus and the 2D graph (\(z\) in the complex plotter).""", """Other letters are on the Aα tab."""),
        "times" to h("Times", """a \times b""", """Next to each other, letters and brackets multiply anyway: \(2x\), \(3(x + 1)\).""", ""),
        "minus" to h("Minus", """a - b""", """Subtracts; at the start it makes a number negative.""", ""),
        "plus" to h("Plus", """a + b""", """Adds.""", ""),
        "point" to h("Decimal point", """3.14""", """Decimals make the answer a decimal; use fractions to stay exact.""", ""),
        "sine integral" to h("Sine integral", """\operatorname{Si}(x) = \int_0^x \frac{\sin t}{t}\,dt""", """The antiderivative of \(\frac{\sin x}{x}\); it tends to \(\frac{\pi}{2}\).""", """Integrals like \(\int \frac{\sin 3x}{x}\,dx\) come out in it."""),
        "cosine integral" to h("Cosine integral", """\operatorname{Ci}(x) = \gamma + \ln x + \int_0^x \frac{\cos t - 1}{t}\,dt""", """An antiderivative of \(\frac{\cos x}{x}\); it tends to 0.""", ""),
        "exponential integral" to h("Exponential integral", """\operatorname{Ei}(x) = -\int_{-x}^{\infty} \frac{e^{-t}}{t}\,dt""", """An antiderivative of \(\frac{e^x}{x}\).""", ""),
        "logarithmic integral" to h("Logarithmic integral", """\operatorname{li}(x) = \int_0^x \frac{dt}{\ln t}""", """About the number of primes up to \(x\).""", ""),
        "error function" to h("Error function", """\operatorname{erf}(x) = \frac{2}{\sqrt{\pi}} \int_0^x e^{-t^2}\,dt""", """The normal distribution's integral: it runs from \(-1\) to 1, and \(\Phi(x) = \frac{1}{2}\left(1 + \operatorname{erf}\frac{x}{\sqrt{2}}\right)\).""", """Integrals of \(e^{-x^2}\) come out in it."""),
        "digamma function" to h("Digamma function", """\psi(x) = \frac{\Gamma'(x)}{\Gamma(x)}""", """The derivative of \(\ln \Gamma\); \(\psi(1) = -\gamma\), Euler's constant.""", """digamma(x)."""),
        "Hurwitz zeta function" to h("Hurwitz zeta function", """\zeta(s, a) = \sum_{n=0}^{\infty} \frac{1}{(n + a)^s}""", """Riemann's \(\zeta\) shifted by \(a\): \(\zeta(s, 1) = \zeta(s)\).""", """hurwitz(s, a), for \(s > 1\)."""),
        "hyperbolic sine integral" to h("Hyperbolic sine integral", """\operatorname{Shi}(x) = \int_0^x \frac{\sinh t}{t}\,dt""", """The antiderivative of \(\frac{\sinh x}{x}\).""", ""),
        "hyperbolic cosine integral" to h("Hyperbolic cosine integral", """\operatorname{Chi}(x) = \gamma + \ln x + \int_0^x \frac{\cosh t - 1}{t}\,dt""", """An antiderivative of \(\frac{\cosh x}{x}\).""", ""),
        "secant" to h("Secant", """\sec x = \frac{1}{\cos x}""", """One over the cosine.""", """In degrees when Deg is on."""),
        "cosecant" to h("Cosecant", """\csc x = \frac{1}{\sin x}""", """One over the sine.""", """"""),
        "cotangent" to h("Cotangent", """\cot x = \frac{\cos x}{\sin x}""", """One over the tangent.""", """"""),
        "hypotenuse" to h("Hypotenuse", """\operatorname{hypot}(x, y) = \sqrt{x^2 + y^2}""", """The length of the hypotenuse, or the distance from the origin to \((x, y)\).""", """hypot(3, 4) = 5."""),
        "two-argument arctangent" to h("Two-argument arctangent", """\operatorname{atan2}(y, x) = \arg(x + iy)""", """The angle of the point \((x, y)\), from \(-\pi\) to \(\pi\): unlike \(\arctan\frac{y}{x}\), it knows the quadrant.""", """atan2(y, x), y first."""),
        "inverse secant" to h("Inverse secant", """\operatorname{arcsec} x = \arccos\frac{1}{x}""", """The angle whose secant is \(x\), for \(|x| \ge 1\).""", """"""),
        "inverse cosecant" to h("Inverse cosecant", """\operatorname{arccsc} x = \arcsin\frac{1}{x}""", """The angle whose cosecant is \(x\), for \(|x| \ge 1\).""", """"""),
        "inverse cotangent" to h("Inverse cotangent", """\operatorname{arccot} x = \arctan\frac{1}{x}""", """The angle whose cotangent is \(x\).""", """"""),
        "sinc function" to h("Sinc", """\operatorname{sinc} x = \frac{\sin x}{x}""", """With \(\operatorname{sinc} 0 = 1\); the shape of a diffraction pattern.""", """"""),
        "hyperbolic secant" to h("Hyperbolic secant", """\operatorname{sech} x = \frac{1}{\cosh x}""", """A bell-shaped curve, the shape of a soliton.""", """"""),
        "hyperbolic cosecant" to h("Hyperbolic cosecant", """\operatorname{csch} x = \frac{1}{\sinh x}""", """One over the hyperbolic sine.""", """"""),
        "hyperbolic cotangent" to h("Hyperbolic cotangent", """\operatorname{coth} x = \frac{\cosh x}{\sinh x}""", """One over the hyperbolic tangent.""", """"""),
        "inverse hyperbolic secant" to h("Inverse hyperbolic secant", """\operatorname{arsech} x = \operatorname{arcosh}\frac{1}{x}""", """For \(0 < x \le 1\).""", """"""),
        "inverse hyperbolic cosecant" to h("Inverse hyperbolic cosecant", """\operatorname{arcsch} x = \operatorname{arsinh}\frac{1}{x}""", """The inverse of \(\operatorname{csch}\).""", """"""),
        "inverse hyperbolic cotangent" to h("Inverse hyperbolic cotangent", """\operatorname{arcoth} x = \operatorname{artanh}\frac{1}{x}""", """For \(|x| > 1\).""", """"""),
        "Heaviside step" to h("Heaviside step", """H(x) = \begin{cases} 0 & x < 0 \\ 1 & x > 0 \end{cases}""", """Switches on at 0 (and is \(\frac{1}{2}\) there).""", """heaviside(x − a) switches on at \(a\)."""),
        "rectangle function" to h("Rectangle function", """\operatorname{rect}(x) = H\left(\tfrac{1}{2} - |x|\right)""", """1 for \(|x| < \frac{1}{2}\), 0 outside: a box of width 1.""", """"""),
        "triangle function" to h("Triangle function", """\operatorname{tri}(x) = \max(1 - |x|,\ 0)""", """A peak of height 1 at 0, down to 0 at \(\pm 1\).""", """"""),
        "ramp function" to h("Ramp", """\operatorname{ramp}(x) = \max(x, 0)""", """0 for negative \(x\), then \(x\) (ReLU).""", """"""),
        "pulse" to h("Pulse", """\operatorname{pulse}(x, a, b) = H(x - a) - H(x - b)""", """1 between \(a\) and \(b\), 0 elsewhere.""", """pulse(x, a, b)."""),
        "sawtooth wave" to h("Sawtooth wave", """\operatorname{saw}(x) = x - \lfloor x \rfloor""", """Rises from 0 to 1, then drops back, every unit of \(x\).""", """Stretch it: sawtooth(x/T) has period \(T\)."""),
        "square wave" to h("Square wave", """\operatorname{sq}(x) = \operatorname{sgn}\sin 2\pi x""", """\(+1\) for the first half of each period of 1, \(-1\) for the second.""", """"""),
        "triangle wave" to h("Triangle wave", """\operatorname{tw}(x) = 4\left|x - \left\lfloor x + \tfrac{1}{2} \right\rfloor\right| - 1""", """Zig-zags between \(-1\) and 1 with period 1.""", """"""),
        "logistic sigmoid" to h("Logistic sigmoid", """\sigma(x) = \frac{1}{1 + e^{-x}}""", """An S-curve from 0 to 1, \(\frac{1}{2}\) at 0: logistic growth, and neural networks.""", """"""),
        "softplus" to h("Softplus", """\operatorname{softplus}(x) = \ln(1 + e^{x})""", """A smooth ramp: its derivative is the sigmoid.""", """"""),
        "clamp" to h("Clamp", """\operatorname{clamp}(x, a, b) = \min(\max(x, a),\ b)""", """\(x\) kept between \(a\) and \(b\).""", """clamp(x, a, b), with \(a \le b\)."""),
        "linear interpolation" to h("Linear interpolation", """\operatorname{lerp}(a, b, t) = a + (b - a)\,t""", """\(a\) at \(t = 0\), \(b\) at \(t = 1\), in proportion between.""", """"""),
        "smoothstep" to h("Smoothstep", """S(x) = 3u^2 - 2u^3,\quad u = \operatorname{clamp}(x, 0, 1)""", """Goes from 0 to 1 between \(x = 0\) and 1, flat at both ends.""", """"""),
        "Gaussian" to h("Gaussian", """g(x) = e^{-x^2}""", """The bell curve, peak 1 at 0.""", """normpdf on the Statistics tab is the normalized one."""),
        "wrap" to h("Wrap", """\operatorname{wrap}(x, a, b) = x - (b - a)\left\lfloor \frac{x - a}{b - a} \right\rfloor""", """\(x\) brought into the range from \(a\) to \(b\) by whole periods, as angles wrap around.""", """wrap(x, a, b)."""),
        "is prime" to h("Is prime", """\operatorname{isprime}(n) \in \{0, 1\}""", """1 if \(n\) is prime, 0 if not.""", """Large numbers are tested with Miller–Rabin."""),
        "next prime" to h("Next prime", """\operatorname{nextprime}(n) = \min\{p > n\}""", """The first prime after \(n\).""", """"""),
        "previous prime" to h("Previous prime", """\operatorname{prevprime}(n) = \max\{p < n\}""", """The last prime before \(n\).""", """"""),
        "n-th prime" to h("n-th prime", """p_n:\quad p_1 = 2,\ p_2 = 3,\ p_3 = 5,\ \ldots""", """The \(n\)-th prime, up to \(n = 2\,000\,000\).""", """"""),
        "prime counting function" to h("Prime counting function", """\pi(n) = \#\{p \le n\}""", """How many primes there are up to \(n\); about \(\frac{n}{\ln n}\).""", """"""),
        "Euler's totient" to h("Euler's totient", """\varphi(n) = n \prod_{p \mid n} \left(1 - \frac{1}{p}\right)""", """How many of \(1, \ldots, n\) share no factor with \(n\).""", """"""),
        "number of divisors" to h("Number of divisors", """\tau(n) = \prod (e_i + 1)""", """How many numbers divide \(n\), from its prime powers \(p_i^{e_i}\).""", """"""),
        "sum of divisors" to h("Sum of divisors", """\sigma(n) = \sum_{d \mid n} d""", """All the divisors of \(n\) added; \(\sigma(n) = 2n\) for a perfect number.""", """"""),
        "Möbius function" to h("Möbius function", """\mu(n) = \begin{cases} (-1)^k & n \text{ is } k \text{ distinct primes multiplied} \\ 0 & \text{a square divides } n \end{cases}""", """Used to invert sums over divisors.""", """"""),
        "double factorial" to h("Double factorial", """n!! = n\,(n - 2)\,(n - 4) \cdots""", """Every other number down to 1 or 2: \(9!! = 945\).""", """"""),
        "Fibonacci number" to h("Fibonacci number", """F_n = F_{n-1} + F_{n-2},\quad F_0 = 0,\ F_1 = 1""", """0, 1, 1, 2, 3, 5, 8, 13, …; \(\frac{F_{n+1}}{F_n} \to \varphi\), the golden ratio.""", """"""),
        "Lucas number" to h("Lucas number", """L_n = L_{n-1} + L_{n-2},\quad L_0 = 2,\ L_1 = 1""", """2, 1, 3, 4, 7, 11, …: \(L_n = F_{n-1} + F_{n+1}\).""", """"""),
        "Catalan number" to h("Catalan number", """C_n = \frac{1}{n + 1}\binom{2n}{n}""", """1, 1, 2, 5, 14, 42, …: ways to bracket a product, triangulate a polygon, …""", """"""),
        "Bernoulli number" to h("Bernoulli number", """\frac{x}{e^x - 1} = \sum_{n=0}^{\infty} B_n \frac{x^n}{n!}""", """\(B_0 = 1\), \(B_1 = -\frac{1}{2}\), \(B_2 = \frac{1}{6}\), and 0 for odd \(n > 1\).""", """They give \(\zeta(2n)\) and the sums of powers."""),
        "partition number" to h("Partition number", """p(n):\quad p(4) = 5\ \ (4,\ 3{+}1,\ 2{+}2,\ 2{+}1{+}1,\ 1{+}1{+}1{+}1)""", """The ways to write \(n\) as a sum of positive whole numbers, order not mattering.""", """"""),
        "golden ratio" to h("Golden ratio", """\varphi = \frac{1 + \sqrt{5}}{2} \approx 1.618""", """The ratio with \(\varphi = 1 + \frac{1}{\varphi}\); the limit of \(\frac{F_{n+1}}{F_n}\).""", """"""),
        "silver ratio" to h("Silver ratio", """\delta_S = 1 + \sqrt{2} \approx 2.414""", """\(\delta_S = 2 + \frac{1}{\delta_S}\): the limit of Pell numbers' ratios.""", """"""),
        "plastic ratio" to h("Plastic ratio", """\rho^3 = \rho + 1,\quad \rho \approx 1.3247""", """The real root of \(x^3 = x + 1\); the limit of the Padovan sequence's ratios.""", """"""),
        "Euler–Mascheroni constant" to h("Euler–Mascheroni constant", """\gamma = \lim_{n \to \infty} \left(H_n - \ln n\right) \approx 0.5772""", """How far the harmonic numbers stay above \(\ln n\).""", """"""),
        "Catalan's constant" to h("Catalan's constant", """G = \sum_{k=0}^{\infty} \frac{(-1)^k}{(2k + 1)^2} \approx 0.9160""", """Appears in integrals and in counting.""", """"""),
        "Apéry's constant" to h("Apéry's constant", """\zeta(3) = \sum_{n=1}^{\infty} \frac{1}{n^3} \approx 1.2021""", """Shown irrational by Apéry in 1978.""", """"""),
        "omega constant" to h("Omega constant", """\Omega\, e^{\Omega} = 1,\quad \Omega = W(1) \approx 0.5671""", """Lambert's \(W\) at 1.""", """"""),
        "Khinchin's constant" to h("Khinchin's constant", """K_0 \approx 2.6855""", """The geometric mean of the terms of almost every number's continued fraction.""", """"""),
        "Glaisher–Kinkelin constant" to h("Glaisher–Kinkelin constant", """A \approx 1.2824""", """Plays the part for the hyperfactorial that \(\sqrt{2\pi}\) plays for \(n!\) in Stirling's formula.""", """"""),
        "Feigenbaum delta" to h("Feigenbaum delta", """\delta \approx 4.6692""", """How fast period doublings come on the way to chaos, the same for every smooth one-hump map.""", """"""),
        "Feigenbaum alpha" to h("Feigenbaum alpha", """\alpha_F \approx 2.5029""", """How the widths shrink at each period doubling.""", """"""),
        "twin prime constant" to h("Twin prime constant", """C_2 = \prod_{p \ge 3} \frac{p(p - 2)}{(p - 1)^2} \approx 0.6602""", """In the expected count of twin primes up to \(x\), about \(2C_2 \frac{x}{\ln^2 x}\).""", """"""),
        "Meissel–Mertens constant" to h("Meissel–Mertens constant", """M = \lim_{n \to \infty} \left(\sum_{p \le n} \frac{1}{p} - \ln\ln n\right) \approx 0.2615""", """The prime analogue of \(\gamma\).""", """"""),
        "lemniscate constant" to h("Lemniscate constant", """\varpi = 2\int_0^1 \frac{dt}{\sqrt{1 - t^4}} \approx 2.6221""", """Half the length of the lemniscate \((x^2 + y^2)^2 = x^2 - y^2\), as \(\pi\) is for the circle.""", """"""),
        "Gelfond's constant" to h("Gelfond's constant", """e^{\pi} \approx 23.1407""", """Transcendental (Gelfond–Schneider): \(e^\pi = (-1)^{-i}\).""", """"""),
        "exponential density" to h("Exponential density", """f(x) = \lambda e^{-\lambda x},\quad x \ge 0""", """Waiting times at a constant rate \(\lambda\).""", """exppdf(x, λ)."""),
        "exponential distribution function" to h("Exponential distribution", """F(x) = 1 - e^{-\lambda x}""", """The chance of waiting at most \(x\).""", """expcdf(x, λ)."""),
        "uniform density" to h("Uniform density", """f(x) = \frac{1}{b - a},\quad a < x < b""", """Every value between \(a\) and \(b\) equally likely.""", """unifpdf(x, a, b)."""),
        "uniform distribution function" to h("Uniform distribution", """F(x) = \frac{x - a}{b - a}""", """From 0 at \(a\) up to 1 at \(b\).""", """unifcdf(x, a, b)."""),
        "cumulative Poisson probability" to h("Cumulative Poisson", """P(X \le k) = e^{-\lambda} \sum_{i=0}^{k} \frac{\lambda^i}{i!}""", """At most \(k\) events at rate \(\lambda\).""", """poissoncdf(λ, k)."""),
        "geometric probability" to h("Geometric probability", """P(X = k) = (1 - p)^{k - 1}\, p""", """The first success on trial \(k\).""", """geompdf(p, k)."""),
        "cumulative geometric probability" to h("Cumulative geometric", """P(X \le k) = 1 - (1 - p)^k""", """A success within \(k\) trials.""", """geomcdf(p, k)."""),
        "chi-squared density" to h("Chi-squared density", """f(x) = \frac{x^{k/2 - 1} e^{-x/2}}{2^{k/2}\,\Gamma(k/2)}""", """The sum of \(k\) squared standard normals.""", """chi2pdf(x, k)."""),
        "chi-squared distribution function" to h("Chi-squared distribution", """F(x) = 1 - \frac{\Gamma(k/2,\ x/2)}{\Gamma(k/2)}""", """For tests of fit and of variance.""", """chi2cdf(x, k)."""),
        "log-normal density" to h("Log-normal density", """f(x) = \frac{1}{x\sigma\sqrt{2\pi}}\, e^{-\frac{(\ln x - \mu)^2}{2\sigma^2}}""", """\(\ln X\) normal: sizes, incomes, growth.""", """lognpdf(x, μ, σ)."""),
        "log-normal distribution function" to h("Log-normal distribution", """F(x) = \frac{1}{2}\left(1 + \operatorname{erf}\frac{\ln x - \mu}{\sigma\sqrt{2}}\right)""", """The chance that a log-normal value is at most \(x\).""", """logncdf(x, μ, σ)."""),
        "Cauchy density" to h("Cauchy density", """f(x) = \frac{1}{\pi\gamma\left(1 + \left(\frac{x - x_0}{\gamma}\right)^2\right)}""", """A bell with tails so heavy it has no mean.""", """cauchypdf(x, x₀, γ)."""),
        "Cauchy distribution function" to h("Cauchy distribution", """F(x) = \frac{1}{2} + \frac{1}{\pi}\arctan\frac{x - x_0}{\gamma}""", """The chance of a value at most \(x\); the median is \(x_0\).""", """cauchycdf(x, x₀, γ)."""),
        "Weibull density" to h("Weibull density", """f(x) = \frac{k}{\lambda}\left(\frac{x}{\lambda}\right)^{k-1} e^{-(x/\lambda)^k}""", """Lifetimes and failure rates; \(k = 1\) is the exponential.""", """weibpdf(x, k, λ)."""),
        "Weibull distribution function" to h("Weibull distribution", """F(x) = 1 - e^{-(x/\lambda)^k}""", """The chance of failing by time \(x\).""", """weibcdf(x, k, λ)."""),
        "Stirling number of the first kind" to h("Stirling numbers, first kind", """s(n, k):\quad s(4, 2) = 11""", """Permutations of \(n\) things with exactly \(k\) cycles.""", """stirling1(n, k)."""),
        "Stirling number of the second kind" to h("Stirling numbers, second kind", """S(n, k):\quad S(4, 2) = 7""", """Ways to split \(n\) things into \(k\) non-empty groups.""", """stirling2(n, k)."""),
        "Bell number" to h("Bell numbers", """B_n = \sum_{k=0}^{n} S(n, k)""", """All the ways to split \(n\) things into groups: 1, 1, 2, 5, 15, 52, …""", """"""),
        "derangements" to h("Derangements", """!n = n! \sum_{k=0}^{n} \frac{(-1)^k}{k!}""", """Orderings with nothing in its own place; about \(\frac{n!}{e}\).""", """"""),
        "Narayana number" to h("Narayana numbers", """N(n, k) = \frac{1}{n}\binom{n}{k}\binom{n}{k - 1}""", """Split the Catalan numbers by peaks.""", """"""),
        "Lah number" to h("Lah numbers", """L(n, k) = \binom{n - 1}{k - 1}\frac{n!}{k!}""", """Ways to split \(n\) things into \(k\) ordered lists.""", """"""),
        "Eulerian number" to h("Eulerian numbers", """A(n, k)""", """Permutations of \(n\) with exactly \(k\) ascents.""", """"""),
        "rising factorial" to h("Rising factorial", """x^{(n)} = x(x + 1)\cdots(x + n - 1)""", """\(n\) factors climbing from \(x\); \(x\) may be a letter.""", """rising(x, n)."""),
        "falling factorial" to h("Falling factorial", """(x)_n = x(x - 1)\cdots(x - n + 1)""", """\(n\) ordered choices from \(x\); \(x\) may be a letter.""", """falling(x, n)."""),
        "superfactorial" to h("Superfactorial", """\operatorname{sf}(n) = 1!\,2!\cdots n!""", """The product of the first \(n\) factorials.""", """"""),
        "harmonic number" to h("Harmonic numbers", """H_n = 1 + \frac{1}{2} + \cdots + \frac{1}{n}""", """Exact fractions; \(H_n \approx \ln n + \gamma\).""", """"""),
        "triangular number" to h("Triangular numbers", """T_n = \frac{n(n + 1)}{2}""", """1, 3, 6, 10, …: dots in a triangle.""", """"""),
        "Motzkin number" to h("Motzkin numbers", """M_n:\quad 1, 1, 2, 4, 9, 21, 51, \ldots""", """Ways to draw non-crossing chords between \(n\) points on a circle.""", """"""),
        "Pell number" to h("Pell numbers", """P_n = 2P_{n-1} + P_{n-2},\quad P_0 = 0,\ P_1 = 1""", """0, 1, 2, 5, 12, 29, …; \(\frac{P_{n+1}}{P_n} \to 1 + \sqrt{2}\).""", """"""),
        "primorial" to h("Primorial", """n\# = \prod_{p \le n} p""", """The product of the primes up to \(n\).""", """"""),
        "mode" to h("Mode", """\text{mode}(x_1, \ldots, x_n)""", """The most common value (the smallest, if there's a tie).""", """Values with commas."""),
        "range" to h("Range", """\max x_i - \min x_i""", """How far the values spread from the smallest to the largest.""", """"""),
        "first quartile" to h("First quartile", """Q_1""", """The median of the lower half.""", """"""),
        "third quartile" to h("Third quartile", """Q_3""", """The median of the upper half.""", """"""),
        "interquartile range" to h("Interquartile range", """\text{IQR} = Q_3 - Q_1""", """The spread of the middle half of the values.""", """"""),
        "geometric mean" to h("Geometric mean", """\sqrt[n]{x_1 x_2 \cdots x_n}""", """The mean for growth rates and ratios.""", """"""),
        "harmonic mean" to h("Harmonic mean", """\frac{n}{\frac{1}{x_1} + \cdots + \frac{1}{x_n}}""", """The mean for rates, such as average speed over equal distances.""", """"""),
        "root mean square" to h("Root mean square", """\sqrt{\frac{x_1^2 + \cdots + x_n^2}{n}}""", """The quadratic mean: effective values of alternating currents.""", """"""),
        "mean absolute deviation" to h("Mean absolute deviation", """\frac{1}{n}\sum |x_i - \bar{x}|""", """The average distance from the mean.""", """"""),
        "coefficient of variation" to h("Coefficient of variation", """c_v = \frac{s}{\bar{x}}""", """The spread relative to the mean.""", """"""),
        "skewness" to h("Skewness", """\frac{m_3}{m_2^{3/2}},\quad m_k = \frac{1}{n}\sum (x_i - \bar{x})^k""", """Positive when the long tail is on the right.""", """"""),
        "excess kurtosis" to h("Excess kurtosis", """\frac{m_4}{m_2^2} - 3""", """How heavy the tails are, 0 for the normal.""", """"""),
        "count" to h("Count", """n""", """How many values there are.""", """"""),
        "sum of squares" to h("Sum of squares", """\sum x_i^2""", """The values squared and added.""", """"""),
        "product of a list" to h("Product of a list", """\prod x_i""", """All the values multiplied.""", """"""),
        "imaginary error function" to h("Imaginary error function", """\operatorname{erfi}(x) = \frac{2}{\sqrt{\pi}} \int_0^x e^{t^2}\,dt""", """\(-i \operatorname{erf}(ix)\); an antiderivative of \(e^{x^2}\), up to \(\frac{\sqrt{\pi}}{2}\).""", ""),
        "Fresnel sine integral" to h("Fresnel S", """S(x) = \int_0^x \sin\frac{\pi t^2}{2}\,dt""", """Tends to \(\frac{1}{2}\); integrals of \(\sin x^2\) come out in it.""", ""),
        "Fresnel cosine integral" to h("Fresnel C", """C(x) = \int_0^x \cos\frac{\pi t^2}{2}\,dt""", """Tends to \(\frac{1}{2}\); integrals of \(\cos x^2\) come out in it.""", ""),
        "upper incomplete gamma function" to h("Incomplete gamma", """\Gamma(s, x) = \int_x^{\infty} t^{s-1} e^{-t}\,dt""", """\(\Gamma(s, 0) = \Gamma(s)\).""", """Integrals like \(\int \sqrt{x}\, e^{-x}\,dx\) come out in it."""),
        "incomplete elliptic integral of the first kind" to h("Elliptic integral F", """F(\varphi \mid m) = \int_0^{\varphi} \frac{d\theta}{\sqrt{1 - m \sin^2\theta}}""", """With \(E(\varphi \mid m) = \int_0^{\varphi} \sqrt{1 - m \sin^2\theta}\,d\theta\), the arc length of an ellipse.""", ""),
        "polylogarithm" to h("Polylogarithm", """\operatorname{Li}_s(z) = \sum_{k=1}^{\infty} \frac{z^k}{k^s}""", """\(\operatorname{Li}_1(z) = -\ln(1 - z)\), \(\operatorname{Li}_s(1) = \zeta(s)\).""", ""),
        "Lambert W function" to h("Lambert W", """W(z) e^{W(z)} = z""", """The inverse of \(w e^w\); the principal branch, defined for \(z \ge -1/e\).""", """Solves things like \(x e^x = 2\)."""),
        "Bessel function of the first kind" to h("Bessel function J", """J_a(x) = \sum_{k=0}^{\infty} \frac{(-1)^k}{k!\,\Gamma(k + a + 1)}\left(\frac{x}{2}\right)^{2k + a}""", """Solutions of Bessel's equation that stay finite at 0; they describe waves on a drum.""", """The order \(a\) goes in the small box."""),
        "Bessel function of the second kind" to h("Bessel function Y", """Y_a(x) = \frac{J_a(x)\cos a\pi - J_{-a}(x)}{\sin a\pi}""", """The second solution of Bessel's equation; it goes to \(-\infty\) at 0.""", """The order \(a\) goes in the small box."""),
        "Hadamard product" to h("Hadamard product", """A \circ B""", """Multiplies matching entries; both matrices must be the same size.""", ""),
        "Kronecker product" to h("Kronecker product", """A \otimes B""", """Every entry of A multiplied by the whole of B, giving an \(mp \times nq\) matrix.""", ""),
        "conjugate transpose" to h("Hermitian conjugate", """A^{\mathrm{H}} = \overline{A^\top}""", """The transpose with every entry conjugated (the adjoint).""", """Real matrices: the same as the transpose."""),
        "list brackets" to h("List of points", """[(1,\ 2),\ (2,\ 3.5),\ (4,\ 5)]""", """A list of points, drawn as dots. With a list on the graph, a function with unknowns (like \(ax + b\)) gets a Fit button that sets its sliders to the best fit.""", """Write each point as (x, y), separated by commas."""),
        "store in variable" to h("Store in a variable", """a := 5""", """Gives a letter a value that later calculations use.""", """Type the letter, :=, then the value. Store a function the same way: \(f(x) := x^2\)."""),
        "symbol builder" to h("Symbol builder", """\hat{x}_{1},\ \dot{\theta}^{2},\ \vec{\mathfrak{g}}_{i}""", """Make your own symbol: a letter (Latin, Greek, calligraphic or Fraktur), an accent, a subscript and a superscript. It works as a variable like any letter.""", """Saved symbols appear at the top of this tab."""),
        "saved symbol" to h("Your symbol", "", """A symbol you built; it works as a variable like any letter.""", """Remove it with the button below, or in the symbol builder."""),
        "list of constants with names" to h("All constants", """c = 299\,792\,458\ \mathrm{m}\,\mathrm{s}^{-1}""", """Every constant with its name, value and unit, in the current unit system.""", ""),
    )

    private val SUPERSCRIPT = mapOf('-' to '⁻', '0' to '⁰', '1' to '¹', '2' to '²', '3' to '³', '4' to '⁴', '5' to '⁵', '6' to '⁶', '7' to '⁷', '8' to '⁸', '9' to '⁹')

    /** 6.62607015e-34 → 6.62607015 × 10⁻³⁴ */
    private fun scientific(d: String): String {
        val (m, e) = d.split('e', 'E').let { it[0] to it.getOrNull(1) }
        return if (e == null) m.replace("-", "−") else m.replace("-", "−") + " × 10" + e.trimStart('+').map { SUPERSCRIPT[it] ?: it }.joinToString("")
    }

    /** The keys by group, for the documentation's key reference (spoken names, as in [of]). */
    val groups: List<Pair<String, List<String>>> = listOf(
        "Powers, roots and logs" to listOf("pi", "e", "e to the power", "times ten to the power", "squared", "power", "square root", "nth root", "natural log", "log base a", "ten to the power", "previous answer"),
        "Trigonometry" to listOf("sin", "cos", "tan", "inverse sin", "inverse cos", "inverse tan", "hyperbolic sin", "hyperbolic cos", "hyperbolic tan", "inverse hyperbolic sin", "inverse hyperbolic cos", "inverse hyperbolic tan"),
        "Calculus" to listOf("integral", "derivative", "higher derivative", "limit", "one-sided limit", "Taylor series", "solve differential equation", "y prime", "y double prime", "infinity", "sum", "product"),
        "Vector calculus" to listOf("partial derivative", "higher partial derivative", "double integral", "triple integral", "gradient", "divergence", "curl", "Laplacian", "Jacobian matrix", "Hessian matrix", "vector with 2 components", "vector with 3 components"),
        "Linear algebra" to listOf("matrix", "inverse", "transpose", "conjugate transpose", "determinant", "eigenvalues", "eigenvectors", "characteristic polynomial", "trace", "reduced row echelon form", "rank", "dot product", "cross product", "Hadamard product", "Kronecker product"),
        "Complex numbers" to listOf("i, the imaginary unit", "real part", "imaginary part", "conjugate", "argument", "e to the i theta", "z", "w", "contour integral", "residue"),
        "Special functions" to listOf("gamma function", "Riemann zeta function", "Lambert W function", "Bessel function of the first kind", "Bessel function of the second kind", "sine integral", "cosine integral", "exponential integral", "logarithmic integral", "imaginary error function", "Fresnel sine integral", "Fresnel cosine integral", "upper incomplete gamma function", "incomplete elliptic integral of the first kind", "polylogarithm", "error function", "digamma function", "Hurwitz zeta function", "hyperbolic sine integral", "hyperbolic cosine integral"),
        "More functions" to listOf("absolute value", "mod", "percent"),
        "More trigonometry" to listOf("secant", "cosecant", "cotangent", "hypotenuse", "two-argument arctangent", "inverse secant", "inverse cosecant", "inverse cotangent", "sinc function", "hyperbolic secant", "hyperbolic cosecant", "hyperbolic cotangent", "inverse hyperbolic secant", "inverse hyperbolic cosecant", "inverse hyperbolic cotangent"),
        "Signals" to listOf("Heaviside step", "rectangle function", "triangle function", "ramp function", "pulse", "sawtooth wave", "square wave", "triangle wave", "logistic sigmoid", "softplus", "clamp", "linear interpolation", "smoothstep", "Gaussian", "wrap"),
        "Primes and sequences" to listOf("is prime", "next prime", "previous prime", "n-th prime", "prime counting function", "Euler's totient", "number of divisors", "sum of divisors", "Möbius function", "double factorial", "Fibonacci number", "Lucas number", "Catalan number", "Bernoulli number", "partition number"),
        "Mathematical constants" to listOf("golden ratio", "silver ratio", "plastic ratio", "Euler–Mascheroni constant", "Catalan's constant", "Apéry's constant", "omega constant", "Khinchin's constant", "Glaisher–Kinkelin constant", "Feigenbaum delta", "Feigenbaum alpha", "twin prime constant", "Meissel–Mertens constant", "lemniscate constant", "Gelfond's constant"),
        "Distributions" to listOf("exponential density", "exponential distribution function", "uniform density", "uniform distribution function", "cumulative Poisson probability", "geometric probability", "cumulative geometric probability", "chi-squared density", "chi-squared distribution function", "log-normal density", "log-normal distribution function", "Cauchy density", "Cauchy distribution function", "Weibull density", "Weibull distribution function"),
        "Combinatorics" to listOf("Stirling number of the first kind", "Stirling number of the second kind", "Bell number", "derangements", "Narayana number", "Lah number", "Eulerian number", "rising factorial", "falling factorial", "superfactorial", "harmonic number", "triangular number", "Motzkin number", "Pell number", "primorial"),
        "Statistics II" to listOf("mode", "range", "first quartile", "third quartile", "interquartile range", "geometric mean", "harmonic mean", "root mean square", "mean absolute deviation", "coefficient of variation", "skewness", "excess kurtosis", "count", "sum of squares", "product of a list"),
        "Number theory" to listOf("greatest common divisor", "least common multiple", "floor", "ceiling", "round", "fractional part", "sign", "maximum", "minimum"),
        "Statistics" to listOf("factorial", "n choose k", "permutations", "mean", "median", "sample standard deviation", "population standard deviation", "sample variance", "normal density", "normal distribution function", "inverse normal", "binomial probability", "cumulative binomial probability", "Poisson probability", "sum of a list", "comma for lists", "list brackets"),
        "Variables and symbols" to listOf("store in variable", "symbol builder", "saved symbol", "list of constants with names"),
        "Number pad" to listOf("all clear", "brackets", "divide", "times", "minus", "plus", "point", "backspace", "enter", "x"),
    ).map { (t, keys) -> t to keys.filter { it in all } }

    /** Help for a key by its spoken name; constants show their value in LaTeX and link to NIST; otherwise just the name. */
    fun of(spoken: String): KeyHelp {
        all[spoken]?.let { return it }
        com.example.cas.engine.Constant.entries.firstOrNull { it.description.substringBefore(" (") == spoken }?.let { k ->
            return KeyHelp(
                k.description,
                ConstantLatex.formula(k),
                if (k.exact) "Exact: fixed by the SI definition of the units." else "Measured value (CODATA 2022).",
                "The Rad/Deg switch becomes SI / Planck / Atomic / Natural units on this tab.",
                ConstantLatex.nistLink(k),
            )
        }
        // Digits and letters share one explanation each.
        if (spoken.length == 1 && spoken[0].isDigit()) {
            return KeyHelp("Digit $spoken", "", """Types $spoken.""", """Next to a letter or bracket it multiplies: \(2x\), \(3(x + 1)\).""")
        }
        val letterFamilies = listOf("letter ", "capital ", "calligraphic ", "fraktur ")
        val greek = setOf("alpha", "beta", "gamma", "delta", "epsilon", "zeta", "eta", "theta", "iota", "kappa", "lambda", "mu", "nu",
            "xi", "rho", "sigma", "tau", "upsilon", "phi", "chi", "psi", "omega")
        if (letterFamilies.any { spoken.startsWith(it) } || spoken in greek) {
            // The letter in LaTeX, shown adding up like any variable.
            val name = spoken.substringAfterLast(' ')
            val tex = when {
                spoken.startsWith("calligraphic ") -> "\\mathcal{$name}"
                spoken.startsWith("fraktur ") -> "\\mathfrak{$name}"
                name.length == 1 -> name
                spoken.startsWith("capital ") -> "\\" + name.replaceFirstChar { it.uppercase() }
                else -> "\\$name"
            }
            return KeyHelp(
                spoken.replaceFirstChar { it.uppercase() },
                "2$tex + 3$tex = 5$tex",
                """A letter to use as a variable, like \(x\). Letters side by side multiply: \(2ab\) is \(2 \cdot a \cdot b\).""",
                """Give it a value with :=, as in \(a := 5\); in the graphs, a letter with no value gets a slider.""",
            )
        }
        return KeyHelp(spoken.replaceFirstChar { it.uppercase() })
    }
}

/**
 * A constant as LaTeX: its symbol (italic letters, upright labels in subscripts: m_{\mathrm{n}}),
 * its value with digits grouped in threes and a real power of ten, and its unit in upright SI
 * style with negative exponents (\mathrm{m}^{3}\,\mathrm{kg}^{-1}\,\mathrm{s}^{-2}).
 */
object ConstantLatex {
    private val SUPER = mapOf('⁰' to '0', '¹' to '1', '²' to '2', '³' to '3', '⁴' to '4', '⁵' to '5', '⁶' to '6', '⁷' to '7', '⁸' to '8', '⁹' to '9', '⁻' to '-', '−' to '-')

    /** Text with Unicode superscripts (¹²C, m⁻²) as LaTeX, upright unless [italic]. */
    private fun text(t: String, italic: Boolean): String {
        val sb = StringBuilder()
        var i = 0
        while (i < t.length) {
            if (t[i] in SUPER.keys && t[i] != '−') {
                val start = i
                while (i < t.length && t[i] in SUPER.keys && t[i] != '−') i++
                sb.append("^{").append(t.substring(start, i).map { SUPER[it] }.joinToString("")).append("}")
                continue
            }
            val start = i
            while (i < t.length && !(t[i] in SUPER.keys && t[i] != '−')) i++
            val run = t.substring(start, i)
            sb.append(if (italic || run.all { !it.isLetter() }) run.replace("′", "'") else "\\mathrm{$run}")
        }
        return sb.toString()
    }

    fun symbol(k: com.example.cas.engine.Constant): String = k.pieces.joinToString("") { p ->
        // A letter with a prime (b′) is italic, as a single letter would be.
        val italic = p.italic || (p.text.length == 2 && p.text[0].isLetter() && p.text[1] == '′')
        text(p.text, italic) +
            (if (p.sub.isNotEmpty()) "_{" + text(p.sub, false) + "}" else "") +
            // A prime is written as LaTeX writes it, b'; other superscripts in braces.
            (if (p.sup.isNotEmpty() && p.sup.all { it == '′' }) "'".repeat(p.sup.length)
            else if (p.sup.isNotEmpty()) "^{" + p.sup.map { SUPER[it] ?: it }.joinToString("") + "}" else "")
    }

    /** 6.62607015e-34 → 6.626\,070\,15 \times 10^{-34} */
    fun value(decimal: String): String {
        val (m, e) = decimal.lowercase().split('e').let { it[0] to it.getOrNull(1) }
        val (ip, fp) = m.split('.').let { it[0] to it.getOrElse(1) { "" } }
        // Group the whole-number part in threes from the right: 12906 → 12\,906.
        val intGrouped = ip.reversed().chunked(3).map { it.reversed() }.reversed().joinToString("\\,")
        val fracGrouped = fp.chunked(3).joinToString("\\,")
        val mantissa = if (fp.isEmpty()) intGrouped else "$intGrouped.$fracGrouped"
        return if (e == null) mantissa else "$mantissa \\times 10^{${e.trimStart('+').toInt()}}"
    }

    /** "m³/(kg·s²)" → \mathrm{m}^{3}\,\mathrm{kg}^{-1}\,\mathrm{s}^{-2} */
    fun unit(u: String): String {
        if (u.isEmpty()) return ""
        val slash = u.indexOf('/')
        val num = if (slash < 0) u else u.substring(0, slash)
        val den = if (slash < 0) "" else u.substring(slash + 1).removePrefix("(").removeSuffix(")")
        fun factors(part: String, sign: Int): List<String> = part.split('·', ' ').filter { it.isNotBlank() }.map { f ->
            val base = f.takeWhile { it !in SUPER.keys }
            val sup = f.drop(base.length).map { SUPER[it] ?: it }.joinToString("")
            val power = (if (sup.isEmpty()) 1 else sup.toInt()) * sign
            "\\mathrm{$base}" + if (power == 1) "" else "^{$power}"
        }
        return (factors(num, 1) + factors(den, -1)).joinToString("\\,")
    }

    /** The whole formula; long ones go over two lines. */
    fun formula(k: com.example.cas.engine.Constant): String {
        val sym = symbol(k)
        val v = value(k.decimal)
        val u = unit(k.unit)
        val rhs = v + if (u.isEmpty()) "" else "\\;$u"
        // One line: the card scrolls sideways for long values.
        return "$sym = $rhs"
    }

    /** NIST CODATA codes for the common constants; others link to NIST's constants index. */
    private val NIST = mapOf(
        "c0" to "c", "h" to "h", "hbar" to "hbar", "kB" to "k", "G" to "bg", "qe" to "e", "me" to "me", "mp" to "mp",
        "mn" to "mn", "NA" to "na", "R" to "r", "F" to "f", "alpha" to "alph", "mu0" to "mu0", "eps0" to "ep0",
        "sigma" to "sigma", "a0" to "bohrrada0", "Rinf" to "ryd", "Eh" to "hr", "muB" to "mub", "muN" to "mun",
        "Z0" to "z0", "mmu" to "mmu", "mtau" to "mtau", "re" to "re", "RK" to "rk", "KJ" to "kjos", "Phi0" to "flxquhs2e",
    )

    fun nistLink(k: com.example.cas.engine.Constant): String =
        NIST[k.id]?.let { "https://physics.nist.gov/cgi-bin/cuu/Value?$it" } ?: "https://physics.nist.gov/cuu/Constants/index.html"
}

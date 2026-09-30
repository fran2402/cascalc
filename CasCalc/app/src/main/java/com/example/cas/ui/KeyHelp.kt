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
        "transpose" to h("Transpose", """(A^T)_{ij} = A_{ji}""", """Swaps rows and columns.""", ""),
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
        "fractional part" to h("Fractional part", """\{x\} = x - \lfloor x \rfloor""", "", ""),
        "sign" to h("Sign", """\text{sgn}\,x = \frac{x}{|x|}""", """\(-1\), \(0\) or \(1\).""", ""),
        "minimum" to h("Minimum", """\min(a, b)""", "", ""),
        "maximum" to h("Maximum", """\max(a, b)""", "", ""),
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
        "Lambert W function" to h("Lambert W", """W(z) e^{W(z)} = z""", """The inverse of \(w e^w\); the principal branch, defined for \(z \ge -1/e\).""", """Solves things like \(x e^x = 2\)."""),
        "Bessel function of the first kind" to h("Bessel function J", """J_a(x) = \sum_{k=0}^{\infty} \frac{(-1)^k}{k!\,\Gamma(k + a + 1)}\left(\frac{x}{2}\right)^{2k + a}""", """Solutions of Bessel's equation that stay finite at 0; they describe waves on a drum.""", """The order \(a\) goes in the small box."""),
        "Bessel function of the second kind" to h("Bessel function Y", """Y_a(x) = \frac{J_a(x)\cos a\pi - J_{-a}(x)}{\sin a\pi}""", """The second solution of Bessel's equation; it goes to \(-\infty\) at 0.""", """The order \(a\) goes in the small box."""),
        "Hadamard product" to h("Hadamard product", """A \circ B""", """Multiplies matching entries; both matrices must be the same size.""", ""),
        "Kronecker product" to h("Kronecker product", """A \otimes B""", """Every entry of A multiplied by the whole of B, giving an \(mp \times nq\) matrix.""", ""),
        "conjugate transpose" to h("Hermitian conjugate", """A^H = \overline{A^T}""", """The transpose with every entry conjugated (the adjoint).""", """Real matrices: the same as the transpose."""),
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

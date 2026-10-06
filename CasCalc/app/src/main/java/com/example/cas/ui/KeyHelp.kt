package com.example.cas.ui

/**
 * What long-pressing a key shows: its name and formula, what it is, how to use it step by step,
 * and worked examples ([KeyGuides]). [formula] is LaTeX (a newline starts another line); [about]
 * and [steps] are text with inline LaTeX between \( and \), drawn by the calculator's own
 * renderer via [com.example.cas.engine.LatexParser]. Keyed by spoken name (Keys.kt).
 */
data class KeyHelp(
    val title: String,
    val formula: String = "",
    val about: String = "",
    val steps: List<String> = emptyList(),
    val examples: List<KeyGuides.Example> = emptyList(),
    val link: String? = null,
)

object KeyHelps {
    /** A key's title and formula; the rest comes from its guide. */
    private fun h(title: String, formula: String) = KeyHelp(title, formula)

    private fun withGuide(k: KeyHelp, g: KeyGuides.Guide?) = if (g == null) k else k.copy(about = g.about, steps = g.steps, examples = g.examples)
    private val all: Map<String, KeyHelp> by lazy { base.mapValues { (k, v) -> withGuide(v, KeyGuides.all[k]) } }
    private val base: Map<String, KeyHelp> = mapOf(
        "pi" to h("π", """\pi = 3.14159\,26535\ldots"""),
        "e" to h("Euler's number", """e = \lim_{n\to\infty} \left(1 + \frac{1}{n}\right)^n = 2.71828\ldots"""),
        "power" to h("Power", """x^n"""),
        "nth root" to h("nth root", """\sqrt[n]{x} = x^{1/n}"""),
        "natural log" to h("Natural logarithm", """\ln x = \log_e x"""),
        "log base a" to h("Logarithm", """\log_a x = \frac{\ln x}{\ln a}"""),
        "previous answer" to h("Previous answer", """\text{Ans}"""),
        "sin" to h("Sine", """\sin\theta = \frac{\text{opposite}}{\text{hypotenuse}}"""),
        "cos" to h("Cosine", """\cos\theta = \frac{\text{adjacent}}{\text{hypotenuse}}"""),
        "tan" to h("Tangent", """\tan\theta = \frac{\sin\theta}{\cos\theta}"""),
        "inverse sin" to h("Arcsine", """\text{asin}\,x = \theta \quad \text{where} \quad \sin\theta = x
-\frac{\pi}{2} \le \theta \le \frac{\pi}{2}"""),
        "inverse cos" to h("Arccosine", """\text{acos}\,x = \theta \quad \text{where} \quad \cos\theta = x
0 \le \theta \le \pi"""),
        "inverse tan" to h("Arctangent", """\text{atan}\,x = \theta \quad \text{where} \quad \tan\theta = x
-\frac{\pi}{2} < \theta < \frac{\pi}{2}"""),
        "hyperbolic sin" to h("Hyperbolic sine", """\sinh x = \frac{e^x - e^{-x}}{2}"""),
        "hyperbolic cos" to h("Hyperbolic cosine", """\cosh x = \frac{e^x + e^{-x}}{2}"""),
        "hyperbolic tan" to h("Hyperbolic tangent", """\tanh x = \frac{\sinh x}{\cosh x}"""),
        "inverse hyperbolic sin" to h("Inverse hyperbolic sine", """\text{asinh}\,x = \ln\left(x + \sqrt{x^2 + 1}\right)"""),
        "inverse hyperbolic cos" to h("Inverse hyperbolic cosine", """\text{acosh}\,x = \ln\left(x + \sqrt{x^2 - 1}\right), \quad x \ge 1"""),
        "inverse hyperbolic tan" to h("Inverse hyperbolic tangent", """\text{atanh}\,x = \frac{1}{2}\ln\frac{1 + x}{1 - x}, \quad |x| < 1"""),
        "equals sign" to h("Equals sign", """a = b"""),
        "comma" to h("Comma", """a,\ b"""),
        "less than" to h("Less than", """a < b"""),
        "greater than" to h("Greater than", """a > b"""),
        "integral" to h("Integral", """\int_a^b f(x)\,dx = F(b) - F(a)
F' = f"""),
        "limit" to h("Limit", """\lim_{x\to a} f(x)"""),
        "Taylor series" to h("Taylor series", """f(x) \approx \sum_{n=0}^{N} \frac{f^{(n)}(a)}{n!}(x - a)^n"""),
        "solve differential equation" to h("Differential equation", """\text{dsolve}(y'' + y = 0,\ y(0) = 1,\ y'(0) = 0)
\to\ y = \cos x"""),
        "y prime" to h("First derivative of y", """y' = \frac{dy}{dx}"""),
        "y double prime" to h("Second derivative of y", """y'' = \frac{d^2 y}{dx^2}"""),
        "infinity" to h("Infinity", """\infty"""),
        "sum" to h("Sum", """\sum_{k=1}^{n} k^2 = \frac{n(n + 1)(2n + 1)}{6}"""),
        "product" to h("Product", """\prod_{k=1}^{n} k = n!"""),
        "partial derivative" to h("Partial derivative", """\frac{\partial f}{\partial x}"""),
        "gradient" to h("Gradient", """\nabla f = \begin{bmatrix} \partial f/\partial x \\ \partial f/\partial y \\ \partial f/\partial z \end{bmatrix}"""),
        "divergence" to h("Divergence", """\nabla \cdot F = \frac{\partial P}{\partial x} + \frac{\partial Q}{\partial y} + \frac{\partial R}{\partial z}"""),
        "curl" to h("Curl", """\nabla \times F = \begin{bmatrix} R_y - Q_z \\ P_z - R_x \\ Q_x - P_y \end{bmatrix}"""),
        "Jacobian matrix" to h("Jacobian", """J = \left[\frac{\partial F_i}{\partial x_j}\right]"""),
        "Hessian matrix" to h("Hessian", """H = \left[\frac{\partial^2 f}{\partial x_i \partial x_j}\right]"""),
        "matrix" to h("Matrix", """A = \begin{bmatrix} a & b \\ c & d \end{bmatrix}"""),
        "inverse" to h("Inverse", """A A^{-1} = I
\begin{bmatrix} a & b \\ c & d \end{bmatrix}^{-1} = \frac{1}{ad - bc}\begin{bmatrix} d & -b \\ -c & a \end{bmatrix}"""),
        "transpose" to h("Transpose", """(A^\top)_{ij} = A_{ji}"""),
        "determinant" to h("Determinant", """\det \begin{bmatrix} a & b \\ c & d \end{bmatrix} = ad - bc"""),
        "eigenvalues" to h("Eigenvalues", """\det(A - \lambda I) = 0"""),
        "eigenvectors" to h("Eigenvectors", """A v = \lambda v"""),
        "characteristic polynomial" to h("Characteristic polynomial", """p(\lambda) = \det(\lambda I - A)"""),
        "trace" to h("Trace", """\text{tr}\,A = \sum_{i=1}^{n} a_{ii}"""),
        "reduced row echelon form" to h("Reduced row echelon form", """\text{rref}\begin{bmatrix} 1 & 2 \\ 3 & 4 \end{bmatrix} = \begin{bmatrix} 1 & 0 \\ 0 & 1 \end{bmatrix}"""),
        "rank" to h("Rank", """\text{rk}\,A"""),
        "dot product" to h("Dot product", """u \cdot v = \sum_i u_i v_i = |u||v|\cos\theta"""),
        "cross product" to h("Cross product", """u \times v = \begin{bmatrix} u_2 v_3 - u_3 v_2 \\ u_3 v_1 - u_1 v_3 \\ u_1 v_2 - u_2 v_1 \end{bmatrix}"""),
        "i, the imaginary unit" to h("Imaginary unit", """i^2 = -1"""),
        "real part" to h("Real part", """\Re(a + bi) = a"""),
        "imaginary part" to h("Imaginary part", """\Im(a + bi) = b"""),
        "conjugate" to h("Conjugate", """(a + bi)^* = a - bi"""),
        "argument" to h("Argument", """z = |z|\,e^{i \arg z}, \quad -\pi < \arg z \le \pi"""),
        "e to the i theta" to h("Euler's formula", """e^{i\theta} = \cos\theta + i\sin\theta"""),
        "gamma function" to h("Gamma function", """\Gamma(z) = \int_0^\infty t^{z - 1} e^{-t}\,dt
\Gamma(n) = (n - 1)!"""),
        "Riemann zeta function" to h("Riemann zeta function", """\zeta(s) = \sum_{n=1}^{\infty} \frac{1}{n^s}"""),
        "z" to h("z", """z = x + iy"""),
        "w" to h("w", """w = f(z)"""),
        "contour integral" to h("Contour integral", """\oint_{|z - a| = r} f(z)\,dz
= 2\pi i \sum_k \operatorname{Res}_{z = a_k} f"""),
        "residue" to h("Residue", """\operatorname{Res}_{z=a} f = \frac{1}{2\pi i}\oint f(z)\,dz"""),
        "absolute value" to h("Absolute value", """|x| = \sqrt{x^2}"""),
        "mod" to h("Remainder", """a \bmod b = a - b\left\lfloor \frac{a}{b} \right\rfloor"""),
        "factorial" to h("Factorial", """n! = 1 \cdot 2 \cdots n, \quad 0! = 1"""),
        "n choose k" to h("Combinations", """\binom{n}{k} = \frac{n!}{k!\,(n - k)!}"""),
        "permutations" to h("Permutations", """P(n, k) = \frac{n!}{(n - k)!}"""),
        "mean" to h("Mean", """\bar{x} = \frac{1}{n}\sum_{i=1}^{n} x_i"""),
        "median" to h("Median", """\text{med}(x_1, \ldots, x_n)"""),
        "sample standard deviation" to h("Sample standard deviation", """s = \sqrt{\frac{1}{n - 1}\sum_{i=1}^{n} (x_i - \bar{x})^2}"""),
        "population standard deviation" to h("Population standard deviation", """\sigma = \sqrt{\frac{1}{n}\sum_{i=1}^{n} (x_i - \bar{x})^2}"""),
        "sample variance" to h("Sample variance", """s^2 = \frac{1}{n - 1}\sum_{i=1}^{n} (x_i - \bar{x})^2"""),
        "normal density" to h("Normal density", """\varphi(x;\ \mu, \sigma) = \frac{1}{\sigma\sqrt{2\pi}}\, e^{-\frac{(x - \mu)^2}{2\sigma^2}}"""),
        "normal distribution function" to h("Normal distribution", """\Phi(x;\ \mu, \sigma)
= \frac{1}{2}\left(1 + \text{erf}\,\frac{x - \mu}{\sigma\sqrt{2}}\right)"""),
        "inverse normal" to h("Inverse normal", """\Phi^{-1}(p) = z, \quad \Phi(z) = p"""),
        "binomial probability" to h("Binomial probability", """P(X = k) = \binom{n}{k} p^k (1 - p)^{n - k}"""),
        "cumulative binomial probability" to h("Cumulative binomial", """P(X \le k) = \sum_{i=0}^{k} \binom{n}{i} p^i (1 - p)^{n - i}"""),
        "Poisson probability" to h("Poisson probability", """P(X = k) = \frac{\lambda^k e^{-\lambda}}{k!}"""),
        "sum of a list" to h("Sum of a list", """\sum_{i=1}^{n} x_i"""),
        "comma for lists" to h("Comma", """x_1,\ x_2,\ \ldots"""),
        "all clear" to h("All clear", ""),
        "brackets" to h("Brackets", """(\ )"""),
        "divide" to h("Divide", """\frac{a}{b}"""),
        "backspace" to h("Backspace", ""),
        "enter" to h("Enter", """2 + 3 = 5"""),
        "equals sign" to h("Equals sign", """a = b"""),
        "x" to h("x", """2x + 3x = 5x"""),
        "times" to h("Times", """a \times b"""),
        "minus" to h("Minus", """a - b"""),
        "plus" to h("Plus", """a + b"""),
        "point" to h("Decimal point", """3.14"""),
        "sine integral" to h("Sine integral", """\operatorname{Si}(x) = \int_0^x \frac{\sin t}{t}\,dt"""),
        "cosine integral" to h("Cosine integral", """\operatorname{Ci}(x) = \gamma + \ln x + \int_0^x \frac{\cos t - 1}{t}\,dt"""),
        "exponential integral" to h("Exponential integral", """\operatorname{Ei}(x) = -\int_{-x}^{\infty} \frac{e^{-t}}{t}\,dt"""),
        "logarithmic integral" to h("Logarithmic integral", """\operatorname{li}(x) = \int_0^x \frac{dt}{\ln t}"""),
        "error function" to h("Error function", """\operatorname{erf}(x) = \frac{2}{\sqrt{\pi}} \int_0^x e^{-t^2}\,dt"""),
        "digamma function" to h("Digamma function", """\psi(x) = \frac{\Gamma'(x)}{\Gamma(x)}"""),
        "Hurwitz zeta function" to h("Hurwitz zeta function", """\zeta(s, a) = \sum_{n=0}^{\infty} \frac{1}{(n + a)^s}"""),
        "hyperbolic sine integral" to h("Hyperbolic sine integral", """\operatorname{Shi}(x) = \int_0^x \frac{\sinh t}{t}\,dt"""),
        "hyperbolic cosine integral" to h("Hyperbolic cosine integral", """\operatorname{Chi}(x) = \gamma + \ln x + \int_0^x \frac{\cosh t - 1}{t}\,dt"""),
        "secant" to h("Secant", """\sec x = \frac{1}{\cos x}"""),
        "cosecant" to h("Cosecant", """\csc x = \frac{1}{\sin x}"""),
        "cotangent" to h("Cotangent", """\cot x = \frac{\cos x}{\sin x}"""),
        "inverse secant" to h("Inverse secant", """\operatorname{arcsec} x = \arccos\frac{1}{x}"""),
        "inverse cosecant" to h("Inverse cosecant", """\operatorname{arccsc} x = \arcsin\frac{1}{x}"""),
        "inverse cotangent" to h("Inverse cotangent", """\operatorname{arccot} x = \arctan\frac{1}{x}"""),
        "hyperbolic secant" to h("Hyperbolic secant", """\operatorname{sech} x = \frac{1}{\cosh x}"""),
        "hyperbolic cosecant" to h("Hyperbolic cosecant", """\operatorname{csch} x = \frac{1}{\sinh x}"""),
        "hyperbolic cotangent" to h("Hyperbolic cotangent", """\operatorname{coth} x = \frac{\cosh x}{\sinh x}"""),
        "inverse hyperbolic secant" to h("Inverse hyperbolic secant", """\operatorname{arsech} x = \operatorname{arcosh}\frac{1}{x}"""),
        "inverse hyperbolic cosecant" to h("Inverse hyperbolic cosecant", """\operatorname{arcsch} x = \operatorname{arsinh}\frac{1}{x}"""),
        "inverse hyperbolic cotangent" to h("Inverse hyperbolic cotangent", """\operatorname{arcoth} x = \operatorname{artanh}\frac{1}{x}"""),
        "Heaviside step" to h("Heaviside step", """H(x) = \begin{cases} 0 & x < 0 \\ 1 & x > 0 \end{cases}"""),
        "rectangle function" to h("Rectangle function", """\operatorname{rect}(x) = H\left(\tfrac{1}{2} - |x|\right)"""),
        "triangle function" to h("Triangle function", """\operatorname{tri}(x) = \max(1 - |x|,\ 0)"""),
        "ramp function" to h("Ramp", """\operatorname{ramp}(x) = \max(x, 0)"""),
        "pulse" to h("Pulse", """\operatorname{pulse}(x, a, b) = H(x - a) - H(x - b)"""),
        "sawtooth wave" to h("Sawtooth wave", """\operatorname{saw}(x) = x - \lfloor x \rfloor"""),
        "square wave" to h("Square wave", """\operatorname{sq}(x) = \operatorname{sgn}\sin 2\pi x"""),
        "triangle wave" to h("Triangle wave", """\operatorname{tw}(x) = 4\left|x - \left\lfloor x + \tfrac{1}{2} \right\rfloor\right| - 1"""),
        "logistic sigmoid" to h("Logistic sigmoid", """\sigma(x) = \frac{1}{1 + e^{-x}}"""),
        "softplus" to h("Softplus", """\operatorname{softplus}(x) = \ln(1 + e^{x})"""),
        "clamp" to h("Clamp", """\operatorname{clamp}(x, a, b) = \min(\max(x, a),\ b)"""),
        "linear interpolation" to h("Linear interpolation", """\operatorname{lerp}(a, b, t) = a + (b - a)\,t"""),
        "smoothstep" to h("Smoothstep", """S(x) = 3u^2 - 2u^3,\quad u = \operatorname{clamp}(x, 0, 1)"""),
        "Gaussian" to h("Gaussian", """g(x) = e^{-x^2}"""),
        "wrap" to h("Wrap", """\operatorname{wrap}(x, a, b) = x - (b - a)\left\lfloor \frac{x - a}{b - a} \right\rfloor"""),
        "exponential density" to h("Exponential density", """f(x) = \lambda e^{-\lambda x},\quad x \ge 0"""),
        "exponential distribution function" to h("Exponential distribution", """F(x) = 1 - e^{-\lambda x}"""),
        "uniform density" to h("Uniform density", """f(x) = \frac{1}{b - a},\quad a < x < b"""),
        "uniform distribution function" to h("Uniform distribution", """F(x) = \frac{x - a}{b - a}"""),
        "cumulative Poisson probability" to h("Cumulative Poisson", """P(X \le k) = e^{-\lambda} \sum_{i=0}^{k} \frac{\lambda^i}{i!}"""),
        "geometric probability" to h("Geometric probability", """P(X = k) = (1 - p)^{k - 1}\, p"""),
        "cumulative geometric probability" to h("Cumulative geometric", """P(X \le k) = 1 - (1 - p)^k"""),
        "chi-squared density" to h("Chi-squared density", """f(x) = \frac{x^{k/2 - 1} e^{-x/2}}{2^{k/2}\,\Gamma(k/2)}"""),
        "chi-squared distribution function" to h("Chi-squared distribution", """F(x) = 1 - \frac{\Gamma(k/2,\ x/2)}{\Gamma(k/2)}"""),
        "log-normal density" to h("Log-normal density", """f(x) = \frac{1}{x\sigma\sqrt{2\pi}}\, e^{-\frac{(\ln x - \mu)^2}{2\sigma^2}}"""),
        "log-normal distribution function" to h("Log-normal distribution", """F(x) = \frac{1}{2}\left(1 + \operatorname{erf}\frac{\ln x - \mu}{\sigma\sqrt{2}}\right)"""),
        "Cauchy density" to h("Cauchy density", """f(x) = \frac{1}{\pi\gamma\left(1 + \left(\frac{x - x_0}{\gamma}\right)^2\right)}"""),
        "Cauchy distribution function" to h("Cauchy distribution", """F(x) = \frac{1}{2} + \frac{1}{\pi}\arctan\frac{x - x_0}{\gamma}"""),
        "Weibull density" to h("Weibull density", """f(x) = \frac{k}{\lambda}\left(\frac{x}{\lambda}\right)^{k-1} e^{-(x/\lambda)^k}"""),
        "Weibull distribution function" to h("Weibull distribution", """F(x) = 1 - e^{-(x/\lambda)^k}"""),
        "Stirling number of the first kind" to h("Stirling numbers, first kind", """s(n, k):\quad s(4, 2) = 11"""),
        "Stirling number of the second kind" to h("Stirling numbers, second kind", """S(n, k):\quad S(4, 2) = 7"""),
        "Bell number" to h("Bell numbers", """B(n) = \sum_{k=0}^{n} S(n, k)"""),
        "derangements" to h("Derangements", """!n = n! \sum_{k=0}^{n} \frac{(-1)^k}{k!}"""),
        "Narayana number" to h("Narayana numbers", """N(n, k) = \frac{1}{n}\binom{n}{k}\binom{n}{k - 1}"""),
        "Lah number" to h("Lah numbers", """L(n, k) = \binom{n - 1}{k - 1}\frac{n!}{k!}"""),
        "Eulerian number" to h("Eulerian numbers", """A(n, k) = \sum_{j=0}^{k} (-1)^j \binom{n + 1}{j} (k + 1 - j)^n"""),
        "rising factorial" to h("Rising factorial", """x^{(n)} = x(x + 1)\cdots(x + n - 1)"""),
        "falling factorial" to h("Falling factorial", """(x)_n = x(x - 1)\cdots(x - n + 1)"""),
        "superfactorial" to h("Superfactorial", """\operatorname{sf}(n) = 1!\,2!\cdots n!"""),
        "harmonic number" to h("Harmonic numbers", """H_n = 1 + \frac{1}{2} + \cdots + \frac{1}{n}"""),
        "triangular number" to h("Triangular numbers", """T_n = \frac{n(n + 1)}{2}"""),
        "Motzkin number" to h("Motzkin numbers", """M_n:\quad 1, 1, 2, 4, 9, 21, 51, \ldots"""),
        "Pell number" to h("Pell numbers", """P_n = 2P_{n-1} + P_{n-2},\quad P_0 = 0,\ P_1 = 1"""),
        "primorial" to h("Primorial", """n\# = \prod_{p \le n} p"""),
        "Legendre polynomial" to h("Legendre polynomial", """(n + 1) P_{n+1} = (2n + 1) x P_n - n P_{n-1}"""),
        "Hermite polynomial" to h("Hermite polynomial", """H_{n+1} = 2x H_n - 2n H_{n-1}"""),
        "probabilists' Hermite polynomial" to h("Probabilists' Hermite polynomial", """He_{n+1} = x He_n - n He_{n-1}"""),
        "Laguerre polynomial" to h("Laguerre polynomial", """(n + 1) L_{n+1} = (2n + 1 - x) L_n - n L_{n-1}"""),
        "generalized Laguerre polynomial" to h("Generalized Laguerre polynomial", """(n + 1) L_{n+1}^{\alpha}(x) = (2n + 1 + \alpha - x) L_n^{\alpha}(x) - (n + \alpha) L_{n-1}^{\alpha}(x)"""),
        "Chebyshev polynomial of the first kind" to h("Chebyshev T", """T_n(\cos\theta) = \cos n\theta"""),
        "Chebyshev polynomial of the second kind" to h("Chebyshev U", """U_n(\cos\theta) = \frac{\sin (n + 1)\theta}{\sin\theta}"""),
        "Gegenbauer polynomial" to h("Gegenbauer polynomial", """(n + 1) C_{n+1}^{\alpha}(x) = 2(n + \alpha) x C_n^{\alpha}(x) - (n + 2\alpha - 1) C_{n-1}^{\alpha}(x)"""),
        "associated Legendre function" to h("Associated Legendre function", """P_n^m(x) = (-1)^m (1 - x^2)^{m/2} \frac{d^m}{dx^m} P_n(x)"""),
        "Bernoulli polynomial" to h("Bernoulli polynomial", """B_n(x) = \sum_{k=0}^{n} \binom{n}{k} B_k x^{n-k}"""),
        "Fibonacci polynomial" to h("Fibonacci polynomial", """F_{n+1} = x F_n + F_{n-1}"""),
        "Lucas polynomial" to h("Lucas polynomial", """L_{n+1} = x L_n + L_{n-1}"""),
        "Bessel polynomial" to h("Bessel polynomial", """y_{n+1} = (2n + 1) x y_n + y_{n-1}"""),
        "Touchard polynomial" to h("Touchard polynomial", """T_n(x) = \sum_{k=0}^{n} S(n, k) x^k"""),
        "cyclotomic polynomial" to h("Cyclotomic polynomial", """\Phi_n(x) = \prod (x - \omega)"""),
        "imaginary error function" to h("Imaginary error function", """\operatorname{erfi}(x) = \frac{2}{\sqrt{\pi}} \int_0^x e^{t^2}\,dt"""),
        "Fresnel sine integral" to h("Fresnel S", """S(x) = \int_0^x \sin\frac{\pi t^2}{2}\,dt"""),
        "Fresnel cosine integral" to h("Fresnel C", """C(x) = \int_0^x \cos\frac{\pi t^2}{2}\,dt"""),
        "upper incomplete gamma function" to h("Incomplete gamma", """\Gamma(s, x) = \int_x^{\infty} t^{s-1} e^{-t}\,dt"""),
        "incomplete elliptic integral of the first kind" to h("Elliptic integral F", """F(\varphi \mid m) = \int_0^{\varphi} \frac{d\theta}{\sqrt{1 - m \sin^2\theta}}"""),
        "polylogarithm" to h("Polylogarithm", """\operatorname{Li}_s(z) = \sum_{k=1}^{\infty} \frac{z^k}{k^s}"""),
        "Lambert W function" to h("Lambert W", """W(z) e^{W(z)} = z"""),
        "Bessel function of the first kind" to h("Bessel function J", """J_a(x) = \sum_{k=0}^{\infty} \frac{(-1)^k}{k!\,\Gamma(k + a + 1)}\left(\frac{x}{2}\right)^{2k + a}"""),
        "Bessel function of the second kind" to h("Bessel function Y", """Y_a(x) = \frac{J_a(x)\cos a\pi - J_{-a}(x)}{\sin a\pi}"""),
        "Hadamard product" to h("Hadamard product", """A \circ B"""),
        "Kronecker product" to h("Kronecker product", """A \otimes B"""),
        "conjugate transpose" to h("Hermitian conjugate", """A^{\mathrm{H}} = \overline{A^\top}"""),
        "store in variable" to h("Store in a variable", """a := 5"""),
        "symbol builder" to h("Symbol builder", """\hat{x}_{1},\ \dot{\theta}^{2},\ \vec{\mathfrak{g}}_{i}"""),
        "saved symbol" to h("Saved symbol", """\hat{x}_{1}"""),
        "list of constants with names" to h("Physical constants", """c,\ h,\ \hbar,\ k_{\mathrm{B}},\ N_{\mathrm{A}}"""),
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
        "Reciprocal trigonometry" to listOf("cosecant", "inverse cosecant", "hyperbolic cosecant", "inverse hyperbolic cosecant", "secant", "inverse secant", "hyperbolic secant", "inverse hyperbolic secant", "cotangent", "inverse cotangent", "hyperbolic cotangent", "inverse hyperbolic cotangent"),
        "Signals" to listOf("Heaviside step", "rectangle function", "triangle function", "ramp function", "pulse", "sawtooth wave", "square wave", "triangle wave", "logistic sigmoid", "softplus", "clamp", "linear interpolation", "smoothstep", "Gaussian", "wrap"),
        "Distributions" to listOf("exponential density", "exponential distribution function", "uniform density", "uniform distribution function", "cumulative Poisson probability", "geometric probability", "cumulative geometric probability", "chi-squared density", "chi-squared distribution function", "log-normal density", "log-normal distribution function", "Cauchy density", "Cauchy distribution function", "Weibull density", "Weibull distribution function"),
        "Combinatorics" to listOf("Stirling number of the first kind", "Stirling number of the second kind", "Bell number", "derangements", "Narayana number", "Lah number", "Eulerian number", "rising factorial", "falling factorial", "superfactorial", "harmonic number", "triangular number", "Motzkin number", "Pell number", "primorial"),
        "Polynomials" to listOf("Legendre polynomial", "Hermite polynomial", "probabilists' Hermite polynomial", "Laguerre polynomial", "generalized Laguerre polynomial", "Chebyshev polynomial of the first kind", "Chebyshev polynomial of the second kind", "Gegenbauer polynomial", "associated Legendre function", "Bernoulli polynomial", "Fibonacci polynomial", "Lucas polynomial", "Bessel polynomial", "Touchard polynomial", "cyclotomic polynomial"),
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
                listOf("Tap it wherever the constant goes.", "On this tab the Rad/Deg switch picks the units: SI, Planck, atomic or natural."),
                link = ConstantLatex.nistLink(k),
            )
        }
        // Digits and letters share one explanation each.
        if (spoken.length == 1 && spoken[0].isDigit()) return withGuide(KeyHelp("Digit $spoken"), KeyGuides.digit)
        val letterFamilies = listOf("letter ", "capital ", "calligraphic ", "fraktur ")
        val greek = setOf("alpha", "beta", "gamma", "delta", "epsilon", "zeta", "eta", "theta", "iota", "kappa", "lambda", "mu", "nu",
            "xi", "rho", "sigma", "tau", "upsilon", "phi", "chi", "psi", "omega")
        if (letterFamilies.any { spoken.startsWith(it) } || spoken in greek) {
            val name = spoken.substringAfterLast(' ')
            val tex = when {
                spoken.startsWith("calligraphic ") -> "\\mathcal{$name}"
                spoken.startsWith("fraktur ") -> "\\mathfrak{$name}"
                name.length == 1 -> name
                spoken.startsWith("capital ") -> "\\" + name.replaceFirstChar { it.uppercase() }
                else -> "\\$name"
            }
            return withGuide(KeyHelp(spoken.replaceFirstChar { it.uppercase() }, "2$tex + 3$tex = 5$tex"), KeyGuides.letter)
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

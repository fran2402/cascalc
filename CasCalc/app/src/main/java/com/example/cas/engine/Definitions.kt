package com.example.cas.engine

/**
 * What the extra functions mean, for their steps: a short title and the defining formula in
 * LaTeX (reciprocal trigonometry, distributions, signals, combinatorics, special polynomials).
 */
object Definitions {
    class Def(val title: String, val formula: String)

    private fun d(title: String, formula: String) = Def(title, formula)

    val all: Map<String, Def> = mapOf(
        // Reciprocal trigonometry.
        "sec" to d("Secant", "\\sec x = \\frac{1}{\\cos x}"),
        "csc" to d("Cosecant", "\\csc x = \\frac{1}{\\sin x}"),
        "cot" to d("Cotangent", "\\cot x = \\frac{\\cos x}{\\sin x}"),
        "asec" to d("Inverse secant", "\\operatorname{asec} x = \\arccos \\frac{1}{x}"),
        "acsc" to d("Inverse cosecant", "\\operatorname{acsc} x = \\arcsin \\frac{1}{x}"),
        "acot" to d("Inverse cotangent", "\\operatorname{acot} x = \\arctan \\frac{1}{x}"),
        "sech" to d("Hyperbolic secant", "\\operatorname{sech} x = \\frac{1}{\\cosh x}"),
        "csch" to d("Hyperbolic cosecant", "\\operatorname{csch} x = \\frac{1}{\\sinh x}"),
        "coth" to d("Hyperbolic cotangent", "\\operatorname{coth} x = \\frac{\\cosh x}{\\sinh x}"),
        "asech" to d("Inverse hyperbolic secant", "\\operatorname{asech} x = \\operatorname{acosh} \\frac{1}{x}"),
        "acsch" to d("Inverse hyperbolic cosecant", "\\operatorname{acsch} x = \\operatorname{asinh} \\frac{1}{x}"),
        "acoth" to d("Inverse hyperbolic cotangent", "\\operatorname{acoth} x = \\operatorname{atanh} \\frac{1}{x}"),
        // Distributions.
        "exppdf" to d("Exponential density", "f(x) = \\lambda e^{-\\lambda x}, \\quad x \\ge 0"),
        "expcdf" to d("Exponential distribution", "F(x) = 1 - e^{-\\lambda x}, \\quad x \\ge 0"),
        "unifpdf" to d("Uniform density", "f(x) = \\frac{1}{b - a}, \\quad a \\le x \\le b"),
        "unifcdf" to d("Uniform distribution", "F(x) = \\frac{x - a}{b - a}, \\quad a \\le x \\le b"),
        "poissoncdf" to d("Cumulative Poisson", "P(X \\le k) = \\sum_{j=0}^{k} \\frac{\\lambda^j e^{-\\lambda}}{j!}"),
        "geompdf" to d("Geometric probability", "P(X = k) = (1 - p)^{k-1} p"),
        "geomcdf" to d("Cumulative geometric", "P(X \\le k) = 1 - (1 - p)^k"),
        "chi2pdf" to d("Chi-squared density", "f(x) = \\frac{x^{k/2 - 1} e^{-x/2}}{2^{k/2} \\Gamma(k/2)}"),
        "chi2cdf" to d("Chi-squared distribution", "F(x) = \\frac{\\gamma(k/2, x/2)}{\\Gamma(k/2)}"),
        "lognpdf" to d("Log-normal density", "f(x) = \\frac{1}{x \\sigma \\sqrt{2\\pi}} e^{-\\frac{(\\ln x - \\mu)^2}{2\\sigma^2}}"),
        "logncdf" to d("Log-normal distribution", "F(x) = \\Phi\\left(\\frac{\\ln x - \\mu}{\\sigma}\\right)"),
        "cauchypdf" to d("Cauchy density", "f(x) = \\frac{1}{\\pi \\gamma \\left(1 + \\left(\\frac{x - x_0}{\\gamma}\\right)^2\\right)}"),
        "cauchycdf" to d("Cauchy distribution", "F(x) = \\frac{1}{2} + \\frac{1}{\\pi} \\arctan \\frac{x - x_0}{\\gamma}"),
        "weibpdf" to d("Weibull density", "f(x) = \\frac{k}{\\lambda} \\left(\\frac{x}{\\lambda}\\right)^{k-1} e^{-(x/\\lambda)^k}"),
        "weibcdf" to d("Weibull distribution", "F(x) = 1 - e^{-(x/\\lambda)^k}"),
        // Signals.
        "heaviside" to d("Heaviside step", "H(x) = 0 \\text{ for } x < 0, \\quad 1 \\text{ for } x \\ge 0"),
        "rect" to d("Rectangle function", "\\operatorname{rect} x = 1 \\text{ for } |x| < \\frac{1}{2}, \\quad 0 \\text{ outside}"),
        "tri" to d("Triangle function", "\\operatorname{tri} x = \\max(1 - |x|, 0)"),
        "ramp" to d("Ramp", "\\operatorname{ramp} x = \\max(x, 0)"),
        "pulse" to d("Pulse", "1 \\text{ for } a \\le x \\le b, \\quad 0 \\text{ outside}"),
        "sawtooth" to d("Sawtooth wave", "x - \\lfloor x \\rfloor"),
        "squarewave" to d("Square wave", "\\operatorname{sgn} \\sin 2\\pi x"),
        "trianglewave" to d("Triangle wave", "1 - 4 \\left| x - \\lfloor x \\rfloor - \\frac{1}{2} \\right|"),
        "sigmoid" to d("Logistic sigmoid", "\\sigma(x) = \\frac{1}{1 + e^{-x}}"),
        "softplus" to d("Softplus", "\\ln(1 + e^x)"),
        "clamp" to d("Clamp", "\\min(\\max(x, a), b)"),
        "lerp" to d("Linear interpolation", "a + (b - a) t"),
        "smoothstep" to d("Smoothstep", "3x^2 - 2x^3 \\text{ for } 0 \\le x \\le 1"),
        "gauss" to d("Gaussian", "e^{-x^2}"),
        "wrap" to d("Wrap", "a + (x - a) \\bmod (b - a)"),
        // Combinatorics.
        "stirling1" to d("Stirling numbers of the first kind", "s(n + 1, k) = s(n, k - 1) - n \\, s(n, k)"),
        "stirling2" to d("Stirling numbers of the second kind", "S(n, k) = \\frac{1}{k!} \\sum_{j=0}^{k} (-1)^j \\binom{k}{j} (k - j)^n"),
        "bell" to d("Bell numbers", "B(n) = \\sum_{k=0}^{n} S(n, k)"),
        "subfactorial" to d("Derangements", "!n = n! \\sum_{k=0}^{n} \\frac{(-1)^k}{k!}"),
        "narayana" to d("Narayana numbers", "N(n, k) = \\frac{1}{n} \\binom{n}{k} \\binom{n}{k - 1}"),
        "lah" to d("Lah numbers", "L(n, k) = \\binom{n - 1}{k - 1} \\frac{n!}{k!}"),
        "eulerian" to d("Eulerian numbers", "A(n, k) = \\sum_{j=0}^{k} (-1)^j \\binom{n + 1}{j} (k + 1 - j)^n"),
        "rising" to d("Rising factorial", "x^{(n)} = x (x + 1) \\cdots (x + n - 1)"),
        "falling" to d("Falling factorial", "(x)_n = x (x - 1) \\cdots (x - n + 1)"),
        "superfactorial" to d("Superfactorial", "\\operatorname{sf}(n) = \\prod_{k=1}^{n} k!"),
        "harmonic" to d("Harmonic numbers", "H_n = \\sum_{k=1}^{n} \\frac{1}{k}"),
        "triangular" to d("Triangular numbers", "T_n = \\frac{n (n + 1)}{2}"),
        "motzkin" to d("Motzkin numbers", "M_{n+1} = M_n + \\sum_{k=0}^{n-1} M_k M_{n-1-k}"),
        "pell" to d("Pell numbers", "P_n = 2 P_{n-1} + P_{n-2}, \\quad P_0 = 0, P_1 = 1"),
        "primorial" to d("Primorial", "n\\# = \\prod_{p \\le n} p"),
        // Polynomials.
        "legendre" to d("Legendre polynomials", "(n + 1) P_{n+1}(x) = (2n + 1) x P_n(x) - n P_{n-1}(x)"),
        "hermite" to d("Hermite polynomials", "H_{n+1}(x) = 2x H_n(x) - 2n H_{n-1}(x)"),
        "hermitehe" to d("Probabilists' Hermite polynomials", "He_{n+1}(x) = x He_n(x) - n He_{n-1}(x)"),
        "laguerre" to d("Laguerre polynomials", "(n + 1) L_{n+1}(x) = (2n + 1 - x) L_n(x) - n L_{n-1}(x)"),
        "genlaguerre" to d("Generalized Laguerre polynomials", "(n + 1) L_{n+1}^{\\alpha} = (2n + 1 + \\alpha - x) L_n^{\\alpha} - (n + \\alpha) L_{n-1}^{\\alpha}"),
        "chebyshevt" to d("Chebyshev polynomials of the first kind", "T_{n+1}(x) = 2x T_n(x) - T_{n-1}(x)"),
        "chebyshevu" to d("Chebyshev polynomials of the second kind", "U_{n+1}(x) = 2x U_n(x) - U_{n-1}(x)"),
        "gegenbauer" to d("Gegenbauer polynomials", "(n + 1) C_{n+1}^{\\alpha} = 2(n + \\alpha) x C_n^{\\alpha} - (n + 2\\alpha - 1) C_{n-1}^{\\alpha}"),
        "assoclegendre" to d("Associated Legendre functions", "P_n^m(x) = (-1)^m (1 - x^2)^{m/2} \\frac{d^m}{dx^m} P_n(x)"),
        "bernoullipoly" to d("Bernoulli polynomials", "B_n(x) = \\sum_{k=0}^{n} \\binom{n}{k} B_k x^{n-k}"),
        "fibpoly" to d("Fibonacci polynomials", "F_{n+1}(x) = x F_n(x) + F_{n-1}(x)"),
        "lucaspoly" to d("Lucas polynomials", "L_{n+1}(x) = x L_n(x) + L_{n-1}(x)"),
        "besselpoly" to d("Bessel polynomials", "y_{n+1}(x) = (2n + 1) x y_n(x) + y_{n-1}(x)"),
        "touchard" to d("Touchard polynomials", "T_n(x) = \\sum_{k=0}^{n} S(n, k) x^k"),
        "cyclotomic" to d("Cyclotomic polynomials", "\\Phi_n(x) = \\prod_{\\gcd(k, n) = 1} \\left(x - e^{2\\pi i k/n}\\right)"),
    )
}

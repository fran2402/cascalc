package com.example.cas.cas

import com.example.cas.math.Rational
import com.example.cas.math.exactRoot
import java.math.BigInteger

/**
 * Automatic simplification, applied every time an expression is built:
 *  - sums: flatten, add numbers, combine like terms (2x + 3x = 5x)
 *  - products: flatten, multiply numbers, combine equal bases (x·x² = x³),
 *    merge radicals (√2·√3 = √6), i·i = −1
 *  - powers: exact rational powers with surds extracted (√8 = 2√2, 1/√2 = √2/2),
 *    (x^a)^n, (xy)^n, e^(ln x) = x
 *  - functions: exact values (sin(π/6) = 1/2, asin(√3/2) = π/3, ln e = 1, 5! = 120),
 *    symmetry (sin(−x) = −sin x) and inverse pairs (sin(asin x) = x).
 * Products are never expanded automatically; that's what [Algebra.expand] is for.
 */
object Simplify {

    // ---- Canonical order -------------------------------------------------------

    private fun rank(e: Expr): Int = when (e) {
        is Num, is Flt -> 0
        is Pow -> if (e.base is Num) 1 else if (e.base is Sym) 4 else 6
        is Sym -> if (e.name == "i") 2 else if (e.name in CONSTANT_SYMBOLS) 3 else 4
        is Fn -> 5
        is Add -> 7
        is Mul -> 8
        else -> 9
    }

    val order = Comparator<Expr> { a, b ->
        val r = rank(a) - rank(b)
        if (r != 0) r else Printer.key(a).compareTo(Printer.key(b))
    }

    // ---- Sums --------------------------------------------------------------------

    fun sum(xs: List<Expr>): Expr {
        val flat = ArrayList<Expr>()
        for (x in xs) if (x is Add) flat += x.terms else flat += x
        if (flat.any { it is Eq || it is Seq || it is Rel }) throw MathError("Can't add equations")
        if (flat.any { it is Mat }) return Matrices.addAll(flat)

        var q = Rational.ZERO
        var f: Double? = null
        val groups = LinkedHashMap<Expr, Expr>()
        for (t in flat) when (t) {
            is Num -> q += t.q
            is Flt -> f = (f ?: 0.0) + t.d
            else -> {
                val (c, m) = splitCoefficient(t)
                groups[m] = groups[m]?.let { numAdd(it, c) } ?: c
            }
        }
        val terms = ArrayList<Expr>()
        for ((m, c) in groups) if (!isZeroNumber(c)) terms += product(listOf(c, m))
        if (f != null) { val v = f!! + q.toDouble(); if (v != 0.0 || terms.isEmpty()) terms += Flt(v) }
        else if (q.signum != 0) terms += Num(q)
        val out = when (terms.size) {
            0 -> ZERO
            1 -> terms[0]
            else -> Add(terms.sortedWith(order))
        }
        return collapseFloats(out)
    }

    /** 3x²y → (3, x²y). */
    fun splitCoefficient(t: Expr): Pair<Expr, Expr> {
        if (t is Mul && t.factors[0].isNumber) {
            val rest = t.factors.drop(1)
            return t.factors[0] to (if (rest.size == 1) rest[0] else Mul(rest))
        }
        return ONE to t
    }

    private fun numAdd(a: Expr, b: Expr): Expr =
        if (a is Num && b is Num) Num(a.q + b.q) else Flt(numValue(a) + numValue(b))

    private fun numValue(a: Expr) = when (a) { is Num -> a.q.toDouble(); is Flt -> a.d; else -> error("not a number") }
    private fun isZeroNumber(a: Expr) = (a is Num && a.q.signum == 0) || (a is Flt && a.d == 0.0)

    /** Anything constant that already contains a decimal becomes a decimal: 0.5 + π → 3.6415… */
    private fun collapseFloats(e: Expr): Expr {
        if (e is Flt || e is Num || !e.contains { it is Flt } || !e.isConstant) return e
        return runCatching { Numeric.fromCD(Numeric.eval(e)) }.getOrDefault(e)
    }

    // ---- Products ------------------------------------------------------------------

    fun product(xs: List<Expr>): Expr {
        val flat = ArrayList<Expr>()
        for (x in xs) if (x is Mul) flat += x.factors else flat += x
        if (flat.any { it is Eq || it is Seq || it is Rel }) throw MathError("Can't multiply equations")
        if (flat.any { it is Mat }) return Matrices.multiply(flat)

        var c = Rational.ONE
        var f: Double? = null
        var factors: List<Expr> = flat
        var previous: List<Expr>? = null
        for (round in 0 until 6) {
            val bases = LinkedHashMap<Expr, MutableList<Expr>>()
            for (x in factors) when (x) {
                is Num -> c *= x.q
                is Flt -> f = (f ?: 1.0) * x.d
                is Pow -> bases.getOrPut(x.base) { mutableListOf() }.add(x.exp)
                else -> bases.getOrPut(x) { mutableListOf() }.add(ONE)
            }
            if (c.signum == 0) return ZERO
            val built = ArrayList<Expr>()
            for ((b, es) in bases) {
                val p = power(b, sum(es))
                if (p is Mul) built += p.factors else built += p
            }
            // √2·√3 → √6: numeric radicals with the same exponent share one root.
            val radicals = built.filter { it is Pow && it.base is Num && it.exp is Num }.groupBy { (it as Pow).exp }
            val merged = ArrayList<Expr>(built.filterNot { it is Pow && it.base is Num && it.exp is Num })
            for ((e, group) in radicals) {
                if (group.size == 1) merged += group[0]
                else {
                    val base = group.fold(Rational.ONE) { acc, g -> acc * ((g as Pow).base as Num).q }
                    val p = powerNum(base, (e as Num).q)
                    if (p is Mul) merged += p.factors else merged += p
                }
            }
            val numbersLeft = merged.any { it is Num || it is Flt }
            val sorted = merged.sortedWith(order)
            factors = sorted
            if (!numbersLeft && sorted == previous) break
            previous = sorted
        }
        // Absorb any numbers produced in the last round.
        val rest = ArrayList<Expr>()
        for (x in factors) when (x) {
            is Num -> c *= x.q
            is Flt -> f = (f ?: 1.0) * x.d
            else -> rest += x
        }
        if (c.signum == 0) return ZERO
        val coefficient: Expr? = when {
            f != null -> Flt(f!! * c.toDouble())
            c != Rational.ONE -> Num(c)
            else -> null
        }
        val all = if (coefficient != null) listOf(coefficient) + rest.sortedWith(order) else rest.sortedWith(order)
        val out = when (all.size) {
            0 -> ONE
            1 -> all[0]
            else -> Mul(all)
        }
        return collapseFloats(out)
    }

    // ---- Powers ------------------------------------------------------------------------

    fun power(b: Expr, e: Expr): Expr {
        if (b is Mat) return Matrices.power(b, e)
        if (b is Eq || e is Eq || b is Seq || e is Seq || b is Rel || e is Rel) throw MathError("Can't raise an equation to a power")
        if (e is Mat) throw MathError("An exponent can't be a matrix")
        return when {
            e is Num && e.q.signum == 0 -> if (b == ZERO) throw MathError("0^0 is undefined") else ONE
            e == ONE -> b
            b == ONE -> ONE
            b == ZERO -> when {
                (e is Num && e.q.signum > 0) || (e is Flt && e.d > 0) -> ZERO
                e is Num || e is Flt -> throw MathError("Can't divide by 0")
                else -> Pow(b, e)
            }
            b is Num && e is Num -> powerNum(b.q, e.q)
            // √(a + bi) exactly when |a + bi| is rational: √(3 + 4i) = 2 + i.
            e == HALF && b is Add && b.isConstant && I in b.terms.flatMap { if (it is Mul) it.factors else listOf(it) } -> complexSqrt(b) ?: Pow(b, e)
            (b is Flt || e is Flt) && b.isConstant && e.isConstant -> Numeric.fromCD(Numeric.eval(Pow(b, e)))
            b == I && e is Num && e.q.isInteger -> when (e.q.num.mod(BigInteger.valueOf(4)).toInt()) {
                0 -> ONE; 1 -> I; 2 -> MINUS_ONE; else -> Mul(listOf(MINUS_ONE, I))
            }
            b is Pow && e is Num && e.q.isInteger -> power(b.base, product(listOf(b.exp, e)))
            b is Pow && isPositiveConstant(b.base) -> power(b.base, product(listOf(b.exp, e)))
            b is Mul && e is Num && e.q.isInteger -> product(b.factors.map { power(it, e) })
            // (−x − 1)⁻¹ = −(x + 1)⁻¹: keep sums in brackets positive.
            b is Add && e is Num && e.q.isInteger && b.terms.all { isNegative(it) } ->
                product(listOf(power(MINUS_ONE, e), power(sum(b.terms.map { product(listOf(MINUS_ONE, it)) }), e)))
            b is Mul && e is Num && b.factors[0] is Num && (b.factors[0] as Num).q.signum > 0 -> {
                val rest = b.factors.drop(1)
                product(listOf(power(b.factors[0], e), Pow(if (rest.size == 1) rest[0] else Mul(rest), e)))
            }
            b == E && e is Fn && e.name == "ln" -> e.args[0]
            // e^(ln a + qπi + rest) = a · (cos qπ + i sin qπ) · e^rest, e.g. e^(ln 2 + πi) = −2.
            b == E && e is Add && e.terms.any { t -> isLn(t) || piImaginaryPart(t) != null } -> {
                val lnArgs = e.terms.filter { isLn(it) }.map { (it as Fn).args[0] }
                val turns = e.terms.mapNotNull { piImaginaryPart(it) }
                val rest = e.terms.filter { !isLn(it) && piImaginaryPart(it) == null }
                val q = turns.fold(Rational.ZERO) { acc, x -> acc + x }
                val unit = sum(listOf(function("cos", listOf(product(listOf(Num(q), PI)))), product(listOf(I, function("sin", listOf(product(listOf(Num(q), PI))))))))
                product(lnArgs + unit + (if (rest.isEmpty()) emptyList() else listOf(power(E, sum(rest)))))
            }
            b == E && e is Mul && e.factors.size == 2 && e.factors[0] is Num && (e.factors[1] as? Fn)?.name == "ln" ->
                power((e.factors[1] as Fn).args[0], e.factors[0])
            else -> Pow(b, e)
        }
    }

    private fun isPositiveConstant(x: Expr) = (x is Num && x.q.signum > 0) || x == PI || x == E

    /** Exact rational powers, with q-th powers pulled out of roots and denominators made rational. */
    fun powerNum(b: Rational, e: Rational): Expr {
        if (e.isInteger) {
            val n = e.num
            if (b.signum == 0) return if (n.signum() > 0) ZERO else throw MathError("Can't divide by 0")
            // Beyond about 120 000 digits, stop: exact arithmetic (and showing it) would take too long.
            if (n.bitLength() > 31 || (b.num.bitLength() + b.den.bitLength()).toLong() * n.abs().toLong() > 400_000) {
                if (b.abs() == Rational.ONE) return Num(if (b.signum > 0 || !n.testBit(0)) Rational.ONE else -Rational.ONE)
                throw MathError("That number is too large")
            }
            return Num(b.pow(n.toInt()))
        }
        if (b.signum == 0) return if (e.signum > 0) ZERO else throw MathError("Can't divide by 0")
        if (e.den.bitLength() > 7 || e.num.bitLength() > 30) return Pow(Num(b), Num(e))
        val q = e.den.toInt()
        val p = e.num.toInt()
        if (b.signum < 0) {
            if (q % 2 == 1) {
                val m = powerNum(-b, e)
                return if (p % 2 != 0) product(listOf(MINUS_ONE, m)) else m
            }
            if (q == 2) return product(listOf(powerNum(-b, e), power(I, Num(p.toLong()))))
            return Pow(Num(b), Num(e))
        }
        val k = Math.floorDiv(p, q)
        val r = p - k * q
        // b^(p/q) = b^k · num^(r/q) · den^((q−r)/q) / den
        val (c1, r1) = extractRoot(b.num, r, q) ?: return Pow(Num(b), Num(e))
        val (c2, r2) = extractRoot(b.den, q - r, q) ?: return Pow(Num(b), Num(e))
        val coefficient = b.pow(k) * Rational.of(c1) * Rational.of(c2) / Rational.of(b.den)
        val exps = HashMap<BigInteger, Int>()
        for ((pr, x) in r1) exps[pr] = (exps[pr] ?: 0) + x
        for ((pr, x) in r2) exps[pr] = (exps[pr] ?: 0) + x
        val radical = radical(exps, q)
        return when {
            radical == null -> Num(coefficient)
            coefficient == Rational.ONE -> radical
            else -> Mul(listOf(Num(coefficient), radical))
        }
    }

    /** m^(s/q) = coefficient · (∏ p^rem)^(1/q); returns the coefficient and the leftover prime exponents. */
    private fun extractRoot(m: BigInteger, s: Int, q: Int): Pair<BigInteger, Map<BigInteger, Int>>? {
        if (m == BigInteger.ONE || s == 0) return BigInteger.ONE to emptyMap()
        val primes = factorize(m) ?: return null
        var coefficient = BigInteger.ONE
        val rest = HashMap<BigInteger, Int>()
        for ((pr, a) in primes) {
            val t = a * s
            coefficient *= pr.pow(t / q)
            if (t % q != 0) rest[pr] = t % q
        }
        return coefficient to rest
    }

    private fun radical(exps: Map<BigInteger, Int>, q: Int): Expr? {
        if (exps.isEmpty()) return null
        var g = q
        for (x in exps.values) g = gcd(g, x)
        var radicand = BigInteger.ONE
        for ((pr, x) in exps) radicand *= pr.pow(x / g)
        if (radicand == BigInteger.ONE) return null
        return Pow(Num(Rational.of(radicand)), Num(Rational.of(1, (q / g).toLong())))
    }

    private tailrec fun gcd(a: Int, b: Int): Int = if (b == 0) a else gcd(b, a % b)

    /** Prime factorization by trial division; null if the number is too large to factor quickly. */
    fun factorize(m: BigInteger): Map<BigInteger, Int>? {
        var n = m
        val out = LinkedHashMap<BigInteger, Int>()
        var d = BigInteger.TWO
        val limit = BigInteger.valueOf(1_000_000)
        while (d * d <= n && d <= limit) {
            while ((n % d).signum() == 0) { out[d] = (out[d] ?: 0) + 1; n /= d }
            d = if (d == BigInteger.TWO) BigInteger.valueOf(3) else d + BigInteger.TWO
        }
        if (n > BigInteger.ONE) {
            if (d * d <= n) {
                // Large leftover: only handle it if it's a perfect square.
                val s = exactRoot(n, 2) ?: return if (n.bitLength() < 128) out.also { it[n] = (it[n] ?: 0) + 1 } else null
                out[s] = (out[s] ?: 0) + 2
            } else out[n] = (out[n] ?: 0) + 1
        }
        return out
    }

    // ---- Functions -------------------------------------------------------------------

    private val ODD = setOf("sin", "tan", "asin", "atan", "sinh", "tanh", "asinh", "atanh", "erf", "erfi", "si", "shi", "fresnels", "fresnelc")
    private val INVERSE = mapOf(
        "sin" to "asin", "cos" to "acos", "tan" to "atan",
        "sinh" to "asinh", "cosh" to "acosh", "tanh" to "atanh", "ln" to "exp",
    )

    fun function(name: String, args: List<Expr>): Expr {
        // A held integral stays as it is until something integrates it numerically.
        if (name == "integral") return Fn(name, args)
        // A held Σ or Π is worked out once its bounds are numbers.
        if ((name == "sum" || name == "product") && args.size == 4) {
            val (body, v, lo, hi) = args
            // Its variable is only a variable until something substitutes for it; then the sum is over.
            val k = v as? Sym ?: return if (lo.isConstant && hi.isConstant) body else Fn(name, args)
            return if (lo.isConstant && hi.isConstant) Calculus.sum(body, k, lo, hi, name == "product")
            else Fn(name, args)
        }
        // A held contour integral is worked out once its circle is numeric (a limit substituting R = 3, say).
        if (name == "contour" && args.size == 4) {
            return if (args[2].isConstant && args[3].isConstant) com.example.cas.graph.ComplexIntegrals.circle(args[0], args[1] as Sym, args[2], args[3])
            else Fn(name, args)
        }
        if (args.any { it is Eq || it is Seq }) throw MathError("$name can't take an equation")
        val x = args[0]
        if (args.all { it.isConstant } && args.any { it.contains { e -> e is Flt } } && name in NUMERIC_OK) {
            return Numeric.fromCD(Numeric.eval(Fn(name, args)))
        }
        // Odd functions: sin(−x) = −sin(x); cos is even.
        if (args.size == 1 && isNegative(x)) {
            // Odd: f(−x) = −f(x), except asin on its branch cut (|x| > 1), where the principal value isn't odd.
            val onCut = name == "asin" && x.isConstant && x !is Mat && runCatching { kotlin.math.abs(Numeric.real(x)) > 1 }.getOrDefault(false)
            if (name in ODD && !onCut) return product(listOf(MINUS_ONE, function(name, listOf(product(listOf(MINUS_ONE, x))))))
            if (name == "cos" || name == "cosh" || name == "abs") return function(name, listOf(product(listOf(MINUS_ONE, x))))
        }
        // sin(asin x) = x …
        if (args.size == 1 && x is Fn && INVERSE[name] == x.name && name != "ln") return x.args[0]
        // cos(asin z) = √(1 − z²), cosh(asinh z) = √(1 + z²), sinh(acosh z) = √(z² − 1), … (what
        // a trigonometric substitution leaves behind).
        if (args.size == 1 && x is Fn && x.args.size == 1) {
            val z = x.args[0]
            fun root(e: Expr) = power(e, HALF)
            val zz = power(z, TWO)
            when (name to x.name) {
                "cos" to "asin", "sin" to "acos" -> return root(sum(listOf(ONE, product(listOf(MINUS_ONE, zz)))))
                "tan" to "asin" -> return product(listOf(z, power(sum(listOf(ONE, product(listOf(MINUS_ONE, zz)))), num(-1, 2))))
                "cosh" to "asinh" -> return root(sum(listOf(zz, ONE)))
                "sinh" to "acosh" -> return root(sum(listOf(zz, MINUS_ONE)))
                "cos" to "atan" -> return power(sum(listOf(zz, ONE)), num(-1, 2))
                "sin" to "atan" -> return product(listOf(z, power(sum(listOf(zz, ONE)), num(-1, 2))))
            }
        }
        return when (name) {
            "sin", "cos", "tan" -> trig(name, x) ?: Fn(name, args)
            "asin", "acos", "atan" -> inverseTrig(name, x) ?: complexInverseTrig(name, x) ?: Fn(name, args)
            "sinh", "tanh", "asinh", "atanh" -> if (x == ZERO) ZERO else Fn(name, args)
            "cosh" -> if (x == ZERO) ONE else Fn(name, args)
            "acosh" -> if (x == ONE) ZERO else Fn(name, args)
            "ln" -> when {
                x == ONE -> ZERO
                x == E -> ONE
                x == ZERO -> throw MathError("ln(0) is undefined")
                x is Pow && x.base == E -> x.exp
                // Negative and complex numbers: ln|z| + i arg z, so ln(−2) = ln 2 + πi, ln i = πi/2.
                x.isConstant && x !is Mat && isComplexOrNegative(x) -> {
                    val (re, im) = splitComplex(x)
                    val modulus = if (im == ZERO) power(re, TWO).let { power(it, HALF) } else power(sum(listOf(power(re, TWO), power(im, TWO))), HALF)
                    sum(listOf(function("ln", listOf(modulus)), product(listOf(exactArg(re, im), I))))
                }
                else -> Fn(name, args)
            }
            "log" -> log(args[0], args[1])
            "abs" -> abs(x)
            "fact" -> factorial(x)
            "binom" -> binomial(args[0], args[1])
            "mod" -> mod(args[0], args[1])
            "gcd", "lcm" -> gcdLcm(name, args[0], args[1])
            "floor", "ceil", "round" -> rounding(name, x)
            "Re", "Im", "conj" -> complexPart(name, x)
            "arg" -> if (x.isConstant && x !is Mat) splitComplex(x).let { (re, im) -> exactArg(re, im) } else Fn(name, args)
            "min", "max" -> minMax(name, args[0], args[1])
            // The step at a number: 1 from 0 on, 0 before; the impulse is 0 away from 0.
            "heaviside" -> if (x.isConstant && x !is Mat) runCatching { if (Numeric.real(x) >= 0) ONE else ZERO }.getOrDefault(Fn(name, args)) else Fn(name, args)
            "dirac" -> when {
                x.isConstant && x !is Mat && runCatching { Numeric.real(x) != 0.0 }.getOrDefault(false) -> ZERO
                // δ is even: δ(−u) = δ(u), written with the leading sign positive.
                x is Mul && (x.factors.first() as? Num)?.q?.signum == -1 -> Fn(name, listOf(product(listOf(MINUS_ONE, x))))
                else -> Fn(name, args)
            }
            "sgn" -> when {
                x is Num -> Num(x.q.signum.toLong())
                x.isConstant && x !is Mat -> Num(kotlin.math.sign(Numeric.real(x)).toLong())
                else -> Fn(name, args)
            }
            // Fractional part {x} = x − ⌊x⌋.
            "frac" -> if (x is Num) Num(x.q - com.example.cas.math.Rational.of(x.q.floor())) else Fn(name, args)
            "gamma" -> gammaExact(x) ?: Fn(name, args)
            "erf" -> if (x == ZERO) ZERO else Fn(name, args)
            // No exact values here: ψ and ζ′ at numbers are decimals.
            "digamma", "zetaprime", "lambertw", "besselj", "bessely" ->
                if (args.all { it.isConstant && it !is Mat }) Numeric.fromCD(Numeric.eval(Fn(name, args))) else Fn(name, args)
            "perm" -> Statistics.permutations(args[0], args[1])
            // Φ⁻¹(p): the z with Φ(z) = p, a decimal (½ gives exactly 0).
            "invnorm" -> when {
                x == Num(Rational.of(1, 2)) -> ZERO
                x.isConstant && x !is Mat -> Flt(Statistics.inverseNormal(Numeric.real(x)))
                else -> Fn(name, args)
            }
            "zeta" -> zetaExact(x) ?: Fn(name, args)
            "hurwitz" -> hurwitzExact(args[0], args[1]) ?: Fn(name, args)
            // Odd functions and integrals from 0 vanish at 0.
            "si", "shi", "erfi", "fresnels", "fresnelc" -> if (x == ZERO) ZERO else if (x is Mul && isNegative(x)) product(listOf(MINUS_ONE, function(name, listOf(product(listOf(MINUS_ONE, x)))))) else Fn(name, args)
            "ei" -> if (x == ZERO) throw MathError("Ei has a pole at 0") else Fn(name, args)
            "li" -> if (x == ONE) throw MathError("li has a pole at 1") else if (x == ZERO) ZERO else Fn(name, args)
            "ellipticf", "elliptice" -> when { x == ZERO -> ZERO; args[1] == ZERO -> x; else -> Fn(name, args) }
            // Γ(1, x) = e^(−x); Γ(s, 0) = Γ(s).
            "gammainc" -> when { x == ONE -> power(E, product(listOf(MINUS_ONE, args[1]))); args[1] == ZERO -> function("gamma", listOf(x)); else -> Fn(name, args) }
            "polylog" -> polylogExact(args[0], args[1]) ?: Fn(name, args)
            "hadamard" -> Matrices.hadamard(x as? Mat ?: throw MathError("\$\\circ\$ needs two matrices"), args[1] as? Mat ?: throw MathError("\$\\circ\$ needs two matrices"))
            "kron" -> Matrices.kronecker(x as? Mat ?: throw MathError("\$\\otimes\$ needs two matrices"), args[1] as? Mat ?: throw MathError("\$\\otimes\$ needs two matrices"))
            "hermitian" -> Matrices.hermitian(x as? Mat ?: throw MathError("Aᴴ needs a matrix"))
            "det" -> Matrices.det(x)
            "trace" -> LinearAlgebra.trace(x)
            "rank" -> LinearAlgebra.rank(x)
            "rref" -> LinearAlgebra.rref(x)
            "dot" -> LinearAlgebra.dot(args[0], args[1])
            "cross" -> LinearAlgebra.cross(args[0], args[1])
            "transpose" -> Matrices.transpose(x)
            // Whole-number functions and special polynomials: worked out once their arguments are
            // numbers (after a substitution, in a sum's terms, at a point), else left as they are.
            in NumberTheory.NAMES -> NumberTheory.eval(name, args) ?: Fn(name, args)
            in MoreMath.INTEGERS -> MoreMath.integers(name, args) ?: Fn(name, args)
            in MoreMath.POLYNOMIALS -> MoreMath.polynomial(name, args) ?: Fn(name, args)
            else -> Fn(name, args)
        }
    }

    private val NUMERIC_OK = setOf(
        "sin", "cos", "tan", "asin", "acos", "atan", "sinh", "cosh", "tanh", "asinh", "acosh", "atanh",
        "ln", "log", "abs", "floor", "ceil", "round", "Re", "Im", "conj", "arg", "fact", "binom", "mod", "min", "max", "sgn", "heaviside", "frac", "gamma", "zeta", "erf", "perm", "invnorm", "digamma", "zetaprime", "lambertw", "besselj", "bessely", "hurwitz", "polylog",
        "si", "ci", "shi", "chi", "ei", "li", "erfi", "fresnels", "fresnelc", "gammainc", "ellipticf", "elliptice",
    )

    fun isNegative(x: Expr): Boolean = when (x) {
        is Num -> x.q.signum < 0
        is Flt -> x.d < 0
        is Mul -> isNegative(x.factors[0])
        else -> false
    }

    /** q such that x = q·π, if there is one. */
    private fun piMultiple(x: Expr): Rational? = when {
        x == ZERO -> Rational.ZERO
        x == PI -> Rational.ONE
        x is Mul && x.factors.size == 2 && x.factors[0] is Num && x.factors[1] == PI -> (x.factors[0] as Num).q
        else -> null
    }

    private fun sinDegrees(d: Int): Expr? {
        val a = Math.floorMod(d, 360)
        val quarter = when (a % 180) {
            0 -> ZERO
            30, 150 -> HALF
            45, 135 -> product(listOf(HALF, power(TWO, HALF)))
            60, 120 -> product(listOf(HALF, power(Num(3), HALF)))
            90 -> ONE
            else -> return null
        }
        return if (a > 180) product(listOf(MINUS_ONE, quarter)) else quarter
    }

    private fun trig(name: String, x: Expr): Expr? {
        val k = piMultiple(x) ?: return null
        val deg = k * Rational.of(180)
        if (!deg.isInteger) return null
        val d = deg.num.toInt()
        val s = sinDegrees(d) ?: return null
        val c = sinDegrees(d + 90)!!
        return when (name) {
            "sin" -> s
            "cos" -> c
            else -> {
                if (c == ZERO) throw MathError("tan is undefined here")
                product(listOf(s, power(c, MINUS_ONE)))
            }
        }
    }

    private val asinTable: List<Pair<Expr, Rational>> by lazy {
        listOf(
            ZERO to Rational.ZERO,
            HALF to Rational.of(1, 6),
            product(listOf(HALF, power(TWO, HALF))) to Rational.of(1, 4),
            product(listOf(HALF, power(Num(3), HALF))) to Rational.of(1, 3),
            ONE to Rational.of(1, 2),
        )
    }
    private val atanTable: List<Pair<Expr, Rational>> by lazy {
        listOf(
            ZERO to Rational.ZERO,
            product(listOf(Num(Rational.of(1, 3)), power(Num(3), HALF))) to Rational.of(1, 6),
            ONE to Rational.of(1, 4),
            power(Num(3), HALF) to Rational.of(1, 3),
        )
    }

    private fun inverseTrig(name: String, x: Expr): Expr? {
        val table = if (name == "atan") atanTable else asinTable
        val angle = table.firstOrNull { it.first == x }?.second ?: return null
        val a = product(listOf(Num(angle), PI))
        return if (name == "acos") sum(listOf(product(listOf(HALF, PI)), product(listOf(MINUS_ONE, a)))) else a
    }

    private fun log(base: Expr, x: Expr): Expr {
        if (x == ONE) return ZERO
        if (x == base) return ONE
        if (base is Num && x is Num && base.q.signum > 0 && x.q.signum > 0 && base.q != Rational.ONE) {
            var p = base.q
            for (k in 1..200) {
                if (p == x.q) return Num(k.toLong())
                if (p.reciprocal() == x.q) return Num(-k.toLong())
                p *= base.q
                if (p.num.bitLength() > x.q.num.bitLength() + x.q.den.bitLength() + 64) break
            }
        }
        if (base == E) return function("ln", listOf(x))
        return Fn("log", listOf(base, x))
    }

    private fun abs(x: Expr): Expr {
        if (x is Mat) return LinearAlgebra.norm(x)
        if (x is Num) return Num(x.q.abs())
        if (x is Flt) return Flt(kotlin.math.abs(x.d))
        if (x == I) return ONE
        if (x is Fn && x.name == "abs") return x
        if (x.isConstant && x !is Mat) {
            val v = Numeric.eval(x)
            if (v.isReal && v.re != 0.0) return if (v.re > 0) x else product(listOf(MINUS_ONE, x))
            if (!v.isReal) {
                val (re, im) = splitComplex(x)
                return power(sum(listOf(power(re, TWO), power(im, TWO))), HALF)
            }
        }
        return Fn("abs", listOf(x))
    }

    /** Γ(n) = (n − 1)!, Γ(½) = √π and so on for half-integers. */
    private fun gammaExact(x: Expr): Expr? {
        val q = (x as? Num)?.q ?: return null
        if (q.isInteger) {
            if (q.signum <= 0) throw MathError("\$\\Gamma\$ is undefined at 0 and negative integers")
            if (q.num > java.math.BigInteger.valueOf(3000)) return null
            return factorial(Num(q - Rational.ONE))
        }
        if (q.den == java.math.BigInteger.TWO && q.abs().num.bitLength() < 12) {
            // Γ(n + ½) = (2n)! √π / (4ⁿ n!), and the reflection for negative values.
            val n = (q - Rational.of(1, 2)).num.toInt()
            if (n >= 0) {
                var c = Rational.ONE
                for (k in 1..n) c *= Rational.of((2 * k - 1).toLong(), 2)
                return product(listOf(Num(c), power(PI, HALF)))
            }
            var c = Rational.ONE
            for (k in 1..-n) c *= Rational.of(-2, (2 * k - 1).toLong())
            return product(listOf(Num(c), power(PI, HALF)))
        }
        return null
    }

    /** ζ(2n) = (−1)ⁿ⁺¹ B₂ₙ (2π)²ⁿ / (2 (2n)!), ζ(0) = −½, ζ(−n) = −Bₙ₊₁/(n + 1). */
    /**
     * ζ(s, q) where it's plain ζ: q = 1 is ζ(s), q = ½ is (2ˢ − 1)ζ(s), and q a whole number or
     * a half more drops the first terms: ζ(s, n) = ζ(s) − Σ_{j<n} j^(−s).
     */
    private fun hurwitzExact(s: Expr, q: Expr): Expr? {
        val r = (q as? Num)?.q ?: return null
        if (r.signum <= 0) return null
        val frac = r - Rational.of(r.floor())
        val n = r.floor().toLong()
        if (n > 200) return null
        return when (frac) {
            Rational.ZERO -> sum(listOf(function("zeta", listOf(s))) + (1 until n).map { j -> product(listOf(MINUS_ONE, power(Num(j), product(listOf(MINUS_ONE, s))))) })
            Rational.of(1, 2) -> sum(listOf(product(listOf(sum(listOf(power(TWO, s), MINUS_ONE)), function("zeta", listOf(s))))) +
                (0 until n).map { j -> product(listOf(MINUS_ONE, power(Num(Rational.of(2 * j + 1, 2)), product(listOf(MINUS_ONE, s))))) })
            else -> null
        }
    }

    /** Li_s(z) at z = 1 (ζ(s)), −1 (−η(s)) and 0, and for s = 1, 0, −1…: −ln(1 − z), z/(1 − z), … */
    private fun polylogExact(s: Expr, z: Expr): Expr? {
        fun minus(x: Expr) = product(listOf(MINUS_ONE, x))
        fun oneMinus(x: Expr) = sum(listOf(ONE, minus(x)))
        return when {
            z == ZERO -> ZERO
            z == ONE -> function("zeta", listOf(s))
            // −η(s) = −(1 − 2^(1−s)) ζ(s)
            z == MINUS_ONE -> minus(product(listOf(oneMinus(power(TWO, oneMinus(s))), function("zeta", listOf(s)))))
            s == ONE -> minus(function("ln", listOf(oneMinus(z))))
            s == ZERO -> product(listOf(z, power(oneMinus(z), MINUS_ONE)))
            else -> null
        }
    }

    private fun zetaExact(x: Expr): Expr? {
        val q = (x as? Num)?.q ?: return null
        if (!q.isInteger) return null
        val n = q.num.toInt()
        if (q.num.bitLength() > 7) return null
        return when {
            n == 1 -> throw MathError("\$\\zeta\$ has a pole at 1")
            n == 0 -> Num(Rational.of(-1, 2))
            n < 0 -> Num(-Bernoulli.of(1 - n) / Rational.of((1 - n).toLong()))
            n % 2 == 0 -> {
                var fact = Rational.ONE
                for (k in 2..n) fact *= Rational.of(k.toLong())
                val c = Bernoulli.of(n).abs() * Rational.of(2).pow(n - 1) / fact
                product(listOf(Num(c), power(PI, Num(n.toLong()))))
            }
            else -> null // odd values like ζ(3) have no known closed form
        }
    }

    private fun minMax(name: String, a: Expr, b: Expr): Expr {
        if (a == b) return a
        if (a.isConstant && b.isConstant && a !is Mat && b !is Mat) {
            val c = if (a is Num && b is Num) a.q.compareTo(b.q) else Numeric.real(a).compareTo(Numeric.real(b))
            return if ((c <= 0) == (name == "min")) a else b
        }
        return Fn(name, listOf(a, b))
    }

    private fun factorial(x: Expr): Expr {
        if (x is Num && x.q.isInteger) {
            val n = x.q.num
            if (n.signum() < 0) throw MathError("Factorial of a negative integer is undefined")
            if (n > BigInteger.valueOf(5000)) throw MathError("That factorial is too large")
            var acc = BigInteger.ONE
            for (k in 2..n.toInt()) acc *= BigInteger.valueOf(k.toLong())
            return Num(Rational.of(acc))
        }
        return Fn("fact", listOf(x))
    }

    private fun binomial(n: Expr, k: Expr): Expr {
        if (n is Num && k is Num && n.q.isInteger && k.q.isInteger) {
            val nn = n.q.num
            val kk = k.q.num
            if (kk.signum() < 0 || (nn.signum() >= 0 && kk > nn)) return ZERO
            if (kk > BigInteger.valueOf(100_000)) throw MathError("That's too large")
            var acc = Rational.ONE
            var j = BigInteger.ZERO
            while (j < kk) {
                acc = acc * Rational.of(nn - j) / Rational.of(j + BigInteger.ONE)
                j += BigInteger.ONE
            }
            return Num(acc)
        }
        return Fn("binom", listOf(n, k))
    }

    private fun mod(a: Expr, b: Expr): Expr {
        if (b == ZERO) throw MathError("mod 0 is undefined")
        if (a is Num && b is Num) {
            val q = (a.q / b.q).floor()
            return Num(a.q - b.q * Rational.of(q))
        }
        return Fn("mod", listOf(a, b))
    }

    private fun gcdLcm(name: String, a: Expr, b: Expr): Expr {
        if (a is Num && b is Num && a.q.isInteger && b.q.isInteger) {
            val x = a.q.num
            val y = b.q.num
            val g = x.gcd(y)
            return Num(Rational.of(if (name == "gcd") g else if (g.signum() == 0) BigInteger.ZERO else (x * y).abs() / g))
        }
        return Fn(name, listOf(a, b))
    }

    private fun rounding(name: String, x: Expr): Expr {
        val q: Rational? = when {
            x is Num -> x.q
            x.isConstant && x !is Mat -> {
                val v = Numeric.real(x)
                return Num(
                    Rational.of(
                        java.math.BigDecimal(
                            when (name) {
                                "floor" -> kotlin.math.floor(v)
                                "ceil" -> kotlin.math.ceil(v)
                                else -> kotlin.math.floor(v + 0.5)
                            },
                        ).toBigInteger(),
                    ),
                )
            }
            else -> null
        }
        q ?: return Fn(name, listOf(x))
        return Num(
            Rational.of(
                when (name) {
                    "floor" -> q.floor()
                    "ceil" -> q.ceil()
                    else -> (q + Rational.of(1, 2)).floor()
                },
            ),
        )
    }

    /** The principal √(a + bi) = √((m + a)/2) + i·sgn(b)·√((m − a)/2), m = |a + bi|, when m is rational. */
    private fun complexSqrt(z: Expr): Expr? {
        val (re, im) = splitComplex(z)
        if (re !is Num || im !is Num) return null
        val m2 = re.q * re.q + im.q * im.q
        val m = com.example.cas.math.exactRoot(m2.num, 2)?.let { n -> com.example.cas.math.exactRoot(m2.den, 2)?.let { d -> Rational.of(n, d) } } ?: return null
        val half = Rational.of(1, 2)
        val a = power(Num((m + re.q) * half), HALF)
        val c = power(Num((m - re.q) * half), HALF)
        return sum(listOf(a, product(listOf(Num(im.q.signum.toLong()), c, I))))
    }

    private fun isLn(t: Expr) = t is Fn && t.name == "ln"

    /** For a term q·π·i (q rational), q; otherwise null. */
    private fun piImaginaryPart(t: Expr): Rational? {
        val fs = if (t is Mul) t.factors else listOf(t)
        if (I !in fs || PI !in fs) return null
        val others = fs.filter { it != I && it != PI }
        return when {
            others.isEmpty() -> Rational.ONE
            others.size == 1 && others[0] is Num -> (others[0] as Num).q
            else -> null
        }
    }

    private fun isComplexOrNegative(x: Expr): Boolean {
        val (re, im) = splitComplex(x)
        if (im != ZERO) return true
        return runCatching { Numeric.real(re) < 0 }.getOrDefault(false)
    }

    /** arg(re + i·im) exactly: 0, π, ±π/2, or atan(im/re) moved into the right quadrant (−π, π]. */
    fun exactArg(re: Expr, im: Expr): Expr {
        val r = Numeric.real(re)
        val i = Numeric.real(im)
        return when {
            im == ZERO || i == 0.0 -> if (r >= 0) ZERO else PI
            re == ZERO || r == 0.0 -> product(listOf(Num(Rational.of(if (i > 0) 1L else -1L, 2L)), PI))
            else -> {
                val base = function("atan", listOf(product(listOf(im, power(re, MINUS_ONE)))))
                when {
                    r > 0 -> base
                    i > 0 -> sum(listOf(base, PI))
                    else -> sum(listOf(base, product(listOf(MINUS_ONE, PI))))
                }
            }
        }
    }

    /**
     * asin and acos of real numbers outside [−1, 1], as C99 and Python do:
     * asin x = sgn(x)·π/2 + i ln(|x| + √(x² − 1)); acos x = −i ln(x + √(x² − 1)) for x > 1,
     * π − i ln(|x| + √(x² − 1)) for x < −1.
     */
    private fun complexInverseTrig(name: String, x: Expr): Expr? {
        if (name == "atan" || !x.isConstant || x is Mat || splitComplex(x).second != ZERO) return null
        val v = runCatching { Numeric.real(x) }.getOrNull() ?: return null
        if (kotlin.math.abs(v) <= 1) return null
        val ax = if (v > 0) x else product(listOf(MINUS_ONE, x))
        val log = function("ln", listOf(sum(listOf(ax, power(sum(listOf(power(ax, TWO), MINUS_ONE)), HALF)))))
        return when (name) {
            "asin" -> sum(listOf(product(listOf(Num(Rational.of(if (v > 0) 1L else -1L, 2L)), PI)), product(listOf(I, log))))
            else -> if (v > 0) product(listOf(MINUS_ONE, I, log)) else sum(listOf(PI, product(listOf(MINUS_ONE, I, log))))
        }
    }

    /** Splits z into (real, imaginary) parts, treating variables as real. */
    fun splitComplex(z: Expr): Pair<Expr, Expr> {
        val terms = if (z is Add) z.terms else listOf(z)
        val re = ArrayList<Expr>()
        val im = ArrayList<Expr>()
        for (t in terms) {
            val f = if (t is Mul) t.factors else listOf(t)
            if (I in f) im += product(f.filter { it != I }) else re += t
        }
        return sum(re) to sum(im)
    }

    private fun complexPart(name: String, x: Expr): Expr {
        // A constant like e^(iπ/3): its exact a + bi first; if that can't be found, Re stays as is.
        val exact = if (x.isConstant && x !is Mat && !x.contains { it is Flt }) runCatching { ComplexArith.split(x) }.getOrNull() else null
        val (re, im) = exact ?: splitComplex(x)
        if (exact == null && (re.contains { it == I } || im.contains { it == I })) return Fn(name, listOf(x))
        return when (name) {
            "Re" -> re
            "Im" -> im
            else -> sum(listOf(re, product(listOf(MINUS_ONE, im, I))))
        }
    }
}

/** Bernoulli numbers Bₙ as exact fractions (B₁ = −½). */
object Bernoulli {
    private val cache = mutableListOf(Rational.ONE)
    fun of(n: Int): Rational {
        while (cache.size <= n) {
            val m = cache.size
            // Σ_{k=0}^{m} C(m+1, k) B_k = 0
            var sum = Rational.ZERO
            var binom = Rational.ONE // C(m+1, 0)
            for (k in 0 until m) {
                sum += binom * cache[k]
                binom = binom * Rational.of((m + 1 - k).toLong()) / Rational.of((k + 1).toLong())
            }
            cache += -sum / Rational.of((m + 1).toLong())
        }
        return cache[n]
    }
}

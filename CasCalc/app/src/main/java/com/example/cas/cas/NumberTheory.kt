package com.example.cas.cas

import com.example.cas.math.Rational
import java.math.BigInteger

/**
 * Primes and integer sequences, for the Primes and sequences tab: exact, on whole numbers.
 * Each gives null when its argument isn't a whole number yet (a letter, say), so the call
 * stays as it is, like any function of an unknown.
 */
object NumberTheory {
    val NAMES = setOf(
        "isprime", "nextprime", "prevprime", "prime", "primepi", "totient", "ndivisors", "sigma", "moebius",
        "fib", "lucas", "catalan", "bernoulli", "partitions", "dfact",
    )

    fun eval(name: String, args: List<Expr>): Expr? {
        val n = (args.getOrNull(0) as? Num)?.q?.takeIf { it.isInteger }?.num ?: run {
            // A decimal or a fraction is never a whole number: say so rather than leave it.
            val a = args.getOrNull(0)
            if (a is Flt || (a is Num && !a.q.isInteger)) throw MathError("$name takes a whole number")
            return null
        }
        return when (name) {
            "isprime" -> Num(if (isPrime(n)) 1L else 0L)
            "nextprime" -> big(if (n < BigInteger.TWO) BigInteger.TWO else n.nextProbablePrime())
            "prevprime" -> big(prevPrime(n))
            "prime" -> Num(nthPrime(small(n, 1, 2_000_000, "the n-th prime")).toLong())
            "primepi" -> Num(primePi(small(n, Int.MIN_VALUE, 50_000_000, "π(n)")).toLong())
            "totient" -> Num(factor(positive(n, "φ(n)")).fold(1L) { acc, (p, e) -> acc * (p - 1) * pow(p, e - 1) })
            "ndivisors" -> Num(factor(positive(n, "τ(n)")).fold(1L) { acc, (_, e) -> acc * (e + 1) })
            "sigma" -> big(factor(positive(n, "σ(n)")).fold(BigInteger.ONE) { acc, (p, e) ->
                acc * (BigInteger.valueOf(p).pow(e + 1) - BigInteger.ONE) / BigInteger.valueOf(p - 1)
            })
            "moebius" -> factor(positive(n, "μ(n)")).let { fs -> Num(if (fs.any { it.second > 1 }) 0L else if (fs.size % 2 == 0) 1L else -1L) }
            "fib" -> big(fibonacci(small(n, -100_000, 100_000, "Fibonacci numbers")))
            "lucas" -> small(n, -100_000, 100_000, "Lucas numbers").let { k -> big(fibonacci(k - 1) + fibonacci(k + 1)) }
            "catalan" -> small(n, 0, 20_000, "Catalan numbers").let { k ->
                big(binomial(2 * k, k) / BigInteger.valueOf(k + 1L))
            }
            "bernoulli" -> Num(bernoulli(small(n, 0, 600, "Bernoulli numbers")))
            "partitions" -> big(partitions(small(n, Int.MIN_VALUE, 10_000, "p(n)")))
            "dfact" -> small(n, -1, 20_000, "n!!").let { k ->
                var r = BigInteger.ONE; var i = k
                while (i > 1) { r *= BigInteger.valueOf(i.toLong()); i -= 2 }
                big(r)
            }
            else -> null
        }
    }

    private fun big(b: BigInteger) = Num(Rational.of(b))

    private fun small(n: BigInteger, lo: Int, hi: Int, what: String): Int {
        if (n < BigInteger.valueOf(lo.toLong())) throw MathError(if (lo >= 0) "$what start at $lo" else "That's too small for $what")
        if (n > BigInteger.valueOf(hi.toLong())) throw MathError("That's too large for $what (up to $hi)")
        return n.toInt()
    }

    private fun positive(n: BigInteger, what: String): Long {
        if (n.signum() <= 0) throw MathError("$what takes a positive whole number")
        if (n.bitLength() > 50) throw MathError("That's too large to factor here")
        return n.toLong()
    }

    private fun pow(p: Long, e: Int): Long { var r = 1L; repeat(e) { r *= p }; return r }

    fun isPrime(n: BigInteger): Boolean {
        if (n < BigInteger.TWO) return false
        if (n.bitLength() <= 62) {
            val m = n.toLong()
            if (m < 4) return true
            if (m % 2 == 0L) return false
            if (m < 1_000_000_000_000L) { var d = 3L; while (d * d <= m) { if (m % d == 0L) return false; d += 2 }; return true }
        }
        return n.isProbablePrime(64)
    }

    private fun prevPrime(n: BigInteger): BigInteger {
        if (n <= BigInteger.TWO) throw MathError("There's no prime below 2")
        var k = n - BigInteger.ONE
        while (!isPrime(k)) k -= BigInteger.ONE
        return k
    }

    /** The primes up to [limit], by the sieve of Eratosthenes. */
    private fun sieve(limit: Int): java.util.BitSet {
        val composite = java.util.BitSet(limit + 1)
        var i = 2
        while (i.toLong() * i <= limit) {
            if (!composite[i]) { var j = i * i; while (j <= limit) { composite.set(j); j += i } }
            i++
        }
        return composite
    }

    private fun primePi(n: Int): Int {
        if (n < 2) return 0
        val composite = sieve(n)
        var count = 0
        for (k in 2..n) if (!composite[k]) count++
        return count
    }

    private fun nthPrime(n: Int): Int {
        // p_n < n (ln n + ln ln n) for n ≥ 6.
        val bound = if (n < 6) 15 else (n * (kotlin.math.ln(n.toDouble()) + kotlin.math.ln(kotlin.math.ln(n.toDouble())))).toInt() + 3
        val composite = sieve(bound)
        var count = 0
        for (k in 2..bound) if (!composite[k] && ++count == n) return k
        throw MathError("Couldn't find the $n-th prime")
    }

    /** The prime factors of n with their powers, by trial division. */
    fun factor(n0: Long): List<Pair<Long, Int>> {
        var n = n0
        val out = mutableListOf<Pair<Long, Int>>()
        var p = 2L
        while (p * p <= n) {
            var e = 0
            while (n % p == 0L) { n /= p; e++ }
            if (e > 0) out += p to e
            p += if (p == 2L) 1 else 2
        }
        if (n > 1) out += n to 1
        return out
    }

    /** F(n) by fast doubling; F(−n) = (−1)ⁿ⁺¹ F(n). */
    fun fibonacci(n: Int): BigInteger {
        if (n < 0) return fibonacci(-n).let { if (n % 2 == 0) it.negate() else it }
        fun pair(k: Int): Pair<BigInteger, BigInteger> {
            if (k == 0) return BigInteger.ZERO to BigInteger.ONE
            val (a, b) = pair(k / 2)
            val c = a * (b * BigInteger.TWO - a)
            val d = a * a + b * b
            return if (k % 2 == 0) c to d else d to (c + d)
        }
        return pair(n).first
    }

    private fun binomial(n: Int, k: Int): BigInteger {
        var r = BigInteger.ONE
        for (i in 1..k) r = r * BigInteger.valueOf((n - k + i).toLong()) / BigInteger.valueOf(i.toLong())
        return r
    }

    /** Bₙ by the Akiyama–Tanigawa algorithm, with B₁ = −½. */
    fun bernoulli(n: Int): Rational {
        val a = Array(n + 1) { Rational.of(1, it + 1L) }
        for (m in 0..n) {
            a[m] = Rational.of(1, m + 1L)
            for (j in m downTo 1) a[j - 1] = (a[j - 1] - a[j]) * Rational.of(j.toLong())
        }
        return if (n == 1) Rational.of(-1, 2) else a[0]
    }

    /** p(n), the ways to write n as a sum of positive whole numbers, by Euler's pentagonal numbers. */
    fun partitions(n: Int): BigInteger {
        if (n < 0) return BigInteger.ZERO
        val p = arrayOfNulls<BigInteger>(n + 1)
        p[0] = BigInteger.ONE
        for (m in 1..n) {
            var s = BigInteger.ZERO
            var k = 1
            while (true) {
                val g1 = k * (3 * k - 1) / 2
                if (g1 > m) break
                val sign = if (k % 2 == 1) 1 else -1
                s = if (sign > 0) s + p[m - g1]!! else s - p[m - g1]!!
                val g2 = k * (3 * k + 1) / 2
                if (g2 <= m) s = if (sign > 0) s + p[m - g2]!! else s - p[m - g2]!!
                k++
            }
            p[m] = s
        }
        return p[n]!!
    }
}

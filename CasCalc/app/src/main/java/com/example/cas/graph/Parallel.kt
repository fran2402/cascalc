package com.example.cas.graph

import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/**
 * Plotting work spread over the phone's cores: a grid's rows are cut into small chunks that the
 * workers (and the calling thread) take in turn, so a slow patch (near a pole, a long sum) doesn't
 * hold the rest up. Each worker gets its own state from `make` (a function caller with its own
 * argument array), built on the calling thread, so nothing is shared between threads.
 */
object Parallel {
    /** Workers besides the calling thread. */
    val threads = (Runtime.getRuntime().availableProcessors() - 1).coerceIn(0, 7)

    private val pool by lazy {
        Executors.newFixedThreadPool(threads.coerceAtLeast(1)) { r ->
            Thread(r, "plot-worker").apply { isDaemon = true; priority = Thread.NORM_PRIORITY - 1 }
        }
    }
    private val inWorker = ThreadLocal.withInitial { false }

    /**
     * Runs [body] over the rows 0 until [n] in chunks, on every core, and returns when all are
     * done. [stop] is checked between chunks: true leaves the rest undone (and returns false).
     */
    fun <T> rows(n: Int, make: () -> T, stop: () -> Boolean = { false }, body: (state: T, from: Int, until: Int) -> Unit): Boolean {
        if (n <= 0) return true
        val workers = minOf(threads, n - 1)
        // Inside a worker already (a nested grid), or nothing to share: on this thread.
        if (workers <= 0 || inWorker.get()) {
            val s = make()
            val chunk = maxOf(1, n / 16)
            var r = 0
            while (r < n) { if (stop()) return false; body(s, r, minOf(n, r + chunk)); r += chunk }
            return true
        }
        val chunk = maxOf(1, n / ((workers + 1) * 6))
        val next = AtomicInteger(0)
        val failure = AtomicReference<Throwable?>(null)
        val stopped = java.util.concurrent.atomic.AtomicBoolean(false)
        fun work(s: T) {
            while (failure.get() == null) {
                val r = next.getAndAdd(chunk)
                if (r >= n) return
                if (stop()) { stopped.set(true); next.set(n); return }
                body(s, r, minOf(n, r + chunk))
            }
        }
        val states = List(workers) { make() }
        val done = CountDownLatch(workers)
        for (s in states) pool.execute {
            inWorker.set(true)
            try { work(s) } catch (t: Throwable) { failure.compareAndSet(null, t) } finally { inWorker.set(false); done.countDown() }
        }
        try { work(make()) } catch (t: Throwable) { failure.compareAndSet(null, t) }
        done.await()
        failure.get()?.let { throw it }
        return !stopped.get()
    }
}

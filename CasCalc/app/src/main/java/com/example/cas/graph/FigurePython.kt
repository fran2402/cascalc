package com.example.cas.graph

import java.util.Locale

/**
 * A figure written as a matplotlib script that draws the same picture: the layout as
 * plt.subplot_mosaic, each layer as its pyplot call with the numbers it was drawn from (a
 * function's samples, a field's grid), and each axes' titles, limits, scales, grid and legend.
 */
object FigurePython {
    private fun n(v: Double): String = when {
        v.isNaN() -> "np.nan"
        v == Double.POSITIVE_INFINITY -> "np.inf"
        v == Double.NEGATIVE_INFINITY -> "-np.inf"
        else -> String.format(Locale.ROOT, "%.6g", v).let { if (it.contains('e') || it.contains('.')) it else it }
    }

    private fun arr(v: DoubleArray): String = "np.array([" + v.joinToString(", ") { n(it) } + "])"
    private fun list(v: List<DoubleArray>): String = "[" + v.joinToString(", ") { arr(it) } + "]"
    private fun str(s: String): String = if (s.contains('\\')) "r\"" + s.replace("\"", "\\\"") + "\"" else "\"" + s.replace("\"", "\\\"") + "\""
    private fun hex(c: Int) = String.format(Locale.ROOT, "\"#%06X\"", c and 0xFFFFFF)
    private fun linestyle(k: Int) = when (k) { 1 -> "\"--\""; 2 -> "\":\""; 3 -> "\"-.\""; 4 -> "(0, (9, 3.5))"; 5 -> "(0, (4, 2.5, 1, 2.5, 1, 2.5))"; else -> "\"-\"" }
    private fun marker(k: Int): String? = if (k < 0) null else when (Marker.of(k)) {
        Marker.Circle -> "o"; Marker.Ring -> "o"; Marker.Square, Marker.OpenSquare -> "s"; Marker.Diamond, Marker.OpenDiamond -> "D"
        Marker.Triangle, Marker.OpenTriangle -> "^"; Marker.TriangleDown, Marker.OpenTriangleDown -> "v"; Marker.Cross -> "x"; Marker.Plus -> "+"
        Marker.Star, Marker.OpenStar -> "*"; Marker.Pentagon, Marker.OpenPentagon -> "p"; Marker.Hexagon, Marker.OpenHexagon -> "h"; Marker.Asterisk -> "1"
    }

    private val LOC = listOf("best", "upper left", "upper center", "upper right", "center left", "center", "center right", "lower left", "lower center", "lower right")

    fun write(d: Figure.Doc, built: Figure.Built): String = buildString {
        val style = when (d.theme) { 1 -> "[\"science\"]  # pip install SciencePlots"; 2 -> "[\"science\", \"dark_background\"]  # pip install SciencePlots"; 3 -> "\"ggplot\""; 4 -> "\"grayscale\""; 5 -> "\"seaborn-v0_8-darkgrid\""
            6 -> "[\"science\", \"nature\"]  # pip install SciencePlots"; 7 -> "[\"science\", \"ieee\"]  # pip install SciencePlots"; 8 -> "\"fivethirtyeight\""
            9 -> "\"tableau-colorblind10\""; 10 -> "\"seaborn-v0_8-colorblind\""; 12, 13, 14, 17, 18 -> "\"dark_background\""; 11 -> "\"Solarize_Light2\""
            else -> null }
        appendLine("import numpy as np")
        appendLine("import matplotlib.pyplot as plt")
        if (d.theme in listOf(1, 2, 6, 7)) appendLine("import scienceplots  # noqa: F401")
        appendLine()
        style?.let { appendLine("plt.style.use($it)") }
        val (cells, _, _) = Figure.cells(d.layout)
        val letters = d.layout.split(';').joinToString("") { it.trim() }.filter { it != '.' }.toList().distinct()
        val mosaic = d.layout.split(';').map { it.trim() }.filter { it.isNotEmpty() }.joinToString(";")
        appendLine("fig, axs = plt.subplot_mosaic(\"$mosaic\", figsize=(6.4, ${n(6.4 * d.aspect)}), layout=\"constrained\")")
        if (d.title.isNotBlank()) appendLine("fig.suptitle(${str(d.title)})")
        cells.indices.forEach { k ->
            val w = built.worked.getOrNull(k) ?: return@forEach
            val a = w.axes
            val name = letters.getOrNull(k) ?: 'A'
            appendLine()
            appendLine("ax = axs[\"$name\"]")
            for (dr in w.drawn) if (dr.error == null) layer(this, dr, w)
            if (a.title.isNotBlank()) appendLine("ax.set_title(${str(a.title)})")
            if (a.xLabel.isNotBlank()) appendLine("ax.set_xlabel(${str(a.xLabel)})")
            if (a.yLabel.isNotBlank()) appendLine("ax.set_ylabel(${str(a.yLabel)})")
            if (w.pie) { appendLine("ax.set_aspect(\"equal\")"); return@forEach }
            if (a.xLog) appendLine("ax.set_xscale(\"log\")")
            if (a.yLog) appendLine("ax.set_yscale(\"log\")")
            appendLine("ax.set_xlim(${n(w.x0)}, ${n(w.x1)})")
            appendLine("ax.set_ylim(${n(w.y0)}, ${n(w.y1)})")
            w.xCategories?.let { c ->
                val pos = if (w.drawn.any { it.layer.kind == Figure.Kind.Bar }) c.indices.map { it.toDouble() } else c.indices.map { it + 1.0 }
                appendLine("ax.set_xticks([${pos.joinToString(", ") { n(it) }}], [${c.joinToString(", ") { str(it) }}])")
            }
            w.yCategories?.let { c -> appendLine("ax.set_yticks([${c.indices.joinToString(", ")}], [${c.joinToString(", ") { str(it) }}])") }
            when (a.grid) { 1 -> appendLine("ax.grid(True)"); 2 -> { appendLine("ax.minorticks_on()"); appendLine("ax.grid(True, which=\"both\")") } else -> appendLine("ax.grid(False)") }
            if (a.equal) appendLine("ax.set_aspect(\"equal\")")
            when (a.spines) {
                1 -> appendLine("ax.spines[[\"top\", \"right\"]].set_visible(False)")
                2 -> { appendLine("ax.spines[[\"left\", \"bottom\"]].set_position(\"zero\")"); appendLine("ax.spines[[\"top\", \"right\"]].set_visible(False)") }
                3 -> appendLine("ax.axis(\"off\")")
            }
            if (a.legend >= 0 && w.drawn.any { it.error == null && it.layer.style.label.isNotBlank() }) appendLine("ax.legend(loc=\"${LOC[a.legend.coerceIn(0, 9)]}\")")
        }
        appendLine()
        appendLine("fig.savefig(\"figure.pdf\")")
        appendLine("plt.show()")
    }

    private fun layer(out: StringBuilder, dr: Figure.Drawn, w: Figure.Worked) {
        val l = dr.layer; val s = l.style
        val color = "color=${hex(dr.color)}"
        val alpha = if (s.alpha < 1.0) ", alpha=${n(s.alpha)}" else ""
        val label = if (s.label.isNotBlank()) ", label=${str(s.label)}" else ""
        val line = ", linestyle=${linestyle(s.line)}, linewidth=${n(s.width)}"
        val mk = marker(s.marker)?.let { ", marker=\"$it\"" } ?: ""
        fun xy(k: Int = 0) = dr.series[k].let { (x, y) -> "${arr(x)}, ${arr(y)}" }
        val cmap = ", cmap=\"${s.cmap.removePrefix("theme").ifBlank { "viridis" }.let { if (it.startsWith("_")) "viridis" else it }}\""
        fun grid(g: Figure.Grid, name: String = "Z") {
            out.appendLine("X, Y = np.meshgrid(np.linspace(${n(g.x0)}, ${n(g.x1)}, ${g.nx}), np.linspace(${n(g.y0)}, ${n(g.y1)}, ${g.ny}))")
            out.appendLine("$name = np.array([" + g.z.joinToString(",\n    ") { r -> "[" + r.joinToString(", ") { n(it) } + "]" } + "])")
        }
        out.append(when (l.kind) {
            Figure.Kind.Plot, Figure.Kind.Parametric, Figure.Kind.Polar -> "ax.plot(${xy()}, $color$line$mk$alpha$label)"
            Figure.Kind.Fit -> {
                val (px, py) = dr.raw
                out.appendLine("ax.scatter(${arr(px)}, ${arr(py)}, s=${n(s.size * s.size * 2)}, $color, alpha=${n(s.alpha * 0.55)})")
                "ax.plot(${xy()}, $color$line$alpha, label=${str(Figure.legendName(dr))})  # least squares: ${FitModel.entries.getOrElse(s.model) { FitModel.Linear }.latex}"
            }
            Figure.Kind.Kde -> "ax.plot(${xy()}, $color$line$alpha$label)  # Gaussian KDE (scipy.stats.gaussian_kde)"
            Figure.Kind.Step -> "ax.step(${xy()}, where=\"post\", $color$line$mk$alpha$label)"
            Figure.Kind.Stem -> "ax.stem(${xy()}$label)"
            Figure.Kind.Scatter -> {
                val sizes = if (dr.shade.isNotEmpty()) ", s=${arr(dr.shade)}" else ", s=${n(s.size * s.size * 2)}"
                "ax.scatter(${xy()}$sizes, $color${marker(s.marker)?.let { ", marker=\"$it\"" } ?: ""}$alpha$label)"
            }
            Figure.Kind.Fill -> "ax.fill(${xy()}, $color, alpha=${n(s.alpha * 0.6)}$label)"
            Figure.Kind.ErrorBar -> "ax.errorbar(${xy()}, yerr=${arr(dr.lower[0])}, fmt=\"${marker(s.marker) ?: "o"}\", capsize=3, $color$alpha$label)"
            Figure.Kind.FillBetween -> "ax.fill_between(${arr(dr.series[0].first)}, ${arr(dr.series[0].second)}, ${arr(dr.lower[0])}, $color, alpha=${n(s.alpha * 0.35)}$label)"
            Figure.Kind.Stack -> {
                val x = dr.series[0].first
                val ys = dr.series.mapIndexed { k, (_, y) -> if (k == 0) y else DoubleArray(y.size) { y[it] - dr.series[k - 1].second[it] } }
                "ax.stackplot(${arr(x)}, ${list(ys)}, alpha=${n(s.alpha * 0.85)}$label)"
            }
            Figure.Kind.Hist -> {
                val edges = dr.series[0].first
                "ax.hist(${arr(dr.raw[0])}, bins=${arr(edges)}${if (s.density) ", density=True" else ""}${if (s.cumulative) ", cumulative=True" else ""}${if (s.line == 1) ", histtype=\"step\"" else ""}, $color$alpha$label)"
            }
            Figure.Kind.Box -> "ax.boxplot(${list(dr.raw)}, patch_artist=True, boxprops=dict(facecolor=${hex(dr.color)}, alpha=0.4)$label)"
            Figure.Kind.Violin -> "ax.violinplot(${list(dr.raw)}, showmedians=True)"
            Figure.Kind.Ecdf -> "ax.ecdf(${arr(dr.raw[0])}, $color$line$alpha$label)"
            Figure.Kind.Hist2D -> "ax.hist2d(${arr(dr.raw[0])}, ${arr(dr.raw[1])}, bins=${if (s.bins > 0) s.bins else 24}$cmap)"
            Figure.Kind.Hexbin -> "ax.hexbin(${arr(dr.raw[0])}, ${arr(dr.raw[1])}, gridsize=${if (s.bins > 0) s.bins else 18}$cmap)"
            Figure.Kind.Bar -> "ax.bar(np.arange(${dr.values.size}) + ${n(dr.offset)}, ${arr(dr.values)}, width=${n(dr.barWidth)}, $color$alpha$label)"
            Figure.Kind.BarH -> "ax.barh(np.arange(${dr.values.size}) + ${n(dr.offset)}, ${arr(dr.values)}, height=${n(dr.barWidth)}, $color$alpha$label)"
            Figure.Kind.Pie -> "ax.pie(${arr(dr.values)}${if (dr.labels.isNotEmpty()) ", labels=[" + dr.labels.joinToString(", ") { str(it) } + "]" else ""}, autopct=\"%1.0f%%\", startangle=90, counterclock=False)"
            Figure.Kind.Events -> "ax.eventplot(${list(dr.raw)}, lineoffsets=${dr.raw.indices.joinToString(", ", "[", "]") { "${it + 1}" }}, linelengths=0.7, linewidths=${n(s.width)})"
            Figure.Kind.Contour -> { grid(dr.grid!!); "cs = ax.contour(X, Y, Z, levels=${s.levels}${if (s.color >= 0) ", colors=${hex(dr.color)}" else cmap}, linewidths=${n(s.width * 0.8)})" }
            Figure.Kind.ContourF -> { grid(dr.grid!!); "cs = ax.contourf(X, Y, Z, levels=${s.levels}$cmap$alpha)\nfig.colorbar(cs, ax=ax)" }
            Figure.Kind.Heatmap -> {
                val g = dr.grid!!
                out.appendLine("Z = np.array([" + g.z.joinToString(",\n    ") { r -> "[" + r.joinToString(", ") { n(it) } + "]" } + "])")
                val extent = if (dr.offset == 1.0) "[-0.5, ${n(g.nx - 0.5)}, -0.5, ${n(g.ny - 0.5)}]" else "[${n(g.x0)}, ${n(g.x1)}, ${n(g.y0)}, ${n(g.y1)}]"
                "im = ax.imshow(Z, origin=\"lower\", extent=$extent, aspect=\"auto\"$cmap$alpha)\nfig.colorbar(im, ax=ax)"
            }
            Figure.Kind.Quiver -> { grid(dr.grid!!, "U"); out.appendLine("V = np.array([" + dr.grid2!!.z.joinToString(",\n    ") { r -> "[" + r.joinToString(", ") { n(it) } + "]" } + "])"); "ax.quiver(X, Y, U, V, $color$alpha)" }
            Figure.Kind.Stream -> { grid(dr.grid!!, "U"); out.appendLine("V = np.array([" + dr.grid2!!.z.joinToString(",\n    ") { r -> "[" + r.joinToString(", ") { n(it) } + "]" } + "])"); "ax.streamplot(X, Y, U, V, $color, linewidth=${n(s.width * 0.7)})" }
            Figure.Kind.HLine -> "ax.axhline(${n(dr.values[0])}, $color$line$alpha$label)"
            Figure.Kind.VLine -> "ax.axvline(${n(dr.values[0])}, $color$line$alpha$label)"
            Figure.Kind.HSpan -> "ax.axhspan(${n(dr.values[0])}, ${n(dr.values[1])}, $color, alpha=${n(s.alpha * 0.22)}$label)"
            Figure.Kind.VSpan -> "ax.axvspan(${n(dr.values[0])}, ${n(dr.values[1])}, $color, alpha=${n(s.alpha * 0.22)}$label)"
            Figure.Kind.Text -> "ax.text(${n(dr.values[0])}, ${n(dr.values[1])}, ${str(l.text)}, ha=\"center\", va=\"center\")"
            Figure.Kind.Annotate -> "ax.annotate(${str(l.text)}, xy=(${n(dr.values[0])}, ${n(dr.values[1])}), xytext=(${n(dr.values[2])}, ${n(dr.values[3])}), ha=\"center\", va=\"center\", arrowprops=dict(arrowstyle=\"->\"))"
        })
        out.appendLine()
    }
}

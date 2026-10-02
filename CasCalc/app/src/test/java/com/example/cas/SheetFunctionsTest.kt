package com.example.cas

import com.example.cas.graph.Sheet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.math.abs

/** The spreadsheet functions against Excel's documented examples and scipy's distributions. */
class SheetFunctionsTest {
    /** [formula] worked out in a column after [cols]. */
    private fun eval(formula: String, vararg cols: List<String>): Sheet.Value {
        val all = cols.toList() + listOf(listOf("=$formula"))
        return Sheet.Book(all).value(cols.size, 0)
    }

    private fun num(formula: String, vararg cols: List<String>): Double {
        val v = eval(formula, *cols)
        assertNull("$formula gave ${v.error}", v.error)
        return v.number ?: error("$formula gave text '${v.text}'")
    }

    private fun near(want: Double, formula: String, vararg cols: List<String>, tol: Double = 1e-9) {
        val got = num(formula, *cols)
        assert(abs(got - want) <= tol * maxOf(1.0, abs(want))) { "$formula = $got, want $want" }
    }

    private fun text(want: String, formula: String, vararg cols: List<String>) {
        val v = eval(formula, *cols)
        assertNull("$formula gave ${v.error}", v.error)
        assertEquals(formula, want, v.toString())
    }

    private fun col(vararg v: Any) = v.map { it.toString() }

    @Test fun distributionsMatchScipy() {
        listOf(
            "NORM.S.INV(0.975)" to 1.959963984540054,
            "NORM.DIST(1, 0, 1, TRUE)" to 0.8413447460685429,
            "NORM.DIST(1.3, 2, 0.5, FALSE)" to 0.29945493127148975,
            "NORM.INV(0.3, 10, 2)" to 8.951198974583917,
            "T.DIST(1.5, 10, TRUE)" to 0.9177463367772799,
            "T.DIST(1.5, 10, FALSE)" to 0.1274447942870917,
            "T.DIST.2T(2.1, 7)" to 0.07387119621292265,
            "T.INV.2T(0.05, 10)" to 2.228138851986274,
            "T.INV(0.1, 4)" to -1.5332062740589436,
            "CHISQ.DIST(3.2, 4, TRUE)" to 0.47506905321389586,
            "CHISQ.INV.RT(0.05, 3)" to 7.814727903251178,
            "CHISQ.INV(0.9, 7)" to 12.017036623780532,
            "F.DIST(2.5, 3, 12, TRUE)" to 0.8908452876049937,
            "F.INV.RT(0.05, 2, 10)" to 4.1028210151304,
            "BINOM.DIST(3, 10, 0.5, FALSE)" to 0.1171875,
            "BINOM.DIST(3, 10, 0.3, TRUE)" to 0.6496107184000002,
            "BINOM.INV(20, 0.4, 0.7)" to 9.0,
            "POISSON.DIST(2, 3, TRUE)" to 0.42319008112684364,
            "POISSON.DIST(4, 2.5, FALSE)" to 0.13360188578108528,
            "EXPON.DIST(1.2, 0.5, TRUE)" to 0.4511883639059736,
            "GAMMA.DIST(2, 3, 1.5, TRUE)" to 0.15063144384932478,
            "GAMMA.DIST(2, 3, 1.5, FALSE)" to 0.15620571147598622,
            "GAMMA.INV(0.6, 2, 3)" to 6.066939735973969,
            "BETA.DIST(0.3, 2, 5, TRUE)" to 0.5798250000000003,
            "BETA.DIST(0.3, 2, 5, FALSE)" to 2.1608999999999994,
            "BETA.INV(0.4, 2, 5)" to 0.2225835336154239,
            "LOGNORM.DIST(2, 0.5, 1, TRUE)" to 0.5765781482392448,
            "LOGNORM.INV(0.3, 0.5, 1)" to 0.9758947732172247,
            "WEIBULL.DIST(1.5, 2, 1, TRUE)" to 0.8946007754381357,
            "HYPGEOM.DIST(2, 5, 10, 30, FALSE)" to 0.3599848427434634,
            "HYPGEOM.DIST(2, 5, 10, 30, TRUE)" to 0.8087659466969812,
            "NEGBINOM.DIST(3, 5, 0.4, FALSE)" to 0.0774144,
            "NEGBINOM.DIST(3, 5, 0.4, TRUE)" to 0.17367040000000003,
            "GAMMA(4.5)" to 11.63172839656745,
            "GAMMA(-1.5)" to 2.363271801207355,
            "GAMMALN(10)" to 12.801827480081469,
            "ERF(0.7)" to 0.6778011938374183,
            "ERFC(2.5)" to 0.00040695201744495886,
            "BESSELJ(2.5, 1)" to 0.4970941024642741,
            "BESSELY(2.5, 2)" to -0.38133584924180336,
            "BESSELI(1.5, 2)" to 0.33783461833568074,
            "BESSELK(1.5, 1)" to 0.2773878004568438,
            "BESSELK(0.2, 0)" to 1.7527038555281462,
            "CONFIDENCE.NORM(0.05, 2.5, 50)" to 0.6929519121748389,
            "CONFIDENCE.T(0.05, 2.5, 50)" to 0.7104921387393246,
        ).forEach { (f, want) -> near(want, f, tol = 1e-7) }
    }

    @Test fun statistics() {
        val x = col(3.1, 4.5, 2.2, 6.7, 5.0, 4.4, 3.9)
        val y = col(2.0, 3.8, 4.1, 5.5, 2.9, 4.0, 6.1, 3.3)
        near(0.686301068385438, "T.TEST(A:A, B:B, 2, 2)", x, y, tol = 1e-7)
        near(0.6881505775881387, "T.TEST(A:A, B:B, 2, 3)", x, y, tol = 1e-7)
        near(0.7560278867119533, "T.TEST(A1:A7, B1:B7, 2, 1)", x, y, tol = 1e-7)
        near(0.3741288232914836, "SKEW(A:A)", x)
        near(0.28864665375625714, "SKEW.P(A:A)", x)
        near(0.7811561562858449, "KURT(A:A)", x)
        near(3.74, "PERCENTILE(A:A, 0.3)", x)
        near(2.0, "MODE(1, 2, 2, 3, 3)")
        near(3.5, "QUARTILE(A:A, 1)", col(1, 2, 4, 7, 8, 9, 10, 12))
        near(5.0, "RANK(7, A:A, 1)", col(7, 3.5, 3.5, 1, 2))
        near(4.0, "RANK.AVG(94, A:A)", col(89, 88, 92, 101, 94, 97, 95))
        near(0.333, "PERCENTRANK(A:A, 2)", col(13, 12, 11, 8, 4, 3, 2, 1, 1, 1))
        near(0.7, "PERCENTRANK.EXC(A:A, 7)", col(1, 2, 3, 6, 6, 6, 7, 8, 9))
        near(3.777777777777778, "TRIMMEAN(A:A, 0.2)", col(4, 5, 6, 7, 2, 3, 4, 5, 1, 2, 3))
        near(5.476986969656962, "GEOMEAN(A:A)", col(4, 5, 8, 7, 11, 4, 3))
        near(10.607253086419753, "FORECAST(30, A:A, B:B)", col(6, 7, 9, 15, 21), col(20, 28, 31, 38, 40))
        near(2.0, "COUNTIFS(A:A, \">2\", B:B, \"x*\")", col(1, 3, 5, 7), col("xa", "xb", "y", "xc"))
        near(6.0, "AVERAGEIFS(A:A, B:B, \"x?\", A:A, \">1\")", col(1, 3, 5, 9), col("xa", "xb", "y", "xc"))
        near(9.0, "MAXIFS(A:A, B:B, \"<>y\")", col(1, 3, 5, 9), col("xa", "xb", "y", "xc"))
        near(4.0, "SUMIFS(A:A, B:B, \"x*\", A:A, \"<9\")", col(1, 3, 5, 9), col("xa", "xb", "y", "xc"))
        near(2.0, "COUNTIF(A:A, \"apple\")", col("Apple", "pear", "APPLE"))
        near(1.0, "COUNTBLANK(A1:A3)", col("1", "", "x"))
        near(3.0, "AVERAGEA(A:A)", col(4, "x", 5))
    }

    @Test fun math() {
        listOf(
            "MROUND(10, 3)" to 9.0, "EVEN(1.5)" to 2.0, "ODD(-1.5)" to -3.0, "GCD(24, 36)" to 12.0, "LCM(24, 36)" to 72.0,
            "QUOTIENT(-10, 3)" to -3.0, "TRUNC(-8.9)" to -8.0, "CEILING.MATH(-5.5, 2, -1)" to -6.0, "FLOOR.MATH(-5.5, 2, -1)" to -4.0,
            "CEILING.MATH(-5.5, 2)" to -4.0, "FLOOR(-2.5, -2)" to -2.0, "MULTINOMIAL(2, 3, 4)" to 1260.0, "PERMUT(100, 3)" to 970200.0,
            "COMBINA(4, 3)" to 20.0, "FACTDOUBLE(7)" to 105.0, "COMBIN(8, 2)" to 28.0, "SQRTPI(1)" to 1.772453850905516,
            "ACOT(2)" to 0.4636476090008061, "ACOTH(6)" to 0.16823611831060645, "SEC(45)" to 1.9035944074044246,
            "5%" to 0.05, "2^3^2" to 512.0, "-2^2" to 4.0, "10/4" to 2.5, "LOG(8, 2)" to 3.0, "ROUND(-2.5, 0)" to -3.0,
        ).forEach { (f, want) -> near(want, f) }
        near(-55.0, "SUMX2MY2(A:A, B:B)", col(2, 3, 9, 1, 8, 7, 5), col(6, 5, 11, 7, 5, 4, 4))
        near(1.0 + 2 + 4 + 8, "SERIESSUM(2, 0, 1, A1:A4)", col(1, 1, 1, 1))
        near(88.0, "MDETERM(A1:D4)", col(1, 1, 1, 7), col(3, 3, 1, 3), col(8, 6, 1, 10), col(5, 1, 0, 2))
        near(10.0, "SUBTOTAL(9, A1:A4)", col(1, 2, 3, 4))
        near(4.0, "AGGREGATE(14, 6, A1:A4, 1)", col(1, "=1/0", 3, 4))
    }

    @Test fun text() {
        text("Sale", "LEFT(\"Sale Price\", 4)")
        text("Flow", "MID(\"Fluid Flow\", 7, 20)")
        text("Quarter 1, 2012", "SUBSTITUTE(\"Quarter 1, 2011\", \"1\", \"2\", 3)")
        text("This Is A Title", "PROPER(\"this is a TITLE\")")
        text("a b", "TRIM(\"  a   b \")")
        near(8.0, "FIND(\"M\", \"Miriam McGovern\", 3)")
        near(7.0, "SEARCH(\"e\", \"Statements\", 6)")
        near(6.0, "SEARCH(\"m?nt\", \"Statements\")")
        text("MCMXCIX", "ROMAN(1999)")
        near(1999.0, "ARABIC(\"MCMXCIX\")")
        text("FF", "BASE(255, 16)")
        near(255.0, "DECIMAL(\"FF\", 16)")
        text("-*-*-*", "REPT(\"-*\", 3)")
        text("a-b", "TEXTBEFORE(\"a-b-c\", \"-\", 2)")
        text("b-c", "TEXTAFTER(\"a-b-c\", \"-\")")
        near(1000.0, "VALUE(\"$1,000\")")
        text("1,234.6", "FIXED(1234.567, 1)")
        text("(\$1,234.57)", "DOLLAR(-1234.567, 2)")
        text("A", "CHAR(65)")
        text("x, 2, z", "TEXTJOIN(\", \", TRUE, A1:A4)", col("x", 2, "", "z"))
        text("x2z", "CONCAT(A1:A4)", col("x", 2, "", "z"))
        text("say \"hi\"", "\"say \"\"hi\"\"\"")
        text("1,234.57", "TEXT(1234.567, \"#,##0.00\")")
        text("25.6%", "TEXT(0.256, \"0.0%\")")
        text("1.23E+04", "TEXT(12345, \"0.00E+00\")")
        text("2024-03-05", "TEXT(DATE(2024, 3, 5), \"yyyy-mm-dd\")")
        text("18:00", "TEXT(0.75, \"hh:mm\")")
        text("Mar 5, 2024", "TEXT(DATE(2024, 3, 5), \"mmm d, yyyy\")")
        text("ab3", "\"a\"&\"b\"&1+2")
        near(1.0, "\"abc\"=\"ABC\"")
        near(1.0, "EXACT(\"a\", \"a\")")
    }

    @Test fun lookups() {
        val names = col("ant", "bee", "cat", "dog")
        val nums = col(10, 20, 30, 40)
        text("30", "VLOOKUP(\"cat\", A1:B4, 2, FALSE)", names, nums)
        text("20", "VLOOKUP(25, B1:B4, 1)", names, nums)
        text("dog", "XLOOKUP(40, B:B, A:A)", names, nums)
        text("none", "XLOOKUP(45, B:B, A:A, \"none\")", names, nums)
        text("dog", "XLOOKUP(45, B:B, A:A, \"none\", -1)", names, nums)
        text("ant", "XLOOKUP(5, B:B, A:A, , 1)", names, nums)
        text("cat", "INDEX(A:A, MATCH(30, B:B, 0))", names, nums)
        near(3.0, "MATCH(\"c*\", A:A, 0)", names, nums)
        near(2.0, "MATCH(25, B:B)", names, nums)
        near(90.0, "SUM(OFFSET(B1, 1, 0, 3))", names, nums)
        near(20.0, "INDIRECT(\"B2\")", names, nums)
        near(100.0, "SUM(INDIRECT(\"B1:B4\"))", names, nums)
        text("\$C\$2", "ADDRESS(2, 3)")
        text("C2", "ADDRESS(2, 3, 4)")
        text("b", "CHOOSE(2, \"a\", \"b\", \"c\")")
        near(4.0, "ROWS(A1:B4)", names, nums)
        near(2.0, "COLUMNS(A1:B4)", names, nums)
        text("bee", "HLOOKUP(\"x\", A1:B2, 2, FALSE)", col("x", "bee"), col("y", "z"))
        assertEquals("#N/A", eval("VLOOKUP(\"emu\", A1:B4, 2, FALSE)", names, nums).error)
        assertEquals("#REF!", eval("VLOOKUP(\"ant\", A1:B4, 3, FALSE)", names, nums).error)
    }

    @Test fun logic() {
        text("zero", "IFS(A1<0, \"neg\", A1=0, \"zero\", TRUE, \"pos\")", col(0))
        text("two", "SWITCH(2, 1, \"one\", 2, \"two\", \"many\")")
        text("many", "SWITCH(9, 1, \"one\", 2, \"two\", \"many\")")
        near(1.0, "XOR(TRUE, FALSE)")
        near(1.0, "ISBLANK(A2)", col(1, ""))
        near(1.0, "ISTEXT(A1)", col("x"))
        near(1.0, "ISNA(NA())")
        near(1.0, "ISERR(1/0)")
        near(0.0, "ISERR(NA())")
        near(2.0, "ERROR.TYPE(1/0)")
        near(7.0, "IFNA(MATCH(9, A:A, 0), 7)", col(1, 2))
        near(1.0, "ISFORMULA(A1)", col("=1+1"))
        near(1.0, "ISEVEN(-2)")
        near(2.0, "TYPE(\"x\")")
    }

    @Test fun dates() {
        near(39637.0, "DATE(2008, 7, 8)")
        near(3.0, "WEEKDAY(DATE(2008, 7, 8))")
        near(2.0, "WEEKDAY(DATE(2008, 7, 8), 2)")
        near(440.0, "DATEDIF(DATE(2001, 6, 1), DATE(2002, 8, 15), \"D\")")
        near(75.0, "DATEDIF(DATE(2001, 6, 1), DATE(2002, 8, 15), \"YD\")")
        near(14.0, "DATEDIF(DATE(2001, 6, 1), DATE(2002, 8, 15), \"MD\")")
        near(1.0, "DATEDIF(DATE(2001, 6, 1), DATE(2002, 8, 15), \"Y\")")
        near(110.0, "NETWORKDAYS(DATE(2012, 10, 1), DATE(2013, 3, 1))")
        near(109.0, "NETWORKDAYS(DATE(2012, 10, 1), DATE(2013, 3, 1), A1)", col(41235))
        near(40602.0, "EOMONTH(DATE(2011, 1, 1), 1)")
        near(40817.0, "EDATE(DATE(2011, 1, 15), 9)-14")
        near(0.5805555555555556, "YEARFRAC(DATE(2012, 1, 1), DATE(2012, 7, 30))")
        near(10.0, "WEEKNUM(DATE(2012, 3, 9))")
        near(11.0, "WEEKNUM(DATE(2012, 3, 9), 2)")
        near(2008.0, "YEAR(39637)")
        near(18.0, "HOUR(0.75)")
        near(0.5, "TIME(12, 0, 0)")
        near(39637.0, "DATEVALUE(\"2008-07-08\")")
        near(46.0, "WORKDAY(DATE(2008, 10, 1), 151)-39887")
        near(28.0, "DAYS360(DATE(2011, 1, 30), DATE(2011, 2, 28))")
        near(330.0, "DAYS360(DATE(2011, 1, 30), DATE(2011, 12, 31))")
    }

    @Test fun finance() {
        listOf(
            "PMT(5%/12, 360, 200000)" to -1073.6432460242797,
            "FV(0.06/12, 10, -200, -500, 1)" to 2581.4033740601362,
            "PV(0.08/12, 240, 500)" to -59777.145851187815,
            "NPER(0.12/12, -100, -1000, 10000, 1)" to 59.67386567429457,
            "RATE(48, -200, 8000)" to 0.007701472488246008,
            "IPMT(0.1/12, 1, 36, 8000)" to -66.66666666666667,
            "PPMT(0.1/12, 1, 24, 2000)" to -75.62318600836664,
            "NPV(0.1, -10000, 3000, 4200, 6800)" to 1188.4434123352207,
            "EFFECT(0.0525, 4)" to 0.05354266737075841,
            "NOMINAL(0.053543, 4)" to 0.05250031986886323,
            "DDB(2400, 300, 10, 1)" to 480.0,
            "DDB(2400, 300, 10, 10)" to 22.122547200000017,
            "SYD(30000, 7500, 10, 1)" to 4090.909090909091,
            "DB(1000000, 100000, 6, 1, 7)" to 186083.33333333334,
            "DB(1000000, 100000, 6, 2, 7)" to 259639.41666666666,
            "SLN(30000, 7500, 10)" to 2250.0,
            "CUMIPMT(0.09/12, 360, 125000, 13, 24, 0)" to -11135.232130750845,
            "RRI(96, 10000, 11000)" to 0.0009933073762913303,
            "PDURATION(0.025, 2000, 2200)" to 3.859866162622655,
        ).forEach { (f, want) -> near(want, f, tol = 1e-8) }
        near(0.08663094803653171, "IRR(A1:A6)", col(-70000, 12000, 15000, 18000, 21000, 26000), tol = 1e-8)
        near(0.1260941303659051, "MIRR(A1:A6, 0.1, 0.12)", col(-120000, 39000, 30000, 21000, 37000, 46000), tol = 1e-8)
        near(2086.647602031535, "XNPV(0.09, A1:A5, B1:B5)", col(-10000, 2750, 4250, 3250, 2750), col(39448, 39508, 39751, 39859, 39904), tol = 1e-8)
        near(0.37336253351883, "XIRR(A1:A5, B1:B5)", col(-10000, 2750, 4250, 3250, 2750), col(39448, 39508, 39751, 39859, 39904), tol = 1e-6)
    }

    @Test fun engineering() {
        text("1001", "DEC2BIN(9, 4)")
        text("1110011100", "DEC2BIN(-100)")
        near(-1.0, "BIN2DEC(\"1111111111\")")
        near(-100.0, "HEX2DEC(\"FFFFFFFF9C\")")
        text("FF", "DEC2HEX(255)")
        text("00001111", "HEX2BIN(\"F\", 8)")
        near(44.0, "OCT2DEC(\"54\")")
        text("7777777634", "DEC2OCT(-100)")
        near(9.0, "BITAND(13, 25)")
        near(16.0, "BITLSHIFT(4, 2)")
        near(0.45359237, "CONVERT(1, \"lbm\", \"kg\")")
        near(20.0, "CONVERT(68, \"F\", \"C\")")
        near(2500.0, "CONVERT(2.5, \"km\", \"m\")")
        assertEquals("#N/A", eval("CONVERT(1, \"kg\", \"m\")").error)
        near(13.0, "IMABS(\"5+12i\")")
        text("8+i", "IMSUM(\"3+4i\", \"5-3i\")")
        text("27+11i", "IMPRODUCT(\"3+4i\", \"5-3i\")")
        text("5+12i", "IMDIV(\"-238+240i\", \"10+24i\")")
        text("2i", "IMSQRT(\"-4\")")
        text("3-4i", "COMPLEX(3, -4)")
        text("-j", "COMPLEX(0, -1, \"j\")")
        near(-4.0, "IMAGINARY(\"3-4i\")")
        near(1.0, "GESTEP(5, 4)")
    }

    @Test fun everyListedExampleWorks() {
        val data = listOf(col(1, 2, 3, 4, 5), col(2, 4, 5, 4, 5), col(3, 1, 2, 7, 8), col(1, 0, 2, 1, 3), col(5, 1, 2, 4, 8), col(1, 1, 1, 1, 1))
        Sheet.FUNCTIONS.forEach { h ->
            val v = Sheet.Book(data + listOf(listOf("=" + h.example))).value(data.size, 0)
            assertNotEquals("${h.example} gave ${v.error}", "#NAME?", v.error)
            assertNotEquals("${h.example} gave ${v.error}", "#VALUE!", v.error)
        }
    }
}

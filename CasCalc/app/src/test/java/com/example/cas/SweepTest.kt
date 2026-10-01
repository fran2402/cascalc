package com.example.cas

import com.example.cas.cas.Calculus
import com.example.cas.cas.E
import com.example.cas.cas.Expr
import com.example.cas.cas.Flt
import com.example.cas.cas.INF
import com.example.cas.cas.Num
import com.example.cas.cas.Numeric
import com.example.cas.cas.PI
import com.example.cas.cas.Printer
import com.example.cas.cas.Sym
import com.example.cas.cas.add
import com.example.cas.cas.contains
import com.example.cas.cas.div
import com.example.cas.cas.fn
import com.example.cas.cas.mul
import com.example.cas.cas.neg
import com.example.cas.cas.pow
import com.example.cas.cas.sub
import com.example.cas.math.Rational
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Integrals and limits against sympy and mpmath (references generated): each antiderivative
 * must be a closed form whose F(b) − F(a) matches mpmath's quadrature, each limit a closed
 * form equal to sympy's.
 */
class SweepTest {
    /** A small parser for the cases: numbers, x, pi, e, + − * / ^, f(…). */
    private class P(val s: String) {
        var i = 0
        fun parse(): Expr = sum().also { require(i == s.length) { "junk at $i in $s" } }
        private fun sum(): Expr {
            var v = term()
            while (i < s.length && (s[i] == '+' || s[i] == '-')) { val op = s[i++]; val t = term(); v = if (op == '+') add(v, t) else sub(v, t) }
            return v
        }
        private fun term(): Expr {
            var v = unary()
            while (i < s.length && (s[i] == '*' || s[i] == '/')) { val op = s[i++]; val t = unary(); v = if (op == '*') mul(v, t) else div(v, t) }
            return v
        }
        private fun unary(): Expr = if (s[i] == '-') { i++; neg(unary()) } else power()
        private fun power(): Expr { val b = atom(); return if (i < s.length && s[i] == '^') { i++; pow(b, unary()) } else b }
        private fun atom(): Expr {
            if (s[i] == '(') { i++; val v = sum(); i++; return v }
            if (s[i].isDigit() || s[i] == '.') {
                val st = i; while (i < s.length && (s[i].isDigit() || s[i] == '.')) i++
                return Num(Rational.parseDecimal(s.substring(st, i)))
            }
            val st = i; while (i < s.length && s[i].isLetter()) i++
            val name = s.substring(st, i)
            if (i < s.length && s[i] == '(') {
                i++; val args = ArrayList<Expr>(); args += sum(); while (s[i] == ',') { i++; args += sum() }; i++
                return when (name) { "exp" -> pow(E, args[0]); else -> fn(name, *args.toTypedArray()) }
            }
            return when (name) { "pi" -> PI; "e" -> E; "oo" -> INF; else -> Sym(name) }
        }
    }
    private val x = Sym("x")
    private fun parse(s: String) = P(s).parse()
    private fun decimal(e: Expr) = e.contains { it is Flt }

    private class Case(val text: String, val a: Double, val b: Double, val ref: Double)
    private fun I(t: String, a: Double, b: Double, ref: Double) = Case(t, a, b, ref)
    private class Lim(val text: String, val at: String, val side: Int, val ref: Double)
    private fun L(t: String, at: String, side: Int, ref: Double) = Lim(t, at, side, ref)

    private val integrals = listOf(
        I("x^2*exp(-x^2)", 0.0, 1.5, 0.3490447783910523),
        I("exp(-x^2)", 0.0, 2.0, 0.8820813907624216),
        I("exp(-2*x^2+x)", -1.0, 1.0, 1.3164930808792228),
        I("exp(x^2)", 0.0, 1.0, 1.4626517459071815),
        I("x^4*exp(-x^2)", 0.0, 1.0, 0.10026879814501737),
        I("sin(x)/x", 0.5, 3.0, 1.3555451099564015),
        I("cos(x)/x", 0.5, 3.0, 0.29741386481461324),
        I("exp(x)/x", 0.5, 2.0, 4.500014451138717),
        I("exp(-x)/x", 0.5, 2.0, 0.5108730840680997),
        I("sinh(x)/x", 0.5, 2.0, 1.9945706835353085),
        I("cosh(x)/x", 0.5, 2.0, 2.505443767603408),
        I("sin(3*x)/x", 0.2, 2.0, 0.8365587416724264),
        I("sin(x)/x^2", 0.5, 3.0, 1.2092249393363967),
        I("exp(x)/x^2", 0.5, 2.0, 4.102928943073648),
        I("cos(2*x+1)/x", 0.5, 2.0, -0.9418483278977057),
        I("sin(x)/(x+1)", 0.0, 2.0, 0.6709109766018952),
        I("1/ln(x)", 2.0, 5.0, 2.589424529915159),
        I("x/ln(x)", 2.0, 4.0, 5.552131368672009),
        I("sin(x^2)", 0.0, 2.0, 0.8047764893437561),
        I("cos(x^2)", 0.0, 2.0, 0.4614614624332164),
        I("sin(3*x^2)", 0.0, 1.0, 0.5149761744855366),
        I("x^(1/2)*exp(-x)", 0.5, 3.0, 0.6111790716427796),
        I("exp(-x)/x^(1/2)", 0.5, 3.0, 0.5370617222709437),
        I("x^(3/2)*exp(-2*x)", 0.5, 2.0, 0.16283121704261305),
        I("ln(1+x)/x", 0.2, 0.9, 0.561363041439726),
        I("ln(1-x)/x", 0.1, 0.8, -0.9721768089088573),
        I("ln(x)/(1-x)", 0.2, 0.8, -0.8637908245685436),
        I("ln(x)/(1+x)", 0.5, 2.0, 0.05421484695938181),
        I("1/(1-sin(x)^2/2)^(1/2)", 0.0, 1.0, 1.0832167728451687),
        I("(1-sin(x)^2/2)^(1/2)", 0.0, 1.0, 0.92732988362444),
        I("(1-x^2)^(1/2)", 0.0, 0.9, 0.7560352099586474),
        I("(x^2+1)^(1/2)", 0.0, 2.0, 2.957885715089195),
        I("(x^2-1)^(1/2)", 1.2, 3.0, 3.2744533771144835),
        I("1/(x^2-1)^(1/2)", 1.2, 3.0, 1.1403846703243075),
        I("(3+2*x-x^2)^(1/2)", 0.0, 2.0, 3.826445909962073),
        I("1/(x^2+2*x+5)", 0.0, 2.0, 0.25957305712326145),
        I("1/(x^4+1)", 0.0, 1.0, 0.866972987339911),
        I("x^3/(x^2+1)", 0.0, 2.0, 1.1952810437829497),
        I("sin(x)^2", 0.0, 2.0, 1.189200623826982),
        I("cos(x)^4", 0.0, 2.0, 0.5917168213799986),
        I("sin(x)^3*cos(x)^2", 0.0, 2.0, 0.15485974614972117),
        I("sin(x)^2*cos(x)^2", 0.0, 2.0, 0.21908255479301933),
        I("tan(x)^2", 0.0, 1.0, 0.5574077246549022),
        I("tan(x)^3", 0.0, 1.0, 0.5971329400213656),
        I("1/cos(x)", 0.0, 1.0, 1.2261911708835171),
        I("1/sin(x)", 0.5, 2.0, 1.808174488567243),
        I("1/cos(x)^2", 0.0, 1.0, 1.5574077246549023),
        I("exp(x)*sin(x)", 0.0, 2.0, 5.396891009033804),
        I("exp(2*x)*cos(3*x)", 0.0, 1.0, -1.038614555468807),
        I("x*exp(x)*sin(x)", 0.0, 1.0, 0.6436776435894211),
        I("x^2*ln(x)", 1.0, 2.0, 1.0706147037154097),
        I("ln(x)^2", 1.0, 3.0, 1.0291731504290877),
        I("atan(x)", 0.0, 2.0, 1.4095784793711308),
        I("x*atan(x)", 0.0, 2.0, 1.7678717944852262),
        I("asin(x)", 0.0, 0.9, 0.44368245785283816),
        I("x*sin(x)", 0.0, 2.0, 1.7415910999199664),
        I("x^2*cos(3*x)", 0.0, 2.0, 0.07488579633484861),
        I("1/(1+exp(x))", 0.0, 2.0, 0.5662191695169728),
        I("exp(x^(1/2))", 0.5, 2.0, 4.595570434853347),
        I("sin(x^(1/2))", 0.5, 3.0, 2.306111796323321),
        I("x*(1-x^2)^(1/2)", 0.0, 0.9, 0.30572697335757576),
        I("1/(x*ln(x))", 2.0, 4.0, 0.6931471805599453),
        I("erf(x)", 0.0, 2.0, 1.4367884391671952),
        I("x*exp(-x^2)", 0.0, 2.0, 0.4908421805556329),
        I("1/(1+cos(x))", 0.0, 1.0, 0.5463024898437905),
        I("sinh(x)^2", 0.0, 1.0, 0.4067151019617547),
        I("1/(x^2-4)", 3.0, 5.0, 0.19053501301172418),
        I("(x+1)/(x^2+x+1)", 0.0, 2.0, 1.3850240368386155),
        I("x^2/(1-x^2)^(1/2)", 0.0, 0.9, 0.3637343050399868),
        I("1/(x*(x^2-1)^(1/2))", 1.5, 3.0, 0.3898907467728444),
        I("exp(-x)*x^5", 0.0, 3.0, 10.070153043756413),
        I("ln(x)/x^2", 1.0, 3.0, 0.30046257044396346),
        I("sin(ln(x))", 1.0, 3.0, 1.153616928261706),
        I("x/(x^4+1)", 0.0, 1.0, 0.39269908169872414),
        I("cos(x)/(1+sin(x)^2)", 0.0, 2.0, 0.7379281192944075),
        I("1/(x^2*(x^2+1)^(1/2))", 1.0, 2.0, 0.2961795736232002),
        I("x^2*exp(-x^2/2)", 0.0, 2.0, 0.9256174468493829),
        I("1/(x^(1/2)*(1+x))", 0.5, 3.0, 0.8634356850524209),
        I("sin(x)*cos(2*x)", 0.0, 2.0, -0.7014351327152989),
        I("x*cos(x)^2", 0.0, 2.0, 0.41489329973808436),
        I("exp(x)*cos(x)^2", 0.0, 1.0, 1.140365810254805),
        I("x^3*exp(-x^2)", 0.0, 1.0, 0.13212055882855767),
        I("1/(1+x^3)", 0.0, 1.0, 0.8356488482647211),
        I("x/(1+x)^(1/2)", 0.0, 2.0, 1.3333333333333333),
        I("ln(x^2+1)", 0.0, 2.0, 1.4331732604563818),
        I("x*ln(x+1)", 0.0, 2.0, 1.6479184330021646),
        I("cos(x)^3", 0.0, 2.0, 0.6586884452693508),
        I("1/(2+cos(x))", 0.0, 2.0, 0.8456521229740832),
        I("x^2*sin(x)^2", 0.0, 2.0, 2.3223573271595765),
        I("x^(1/2)*ln(x)", 1.0, 2.0, 0.49437658029308956),
        I("x^3*(1+x^2)^(1/2)", 0.0, 1.0, 0.32189514164974603),
        I("1/(x^2*(1+x^2))", 1.0, 2.0, 0.17824944560335781),
        I("exp(x)/(1+exp(2*x))", 0.0, 1.0, 0.4328847416198293),
        I("tan(x)", 0.0, 1.0, 0.6156264703860143),
        I("1/(sin(x)*cos(x))", 0.3, 1.0, 1.616349131408092),
        I("cos(x)^5", 0.0, 1.0, 0.5286328129112156),
        I("sin(x)^4*cos(x)^3", 0.0, 1.0, 0.041702078588825756),
        I("1/(1+sin(x))", 0.0, 1.0, 0.7065920069739766),
        I("exp(-x)*cos(x)^2", 0.0, 2.0, 0.5206940468268971),
        I("x^2*exp(-x)*sin(x)", 0.0, 2.0, 0.5688470377559104),
        I("ln(x)/x^(1/2)", 1.0, 3.0, 0.8775013733098747),
        I("x^5*exp(-x^2)", 0.0, 1.0, 0.08030139707139419),
        I("atan(x)/x^2", 1.0, 2.0, 0.4668256191232708),
        I("asin(x)/(1-x^2)^(1/2)", 0.0, 0.9, 0.6269418833601382),
        I("exp(atan(x))/(1+x^2)", 0.0, 1.0, 1.1932800507380155),
        I("1/(x*(1+ln(x)^2))", 1.0, 3.0, 0.8323529090607977),
        I("x/(4-x^2)^(1/2)", 0.0, 1.5, 0.6771243444677048),
        I("(x^2+4*x+5)^(1/2)", 0.0, 1.0, 2.6947540047794076),
        I("1/(2*x-x^2)^(1/2)", 0.2, 1.5, 1.450893993599911),
        I("x^2*(1-x^2)^(1/2)", 0.0, 0.9, 0.17037450950602548),
        I("1/(x^2+1)^2", 0.0, 1.0, 0.6426990816987241),
        I("x^2/(x^2+1)^2", 0.0, 1.0, 0.1426990816987242),
        I("1/(x^3-1)", 2.0, 3.0, 0.0753893510232044),
        I("(x^2+1)/(x^4+1)", 0.0, 1.0, 1.1107207345395915),
        I("1/(x^4-1)", 2.0, 3.0, 0.030417749724959135),
        I("1/(x^6+1)", 0.0, 1.0, 0.903771773748772),
        I("cosh(x)^3", 0.0, 1.0, 1.716223805850343),
        I("x*sinh(x)", 0.0, 1.0, 0.36787944117144233),
        I("sinh(x)*cos(x)", 0.0, 1.0, 0.4113138654470071),
        I("exp(2*x)*sin(x)^2", 0.0, 1.0, 1.2667743526340511),
        I("sin(x)/(1+cos(x)^2)", 0.0, 2.0, 1.1797462691928746),
        I("x/cos(x)^2", 0.0, 1.0, 0.9417812542688879),
        I("exp(x)*ln(x)", 1.0, 2.0, 2.062586862327095),
        I("sin(x)*ln(x)", 1.0, 2.0, 0.374027912325489),
        I("x*erf(x)", 0.0, 1.0, 0.3144520725925774),
        I("x^2*atan(x)", 0.0, 1.0, 0.21065725122580697),
        I("ln(x+(x^2+1)^(1/2))", 0.0, 1.0, 0.46716002464644796),
        I("exp(sin(x))*cos(x)", 0.0, 1.0, 1.3197768247158532),
        I("sin(2*x)*exp(sin(x))", 0.0, 1.0, 1.264496129024662),
        I("x^(1/3)*exp(-x)", 0.5, 2.0, 0.474004734018725),
        I("1/(x*(1+x)^(1/2))", 0.5, 2.0, 0.9754737726363609),
        I("sin(x)^2/x", 0.5, 2.0, 0.9323399909538946),
        I("1/(1+x^2)^(3/2)", 0.0, 1.0, 0.7071067811865476),
        I("x^2/(1+x^2)^(3/2)", 0.0, 1.0, 0.1742668058329955),
        I("1/cos(x)^3", 0.0, 1.0, 2.0543329332562488),
        I("1/(5+4*cos(x))", 0.0, 1.0, 0.12008472637682129),
        I("sin(x)/cos(x)^3", 0.0, 1.0, 1.2127594104073798),
        I("x*exp(-2*x^2+3*x)", 0.0, 1.0, 1.379031079206233),
        I("exp(-x^2)*erf(x)", 0.0, 1.0, 0.3146746444575077),
        I("cos(x)/x^3", 0.5, 2.0, 1.2546998426606166),
        I("x^2*ln(x)^2", 1.0, 2.0, 0.5674649013049307),
        I("ln(1+x^2)/x^2", 0.5, 2.0, 0.9285703639979381),

    )

    private val limits = listOf(
        L("(x-sin(x))/x^3", "0", 0, 0.16666666666666666),  // 1/6
        L("(sin(tan(x))-tan(sin(x)))/x^7", "0", 0, -0.03333333333333333),  // -1/30
        L("(1-cos(x))/x^2", "0", 0, 0.5),  // 1/2
        L("(exp(x)-1-x)/x^2", "0", 0, 0.5),  // 1/2
        L("x*ln(x)", "0", 1, 0.0),  // 0
        L("x^x", "0", 1, 1.0),  // 1
        L("(1+1/x)^x", "oo", 0, 2.718281828459045),  // E
        L("(1+2/x)^(3*x)", "oo", 0, 403.4287934927351),  // exp(6)
        L("x*sin(1/x)", "oo", 0, 1.0),  // 1
        L("(x^2+x)^(1/2)-x", "oo", 0, 0.5),  // 1/2
        L("ln(x)/x", "oo", 0, 0.0),  // 0
        L("x^2*exp(-x)", "oo", 0, 0.0),  // 0
        L("(tan(x)-x)/x^3", "0", 0, 0.3333333333333333),  // 1/3
        L("(exp(x)-exp(-x))/x", "0", 0, 2.0),  // 2
        L("(1-cos(x))/(x*sin(x))", "0", 0, 0.5),  // 1/2
        L("(x^(1/3)-1)/(x-1)", "1", 0, 0.3333333333333333),  // 1/3
        L("sin(3*x)/sin(5*x)", "0", 0, 0.6),  // 3/5
        L("(cos(x)-exp(-x^2/2))/x^4", "0", 0, -0.08333333333333333),  // -1/12
        L("x*(exp(1/x)-1)", "oo", 0, 1.0),  // 1
        L("(ln(1+x)-x)/x^2", "0", 0, -0.5),  // -1/2
        L("atan(x)", "oo", 0, 1.5707963267948966),  // pi/2
        L("erf(x)", "oo", 0, 1.0),  // 1
        L("si(x)", "oo", 0, 1.5707963267948966),  // pi/2
        L("x^(1/x)", "oo", 0, 1.0),  // 1
        L("(2^x-1)/x", "0", 0, 0.6931471805599453),  // log(2)
        L("((1+x)^(1/2)-(1-x)^(1/2))/x", "0", 0, 1.0),  // 1
        L("((1+x)^(1/x)-e)/x", "0", 0, -1.3591409142295225),  // -E/2
        L("(asin(x)-x)/x^3", "0", 0, 0.16666666666666666),  // 1/6
        L("x^2*ln(x)", "0", 1, 0.0),  // 0
        L("(exp(x)-1)/sin(x)", "0", 0, 1.0),  // 1
        L("sin(x)/x", "oo", 0, 0.0),  // 0
        L("(x^3-8)/(x-2)", "2", 0, 12.0),  // 12
        L("1/x-1/sin(x)", "0", 0, 0.0),  // 0
        L("1/x^2-1/sin(x)^2", "0", 0, -0.3333333333333333),  // -1/3
        L("(cosh(x)-1)/x^2", "0", 0, 0.5),  // 1/2
        L("ln(sin(x))/ln(x)", "0", 1, 1.0),  // 1
        L("x^(1/2)*ln(x)", "0", 1, 0.0),  // 0
        L("x-ln(cosh(x))", "oo", 0, 0.6931471805599453),  // log(2)
        L("tanh(x)", "oo", 0, 1.0),  // 1
        L("(1-x)^(1/x)", "0", 0, 0.36787944117144233),  // exp(-1)
        L("x^sin(x)", "0", 1, 1.0),  // 1
        L("(x^2-1)/(2*x^2-x-1)", "1", 0, 0.6666666666666666),  // 2/3
        L("x*(pi/2-atan(x))", "oo", 0, 1.0),  // 1
        L("(exp(x^2)-cos(x))/x^2", "0", 0, 1.5),  // 3/2
        L("x^3*exp(-x^2)", "oo", 0, 0.0),  // 0
        L("(1+sin(x))^(1/x)", "0", 0, 2.718281828459045),  // E
        L("(tan(x))^(tan(2*x))", "pi/4", -1, 0.36787944117144233),  // exp(-1)
        L("fresnels(x)", "oo", 0, 0.5),  // 1/2
        L("ei(x)", "-oo", 0, 0.0),  // 0
        L("x/(exp(x)-1)", "0", 0, 1.0),  // 1
        L("(1+x)^(1/x)", "0", 0, 2.718281828459045),  // E
        L("(x/(x+1))^x", "oo", 0, 0.36787944117144233),  // exp(-1)
        L("(x^2+2*x)^(1/2)-x", "oo", 0, 1.0),  // 1
        L("x*(ln(x+1)-ln(x))", "oo", 0, 1.0),  // 1
        L("(3^x-2^x)/x", "0", 0, 0.4054651081081644),  // -log(2) + log(3)
        L("sin(x)^x", "0", 1, 1.0),  // 1
        L("(1/x)^tan(x)", "0", 1, 1.0),  // 1
        L("ln(x)*ln(1-x)", "1", -1, 0.0),  // 0
        L("(exp(x)-exp(sin(x)))/(x-sin(x))", "0", 0, 1.0),  // 1
        L("(1-cos(1-cos(x)))/x^4", "0", 0, 0.125),  // 1/8
        L("x^(1/x^2)", "oo", 0, 1.0),  // 1
        L("(x-tan(x))/(x-sin(x))", "0", 0, -2.0),  // -2
        L("x^(1/2)*((x+1)^(1/2)-x^(1/2))", "oo", 0, 0.5),  // 1/2
        L("(2*x+1)/(3*x-4)", "oo", 0, 0.6666666666666666),  // 2/3
        L("exp(x)/x^5", "oo", 0, Double.POSITIVE_INFINITY),  // oo
        L("ln(ln(x))/ln(x)", "oo", 0, 0.0),  // 0
        L("(atan(x)-x)/x^3", "0", 0, -0.3333333333333333),  // -1/3
        L("cos(x)^(1/x^2)", "0", 0, 0.6065306597126334),  // exp(-1/2)
        L("(1+1/x^2)^x", "oo", 0, 1.0),  // 1
        L("(sin(x)-x*cos(x))/x^3", "0", 0, 0.3333333333333333),  // 1/3
        L("1/ln(x)-1/(x-1)", "1", 0, 0.5),  // 1/2
        L("x*sin(1/x)", "0", 0, 0.0),  // 0
        L("(x^x-x)/(1-x+ln(x))", "1", 0, -2.0),  // -2
        L("exp(-1/x^2)/x^10", "0", 0, 0.0),  // 0
        L("x*gamma(x)", "0", 1, 1.0),  // 1
    )

    @Test fun integrals() {
        val bad = ArrayList<String>()
        for (c in integrals) {
            val f = parse(c.text)
            val big = runCatching { Calculus.antiderivative(f, x) }
            val F = big.getOrNull()
            if (F == null) { bad += "∫ ${c.text}: none (${big.exceptionOrNull()?.message})"; continue }
            if (decimal(F) || F.contains { it is com.example.cas.cas.Fn && it.name == "integral" }) { bad += "∫ ${c.text}: not closed: ${Printer.plain(F)}"; continue }
            val v = runCatching { Numeric.real(F, mapOf("x" to c.b)) - Numeric.real(F, mapOf("x" to c.a)) }.getOrElse { Double.NaN }
            if (!(kotlin.math.abs(v - c.ref) <= 1e-7 * maxOf(1.0, kotlin.math.abs(c.ref)))) bad += "∫ ${c.text}: ${Printer.plain(F)} gives $v, expected ${c.ref}"
        }
        bad.forEach { println("SWEEP $it") }
        println("SWEEP integrals: ${integrals.size - bad.size}/${integrals.size}")
        assertTrue(bad.joinToString("\n"), bad.isEmpty())
    }

    @Test fun limits() {
        val bad = ArrayList<String>()
        for (c in limits) {
            val f = parse(c.text)
            val at = if (c.at == "-oo") neg(INF) else parse(c.at)
            val r = runCatching { Calculus.limit(f, x, at, c.side) }
            val v = r.getOrNull()
            if (v == null) { bad += "lim ${c.text}: ${r.exceptionOrNull()?.message}"; continue }
            if (decimal(v)) { bad += "lim ${c.text}: not closed: ${Printer.plain(v)}"; continue }
            if (c.ref.isInfinite()) {
                if (Printer.plain(v) != (if (c.ref > 0) "∞" else "-∞")) bad += "lim ${c.text}: ${Printer.plain(v)}, expected ${c.ref}"
                continue
            }
            val n = runCatching { Numeric.real(v) }.getOrElse { Double.NaN }
            if (!(kotlin.math.abs(n - c.ref) <= 1e-9 * maxOf(1.0, kotlin.math.abs(c.ref)))) bad += "lim ${c.text}: ${Printer.plain(v)} = $n, expected ${c.ref}"
        }
        bad.forEach { println("SWEEP $it") }
        println("SWEEP limits: ${limits.size - bad.size}/${limits.size}")
        assertTrue(bad.joinToString("\n"), bad.isEmpty())
    }
}

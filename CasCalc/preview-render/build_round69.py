# Builds round69.html: a mockup of the redesigned long-press card for a key, on a phone (a sheet
# over the keypad) and on a tablet (a two-column dialog): the key, its formula, what it is, how to
# use it step by step, and worked examples with Try it.
import math
k = dict(on='#1C1C16', onv='#47473B', low='#F6F4E8', c='#F1EEE2', ch='#EBE9DC', chh='#E5E3D6', chhh='#DFDDD0', pri='#5B6133', onpri='#fff',
         pc='#E0E6B2', onpc='#1A1E00', sc='#E2E4C4', onsc='#1B1D0A', tc='#BEECDB', ontc='#002019', tert='#3A665A', outv='#C7C7B5')
def frac(a, b, cls=''): return f'<span class="frac {cls}"><span>{a}</span><span class="bar"></span><span>{b}</span></span>'
def binom(a, b): return f'<span class="paren">(</span><span class="binom"><span>{a}</span><span>{b}</span></span><span class="paren">)</span>'
def i(t): return f'<i>{t}</i>'
def sub(b, s): return f'{b}<sub>{s}</sub>'
def sup(b, s): return f'{b}<sup>{s}</sup>'
def legendre_plot(w, h):
    # P2, P3, P4 on [-1, 1], in three theme colors.
    cols = [k['pri'], k['tert'], '#8B4A55']
    polys = [lambda x: (3*x*x-1)/2, lambda x: (5*x**3-3*x)/2, lambda x: (35*x**4-30*x*x+3)/8]
    def px(x): return 20 + (x + 1) / 2 * (w - 40)
    def py(y): return h / 2 - y * (h / 2 - 14)
    out = f'<line x1="20" y1="{h/2}" x2="{w-20}" y2="{h/2}" stroke="{k["outv"]}" stroke-width="1.5"/><line x1="{w/2}" y1="8" x2="{w/2}" y2="{h-8}" stroke="{k["outv"]}" stroke-width="1.5"/>'
    for c, f in zip(cols, polys):
        pts = ' '.join(f'{px(x):.1f},{py(f(x)):.1f}' for x in [-1 + 2 * t / 120 for t in range(121)])
        out += f'<polyline points="{pts}" fill="none" stroke="{c}" stroke-width="3" stroke-linecap="round"/>'
    labels = ''.join(f'<span class="lg"><span class="dot" style="background:{c}"></span>{sub(i("P"), n)}</span>' for c, n in zip(cols, (2, 3, 4)))
    return f'<svg width="{w}" height="{h}">{out}</svg><div class="legend">{labels}</div>'

def section(title, body, icon=''): return f'<div class="sec"><div class="st">{icon}{title}</div>{body}</div>'
def steps(items): return '<ol class="steps">' + ''.join(f'<li><span class="n">{n+1}</span><span>{t}</span></li>' for n, t in enumerate(items)) + '</ol>'
def example(q, a, note, try_it=True):
    return (f'<div class="ex"><div class="exm"><span class="q">{q}</span><span class="eq">=</span><span class="a">{a}</span></div>'
            f'<div class="exn">{note}</div>' + (f'<span class="try">↵ Try it</span>' if try_it else '') + '</div>')
ICON = lambda p: f'<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="{k["pri"]}" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="margin-right:8px;vertical-align:-3px">{p}</svg>'
I_WHAT = ICON('<circle cx="12" cy="12" r="9"/><path d="M12 11v6"/><path d="M12 7.5v.5"/>')
I_HOW = ICON('<path d="M5 5h14M5 12h14M5 19h9"/>')
I_EX = ICON('<path d="M4 18L10 6l4 8 2-4 4 8"/>')

# ---- Phone: n choose k, as a sheet over the calculator.
phone_card = f'''
<div class="sheet">
  <div class="handle"></div>
  <div class="head">
    <div class="keychip">{binom(i("n"), i("k"))}</div>
    <div><div class="title">Binomial coefficient</div><div class="where">Statistics tab · long-pressed key</div></div>
  </div>
  <div class="formula">{binom(i("n"), i("k"))}<span class="eq">=</span>{frac(i("n")+"!", i("k")+"! ("+i("n")+" − "+i("k")+")!")}</div>
  {section("What it is", '<p>The number of ways to choose ' + i('k') + ' things from ' + i('n') + ', when order doesn’t matter. Also the coefficients of ' + sup('(' + i('a') + ' + ' + i('b') + ')', i('n')) + '.</p>', I_WHAT)}
  {section("How to use", steps(['Tap the key: a stack of two boxes appears.', 'Type ' + i('n') + ' in the top box, then → to move down.', 'Type ' + i('k') + ' in the bottom box, then =.']), I_HOW)}
  {section("Examples", example(binom("5", "2"), "10", "Pairs from 5 people.") + example(binom(i("n"), "2"), frac(i("n")+"("+i("n")+" − 1)", "2", "small"), "Letters work too: kept exact."), I_EX)}
  <div class="actions"><span class="tonal">Pin</span><span class="tonal">Copy LaTeX</span><span class="text">Close</span></div>
</div>'''

# ---- Tablet: Legendre polynomial, as a wide dialog in two columns.
legendre_formula = (f'({i("n")} + 1) {sub(i("P"), i("n")+" + 1")}({i("x")}) <span class="eq">=</span> (2{i("n")} + 1) {i("x")} {sub(i("P"), i("n"))}({i("x")}) − {i("n")} {sub(i("P"), i("n")+" − 1")}({i("x")})')
tablet_card = f'''
<div class="dialog">
  <div class="col left">
    <div class="head">
      <div class="keychip big">{sub(i("P"), "□")}(□)</div>
      <div><div class="title">Legendre polynomial</div><div class="where">Polynomials tab · {sub(i("P"), i("n"))}({i("x")})</div></div>
    </div>
    <div class="formula wide">{legendre_formula}</div>
    <div class="formula small">{sub(i("P"), "0")} = 1, &nbsp; {sub(i("P"), "1")} = {i("x")}</div>
    {section("What it is", '<p>Polynomials orthogonal on [−1, 1]: ' + '∫' + sub('', '−1') + sup('', '1') + ' ' + sub(i('P'), i('m')) + sub(i('P'), i('n')) + ' d' + i('x') + ' = 0 for ' + i('m') + ' ≠ ' + i('n') + '. They describe gravity and charge around a sphere.</p>', I_WHAT)}
    <div class="plot">{legendre_plot(380, 150)}</div>
    <div class="related"><span class="rl">Related</span><span class="chip">{sub(i("P"), i("n"))+sup("", i("m"))}(x) associated</span><span class="chip">{sub(i("C"), i("n"))+sup("", "α")}(x) Gegenbauer</span><span class="chip">{sub(i("T"), i("n"))}(x) Chebyshev</span></div>
  </div>
  <div class="col right">
    {section("How to use", steps(['Tap the key: it shows ' + sub(i('P'), '□') + '(□).', 'Type the degree ' + i('n') + ' in the small box, then →.', 'Type the variable or a number in the brackets, then =.', 'Graph it as ' + i('y') + ' = ' + sub(i('P'), '3') + '(' + i('x') + '), or long-press = for the steps.']), I_HOW)}
    {section("Examples",
        example(sub(i("P"), "2") + "(" + i("x") + ")", frac("3"+i("x")+"<sup>2</sup> − 1", "2", "small"), "The polynomial, expanded exactly.") +
        example(sub(i("P"), "3") + "(" + frac("1", "2", "tiny") + ")", "−" + frac("7", "16", "small"), "At a number: an exact fraction.") +
        example("∫<sub class='lim'>−1</sub><sup class='lim'>1</sup> " + sub(i("P"), "2") + "(" + i("x") + ")" + sup("", "2") + " d" + i("x"), frac("2", "5", "small"), "Its norm: 2 / (2n + 1)."),
        I_EX)}
    <div class="actions"><span class="tonal">Pin</span><span class="tonal">Copy LaTeX</span><span class="text">Close</span></div>
  </div>
</div>'''

keys = ''.join(f'<div class="k">{t}</div>' for t in ['x̄', 'med', 's', 'σ', 's²', binom('n', 'k'), 'P', 'φ', 'Φ', 'Φ⁻¹'])
pad = ''.join(f'<div class="k d">{t}</div>' for t in '789÷456×123−0.=+')
css = f'''
@font-face{{font-family:GSF;src:url(../app/src/main/res/font/google_sans_flex.ttf)}}
@font-face{{font-family:CMR;src:url(../app/src/main/res/font/cm_main.otf)}}
@font-face{{font-family:CMI;src:url(../app/src/main/res/font/cm_italic.otf)}}
body{{margin:0;background:#dcd8cf;font-family:GSF;padding:32px;display:flex;gap:36px;align-items:flex-start;width:max-content}}
.cap{{font:600 16px GSF;color:{k["on"]};margin:0 0 10px 6px}}
i{{font-family:CMI;font-style:normal}}
.formula, .exm, .keychip, .plot .legend, .related .chip, .steps i, p i, .where i {{font-family:CMR}}
sub{{font-size:.7em;vertical-align:-.35em}} sup{{font-size:.7em;vertical-align:.55em}}
.frac{{display:inline-flex;flex-direction:column;align-items:center;vertical-align:middle;margin:0 .15em}}.frac .bar{{height:1.5px;background:currentColor;align-self:stretch;margin:.08em 0}}
.frac.small{{font-size:.8em}}.frac.tiny{{font-size:.7em}}
.binom{{display:inline-flex;flex-direction:column;align-items:center;vertical-align:middle;line-height:1.05}}.paren{{font-size:1.9em;vertical-align:middle;font-family:CMR;line-height:1}}
/* phone */
.phone{{width:412px;height:892px;border-radius:44px;background:{k["low"]};position:relative;overflow:hidden;box-shadow:0 6px 24px #0002}}
.calc{{position:absolute;inset:0;padding:60px 14px 0}}.calc .line{{font:34px CMR;color:{k["on"]};text-align:right;padding:40px 10px}}
.keys{{display:grid;grid-template-columns:repeat(5,1fr);gap:7px;background:{k["c"]};border-radius:24px;padding:8px}}.k{{height:50px;border-radius:25px;background:{k["chhh"]};display:flex;align-items:center;justify-content:center;font:20px CMR}}
.pad{{display:grid;grid-template-columns:repeat(4,1fr);gap:8px;margin-top:12px}}.k.d{{height:62px;border-radius:31px;background:{k["chh"]};font:26px GSF}}
.scrim{{position:absolute;inset:0;background:#0006}}
.sheet{{position:absolute;left:0;right:0;bottom:0;max-height:88%;background:{k["low"]};border-radius:28px 28px 0 0;padding:6px 20px 18px;overflow:hidden}}
.handle{{width:36px;height:4px;border-radius:2px;background:{k["outv"]};margin:8px auto 14px}}
.head{{display:flex;align-items:center;gap:14px;margin-bottom:14px}}
.keychip{{min-width:64px;height:52px;padding:0 12px;border-radius:26px;background:{k["chhh"]};display:flex;align-items:center;justify-content:center;font-size:20px;color:{k["on"]}}}.keychip.big{{font-size:24px;min-width:84px}}
.title{{font:600 22px GSF;color:{k["on"]}}}.where{{font:13px GSF;color:{k["onv"]};margin-top:2px}}
.formula{{background:{k["pc"]};color:{k["onpc"]};border-radius:24px;padding:18px;font-size:30px;text-align:center;margin-bottom:6px;line-height:1.3}}
.formula.wide{{font-size:20px;white-space:nowrap;padding:20px 10px}}.formula.small{{font-size:19px;background:{k["ch"]};color:{k["on"]};padding:8px}}
.eq{{margin:0 .35em}}
.sec{{margin-top:14px}}.st{{font:600 14px GSF;color:{k["pri"]};letter-spacing:.2px;margin-bottom:6px}}
.sec p{{margin:0;font:15px/1.45 GSF;color:{k["on"]}}}
.steps{{list-style:none;margin:0;padding:0;display:flex;flex-direction:column;gap:6px}}.steps li{{display:flex;gap:10px;align-items:center;font:15px GSF;color:{k["on"]};background:{k["c"]};border-radius:16px;padding:8px 12px}}
.steps .n{{width:24px;height:24px;border-radius:12px;background:{k["pri"]};color:#fff;display:flex;align-items:center;justify-content:center;font:600 13px GSF;flex:none}}
.ex{{display:grid;grid-template-columns:1fr auto;align-items:center;background:{k["sc"]};color:{k["onsc"]};border-radius:20px;padding:10px 14px;margin-bottom:8px;gap:2px 10px}}
.exm{{font-size:22px;display:flex;align-items:center}}.exm .a{{color:{k["pri"]}}}.exn{{grid-column:1;font:13px GSF;color:{k["onv"]}}}
.try{{grid-row:1 / span 2;grid-column:2;background:{k["pri"]};color:#fff;border-radius:18px;padding:9px 14px;font:600 13px GSF;white-space:nowrap}}
.lim{{font-size:.6em}}
.actions{{display:flex;gap:8px;margin-top:16px;justify-content:flex-end}}.tonal{{background:{k["sc"]};color:{k["onsc"]};border-radius:20px;padding:10px 16px;font:600 14px GSF}}.text{{color:{k["pri"]};padding:10px 12px;font:600 14px GSF}}
/* tablet */
.tablet{{width:1280px;height:800px;border-radius:36px;background:{k["low"]};position:relative;overflow:hidden;box-shadow:0 6px 24px #0002}}
.tcalc{{position:absolute;inset:0;display:flex}}.tleft{{flex:1;padding:50px}}.tleft .line{{font:40px CMR;text-align:right;padding-top:120px;color:{k["on"]}}}.tright{{width:430px;padding:24px;background:{k["c"]}}}
.dialog{{position:absolute;left:50%;top:50%;transform:translate(-50%,-50%);width:1040px;background:{k["low"]};border-radius:32px;padding:26px 28px;display:flex;gap:28px;box-shadow:0 10px 40px #0003}}
.col{{flex:1;min-width:0}}.col.left{{flex:1.05}}
.plot{{background:{k["c"]};border-radius:20px;padding:8px;margin-top:12px;display:flex;align-items:center;gap:6px}}.legend{{display:flex;flex-direction:column;gap:6px;font-size:16px}}.lg{{display:flex;align-items:center;gap:6px}}.dot{{width:14px;height:4px;border-radius:2px}}
.related{{display:flex;flex-wrap:wrap;gap:6px;align-items:center;margin-top:12px}}.rl{{font:600 13px GSF;color:{k["onv"]};margin-right:4px}}.chip{{border:1px solid {k["outv"]};border-radius:10px;padding:5px 10px;font-size:14px;color:{k["on"]}}}
'''
html = f'''<!doctype html><html><head><meta charset="utf-8"><title>x</title><style>{css}</style></head><body>
<div><div class="cap">Phone · long-press n choose k</div><div class="phone"><div class="calc"><div class="line">{binom("5","2")}</div><div class="keys">{keys}</div><div class="pad">{pad}</div></div><div class="scrim"></div>{phone_card}</div></div>
<div><div class="cap">Tablet · long-press the Legendre polynomial key</div><div class="tablet"><div class="tcalc"><div class="tleft"><div class="line">{sub(i("P"),"3")}(<i>x</i>)</div></div><div class="tright"><div class="keys">{keys}</div><div class="pad">{pad}</div></div></div><div class="scrim"></div>{tablet_card}</div></div>
<script>document.fonts.ready.then(()=>document.title='ready')</script></body></html>'''
open('round69.html', 'w').write(html)

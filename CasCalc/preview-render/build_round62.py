# Builds round62.html: Settings › Calculator tabs (reorder, hide, add more), the link to it, and
# the keypad with the Number theory tab added.
from icons_svg import tab_icons, table_icons
TB, T = tab_icons(), table_icons()
k = dict(bg='#FCF9EE', on='#1C1C16', onv='#47473B', low='#F6F4E8', c='#F1EEE2', ch='#EBE9DC', chh='#E5E3D6', pri='#5B6133', onpri='#fff',
         sc='#E2E4C4', onsc='#1B1D0A', tc='#BEECDB', ontc='#002019', outv='#C8C7B5', out='#77786A')
def ic(svg, size, ink, acc):
    return f'<svg viewBox="0 0 24 24" width="{size}" height="{size}" fill="none" stroke-linecap="round" stroke-linejoin="round" style="--ink:{ink};--acc:{acc};vertical-align:middle;flex:none">{svg}</svg>'
TABS = [("Basic", "Roots", "Powers, roots, logs, factorial, |x|, mod"), ("Trigonometry", "Triangle", "sin, cos, tan, their inverses and hyperbolic forms"),
        ("Calculus", "Area", "Derivatives, integrals, limits, sums, series, ∇ and differential equations"), ("Linear algebra", "Matrix", "Matrices: inverse, transpose, det, eigenvalues, products, rref"),
        ("Statistics", "Stats", "Mean, spread, combinations, normal, binomial and Poisson"), ("Complex numbers", "ComplexC", "i, Re, Im, conjugate, argument, Γ, ζ, Bessel, ∮ and residues"),
        ("Physical constants", "Atom", "c, h, k_B and every other constant, in SI or other systems"), ("Symbols", "Letters", "Latin and Greek letters, and symbols you build")]
EXTRA = [("Number theory", "Integers", "gcd, lcm, mod, floor and ceiling, rounding, sign, max and min"), ("Special functions", "Special", "erf, the sine, cosine, exponential and logarithmic integrals, Fresnel, polylog, digamma, elliptic")]
def bar(names, sel=0):
    out = ''
    for p, n in enumerate(names):
        on = p == sel
        r = '18px' if on else ('18px 7px 7px 18px' if p == 0 else ('7px 18px 18px 7px' if p == len(names) - 1 else '7px'))
        out += f'<div class="tb" style="border-radius:{r};background:{k["pri"] if on else k["chh"]}">{ic(TB[n], 18, k["onpri"] if on else k["onv"], k["chh"] if on else k["pri"])}</div>'
    return f'<div class="bar">{out}</div>'
def row(t, on, hidden_new=False, can_off=True):
    title, icon, about = t
    handle = ic(T['Grip'], 22, k['onv'], k['pri']) if on else ''
    badge = f'<span class="new" style="background:{k["tc"]};color:{k["ontc"]}">NEW</span>' if hidden_new else ''
    end = (f'<div class="sw{"" if can_off else " dis"}" style="background:{k["pri"]}"><div></div></div>' if on else
           f'<div class="add" style="background:{k["sc"]};color:{k["onsc"]}">{ic(T["Add"], 18, k["onsc"], k["pri"])} Add</div>')
    return (f'<div class="row" style="background:{k["chh"]}">{f"<div class=h>{handle}</div>" if on else "<div style=width:12px></div>"}'
            f'<div class="ti" style="background:{k["sc"]}">{ic(TB[icon], 22, k["onsc"], k["pri"])}</div>'
            f'<div class="tx"><div class="tt">{title}{badge}</div><div class="ta" style="color:{k["onv"]}">{about}</div></div>{end}</div>')
def status(): return '<div class="status"><span>9:41</span><span>▴ ▮ 92%</span></div>'
# Phone 1: Settings › Calculator section with the link.
p1 = f'''<div class="phone" style="background:{k["bg"]}">{status()}
<div class="top">{ic(T["Back"], 22, k["on"], k["pri"])}<span class="ttl">Settings</span></div><div class="pg">
<div class="sec" style="color:{k["pri"]}">Calculator</div>
<div class="set"><div><div class="st">Live answer</div><div class="sd" style="color:{k["onv"]}">The result under what you're typing</div></div><div class="sw" style="background:{k["pri"]}"><div></div></div></div>
<div class="set"><div><div class="st">Continue from the answer</div><div class="sd" style="color:{k["onv"]}">An operator after = starts with Ans</div></div><div class="sw" style="background:{k["pri"]}"><div></div></div></div>
<div class="set"><div><div class="st">Explanations on long-press</div><div class="sd" style="color:{k["onv"]}">Formula, theory and how to use each key</div></div><div class="sw" style="background:{k["pri"]}"><div></div></div></div>
<div class="set hl" style="background:{k["c"]}"><div><div class="st">Calculator tabs</div><div class="sd" style="color:{k["onv"]}">8 on the keypad. Reorder them, put some away, or add more: number theory, special functions</div></div>{ic(T["ChevronRight"], 22, k["onv"], k["pri"])}</div>
<div class="sec" style="color:{k["pri"]};margin-top:14px">History</div>
<div class="set"><div><div class="st">History keeps</div><div class="sd" style="color:{k["onv"]}">50 · 100 · 500 · All</div></div></div>
</div></div>'''
# Phone 2: the tabs page.
rows_on = ''.join(row(t, True) for t in TABS)
rows_more = ''.join(row(t, False, hidden_new=True) for t in EXTRA)
p2 = f'''<div class="phone" style="background:{k["bg"]}">{status()}
<div class="top">{ic(T["Back"], 22, k["on"], k["pri"])}<span class="ttl">Calculator tabs</span></div><div class="pg">
<div class="intro" style="color:{k["onv"]}">The groups of keys above the keypad. Drag a tab to reorder it, switch it off to put it away, and add more from below.</div>
<div class="prev" style="background:{k["low"]}">{bar([t[1] for t in TABS])}</div>
<div class="sec" style="color:{k["pri"]}">On the keypad</div>{rows_on}
<div class="sec" style="color:{k["pri"]}">More tabs</div>{rows_more}
<div class="reset" style="color:{k["pri"]}">Reset to the default tabs</div>
</div></div>'''
p2b = p2.replace('<div class="pg">', '<div style="flex:1;overflow:hidden"><div class="pg" style="margin-top:-560px">', 1).replace('Reset to the default tabs</div>\n</div></div>', 'Reset to the default tabs</div>\n</div></div></div>')
# Phone 3: keypad with Number theory added (and Symbols put away), on that tab.
nt = [["gcd(<i>a</i>, <i>b</i>)", "lcm(<i>a</i>, <i>b</i>)", "<i>a</i> mod <i>b</i>", "<i>n</i>!", "(<sup><i>n</i></sup><sub><i>k</i></sub>)"],
      ["floor(<i>x</i>)", "ceil(<i>x</i>)", "round(<i>x</i>)", "frac(<i>x</i>)", "sgn(<i>x</i>)"],
      ["max(<i>a</i>, <i>b</i>)", "min(<i>a</i>, <i>b</i>)", "perm(<i>n</i>, <i>k</i>)", "|<i>x</i>|", ","]]
keys = ''.join(f'<div class="fk" style="background:{k["chh"]}"><span class="m">{c}</span></div>' for r in nt for c in r)
shown3 = [t[1] for t in TABS[:7]] + ["Integers"]
dots = ''.join(f'<span class="dot{" on" if i == 7 else ""}" style="background:{k["pri"] if i == 7 else k["outv"]}"></span>' for i in range(8))
pad = ''.join(f'<div class="k" style="background:{k["sc"] if t in "÷×−+" else (k["pri"] if t == "=" else k["ch"])};color:{"#fff" if t == "=" else k["on"]}">{t}</div>' for t in ['7','8','9','÷','4','5','6','×','1','2','3','−','0','.','=','+'])
p3 = f'''<div class="phone" style="background:{k["low"]}">{status()}
<div class="top">{ic(T["ModeCalculator"], 22, k["on"], k["pri"])}<span class="ttl">Calculator</span></div>
<div style="flex:1"></div>
<div class="input" style="background:{k["bg"]}"><span class="m" style="font-size:28px">gcd(84, 120)<span class="cur" style="background:{k["pri"]}"></span></span><div style="color:{k["onv"]};font-size:17px" class="m">= 12</div></div>
<div class="bar2">{bar(shown3, 7)}</div>
<div class="grid" style="background:{k["c"]}">{keys}</div><div class="dots">{dots}</div>
<div class="pad">{pad}</div></div>'''
html = f'''<!doctype html><html><head><meta charset="utf-8"><title>x</title><style>
@font-face{{font-family:GSF;src:url(../app/src/main/res/font/google_sans_flex.ttf)}}
@font-face{{font-family:CMR;src:url(../app/src/main/res/font/cm_main.otf)}}
@font-face{{font-family:CMI;src:url(../app/src/main/res/font/cm_italic.otf)}}
body{{margin:0;background:#dcd8cf;font-family:GSF;padding:40px;display:flex;gap:48px;width:1940px}}
.lab{{font:600 16px GSF;color:#2a2a22;margin:0 0 14px}}
.phone{{width:412px;height:915px;border-radius:44px;overflow:hidden;box-shadow:0 0 0 10px #1a1a1a,0 30px 60px rgba(0,0,0,.35);display:flex;flex-direction:column;color:{k["on"]}}}
.status{{display:flex;justify-content:space-between;padding:14px 30px 4px;font:500 14px GSF}}.top{{display:flex;align-items:center;gap:12px;padding:8px 16px}}.ttl{{font:400 22px GSF}}
.pg{{padding:4px 20px;display:flex;flex-direction:column;gap:8px;overflow:hidden}}.sec{{font:500 14px GSF;margin-top:6px}}.intro{{font:14px/1.4 GSF}}
.set{{display:flex;align-items:center;justify-content:space-between;gap:12px;padding:8px 6px;border-radius:12px}}.st{{font:16px GSF}}.sd{{font:12.5px/1.35 GSF;margin-top:2px}}
.sw{{width:52px;height:32px;border-radius:16px;position:relative;flex:none}}.sw div{{position:absolute;right:4px;top:4px;width:24px;height:24px;border-radius:12px;background:#fff}}
.row{{display:flex;align-items:center;gap:10px;border-radius:20px;padding:7px 12px 7px 0}}.h{{width:40px;display:flex;justify-content:center}}.ti{{width:40px;height:40px;border-radius:20px;display:flex;align-items:center;justify-content:center;flex:none}}
.tx{{flex:1;min-width:0}}.tt{{font:500 15px GSF;display:flex;align-items:center;gap:8px}}.ta{{font:12px/1.3 GSF;margin-top:1px}}.new{{font:600 10px GSF;border-radius:9px;padding:2px 7px}}
.add{{display:flex;align-items:center;gap:4px;height:38px;border-radius:19px;padding:0 14px 0 10px;font:500 14px GSF;flex:none}}.reset{{text-align:right;font:500 14px GSF;padding:8px 4px}}
.prev{{border-radius:24px;padding:8px}}.bar{{display:flex;gap:2px}}.tb{{flex:1;height:36px;display:flex;align-items:center;justify-content:center}}
.bar2{{padding:4px 10px}}.grid{{margin:4px 8px 0;border-radius:28px;padding:10px 8px;display:grid;grid-template-columns:repeat(5,1fr);gap:7px}}
.fk{{height:50px;border-radius:25px;display:flex;align-items:center;justify-content:center;font-size:14.5px;white-space:nowrap}}.m{{font-family:CMR}}.m i{{font-family:CMI;font-style:normal}}
.dots{{display:flex;justify-content:center;gap:5px;padding:8px}}.dot{{width:5px;height:5px;border-radius:3px}}.dot.on{{width:16px}}
.input{{margin:0 12px 8px;border-radius:24px;padding:12px 18px;text-align:right}}.cur{{display:inline-block;width:2px;height:28px;vertical-align:-5px;margin-left:2px}}
.pad{{display:grid;grid-template-columns:repeat(4,1fr);gap:8px;padding:4px 14px 24px}}.k{{height:56px;border-radius:28px;display:flex;align-items:center;justify-content:center;font:26px GSF}}
</style></head><body><div><p class="lab">Settings › Calculator</p>{p1}</div><div><p class="lab">Calculator tabs</p>{p2}</div><div><p class="lab">…scrolled down: More tabs</p>{p2b}</div><div><p class="lab">The keypad with Number theory added</p>{p3}</div>
<script>document.fonts.ready.then(()=>document.title='ready')</script></body></html>'''
open('round62.html', 'w').write(html)

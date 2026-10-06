# Builds round67.html: the page switches as icons (1 over a triangle; a density with its
# distribution function) beside a narrower Rad/Deg, and the gray signals button. The trigonometry tab's reciprocal page and the statistics tab's
# distributions page (each with its switch beside Rad/Deg), and the graphs' signals button left
# of the quick letters, with the signals keys in the function keys' place.
from icons_svg import key_icons, tab_icons
K, TB = key_icons(), tab_icons()
k = dict(on='#1C1C16', onv='#47473B', low='#F6F4E8', c='#F1EEE2', chh='#E5E3D6', pri='#5B6133', onpri='#fff', sc='#E2E4C4', onsc='#1B1D0A', tc='#BEECDB', ontc='#002019')
def icon(svg, w, h, ink, acc):
    return f'<svg viewBox="0 0 {w} 24" width="{h*w/24:.1f}" height="{h}" fill="none" stroke-linecap="round" stroke-linejoin="round" style="--ink:{ink};--acc:{acc};flex:none">{svg}</svg>'
def key(spoken, kh=54, kw=70):
    svg, w = K[spoken]; a = w / 24
    h = max(26, min(34, kh * .62, (kw - 12) / a))
    return f'<div class="key">{icon(svg, w, h, k["on"], k["pri"])}</div>'
def words(t): return f'<div class="key"><span class="w">{t}</span></div>'
tabs = ['Roots', 'Triangle', 'Area', 'Matrix', 'Stats', 'ComplexC', 'Atom', 'Letters']
def bar(sel):
    out = ''
    for i, t in enumerate(tabs):
        s = i == sel
        r = '18px' if s else ('18px 7px 7px 18px' if i == 0 else ('7px 18px 18px 7px' if i == len(tabs) - 1 else '7px'))
        out += f'<div class="seg" style="border-radius:{r};background:{k["pri"] if s else k["chh"]}">{icon(TB[t], 24, 20, k["onpri"] if s else k["onv"], k["sc"] if s else k["pri"])}</div>'
    return f'<div class="bar">{out}</div>'
def control(pic, on):
    return (f'<div class="ctl"><div class="seg2"><span class="sel">Rad</span><span>Deg</span></div>'
            f'<span class="pill" style="background:{k["pri"] if on else k["chh"]}">{icon(TB[pic], 24, 24, k["onpri"] if on else k["on"], k["sc"] if on else k["pri"])}</span><span style="flex:1"></span><span class="cur">‹</span><span class="cur">›</span></div>')
def panel(title, ctl, sel, rows, cols):
    grid = ''.join(rows)
    return (f'<div class="panel"><div class="cap">{title}</div>{ctl}{bar(sel)}'
            f'<div class="grid" style="grid-template-columns:repeat({cols},1fr)">{grid}</div></div>')
rec = [words(w) for f in ('csc', 'sec', 'cot') for w in (f, 'a' + f, f + 'h', 'a' + f + 'h')]
dist = [key(n) for n in open('/tmp/claude-0/-home-user-cascalc/0118ccc2-c014-5519-92e5-b73b1184cb89/scratchpad/dn.txt').read().strip().split('\n')]
sig = ['H(x)', 'rect', 'tri', 'ramp', 'pulse', 'saw', 'sq', 'triw', 'σ(x)', 'softplus', 'clamp', 'lerp', 'smooth', 'gauss', 'wrap']
signals = [words(s) for s in sig]
letters = (f'<div class="ctl"><span class="sigb">{icon(TB["Signals"], 24, 22, k["on"], k["pri"])}</span>'
           + ''.join(f'<span class="chip">{v}</span>' for v in ['y', 'r', 'θ', 't']) + '</div>')
html = f'''<!doctype html><html><head><meta charset="utf-8"><title>x</title><style>
@font-face{{font-family:GSF;src:url(../app/src/main/res/font/google_sans_flex.ttf)}}
body{{margin:0;background:#dcd8cf;font-family:GSF;padding:32px;width:1290px;display:grid;grid-template-columns:repeat(3,410px);gap:20px}}
.panel{{background:{k["low"]};border-radius:28px;padding:12px 8px 14px}}.cap{{font:600 15px GSF;color:{k["on"]};padding:2px 10px 8px}}
.ctl{{display:flex;align-items:center;gap:8px;padding:2px 8px 6px}}.seg2{{display:flex;border:1px solid #77786A;border-radius:18px;overflow:hidden;font-size:14px}}
.seg2 span{{padding:8px 0;width:52px;text-align:center}}.seg2 .sel{{background:{k["sc"]}}}.pill{{width:52px;height:36px;border-radius:18px;display:flex;align-items:center;justify-content:center}}
.cur{{width:40px;height:40px;border-radius:14px;background:{k["chh"]};display:flex;align-items:center;justify-content:center;font-size:22px}}
.bar{{display:flex;gap:2px;padding:4px 2px}}.seg{{flex:1;height:38px;display:flex;align-items:center;justify-content:center}}
.grid{{background:{k["c"]};border-radius:24px;padding:8px;display:grid;gap:7px;margin-top:6px}}
.key{{height:54px;border-radius:27px;display:flex;align-items:center;justify-content:center;overflow:hidden;background:{k["chh"]}}}
.w{{font:19px "Latin Modern Roman",serif;color:{k["on"]}}}
.sigb{{height:36px;padding:0 12px;border-radius:18px;background:{k["chh"]};display:flex;align-items:center}}
.chip{{height:36px;min-width:52px;border-radius:18px;background:{k["tc"]};color:{k["ontc"]};display:flex;align-items:center;justify-content:center;font:italic 20px serif}}
</style></head><body>
{panel("Trigonometry: reciprocal page", control("ReciprocalTrig", True), 1, rec, 4)}
{panel("Statistics: switch off (first page shown as distributions for comparison)", control("DistributionPage", False), 4, dist, 5)}
{panel("2D graph: signals beside the letters", letters, -1, signals, 5)}
<script>document.fonts.ready.then(()=>document.title='ready')</script></body></html>'''
open('round67.html', 'w').write(html)

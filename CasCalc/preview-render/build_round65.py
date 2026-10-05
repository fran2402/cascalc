# Builds round65.html: the distributions keys with their family letters and the four newest tabs, as on the keypad.
from icons_svg import key_icons, tab_icons, table_icons
K, TB, T = key_icons(), tab_icons(), table_icons()
names = open('/tmp/claude-0/-home-user-cascalc/0118ccc2-c014-5519-92e5-b73b1184cb89/scratchpad/names3.txt').read().strip().split('\n')
tabs = [("Distributions", "Distributions"), ("Vectors", "Vectors"), ("Modular &amp; bits", "Modular"), ("Finance", "Finance"), ("Polynomials", "Polynomials")]
k = dict(on='#1C1C16', onv='#47473B', low='#F6F4E8', c='#F1EEE2', chh='#E5E3D6', pri='#5B6133', onpri='#fff', sc='#E2E4C4', onsc='#1B1D0A', tc='#BEECDB', ontc='#002019')
def icon(svg, w, h, ink, acc):
    return f'<svg viewBox="0 0 {w} 24" width="{h*w/24:.1f}" height="{h}" fill="none" stroke-linecap="round" stroke-linejoin="round" style="--ink:{ink};--acc:{acc};flex:none">{svg}</svg>'
def key(spoken, kw=70, kh=54):
    svg, w = K[spoken]; a = w / 24
    h = max(26, min(34, kh * .62, (kw - 12) / a))
    return f'<div class="key" style="background:{k["chh"]}">{icon(svg, w, h, k["on"], k["pri"])}</div>'
panels = ''
for t, (title, ic) in enumerate(tabs):
    ks = ''.join(key(n) for n in names[t*15:(t+1)*15])
    panels += (f'<div class="panel"><div class="ph"><div class="ti" style="background:{k["pri"]}">{icon(TB[ic], 24, 22, k["onpri"], k["sc"])}</div>'
               f'<span>{title}</span></div><div class="grid" style="background:{k["c"]}">{ks}</div></div>')
html = f'''<!doctype html><html><head><meta charset="utf-8"><title>x</title><style>
@font-face{{font-family:GSF;src:url(../app/src/main/res/font/google_sans_flex.ttf)}}
body{{margin:0;background:#dcd8cf;font-family:GSF;padding:32px;width:840px;display:grid;grid-template-columns:repeat(2,400px);gap:20px}}
.panel{{background:{k["low"]};border-radius:28px;padding:14px 12px 14px}}.ph{{display:flex;align-items:center;gap:10px;font:600 16px GSF;color:{k["on"]};padding:0 6px 10px}}
.ti{{width:36px;height:36px;border-radius:18px;display:flex;align-items:center;justify-content:center}}
.grid{{border-radius:24px;padding:8px;display:grid;grid-template-columns:repeat(5,1fr);gap:7px}}
.key{{height:54px;border-radius:27px;display:flex;align-items:center;justify-content:center;overflow:hidden}}
</style></head><body>{panels}<script>document.fonts.ready.then(()=>document.title='ready')</script></body></html>'''
open('round65.html', 'w').write(html)

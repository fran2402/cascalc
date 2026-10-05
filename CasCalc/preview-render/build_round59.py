# Builds round59.html: the calculator's history as it now looks (question as large as the answer),
# in light and dark, plus the CM-style italic x key.
from icons_svg import key_icons, table_icons
K, T = key_icons(), table_icons()
L = dict(bg='#FCF9EE', on='#1C1C16', onv='#47473B', low='#F6F4E8', c='#F1EEE2', ch='#EBE9DC', chh='#E5E3D6', pri='#5B6133', onpri='#fff',
         pc='#DEE5A0', onpc='#1A1E00', sc='#E2E4C4', onsc='#1B1D0A', outv='#C8C7B5', ter='#3C6A5A', eqbg='#5B6133', eqfg='#fff')
D = dict(bg='#12130C', on='#E5E3D6', onv='#C8C7B5', low='#1B1C14', c='#1F2018', ch='#292A22', chh='#34352C', pri='#C5CB86', onpri='#2F3300',
         pc='#434A12', onpc='#E1E7A0', sc='#45483A', onsc='#E2E4C4', outv='#47473B', ter='#A2D0BF', eqbg='#C5CB86', eqfg='#2F3300')
def ic(name, size, ink, acc, src=T, w=24):
    svg = src[name] if src is T else src[name][0]
    return f'<svg viewBox="0 0 {w} 24" width="{size*w/24}" height="{size}" fill="none" stroke-linecap="round" stroke-linejoin="round" style="--ink:{ink};--acc:{acc};vertical-align:middle">{svg}</svg>'
I = lambda s: f'<i>{s}</i>'
cards = [
    dict(q=f'∫<sub>0</sub><sup>π</sup> sin {I("x")} d{I("x")}', a='2', pin=True),
    dict(q=f'{I("x")}<sup>2</sup> − 5{I("x")} + 6 = 0', a=f'{I("x")} = 2, &nbsp;{I("x")} = 3', eq='⇔', folder='Algebra'),
    dict(q='√8 + √18', a='5√2', approx=True),
    dict(q='det<span class="br">[</span><span class="mat"><span>1</span><span>2</span><span>3</span><span>4</span></span><span class="br">]</span>', a='−2'),
    dict(q=f'<span class="fr"><span>d</span><span>d{I("x")}</span></span> {I("x")}<sup>2</sup> sin {I("x")}', a=f'2{I("x")} sin {I("x")} + {I("x")}<sup>2</sup> cos {I("x")}', focused=True, graph=True),
]
def card(c, k):
    f = c.get('focused')
    qs, asz = (25, 26) if f else (23, 23)
    marks = ''
    if c.get('folder'): marks += f'<span class="fold" style="background:{k["sc"]};color:{k["onsc"]}">{c["folder"]}</span>'
    if c.get('pin'): marks += ic('Pin', 14, k['pri'], k['pri'])
    chip = f'<span class="chip" style="background:{k["pc"]};color:{k["onpc"]}">{ic("ChevronRight", 18, k["onpc"], k["onpc"])} <span class="m">≈</span></span>' if c.get('approx') else ''
    eq = c.get('eq', '=')
    acts = ''
    if f:
        b1 = ('Mode2D', 'Graph') if c.get('graph') else ('Enter', 'Use')
        acts = (f'<div class="acts"><span class="act" style="background:{k["sc"]};color:{k["onsc"]}">{ic(b1[0], 18, k["onsc"], k["pri"])} {b1[1]}</span>'
                f'<span class="act o" style="border-color:{k["outv"]};color:{k["onv"]}">{ic("Bullets", 18, k["onv"], k["pri"])} Steps</span><span style="flex:1"></span>'
                f'{ic("Copy", 18, k["onv"], k["pri"])}<span style="width:14px"></span>{ic("PinOutline", 18, k["onv"], k["pri"])}<span style="width:14px"></span>{ic("More", 20, k["onv"], k["pri"])}</div>')
    return (f'<div class="card{" f" if f else ""}" style="background:{k["ch"] if f else k["c"]}">'
            f'<div class="qrow"><span class="m q" style="font-size:{qs}px;color:{k["on"]}">{c["q"]}</span>{marks}</div>'
            f'<div class="arow">{chip}<span style="flex:1"></span><span class="m" style="font-size:{asz}px;color:{k["pri"]}">{eq}</span>&nbsp;&nbsp;<span class="m" style="font-size:{asz}px;color:{k["on"]}">{c["a"]}</span></div>{acts}</div>')
def keypad(k):
    rows = [['(', ')', '%', '÷'], ['7', '8', '9', '×'], ['4', '5', '6', '−'], ['1', '2', '3', '+'], ['0', '.', 'x', '=']]
    out = ''
    for r in rows:
        for t in r:
            if t in '÷×−+': bg, fg = k['sc'], k['onsc']
            elif t == '=': bg, fg = k['eqbg'], k['eqfg']
            elif t in '()%': bg, fg = k['chh'], k['on']
            else: bg, fg = k['ch'], k['on']
            lab = ic('x', 30, fg, k['pri'], K) if t == 'x' else t
            out += f'<div class="k" style="background:{bg};color:{fg}">{lab}</div>'
    return f'<div class="pad">{out}</div>'
def phone(k):
    cs = ''.join(card(c, k) for c in cards)
    return f'''<div class="phone" style="background:{k["low"]};color:{k["on"]}">
<div class="status"><span>9:41</span><span>▴ ▮ 92%</span></div>
<div class="top">{ic("ModeCalculator", 22, k["on"], k["pri"])}<span class="ttl">Calculator</span><span style="flex:1"></span>{ic("History", 22, k["onv"], k["pri"])}<span style="width:18px"></span>{ic("MoreVert" if "MoreVert" in T else "More", 22, k["onv"], k["pri"])}</div>
<div class="hist">{cs}</div>
<div class="input" style="background:{k["bg"]}"><span class="m" style="font-size:30px">{I("x")}<sup>2</sup> − 1<span class="cur" style="background:{k["pri"]}"></span></span><div class="prev" style="color:{k["onv"]}"><span class="m">=</span> <span class="m">{I("x")}<sup>2</sup> − 1</span></div></div>
<div class="bar"><div class="segs" style="border-color:{k["outv"]}"><span class="seg" style="background:{k["sc"]};color:{k["onsc"]}">✓ Rad</span><span class="seg" style="border-color:{k["outv"]}">Deg</span></div></div>
{keypad(k)}</div>'''
more = [
    dict(q=f'lim<sub>{I("x")}→0</sub> <span class="fr"><span>sin {I("x")}</span><span>{I("x")}</span></span>', a='1'),
    dict(q=f'∑<sub>{I("n")}=1</sub><sup>∞</sup> <span class="fr"><span>1</span><span>{I("n")}<sup>2</sup></span></span>', a='<span class="fr"><span>π<sup>2</sup></span><span>6</span></span>', pin=True),
    dict(q=f'{I("e")}<sup>{I("i")}π</sup> + 1', a='0', folder='Fun'),
]
def section(title, items, k, pinned=False):
    head = f'<div class="sec"><span style="color:{k["pri"]}">{ic("Pin", 18, k["pri"], k["pri"]) + " " if pinned else ""}{title}</span><span class="cnt" style="background:{k["sc"]};color:{k["onsc"]}">{len(items)}</span></div>'
    return head + ''.join(card(c, k) for c in items)
def history_phone(k):
    chips = ''.join(f'<span class="hc" style="{"background:"+k["sc"]+";color:"+k["onsc"] if on else "border:1px solid "+k["outv"]+";color:"+k["onv"]}">{ic(icn, 16, k["onsc"] if on else k["onv"], k["pri"]) + " " if icn else ""}{t}</span>' for t, on, icn in (("All", True, None), ("Pinned", False, "Pin"), ("Algebra", False, "Folder"), ("Fun", False, "Folder")))
    pinned = [cards[0], more[1]]
    today = [dict(cards[4], focused=False), cards[3], cards[2], cards[1]]
    earlier = [more[0], more[2]]
    body = section("Pinned", pinned, k, True) + section("Today", today, k) + section("Earlier", earlier, k)
    return f'''<div class="phone" style="background:{k["low"]};color:{k["on"]}">
<div class="status"><span>9:41</span><span>▴ ▮ 92%</span></div>
<div class="top">{ic("Back", 22, k["on"], k["pri"])}<span class="ttl">History</span><span style="flex:1"></span>{ic("History", 22, k["pri"], k["pri"])}<span style="width:18px"></span>{ic("More", 22, k["onv"], k["pri"])}</div>
<div class="tb"><div class="search" style="background:{k["chh"]};color:{k["onv"]}">{ic("Search", 20, k["onv"], k["pri"])} Search calculations</div><div class="exp" style="background:{k["sc"]}">{ic("Share", 20, k["onsc"], k["pri"])}</div></div>
<div class="chips">{chips}</div>
<div class="hist2">{body}</div></div>'''
html = f'''<!doctype html><html><head><meta charset="utf-8"><title>x</title><style>
@font-face{{font-family:GSF;src:url(../app/src/main/res/font/google_sans_flex.ttf)}}
@font-face{{font-family:CMR;src:url(../app/src/main/res/font/cm_main.otf)}}
@font-face{{font-family:CMI;src:url(../app/src/main/res/font/cm_italic.otf)}}
body{{margin:0;background:#dcd8cf;font-family:GSF;padding:40px;display:flex;gap:56px;width:1500px}}
.phone{{width:412px;height:915px;border-radius:44px;overflow:hidden;box-shadow:0 0 0 10px #1a1a1a,0 30px 60px rgba(0,0,0,.35);display:flex;flex-direction:column}}
.status{{display:flex;justify-content:space-between;padding:14px 30px 4px;font:500 14px GSF}}
.top{{display:flex;align-items:center;gap:10px;padding:6px 20px 6px 20px}}.ttl{{font:400 20px GSF}}
.hist{{flex:1;display:flex;flex-direction:column;justify-content:flex-end;gap:8px;padding:8px 0;overflow:hidden}}
.card{{margin:0 12px;border-radius:22px;padding:14px 16px 14px 20px}}.card.f{{border-radius:28px;padding-bottom:12px}}
.qrow{{display:flex;align-items:center;gap:8px}}.q{{flex:1;white-space:nowrap}}.arow{{display:flex;align-items:center;margin-top:8px;white-space:nowrap}}
.m{{font-family:CMR}}.m i{{font-family:CMI;font-style:normal}}.m sup{{font-size:62%;vertical-align:.55em;line-height:0}}.m sub{{font-size:62%;line-height:0}}
.fr{{display:inline-flex;flex-direction:column;vertical-align:middle;text-align:center;font-size:80%;line-height:1.05}}.fr span:first-child{{border-bottom:1px solid currentColor;padding:0 2px}}
.br{{font-size:150%;vertical-align:-.1em}}.mat{{display:inline-grid;grid-template-columns:auto auto;gap:0 8px;font-size:72%;vertical-align:middle;line-height:1.1;padding:0 3px}}
.fold{{font:500 11px GSF;border-radius:10px;padding:2px 8px}}
.chip{{display:inline-flex;align-items:center;gap:4px;border-radius:16px;padding:4px 14px 4px 8px;font-size:16px}}
.acts{{display:flex;align-items:center;gap:8px;margin-top:10px}}.act{{display:inline-flex;align-items:center;gap:6px;height:40px;border-radius:14px;padding:0 16px 0 12px;font:500 14px GSF}}.act.o{{border:1px solid}}
.input{{margin:0 12px;border-radius:24px;padding:12px 18px 10px;text-align:right}}.cur{{display:inline-block;width:2px;height:30px;vertical-align:-6px;margin-left:2px}}.prev{{font-size:17px;margin-top:2px}}
.bar{{display:flex;padding:8px 16px 4px}}.segs{{display:flex;border:1px solid;border-radius:18px;height:34px;overflow:hidden}}.seg{{width:70px;display:flex;align-items:center;justify-content:center;font:14px GSF}}.seg+.seg{{border-left:1px solid}}
.pad{{display:grid;grid-template-columns:repeat(4,1fr);gap:8px;padding:8px 14px 26px}}.k{{height:60px;border-radius:30px;display:flex;align-items:center;justify-content:center;font:28px GSF}}

.tb{{display:flex;gap:8px;padding:4px 12px}}.search{{flex:1;height:48px;border-radius:24px;display:flex;align-items:center;gap:10px;padding:0 16px;font:16px GSF}}.exp{{width:48px;height:48px;border-radius:24px;display:flex;align-items:center;justify-content:center}}
.chips{{display:flex;gap:8px;padding:8px 12px}}.hc{{display:inline-flex;align-items:center;gap:4px;height:32px;border-radius:10px;padding:0 12px;font:500 14px GSF}}
.hist2{{flex:1;overflow:hidden;display:flex;flex-direction:column;gap:8px;padding-bottom:12px}}.sec{{display:flex;align-items:center;justify-content:space-between;padding:8px 24px 0;font:500 16px GSF}}.cnt{{font:500 12px GSF;border-radius:10px;padding:2px 8px}}
.lab{{font:600 16px GSF;color:#2a2a22;margin:0 0 14px}}
</style></head><body><div><p class="lab">Calculator: the newest card shows its actions</p>{phone(L)}</div><div><p class="lab">History button: full-screen history, light</p>{history_phone(L)}</div><div><p class="lab">and dark</p>{history_phone(D)}</div><script>document.fonts.ready.then(()=>document.title='ready')</script></body></html>'''
open('round59.html', 'w').write(html)

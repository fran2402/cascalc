# The construct UI on tablets: a tool rail by the list, the status card on top.
import os, sys, math
here = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, here)
import importlib.util
spec = importlib.util.spec_from_file_location('r35', os.path.join(here, 'round35.py'))
src = open(os.path.join(here, 'round35.py')).read()
ns = {}
exec(src.split("def grid():")[0].replace("open(os.path.join(here, 'round28.html'))", "open(os.path.join(here, 'round28.html'))"), {'os': os, 'math': math, '__file__': os.path.join(here, 'round35.py')}, ns)
icon, cats = ns['icon'], ns['cats']
head = open(os.path.join(here, 'round28.html')).read(); style = head[head.index('<style>') + 7:head.index('</style>')]
undo = '<svg width="22" height="22" viewBox="0 0 24 24" fill="currentColor"><path d="M12.5 8c-2.65 0-5.05.99-6.9 2.6L2 7v9h9l-3.62-3.62c1.39-1.16 3.16-1.88 5.12-1.88 3.54 0 6.55 2.31 7.6 5.5l2.37-.78C21.08 11.03 17.15 8 12.5 8z"/></svg>'

def rail(sel):
    out = '<div class="rail">'
    for c, tools in cats.items():
        out += f'<div class="rh">{c}</div><div class="rg">'
        for t in tools:
            on = t == sel
            out += f'<div class="rnd{" on" if on else ""}">{icon(t, "var(--onpri)" if on else "var(--on)", "var(--onpri)" if on else "var(--pri)", 24)}</div>'
        out += '</div>'
    return out + '</div>'
def status(tool, step, of, role, instr, picks, close=False):
    dots = '' if of <= 1 else '<div class="dots">' + ''.join(f'<i class="{"d" if k < step - 1 else "n" if k == step - 1 else ""}"></i>' for k in range(of)) + '</div>'
    chips = '' if not picks else '<div class="chips">' + ''.join(f'<span class="chip"><small>{r} </small><em>{n}</em></span>' for r, n in picks) + '</div>'
    stepline = role if of == 0 else f'Step {step} of {of} · {role}'
    btns = (f'<span class="ib">{undo}</span>' if picks else '') + ('<span class="tonal">Close</span>' if close else '') + '<span class="txt">Done</span>'
    return f'<div class="scard"><div class="srow"><div class="sic">{icon(tool, "var(--onpc)", "var(--pri)", 24)}</div><div class="st"><b>{tool}</b><small>{stepline}</small></div>{btns}</div>{dots}<div class="instr">{instr}</div>{chips}</div>'
def keypad():
    keys = '789÷456×123−0.↵+'
    return '<div class="kp"><div class="kprow">' + ''.join(f'<div class="kk">{k}</div>' for k in 'sin cos tan √'.split()) + '</div><div class="kgrid">' + ''.join(f'<div class="kk{" acc" if k == "↵" else ""}">{k}</div>' for k in keys) + '</div></div>'
w = lambda x: f'<span class="w">{x}</span>'
def lines(rows):
    return '<div class="lines">' + ''.join(f'<div class="li"><span class="dot" style="background:{c}"></span><div><div class="m">{t}</div>' + (f'<div class="val">{v}</div>' if v else '') + '</div></div>' for c, t, v in rows) + '<div class="lfab"><span>＋</span></div></div>'
def graph(W, H, svg, overlays):
    g = ''.join(f'<line x1="{x}" y1="0" x2="{x}" y2="{H}" stroke="var(--outv)" opacity=".45"/>' for x in range(0, W, 40)) + ''.join(f'<line x1="0" y1="{y}" x2="{W}" y2="{y}" stroke="var(--outv)" opacity=".45"/>' for y in range(0, H, 40))
    g += f'<line x1="0" y1="{H//2}" x2="{W}" y2="{H//2}" stroke="var(--on)" stroke-width="1.3"/><line x1="{W//2}" y1="0" x2="{W//2}" y2="{H}" stroke="var(--on)" stroke-width="1.3"/>'
    return f'<div class="gbox" style="width:{W}px;height:{H}px"><svg width="{W}" height="{H}">{g}{svg}</svg>{overlays}<div class="gtb"><span>⌂</span><span>⤢</span><span>⋯</span></div></div>'
def pt(x, y, n, c='var(--pri)', glow=False, ring=False):
    s = f'<circle cx="{x}" cy="{y}" r="15" fill="var(--pri)" fill-opacity=".18"/>' if glow else ''
    s += f'<circle cx="{x}" cy="{y}" r="6" fill="{c}"/><text x="{x+9}" y="{y-9}" font-family="CMI" font-size="17" fill="var(--on)">{n}</text>'
    if ring: s += f'<circle cx="{x}" cy="{y}" r="12" fill="none" stroke="var(--pri)" stroke-width="2.6"/>'
    return s
def switcher():
    return '<div class="tsw"><span>▦</span><span class="on">⟋ 2D graphing</span><span>⬡</span><span>◐</span></div>'
def tablet(theme, side, body):
    return f'<div class="{theme}"><div class="tablet"><div class="status"><span>12:30</span><span>▴ ▮</span></div><div class="ttop">{switcher()}<span class="tdots">⋮</span></div><div class="tbody {side}">{body}</div></div></div>'

# 1: landscape, keypad left: keypad | lines | graph (rail on graph's left by the list)
GW, GH = 1280 - 300 - 300, 800 - 32 - 56
cx, cy = GW // 2, GH // 2
O = (cx + 40, cy + 20); R = 130
svg1 = f'<circle cx="{O[0]}" cy="{O[1]}" r="{R}" fill="none" stroke="var(--pri)" stroke-opacity=".16" stroke-width="18"/><circle cx="{O[0]}" cy="{O[1]}" r="{R}" fill="none" stroke="var(--ter)" stroke-width="2.6"/>'
svg1 += f'<ellipse cx="{cx + 210}" cy="{cy + 210}" rx="80" ry="38" fill="none" stroke="var(--pri)" stroke-opacity=".16" stroke-width="18"/><ellipse cx="{cx + 210}" cy="{cy + 210}" rx="80" ry="38" fill="none" stroke="var(--sec)" stroke-width="2.6"/>'
svg1 += pt(O[0], O[1], 'O') + pt(cx - 120, cy - 170, 'A', ring=True)
ov1 = status('Tangent', 2, 2, 'Circle or conic', 'Tap a circle or conic', [('From', 'A')]) + rail('Tangent')
body1 = keypad() + lines([('var(--ter)', f'c = {w("Circle")}(O, 3)', 'r = 3'), ('var(--sec)', f'k = {w("Ellipse")}(F, G, 2)', 'Ellipse'), ('var(--pri)', 'A = (−3, 4)', None)]) + graph(GW, GH, svg1, ov1)
# 2: dark, keypad right: graph | lines | keypad (rail on graph's right by the list)
pts = [(cx - 160, cy + 120), (cx - 110, cy - 110), (cx + 60, cy - 150), (cx + 150, cy + 60)]
svg2 = '<polyline points="' + ' '.join(f'{x},{y}' for x, y in pts) + '" fill="var(--sec)" fill-opacity=".14" stroke="var(--sec)" stroke-width="2.6" stroke-dasharray="7 6"/>' + ''.join(pt(x, y, n, glow=True, ring=True) for (x, y), n in zip(pts, 'ABCD'))
ov2 = status('Polygon', 0, 0, '4 corners', 'Tap the corners, then the first again to close', [('Corners', 'A'), ('', 'B'), ('', 'C'), ('', 'D')], close=True) + rail('Polygon').replace('class="rail"', 'class="rail right"')
body2 = graph(GW, GH, svg2, ov2) + lines([('var(--pri)', 'A = (−4, −3)', None), ('var(--pri)', 'B = (−3, 3)', None), ('var(--pri)', 'C = (1.5, 4)', None), ('var(--pri)', 'D = (4, −1)', None)]) + keypad()
# 3: portrait tablet (narrower than 840 dp): the phone layout, palette at the bottom
PW, PH = 800, 1280
pgW, pgH = PW, 760
svg3 = f'<path d="M{pgW/2-260},{pgH/2-260} Q{pgW/2},{pgH/2+520} {pgW/2+260},{pgH/2-260}" fill="none" stroke="var(--pri)" stroke-width="2.6"/>' + pt(pgW / 2 - 150, pgH / 2 - 40, 'A') + pt(pgW / 2 + 170, pgH / 2 - 110, 'B')
tabs = ''.join(f'<div class="tab{" on" if c == "Circles & shapes" else ""}">{c}</div>' for c in cats)
tiles = ''.join(f'<div class="tile">{icon(t)}<span>{t}</span></div>' for t in cats['Circles & shapes'])
pal = f'<div class="pal"><div class="tabs">{tabs}</div><div class="tiles">{tiles}</div></div>'
portrait = f'''<div class="light"><div class="ptab"><div class="status"><span>12:30</span><span>▴ ▮</span></div><div class="ttop">{switcher()}<span class="tdots">⋮</span></div>
<div class="gbox" style="width:{pgW}px;height:{pgH}px"><svg width="{pgW}" height="{pgH}">{"".join(f'<line x1="{x}" y1="0" x2="{x}" y2="{pgH}" stroke="var(--outv)" opacity=".45"/>' for x in range(0, pgW, 40))}{"".join(f'<line x1="0" y1="{y}" x2="{pgW}" y2="{y}" stroke="var(--outv)" opacity=".45"/>' for y in range(0, pgH, 40))}<line x1="0" y1="{pgH//2}" x2="{pgW}" y2="{pgH//2}" stroke="var(--on)" stroke-width="1.3"/><line x1="{pgW//2}" y1="0" x2="{pgW//2}" y2="{pgH}" stroke="var(--on)" stroke-width="1.3"/>{svg3}</svg>
{status('Move', 0, 0, 'Pick a tool below', 'Drag points to move them', [])}{pal}<div class="gtb"><span>⌂</span><span>⤢</span><span>⋯</span></div></div>
<div class="plist">{lines([('var(--pri)', 'y = x²/4 − 2', None), ('var(--pri)', 'A = (−3, 0.25)', None), ('var(--pri)', 'B = (3.4, 1.4)', None)])}</div></div></div>'''
extra = '''
.tablet{position:relative;width:1280px;height:800px;border-radius:36px;overflow:hidden;background:var(--low);color:var(--on);box-shadow:0 0 0 12px #1a1a1a,0 30px 60px rgba(0,0,0,.35)}
.ptab{position:relative;width:800px;height:1280px;border-radius:36px;overflow:hidden;background:var(--low);color:var(--on);box-shadow:0 0 0 12px #1a1a1a,0 30px 60px rgba(0,0,0,.35)}
.ttop{height:56px;display:flex;align-items:center;justify-content:center;position:relative}
.tdots{position:absolute;right:20px;font:700 22px GSF;color:var(--onv)}
.tsw{display:flex;gap:4px;background:var(--lowest);border-radius:26px;padding:5px;box-shadow:0 3px 10px rgba(0,0,0,.18)}
.tsw span{width:40px;height:38px;border-radius:19px;display:flex;align-items:center;justify-content:center;color:var(--onv);font:18px GSF}
.tsw span.on{width:auto;padding:0 16px;background:var(--pri);color:var(--onpri);font:500 14px GSF}
.tbody{display:flex;height:712px}
.kp{width:300px;background:var(--low);padding:14px 12px;display:flex;flex-direction:column;gap:10px;justify-content:flex-end}
.kprow{display:grid;grid-template-columns:repeat(4,1fr);gap:8px}.kgrid{display:grid;grid-template-columns:repeat(4,1fr);gap:8px}
.kk{height:52px;border-radius:26px;background:var(--chh);display:flex;align-items:center;justify-content:center;font:20px GSF}.kk.acc{background:#ffdcbd;color:#2c1600}
.dark .kk.acc{background:#ffdcbd}
.lines{width:300px;background:var(--bg);padding:10px;position:relative;border-left:1px solid var(--outv);border-right:1px solid var(--outv)}
.li{display:flex;gap:12px;padding:10px 6px;border-bottom:1px solid var(--outv);align-items:flex-start}.dot{width:16px;height:16px;border-radius:8px;flex:none;margin-top:3px}
.m{font:18px CMR}.val{font:14px CMR;color:var(--onv);margin-top:2px}
.lfab{position:absolute;left:14px;bottom:14px;width:48px;height:48px;border-radius:16px;background:var(--pc);color:var(--onpc);display:flex;align-items:center;justify-content:center;font:24px GSF}
.gbox{position:relative;overflow:hidden;flex:none}
.gtb{position:absolute;right:14px;bottom:14px;display:flex;background:var(--chh);border-radius:26px;padding:4px 6px}.gtb span{width:42px;height:44px;display:flex;align-items:center;justify-content:center;color:var(--onv);font-size:19px}
.rail{position:absolute;left:10px;top:10px;bottom:84px;width:118px;background:var(--c);border-radius:26px;padding:10px 8px;box-shadow:0 6px 18px rgba(0,0,0,.22);overflow:hidden;box-sizing:border-box}
.rail.right{left:auto;right:10px}
.rh{font:500 10.5px GSF;color:var(--onv);margin:6px 0 4px 4px;white-space:nowrap}
.rg{display:grid;grid-template-columns:44px 44px;gap:6px;justify-content:center}
.rnd{width:44px;height:44px;border-radius:22px;background:var(--chh);display:flex;align-items:center;justify-content:center}.rnd.on{background:var(--pri)}
.scard{position:absolute;top:10px;left:140px;right:140px;max-width:460px;margin:0 auto;background:var(--chh);border-radius:24px;padding:10px 8px 10px 14px;box-shadow:0 6px 18px rgba(0,0,0,.22);z-index:5}
.ptab .scard{left:12px;right:12px;max-width:none}
.srow{display:flex;align-items:center;gap:10px}
.sic{width:36px;height:36px;border-radius:12px;background:var(--pc);display:flex;align-items:center;justify-content:center;flex:none}
.st{flex:1;display:flex;flex-direction:column}.st b{font:500 16px GSF}.st small{font:500 12px GSF;color:var(--pri)}
.ib{width:40px;height:40px;display:flex;align-items:center;justify-content:center;color:var(--onv)}
.tonal{background:var(--sc);color:var(--onsc);border-radius:20px;padding:9px 14px;font:500 14px GSF}.txt{color:var(--pri);font:500 14px GSF;padding:9px 10px}
.dots{display:flex;gap:6px;margin:6px 0 0 46px}.dots i{width:8px;height:8px;border-radius:4px;background:var(--outv)}.dots i.d{background:var(--pri)}.dots i.n{width:10px;height:10px;border-radius:5px;background:var(--pri);opacity:.4}
.instr{font:12.5px GSF;color:var(--onv);margin:4px 0 0 46px}
.chips{display:flex;gap:6px;margin:8px 0 0 46px}.chip{background:var(--sc);color:var(--onsc);border-radius:14px;padding:3px 10px;display:flex;align-items:center}.chip small{font:11px GSF;opacity:.7}.chip em{font:15px CMI}
.pal{position:absolute;left:10px;right:10px;bottom:76px;max-width:520px;margin:0 auto;background:var(--c);border-radius:28px;padding:10px;box-shadow:0 8px 24px rgba(0,0,0,.25)}
.tabs{display:flex;gap:6px;white-space:nowrap;overflow:hidden}.tab{font:500 13.5px GSF;color:var(--onv);padding:8px 13px;border-radius:18px;flex:none}.tab.on{background:var(--sc);color:var(--onsc)}
.tiles{display:grid;grid-template-columns:repeat(4,1fr);gap:6px;margin-top:8px}
.tile{background:var(--low);border-radius:18px;padding:10px 4px;display:flex;flex-direction:column;align-items:center;gap:4px;font:11.5px/1.15 GSF;text-align:center}
.plist{height:432px;background:var(--bg)}.plist .lines{width:auto;border:none;height:100%;box-sizing:border-box}
'''
html = f'''<!doctype html><html><head><meta charset="utf-8"><title>x</title><style>{style}{extra}</style></head><body style="width:2700px">
<h2>Round 36: Construct on tablets</h2>
<p class="note">On a landscape tablet the graph shares the screen with the keyboard and the list, so the tools stand in a rail at the graph's edge beside the list, always open: every group, every tool as an icon, nothing to fold. The status card sits at the top of the graph (the tool's name shows there), and the legend moves over to make room for the rail. A portrait tablet, narrower than the wide layout, gets the phone's sheet at the bottom.</p>
<div class="row" style="flex-wrap:nowrap;align-items:flex-start">
<div>{tablet("light", "", body1)}<div class="cap" style="width:1280px">Keyboard on the left: keyboard, lines, then the graph with the rail on its left, by the list. Tangent, step 2: the circle and the ellipse glow as what can be tapped.</div></div>
</div>
<div class="row" style="flex-wrap:nowrap;align-items:flex-start">
<div>{tablet("dark", "", body2)}<div class="cap" style="width:1280px">Keyboard on the right (dark theme): graph, lines, keyboard, so the rail moves to the graph's right edge, by the list. Polygon with four corners and Close.</div></div>
<div>{portrait}<div class="cap" style="width:800px">Portrait tablet: the phone layout (the graph above the list), with the tool sheet at the bottom of the graph.</div></div>
</div>
<script>document.fonts.ready.then(()=>document.title='ready')</script></body></html>'''
open(os.path.join(here, 'round36.html'), 'w').write(html)

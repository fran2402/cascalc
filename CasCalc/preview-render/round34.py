# How to turn geometry on and start: a five-step walkthrough.
import os
here = os.path.dirname(os.path.abspath(__file__))
head = open(os.path.join(here, 'round28.html')).read(); style = head[head.index('<style>') + 7:head.index('</style>')]
hexi = '<svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor"><path d="M17.2 3H6.8l-5.2 9 5.2 9h10.4l5.2-9z"/></svg>'
icons = {
 'calc': '<path d="M7 2h10a2 2 0 0 1 2 2v16a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2zm0 2v4h10V4zm1 7v2h2v-2zm4 0v2h2v-2zm-4 4v2h2v-2zm4 0v2h2v-2zm4-4v6h2v-6z"/>',
 'g2': '<path d="M3.5 18.5l6-6 4 4L22 6.9l-1.4-1.4-7.1 8-4-4L2 17z"/>',
 'g3': '<path d="M12 2l9 5v10l-9 5-9-5V7zm0 2.3L5 8.2v7.6l7 3.9 7-3.9V8.2z"/>',
 'cx': '<path d="M12 3a9 9 0 1 0 0 18 9 9 0 0 0 0-18zm0 2a7 7 0 0 1 0 14z"/>',
}
def ic(k, c='currentColor'): return f'<svg width="22" height="22" viewBox="0 0 24 24" fill="{c}">{icons[k]}</svg>'
def switcher(sel):
    out = '<div class="sw4">'
    for k, label in [('calc', 'Calculator'), ('g2', '2D graphing'), ('g3', ''), ('cx', '')]:
        if k == sel: out += f'<div class="seg on">{ic(k)}<span>{label}</span></div>'
        elif k in ('g3', 'cx') or k != sel: out += f'<div class="seg">{ic(k)}</div>'
    return out + '</div>'
def phone(body, theme='light'):
    return f'<div class="{theme}"><div class="phone"><div class="status"><span>9:41</span><span>▴ ▮</span></div>{body}</div></div>'
def step(n, title, body, cap):
    return f'<div class="stepcol"><div class="sh"><span class="num">{n}</span>{title}</div>{phone(body)}<div class="cap">{cap}</div></div>'
tap = '<div class="tap"></div>'
# 1: calculator with menu open
s1 = f'''<div class="topbar">{switcher('calc')}<div class="dots">⋮{tap}</div></div>
<div class="menu"><div>Unit converter</div><div class="hl">Settings{tap}</div><div>Acknowledgements</div></div>
<div class="calcd"><div class="hist">∫₀¹ x² dx<br><b>= 1/3</b></div><div class="input">2 + 2</div></div>
<div class="keys">''' + ''.join(f'<div class="k">{c}</div>' for c in '789÷456×123−0.=+') + '</div>'
# 2: settings, graphs section
s2 = f'''<div class="appbar"><span>←</span><b>Settings</b></div>
<div class="sect">Graphs</div>
<div class="srow"><div><div class="t">Grid lines</div><div class="d">The axes always show</div></div><div class="sw on"><i></i></div></div>
<div class="srow"><div><div class="t">Legend</div><div class="d">Each line's name in the corner</div></div><div class="sw on"><i></i></div></div>
<div class="srow"><div><div class="t">Mark points on curves</div><div class="d">Zeros, extrema and crossings</div></div><div class="sw on"><i></i></div></div>
<div class="srow hlr"><div><div class="t">Geometry <span class="alpha">ALPHA</span></div><div class="d">GeoGebra-style constructions in the 2D graph: name points (A = (1, 2)) and build with Segment, Circle, Intersect… or tap with the Construct tools</div></div><div class="sw on"><i></i>{tap}</div></div>
<div class="srow"><div><div class="t">Starting view</div><div class="d">±5 · ±10 · ±20</div></div></div>'''
# shared graph backdrop
def grid():
    g = ''.join(f'<line x1="{x}" y1="0" x2="{x}" y2="600" stroke="var(--outv)" opacity=".5"/>' for x in range(6, 412, 40)) + ''.join(f'<line x1="0" y1="{y}" x2="412" y2="{y}" stroke="var(--outv)" opacity=".5"/>' for y in range(10, 600, 40))
    curve = ' '.join(f'{206 + x * 40:.1f},{290 - x * x * 40:.1f}' for x in [k / 20 for k in range(-60, 61)])
    return g + '<line x1="0" y1="290" x2="412" y2="290" stroke="var(--on)" stroke-width="1.3"/><line x1="206" y1="0" x2="206" y2="600" stroke="var(--on)" stroke-width="1.3"/>' + f'<polyline points="{curve}" fill="none" stroke="var(--pri)" stroke-width="2.5" opacity=".9"/>'
def graphscreen(inner_svg, overlay, listrows):
    rows = ''.join(f'<div class="li"><span class="dot" style="background:{c}"></span><span class="m">{t}</span></div>' for c, t in listrows)
    return f'''<div class="topbar">{switcher('g2')}<div class="dots">⋮</div></div>
<svg class="plot" width="412" height="600" style="top:96px">{grid()}{inner_svg}</svg>{overlay}
<div class="list">{rows}<div class="fab">＋</div></div>'''
w = lambda x: f'<span class="w">{x}</span>'
# 3: 2D graph with Construct button (tap it)
s3 = graphscreen('', f'<div class="construct"><div class="cbtn">{hexi}<span>Construct</span><span class="a">α</span>{tap}</div></div>',
                 [('var(--pri)', 'y = x²')]) 
# 4: tools open → Segment picked, two taps
tools = ['Point', 'Point on object', 'Intersect', 'Midpoint', 'Segment', 'Line', 'Ray', 'Vector', 'Perpendicular line', 'Parallel line', 'Circle', 'Polygon', 'Ellipse', 'Angle']
tm = ''.join(f'<div class="tl{" on" if t == "Segment" else ""}">{t}{tap if t == "Segment" else ""}</div>' for t in tools)
s4 = graphscreen('', f'<div class="construct"><div class="cbtn">{hexi}<span>Construct</span><span class="a">α</span></div><div class="tmenu">{tm}<div class="tl more">…and 13 more</div></div></div>',
                 [('var(--pri)', 'y = x²')])
# 5: points tapped, segment made; list shows the new lines
seg = '<line x1="126" y1="330" x2="286" y2="210" stroke="var(--ter)" stroke-width="3"/><circle cx="126" cy="330" r="6" fill="var(--pri)"/><circle cx="286" cy="210" r="6" fill="var(--pri)"/><text x="108" y="356" font-family="CMI" font-size="18" fill="var(--on)">A</text><text x="294" y="204" font-family="CMI" font-size="18" fill="var(--on)">B</text>'
s5 = graphscreen(seg, f'<div class="construct"><div class="cbtn">{hexi}<span>Segment</span><span class="a">α</span></div></div><div class="tap" style="left:120px;top:420px"></div><div class="tap" style="left:280px;top:300px"></div><div class="hint">Tap two points for the segment (1 of 2)</div>',
                 [('var(--pri)', 'A = (−2, −1)'), ('var(--pri)', 'B = (2, 2)'), ('var(--ter)', f'f = {w("Segment")}(A, B)')])
extra = '''
.stepcol{width:412px}
.sh{font:600 18px GSF;color:#2a2a22;margin-bottom:14px;display:flex;align-items:center;gap:10px}
.num{width:30px;height:30px;border-radius:15px;background:#5B6133;color:#fff;display:flex;align-items:center;justify-content:center;font:600 15px GSF}
.topbar{display:flex;align-items:center;justify-content:space-between;padding:8px 12px}
.sw4{display:flex;gap:4px;background:var(--lowest);border-radius:30px;padding:5px;box-shadow:0 3px 10px rgba(0,0,0,.18)}
.seg{width:42px;height:42px;border-radius:21px;display:flex;align-items:center;justify-content:center;color:var(--onv)}
.seg.on{width:auto;padding:0 16px;gap:8px;background:var(--pri);color:var(--onpri);font:500 14px GSF}
.dots{position:relative;width:40px;height:40px;display:flex;align-items:center;justify-content:center;font:700 22px GSF;color:var(--onv)}
.menu{position:absolute;right:14px;top:92px;background:var(--c);border-radius:12px;box-shadow:0 8px 24px rgba(0,0,0,.3);padding:8px 0;width:220px;z-index:5}
.menu div{position:relative;padding:12px 16px;font:15px GSF}.menu .hl{background:var(--sc)}
.calcd{padding:30px 22px;text-align:right;font:22px CMR}.hist{color:var(--onv);margin-bottom:40px}.input{font-size:40px}
.keys{position:absolute;left:0;right:0;bottom:0;display:grid;grid-template-columns:repeat(4,1fr);gap:8px;padding:14px;background:var(--c)}
.k{height:64px;border-radius:32px;background:var(--chh);display:flex;align-items:center;justify-content:center;font:24px GSF}
.appbar{display:flex;gap:18px;align-items:center;padding:14px 18px;font:22px GSF}.appbar b{font-weight:400}
.sect{font:600 14px GSF;color:var(--pri);padding:14px 22px 4px}
.srow{display:flex;align-items:center;justify-content:space-between;gap:14px;padding:12px 22px}
.srow.hlr{background:var(--ec);background:color-mix(in srgb, var(--ec) 45%, transparent);border-radius:20px;margin:6px 10px;padding:14px 12px}
.t{font:16px GSF;display:flex;align-items:center;gap:8px}.d{font:13px/1.35 GSF;color:var(--onv);margin-top:3px}
.alpha{font:600 11px GSF;background:var(--ec);color:var(--onec);border-radius:10px;padding:2px 8px}
.sw{position:relative;width:52px;height:32px;border-radius:16px;background:var(--chh);border:2px solid var(--out);flex:none}
.sw i{position:absolute;left:6px;top:6px;width:16px;height:16px;border-radius:8px;background:var(--out)}
.sw.on{background:var(--pri);border-color:var(--pri)}.sw.on i{left:24px;top:2px;width:24px;height:24px;border-radius:12px;background:var(--onpri)}
.plot{position:absolute;left:0}
.construct{position:absolute;top:110px;right:12px;display:flex;flex-direction:column;align-items:flex-end;gap:6px;z-index:4}
.cbtn{position:relative;display:flex;align-items:center;gap:6px;background:var(--sc);color:var(--onsc);border-radius:20px;padding:8px 14px;font:500 14px GSF}
.cbtn .a{opacity:.7;font-size:12px}
.tmenu{width:330px;background:var(--chh);border-radius:18px;padding:6px;box-shadow:0 6px 18px rgba(0,0,0,.25);display:grid;grid-template-columns:1fr 1fr}
.tl{position:relative;font:13px GSF;padding:8px 10px;border-radius:12px;white-space:nowrap}.tl.on{background:var(--sc);color:var(--onsc)}.tl.more{color:var(--onv)}
.list{position:absolute;left:0;right:0;bottom:0;height:230px;background:var(--bg);border-radius:28px 28px 0 0;padding:16px 14px;box-shadow:0 -6px 20px rgba(0,0,0,.15)}
.li{display:flex;align-items:center;gap:12px;padding:10px 6px;border-bottom:1px solid var(--outv)}
.dot{width:16px;height:16px;border-radius:8px;flex:none}.m{font:19px CMR}
.fab{position:absolute;right:18px;bottom:18px;width:56px;height:56px;border-radius:18px;background:var(--pc);color:var(--onpc);display:flex;align-items:center;justify-content:center;font:28px GSF;box-shadow:0 4px 12px rgba(0,0,0,.25)}
.hint{position:absolute;left:50%;transform:translateX(-50%);bottom:250px;background:var(--on);color:var(--bg);font:14px GSF;border-radius:20px;padding:8px 16px;white-space:nowrap}
.tap{position:absolute;right:-6px;top:50%;width:34px;height:34px;margin-top:-17px;border-radius:17px;background:rgba(186,26,26,.28);border:2.5px solid rgba(186,26,26,.85);z-index:9;pointer-events:none}
.phone > .tap{right:auto;margin:0}
.row{gap:28px}
'''
steps = [
    step(1, 'Open the menu', s1, 'In any mode, tap ⋮ (top right), then <b>Settings</b>.'),
    step(2, 'Turn Geometry on', s2, 'Under <b>Graphs</b>, switch on <b>Geometry</b> (marked Alpha). It stays on until you turn it off.'),
    step(3, 'Go to 2D graphing', s3, 'Pick <b>2D graphing</b> in the mode switcher. A <b>Construct</b> button now sits at the top right of the graph.'),
    step(4, 'Pick a tool', s4, 'Tap <b>Construct</b> for the tools, as in GeoGebra\'s toolbar, and pick one, here <b>Segment</b>.'),
    step(5, 'Tap on the graph', s5, 'Tap two places (or existing points): A, B and the segment are made, each as its own line in the list.'),
]
other = '''<div class="ways"><div class="wt">Two more ways in, once Geometry is on</div>
<div class="wrow"><div class="wcard"><div class="wh">Type it</div><div class="wb">In any line of the 2D list, type a capital point like <span class="mono">A = (1, 2)</span>, or a command like <span class="mono">Circle(A, 3)</span>. Command names can be typed letter by letter.</div></div>
<div class="wcard"><div class="wh">＋ › Geometry</div><div class="wb">The add button lists every command with what it does; tap one to start a line with it, the cursor between its brackets.</div></div>
<div class="wcard"><div class="wh">Learn more</div><div class="wb">Settings › Documentation › <b>Geometry</b> has the commands, tools and options, with examples.</div></div></div></div>'''
extra += '''
.ways{margin-top:10px}.wt{font:600 18px GSF;color:#2a2a22;margin-bottom:12px}
.wrow{display:flex;gap:20px}.wcard{width:420px;background:#fcf9ee;border-radius:20px;padding:18px 20px;box-shadow:0 4px 14px rgba(0,0,0,.12)}
.wh{font:600 16px GSF;color:#5B6133;margin-bottom:6px}.wb{font:14px/1.45 GSF;color:#47473B}.mono{font-family:CMR;font-size:16px;color:#1C1C16}
'''
html = f'''<!doctype html><html><head><meta charset="utf-8"><title>x</title><style>{style}{extra}</style></head><body style="width:2300px">
<h2>Getting into geometry (GeoGebra-style, alpha)</h2>
<p class="note">Geometry isn't a separate mode: it's switched on once in Settings, and then the 2D graph understands constructions and shows the Construct tools. Red rings mark where to tap.</p>
<div class="row">{''.join(steps)}</div>
{other}
<script>document.fonts.ready.then(()=>document.title='ready')</script></body></html>'''
open(os.path.join(here, 'round34.html'), 'w').write(html)

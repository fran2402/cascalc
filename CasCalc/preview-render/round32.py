# Builds round32.html from the geometry engine's own drawings (dumped by a probe test as g1/g2.json).
import json, os, sys
sp = sys.argv[1] if len(sys.argv) > 1 else '.'
here = os.path.dirname(os.path.abspath(__file__))
head = open(os.path.join(here, 'round28.html')).read(); style = head[head.index('<style>') + 7:head.index('</style>')]
W, H = 412, 560; xmin, xmax = -6, 6; ymax = 6 * H / W; ymin = -ymax
def X(x): return (x - xmin) / (xmax - xmin) * W
def Y(y): return (ymax - y) / (ymax - ymin) * H
def poly(l): return ' '.join(f'{X(a):.1f},{Y(b):.1f}' for a, b in l if abs(a) < 1e4 and abs(b) < 1e4)
cols = {'F': 'var(--pri)', 'G': 'var(--pri)', 'k': 'var(--ter)', 'P': 'var(--err)', 't': 'var(--err)', 'l': 'var(--sec)', 'c': 'var(--pri)',
        'A': 'var(--pri)', 'B': 'var(--pri)', 'h': 'var(--sec)', 'O': 'var(--ter)', 's': 'var(--ter)', 'Q': 'var(--err)'}
def svg(file, extra_fn=None, highlight=None):
    d = json.load(open(os.path.join(sp, file)))
    grid = ''.join(f'<line x1="{X(x):.1f}" y1="0" x2="{X(x):.1f}" y2="{H}" stroke="var(--outv)" opacity=".55"/>' for x in range(xmin, xmax + 1)) + \
           ''.join(f'<line x1="0" y1="{Y(y):.1f}" x2="{W}" y2="{Y(y):.1f}" stroke="var(--outv)" opacity=".55"/>' for y in range(-9, 10))
    out = [f'<svg class="plot" width="{W}" height="{H}">{grid}<line x1="0" y1="{Y(0)}" x2="{W}" y2="{Y(0)}" stroke="var(--on)" stroke-width="1.3"/><line x1="{X(0)}" y1="0" x2="{X(0)}" y2="{H}" stroke="var(--on)" stroke-width="1.3"/>']
    if extra_fn:
        pts = [(x / 20, extra_fn(x / 20)) for x in range(-120, 121)]
        out.append(f'<polyline points="{poly(pts)}" fill="none" stroke="var(--pri)" stroke-width="2.5"/>')
        out.append(f'<text x="{X(4.3)}" y="{Y(4.3)}" font-family="CMI" font-size="16" fill="var(--on)">f</text>')
    def col(o): return cols.get(o['name'], 'var(--onv)') if o['name'] else 'var(--err)'
    for o in d:
        if highlight and o['name'] in highlight:
            for l in o['lines']: out.append(f'<polyline points="{poly(l)}" fill="none" stroke="var(--pri)" stroke-opacity=".35" stroke-width="12" stroke-linecap="round"/>')
        if o['fill']: out.append(f'<polygon points="{poly(o["fill"])}" fill="{col(o)}" fill-opacity=".2"/>')
        for l in o['lines']: out.append(f'<polyline points="{poly(l)}" fill="none" stroke="{col(o)}" stroke-width="2.4"/>')
    for o in d:
        for p in o['points']:
            out.append(f'<circle cx="{X(p[0]):.1f}" cy="{Y(p[1]):.1f}" r="5" fill="{col(o)}"/>')
            if o['name'] and o['kind'] == 'Point': out.append(f'<text x="{X(p[0]) + 8:.1f}" y="{Y(p[1]) - 8:.1f}" font-family="CMI" font-size="17" fill="var(--on)">{o["name"]}</text>')
    out.append('</svg>')
    return ''.join(out)
hexi = '<svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor"><path d="M17.2 3H6.8l-5.2 9 5.2 9h10.4l5.2-9z"/></svg>'
w = lambda x: f'<span class="w">{x}</span>'
def rowsHtml(rows, cls='li'): return ''.join(f'<div class="{cls}"><span class="dot" style="background:{c}"></span><div><div class="m">{t}</div>' + (f'<div class="val">{v}</div>' if v else '') + '</div></div>' for c, t, v in rows)
def phone(theme, file, tool, hint, rows, fn=None, hl=None):
    return f'''<div class="{theme}"><div class="phone"><div class="status"><span>9:41</span><span>▴ ▮</span></div>
<div style="position:absolute;top:32px;left:0">{svg(file, fn, hl)}</div>
<div class="construct"><div class="cbtn">{hexi}<span>{tool}</span><span style="opacity:.7;font-size:12px">α</span></div></div>
<div class="hint">{hint}</div><div class="list">{rowsHtml(rows)}</div></div></div>'''
rows1 = [('var(--ter)', f'k = {w("Ellipse")}(F, G, 4)', 'Ellipse'), ('var(--err)', f'P = {w("Point")}(k, 0.15)', '= (2.351, 2.141)'), ('var(--err)', f't = {w("Tangent")}(P, k)', None), ('var(--sec)', f'l = {w("Line")}((−5, −3), (5, 3.2))', None)]
rows2 = [('var(--pri)', 'f(x) = x²/4 − 1', None), ('var(--sec)', f'h = {w("RegularPolygon")}(A, B, 6)', None), ('var(--ter)', f's = {w("CircularSector")}(O, (4.5, −2.5), (2.5, −0.5))', 'length = 3.142'), ('var(--err)', f'{w("Intersect")}(f, l)', '= (−3.087, 1.383), (3.887, 2.777)')]
list3 = [('var(--pri)', 'A = (1, 2)', None), ('var(--onv)', f'd = {w("Distance")}(A, B) / 2', '= 2.5'), ('var(--onv)', 'M = (A + B) / 2', '= (2.5, 4)'), ('var(--onv)', 'v = B − A', '= (3, 4)'), ('var(--onv)', 'w = x(A) + y(A)', '= 3'), ('var(--err)', f'Q = {w("Point")}(f, 2)', '= (2, 0)'), ('var(--onv)', f'{w("Area")}(s)', '= 3.142')]
tools = ['Point', 'Point on object', 'Intersect', 'Midpoint', 'Segment', 'Line', 'Ray', 'Vector', 'Perpendicular line', 'Parallel line', 'Perpendicular bisector', 'Angle bisector', 'Tangent', 'Polygon', 'Circle', 'Circle (3 points)', 'Semicircle', 'Arc', 'Sector', 'Ellipse', 'Hyperbola', 'Parabola', 'Conic (5 points)', 'Reflect', 'Angle', 'Distance']
tm = ''.join(f'<div class="tl{" on" if t == "Intersect" else ""}">{t}</div>' for t in tools)
phone3 = f'''<div class="light"><div class="phone" style="background:var(--bg)"><div class="status"><span>9:41</span><span>▴ ▮</span></div>
<div style="padding:8px 14px">{rowsHtml(list3, 'li2')}</div>
<div class="construct" style="top:400px"><div class="cbtn">{hexi}<span>Construct</span><span style="opacity:.7;font-size:12px">α</span></div><div class="tmenu">{tm}</div></div></div></div>'''
extra = '''
.plot{position:absolute;top:0;left:0}
.construct{position:absolute;top:44px;right:10px;display:flex;flex-direction:column;align-items:flex-end;gap:6px}
.cbtn{display:flex;align-items:center;gap:6px;background:var(--pri);color:var(--onpri);border-radius:20px;padding:8px 14px;font:500 14px GSF}
.tmenu{width:340px;background:var(--chh);border-radius:18px;padding:6px;box-shadow:0 6px 18px rgba(0,0,0,.25);display:grid;grid-template-columns:1fr 1fr}
.tl{font:13px GSF;padding:7px 10px;border-radius:12px;white-space:nowrap}.tl.on{background:var(--sc);color:var(--onsc)}
.hint{position:absolute;left:50%;transform:translateX(-50%);top:530px;background:var(--on);color:var(--bg);font:14px GSF;border-radius:20px;padding:8px 16px;white-space:nowrap}
.list{position:absolute;left:0;right:0;bottom:0;height:300px;background:var(--bg);border-radius:28px 28px 0 0;padding:14px 14px;box-shadow:0 -6px 20px rgba(0,0,0,.15)}
.li,.li2{display:flex;align-items:flex-start;gap:12px;padding:8px 6px;border-bottom:1px solid var(--outv)}
.dot{width:16px;height:16px;border-radius:8px;flex:none;margin-top:3px}
.m{font:18px CMR}.val{font:14px CMR;color:var(--onv);margin-top:2px}
'''
p1 = phone('light', 'g1.json', 'Intersect', 'Tap two objects to mark where they cross (2 of 2)', rows1, hl=['k'])
p2 = phone('dark', 'g2.json', 'Point on object', 'Tap a line, circle or curve to put a point on it', rows2, fn=lambda x: x * x / 4 - 1)
html = f'''<!doctype html><html><head><meta charset="utf-8"><title>x</title><style>{style}{extra}</style></head><body>
<h2>Round 32: geometry, second pass (alpha)</h2>
<p class="note">Conics (Ellipse, Hyperbola, Parabola, Conic through five points), arcs and sectors, regular polygons, points that slide along objects, and functions f(x) from the graph in Intersect, Tangent and Point. Arithmetic now mixes with commands. The tools pick objects as well as points, with what's picked highlighted, and name new lines as GeoGebra does. The shapes drawn here come from the engine's own output.</p>
<div class="row"><div>{p1}<div class="cap">The Intersect tool with the ellipse k picked (highlighted). P = Point(k, 0.15) slides round the ellipse when dragged, and t, its tangent, follows.</div></div>
<div>{p2}<div class="cap">Dark theme: f(x) = x²/4 − 1 from the graph's own lines meeting a line, a point Q on f with its tangent, a regular hexagon and a sector.</div></div>
<div>{phone3}<div class="cap">Arithmetic mixed with commands, x( ) and y( ), and the tools in two columns.</div></div></div>
<script>document.fonts.ready.then(()=>document.title='ready')</script></body></html>'''
open(os.path.join(here, 'round32.html'), 'w').write(html)

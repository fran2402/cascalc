# Builds round33.html from the geometry engine's own drawings (h1/h2.json, dumped by a probe test).
import json, os, sys
sp = sys.argv[1]
here = os.path.dirname(os.path.abspath(__file__))
head = open(os.path.join(here, 'round28.html')).read(); style = head[head.index('<style>') + 7:head.index('</style>')]
W, H = 412, 560; xmin, xmax = -6, 6; ymax = 6 * H / W; ymin = -ymax
def X(x): return (x - xmin) / (xmax - xmin) * W
def Y(y): return (ymax - y) / (ymax - ymin) * H
def poly(l): return ' '.join(f'{X(a):.1f},{Y(b):.1f}' for a, b in l if abs(a) < 1e4 and abs(b) < 1e4)
def svg(file, cols):
    d = json.load(open(os.path.join(sp, file)))
    grid = ''.join(f'<line x1="{X(x):.1f}" y1="0" x2="{X(x):.1f}" y2="{H}" stroke="var(--outv)" opacity=".55"/>' for x in range(xmin, xmax + 1)) + \
           ''.join(f'<line x1="0" y1="{Y(y):.1f}" x2="{W}" y2="{Y(y):.1f}" stroke="var(--outv)" opacity=".55"/>' for y in range(-9, 10))
    out = [f'<svg class="plot" width="{W}" height="{H}">{grid}<line x1="0" y1="{Y(0)}" x2="{W}" y2="{Y(0)}" stroke="var(--on)" stroke-width="1.3"/><line x1="{X(0)}" y1="0" x2="{X(0)}" y2="{H}" stroke="var(--on)" stroke-width="1.3"/>']
    col = lambda o: cols.get(o['name'], 'var(--onv)')
    for o in d:
        width = 3.2 if o['kind'] == 'Polyline' else 2.2
        if o['fill']: out.append(f'<polygon points="{poly(o["fill"])}" fill="{col(o)}" fill-opacity=".18"/>')
        dash = ' stroke-dasharray="6 5"' if o['name'] in ('b', 'r') else ''
        for l in o['lines']: out.append(f'<polyline points="{poly(l)}" fill="none" stroke="{col(o)}" stroke-width="{width}"{dash}/>')
    for o in d:
        for p in o['points']:
            out.append(f'<circle cx="{X(p[0]):.1f}" cy="{Y(p[1]):.1f}" r="5" fill="{col(o)}"/>')
            if o['name'] and o['kind'] == 'Point': out.append(f'<text x="{X(p[0]) + 8:.1f}" y="{Y(p[1]) - 8:.1f}" font-family="CMI" font-size="17" fill="var(--on)">{o["name"]}</text>')
    out.append('</svg>')
    return ''.join(out)
hexi = '<svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor"><path d="M17.2 3H6.8l-5.2 9 5.2 9h10.4l5.2-9z"/></svg>'
w = lambda x: f'<span class="w">{x}</span>'
def rowsHtml(rows): return ''.join(f'<div class="li"><span class="dot" style="background:{c}"></span><div><div class="m">{t}</div>' + (f'<div class="val">{v}</div>' if v else '') + '</div></div>' for c, t, v in rows)
def phone(theme, body, rows, tool=None, hint=None):
    top = f'<div class="construct"><div class="cbtn">{hexi}<span>{tool}</span><span style="opacity:.7;font-size:12px">α</span></div></div>' if tool else ''
    h = f'<div class="hint">{hint}</div>' if hint else ''
    return f'''<div class="{theme}"><div class="phone"><div class="status"><span>9:41</span><span>▴ ▮</span></div>
<div style="position:absolute;top:32px;left:0">{body}</div>{top}{h}<div class="list">{rowsHtml(rows)}</div></div></div>'''
c1 = {'c': 'var(--pri)', 'Q': 'var(--err)', 'A': 'var(--pri)', 'O': 'var(--pri)', 'b': 'var(--onv)', 'r': 'var(--onv)', 'P': 'var(--ter)', 'L': 'var(--ter)'}
c2 = {'t': 'var(--sec)', 'A': 'var(--pri)', 'B': 'var(--pri)', 'C': 'var(--pri)', 'O': 'var(--ter)', 'H': 'var(--err)', 'G': 'var(--pri)', 'u': 'var(--err)', 'k': 'var(--ter)'}
rows1 = [('var(--err)', f'Q = {w("Point")}(c, 0.12)', None), ('var(--onv)', f'b = {w("PerpendicularBisector")}(A, Q)', None), ('var(--ter)', f'P = {w("Intersect")}(b, {w("Line")}(O, Q))', None), ('var(--ter)', f'L = {w("Locus")}(P, Q)', None)]
d2 = {o['name']: o for o in json.load(open(os.path.join(sp, 'h2.json')))}
def pt(n): x, y = d2[n]['points'][0]; return f'= ({x:.4g}, {y:.4g})'.replace('-', '−')
rows2 = [('var(--ter)', f'O = {w("Circumcenter")}(A, B, C)', pt('O')), ('var(--err)', f'H = {w("Orthocenter")}(A, B, C)', pt('H')), ('var(--err)', f'u = {w("Line")}(O, H)', None), ('var(--onv)', f'{w("AreCollinear")}(O, G, H)', 'true')]
opts = f'''<div class="light"><div class="phone" style="background:var(--bg)"><div class="status"><span>9:41</span><span>▴ ▮</span></div>
<div style="padding:14px 18px">
<div class="li" style="border:none"><span class="dot" style="background:var(--err)"></span><div><div class="m">Q = {w("Point")}(c, 0.12)</div><div class="val">= (2.18, 2.06)</div></div></div>
<div class="sheet2"><div class="sh2">Point</div><div class="lbl2">Size: 4.0 dp</div><div class="slider"><div style="width:20%"></div></div>
<div class="opt"><span>Show name</span><div class="sw on"><div></div></div></div>
<div class="opt"><span>Show coordinates</span><div class="sw"><div></div></div></div>
<div class="opt"><div><div>Move along its path</div><div class="sub">Round once in 10 s, or back and forth</div></div><div class="sw on"><div></div></div></div></div>
</div></div></div>'''
extra = '''
.plot{position:absolute;top:0;left:0}
.construct{position:absolute;top:44px;right:10px}
.cbtn{display:flex;align-items:center;gap:6px;background:var(--pri);color:var(--onpri);border-radius:20px;padding:8px 14px;font:500 14px GSF}
.hint{position:absolute;left:50%;transform:translateX(-50%);top:530px;background:var(--on);color:var(--bg);font:14px GSF;border-radius:20px;padding:8px 16px;white-space:nowrap}
.list{position:absolute;left:0;right:0;bottom:0;height:300px;background:var(--bg);border-radius:28px 28px 0 0;padding:14px 14px;box-shadow:0 -6px 20px rgba(0,0,0,.15)}
.li{display:flex;align-items:flex-start;gap:12px;padding:8px 6px;border-bottom:1px solid var(--outv)}
.dot{width:16px;height:16px;border-radius:8px;flex:none;margin-top:3px}
.m{font:18px CMR}.val{font:14px CMR;color:var(--onv);margin-top:2px}
.sheet2{margin-top:12px;background:var(--c);border-radius:24px;padding:18px}
.sh2{font:600 14px GSF;color:var(--pri);margin-bottom:8px}.lbl2{font:12px GSF;color:var(--onv)}
.slider{height:16px;border-radius:8px;background:var(--sc);margin:8px 0 14px;overflow:hidden}.slider div{height:100%;background:var(--pri)}
.opt{display:flex;justify-content:space-between;align-items:center;font:15px GSF;padding:10px 0}.sub{font:12px GSF;color:var(--onv)}
.sw{width:52px;height:32px;border-radius:16px;background:var(--chh);border:2px solid var(--out);flex:none;position:relative}
.sw div{position:absolute;left:6px;top:6px;width:16px;height:16px;border-radius:8px;background:var(--out)}
.sw.on{background:var(--pri);border-color:var(--pri)}.sw.on div{left:24px;top:2px;width:24px;height:24px;border-radius:12px;background:var(--onpri)}
'''
html = f'''<!doctype html><html><head><meta charset="utf-8"><title>x</title><style>{style}{extra}</style></head><body>
<h2>Round 33: loci, triangle centers, checks and moving points (alpha)</h2>
<p class="note">Locus(P, Q) draws the curve P traces as Q slides along its object; the Locus tool takes the two points. Triangle centers (Circumcenter, Orthocenter, Incenter, Centroid), a conic's parts (Foci, Vertex, Asymptote, Directrix) and Polar lines are new, as are checks that show true or false (AreCollinear, AreConcyclic, AreParallel, ArePerpendicular, AreEqual). A point on an object can move along it by itself. Shapes here are drawn from the engine's own output.</p>
<div class="row"><div>{phone('light', svg('h1.json', c1), rows1, 'Locus', 'Tap the tracing point, then the one on an object')}<div class="cap">A classic: Q runs round the circle; where AQ's perpendicular bisector meets OQ traces a hyperbola with foci O and A (A is outside the circle).</div></div>
<div>{phone('dark', svg('h2.json', c2), rows2)}<div class="cap">Euler's line: the circumcenter O, centroid G and orthocenter H are collinear, which AreCollinear confirms.</div></div>
<div>{opts}<div class="cap">A point on an object's options: its size, its name and coordinates, and Move along its path. Everything built on it, a locus included, follows.</div></div></div>
<script>document.fonts.ready.then(()=>document.title='ready')</script></body></html>'''
open(os.path.join(here, 'round33.html'), 'w').write(html)

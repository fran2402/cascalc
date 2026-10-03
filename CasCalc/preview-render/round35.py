# The construct UI redesign: palette with drawn tool icons, status card, folded bar, target glow.
import os, math
here = os.path.dirname(os.path.abspath(__file__))
head = open(os.path.join(here, 'round28.html')).read(); style = head[head.index('<style>') + 7:head.index('</style>')]

def icon(tool, ink='var(--on)', acc='var(--pri)', size=30):
    W = 100; thin = 6.5; bold = 9
    out = []
    def L(x1, y1, x2, y2, c=acc, w=bold): out.append(f'<line x1="{x1*W}" y1="{y1*W}" x2="{x2*W}" y2="{y2*W}" stroke="{c}" stroke-width="{w}" stroke-linecap="round"/>')
    def D(x, y, c=ink): out.append(f'<circle cx="{x*W}" cy="{y*W}" r="8.5" fill="{c}"/>')
    def C(x, y, r, c=acc, w=bold, fill='none'): out.append(f'<circle cx="{x*W}" cy="{y*W}" r="{r*W}" stroke="{c}" stroke-width="{w}" fill="{fill}"/>')
    def P(pts, c=acc, w=thin, fill='none', closed=True, extra=''):
        d = 'M' + ' L'.join(f'{x*W},{y*W}' for x, y in pts) + (' Z' if closed else '')
        out.append(f'<path d="{d}" stroke="{c}" stroke-width="{w}" fill="{fill}" stroke-linejoin="round" stroke-linecap="round" {extra}/>')
    def A(cx, cy, r, a0, a1, c=acc, w=bold):
        x0, y0 = cx + r * math.cos(math.radians(a0)), cy + r * math.sin(math.radians(a0)); x1, y1 = cx + r * math.cos(math.radians(a1)), cy + r * math.sin(math.radians(a1))
        large = 1 if abs(a1 - a0) > 180 else 0
        out.append(f'<path d="M{x0*W},{y0*W} A{r*W},{r*W} 0 {large} 1 {x1*W},{y1*W}" stroke="{c}" stroke-width="{w}" fill="none" stroke-linecap="round"/>')
    t = tool
    if t == 'Move': P([(.28,.14),(.28,.8),(.44,.64),(.56,.9),(.66,.85),(.54,.6),(.76,.6)], c=acc, fill=acc, w=2)
    elif t == 'Point': out.append('<circle cx="50" cy="50" r="22" fill="var(--pri)" fill-opacity=".3"/>'); D(.5,.5,acc)
    elif t == 'Point on object': C(.5,.5,.32,ink,thin); D(.73,.27,acc)
    elif t == 'Intersect': L(.12,.8,.88,.25,ink,thin); L(.12,.25,.88,.8,ink,thin); D(.5,.525,acc)
    elif t == 'Midpoint': L(.14,.7,.86,.3,ink,thin); D(.14,.7); D(.86,.3); D(.5,.5,acc)
    elif t == 'Segment': L(.18,.74,.82,.26); D(.18,.74); D(.82,.26)
    elif t == 'Line': L(.02,.88,.98,.12); D(.32,.65); D(.68,.37)
    elif t == 'Ray': L(.2,.75,.98,.15); D(.2,.75); D(.55,.48)
    elif t == 'Vector': L(.18,.78,.78,.24); P([(.86,.16),(.62,.24),(.78,.4)], c=acc, fill=acc, w=2); D(.18,.78)
    elif t == 'Perpendicular': L(.06,.72,.94,.72,ink,thin); L(.5,.06,.5,.94); D(.5,.3)
    elif t == 'Parallel': L(.06,.78,.94,.52,ink,thin); L(.06,.46,.94,.2); D(.4,.36)
    elif t == 'Perpendicular bisector': L(.12,.7,.88,.7,ink,thin); D(.12,.7); D(.88,.7); L(.5,.08,.5,.94)
    elif t == 'Angle bisector': L(.14,.82,.9,.82,ink,thin); L(.14,.82,.62,.12,ink,thin); L(.14,.82,.94,.36); D(.14,.82)
    elif t == 'Tangent': C(.42,.6,.26,ink,thin); L(.04,.34,.96,.34); D(.86,.34)
    elif t == 'Polygon': pts=[(.2,.78),(.12,.36),(.5,.12),(.88,.4),(.74,.82)]; P(pts, c=acc, fill='color-mix(in srgb, var(--pri) 25%, transparent)'); D(.5,.12); D(.2,.78); D(.88,.4)
    elif t == 'Circle': C(.5,.5,.34); D(.5,.5); D(.74,.26)
    elif t == 'Circle through 3': C(.5,.5,.34); D(.16,.5); D(.74,.26); D(.7,.79)
    elif t == 'Semicircle': A(.5,.62,.36,180,360); L(.14,.62,.86,.62,ink,thin); D(.14,.62); D(.86,.62)
    elif t == 'Arc': A(.3,.7,.56,-80,-10); L(.3,.7,.86,.62,ink,5); D(.3,.7)
    elif t == 'Sector': x0,y0=.3+.62*math.cos(math.radians(-75)),.74+.62*math.sin(math.radians(-75)); P([(.3,.74),(x0,y0)], closed=False); out.append(f'<path d="M30,74 L{x0*W},{y0*W} A62,62 0 0 1 92,74 Z" fill="color-mix(in srgb, var(--pri) 30%, transparent)" stroke="{acc}" stroke-width="{thin}" stroke-linejoin="round"/>'); D(.3,.74)
    elif t == 'Ellipse': out.append(f'<ellipse cx="50" cy="50" rx="44" ry="24" stroke="{acc}" stroke-width="{bold}" fill="none"/>'); D(.3,.5); D(.7,.5)
    elif t == 'Hyperbola':
        for s in (1, -1): P([(.5 + s*.16*math.cosh(-1.4+2.8*k/20), .5+.2*math.sinh(-1.4+2.8*k/20)) for k in range(21)], w=bold, closed=False)
    elif t == 'Parabola': P([(.12+.76*k/20, .66-2.4*((.12+.76*k/20)-.5)**2) for k in range(21)], w=bold, closed=False); L(.08,.88,.92,.88,ink,thin); D(.5,.46)
    elif t == 'Conic through 5': out.append(f'<ellipse cx="50" cy="50" rx="42" ry="22" transform="rotate(-25 50 50)" stroke="{acc}" stroke-width="{bold}" fill="none"/>'); [D(x,y) for x,y in [(.12,.62),(.5,.2),(.88,.38),(.5,.8),(.8,.66)]]
    elif t == 'Angle': L(.14,.82,.92,.82,ink,thin); L(.14,.82,.7,.18,ink,thin); A(.14,.82,.38,-49,0); D(.14,.82)
    elif t == 'Distance': L(.14,.5,.86,.5); L(.14,.34,.14,.66,ink,thin); L(.86,.34,.86,.66,ink,thin); [L(.14+.12*k,.5,.14+.12*k,.6,ink,4) for k in range(1,6)]
    elif t == 'Reflect': out.append(f'<line x1="50" y1="6" x2="50" y2="94" stroke="{ink}" stroke-width="{thin}" stroke-dasharray="8 6"/>'); P([(.1,.74),(.4,.74),(.4,.3)], c=ink); P([(.9,.74),(.6,.74),(.6,.3)], fill='color-mix(in srgb, var(--pri) 35%, transparent)')
    elif t == 'Locus': P([(.5+.36*math.cos(k/24*6.283), .5+.22*math.sin(2*k/24*6.283)) for k in range(25)], w=bold, closed=False, extra='stroke-dasharray="2 10"'); D(.86,.5,acc)
    return f'<svg width="{size}" height="{size}" viewBox="0 0 100 100">{"".join(out)}</svg>'

cats = {'Points': ['Move', 'Point', 'Point on object', 'Intersect', 'Midpoint'],
        'Lines': ['Segment', 'Line', 'Ray', 'Vector', 'Perpendicular', 'Parallel', 'Perpendicular bisector', 'Angle bisector', 'Tangent'],
        'Circles & shapes': ['Polygon', 'Circle', 'Circle through 3', 'Semicircle', 'Arc', 'Sector'],
        'Conics': ['Ellipse', 'Hyperbola', 'Parabola', 'Conic through 5'],
        'Measure & more': ['Angle', 'Distance', 'Reflect', 'Locus']}
hexi = '<svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor"><path d="M17.2 3H6.8l-5.2 9 5.2 9h10.4l5.2-9z"/></svg>'
undo = '<svg width="22" height="22" viewBox="0 0 24 24" fill="currentColor"><path d="M12.5 8c-2.65 0-5.05.99-6.9 2.6L2 7v9h9l-3.62-3.62c1.39-1.16 3.16-1.88 5.12-1.88 3.54 0 6.55 2.31 7.6 5.5l2.37-.78C21.08 11.03 17.15 8 12.5 8z"/></svg>'
apps = '<svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor"><path d="M4 8h4V4H4v4zm6 12h4v-4h-4v4zm-6 0h4v-4H4v4zm0-6h4v-4H4v4zm6 0h4v-4h-4v4zm6-10v4h4V4h-4zm-6 4h4V4h-4v4zm6 6h4v-4h-4v4zm0 6h4v-4h-4v4z"/></svg>'

def grid():
    g = ''.join(f'<line x1="{x}" y1="0" x2="{x}" y2="900" stroke="var(--outv)" opacity=".45"/>' for x in range(6, 412, 40)) + ''.join(f'<line x1="0" y1="{y}" x2="412" y2="{y}" stroke="var(--outv)" opacity=".45"/>' for y in range(10, 900, 40))
    return g + '<line x1="0" y1="410" x2="412" y2="410" stroke="var(--on)" stroke-width="1.3"/><line x1="206" y1="0" x2="206" y2="900" stroke="var(--on)" stroke-width="1.3"/>'
def palette(sel_cat, sel_tool, recent):
    tabs = ''.join(f'<div class="tab{" on" if c == sel_cat else ""}">{c}</div>' for c in cats)
    tiles = ''.join(f'<div class="tile{" on" if t == sel_tool else ""}">{icon(t, "var(--onpri)" if t == sel_tool else "var(--on)", "var(--onpri)" if t == sel_tool else "var(--pri)")}<span>{t}</span></div>' for t in cats[sel_cat])
    rec = ''.join(f'<div class="rnd">{icon(t, size=26)}</div>' for t in recent)
    return f'<div class="pal"><div class="tabs">{tabs}</div><div class="tiles">{tiles}</div><div class="rl">Recent</div><div class="recent">{rec}</div></div>'
def folded(tool, recent):
    chips = ''.join(f'<div class="rnd{" on" if t == tool else ""}">{icon(t, "var(--onpri)" if t == tool else "var(--on)", "var(--onpri)" if t == tool else "var(--pri)", 26)}</div>' for t in ['Move'] + recent)
    return f'<div class="pal fold">{chips}<div class="grow"></div><div class="toolsbtn">{apps}<span>Tools</span></div></div>'
def status(tool, step, of, role, instr, picks, close=False):
    dots = '' if of <= 1 else '<div class="dots">' + ''.join(f'<i class="{"d" if k < step - 1 else "n" if k == step - 1 else ""}"></i>' for k in range(of)) + '</div>'
    chips = '' if not picks else '<div class="chips">' + ''.join(f'<span class="chip"><small>{r} </small><em>{n}</em></span>' for r, n in picks) + '</div>'
    stepline = role if of == 0 else f'Step {step} of {of} · {role}'
    btns = (f'<span class="ib">{undo}</span>' if picks else '') + ('<span class="tonal">Close</span>' if close else '') + '<span class="txt">Done</span>'
    return f'''<div class="scard"><div class="srow"><div class="sic">{icon(tool, "var(--onpc)", "var(--pri)", 24)}</div><div class="st"><b>{tool}</b><small>{stepline}</small></div>{btns}</div>{dots}<div class="instr">{instr}</div>{chips}</div>'''
def phone(theme, svg, overlays, listrows):
    rows = ''.join(f'<div class="li"><span class="dot" style="background:{c}"></span><span class="m">{t}</span></div>' for c, t in listrows)
    return f'''<div class="{theme}"><div class="phone"><div class="status-bar"><span>9:41</span><span>▴ ▮</span></div>
<svg class="plot" width="412" height="900">{grid()}{svg}</svg>{overlays}
<div class="bbar"><div class="fab">＋</div><div class="tb"><span>⌂</span><span>⤢</span><span>⋯</span></div></div></div></div>'''
w = lambda x: f'<span class="w">{x}</span>'
def pt(x, y, n, c='var(--pri)', glow=False, ring=False):
    s = f'<circle cx="{x}" cy="{y}" r="15" fill="color-mix(in srgb, var(--pri) 18%, transparent)"/>' if glow else ''
    s += f'<circle cx="{x}" cy="{y}" r="6" fill="{c}"/><text x="{x+9}" y="{y-9}" font-family="CMI" font-size="17" fill="var(--on)">{n}</text>'
    if ring: s += f'<circle cx="{x}" cy="{y}" r="12" fill="none" stroke="var(--pri)" stroke-width="2.6"/>'
    return s
# phone 1: idle with Construct button + palette open (just entered)
svg1 = '<path d="M86,250 Q206,610 326,250" fill="none" stroke="var(--pri)" stroke-width="2.6"/>' + pt(126, 330, 'A') + pt(286, 290, 'B')
ov1 = status('Move', 0, 0, 'Pick a tool below', 'Drag points to move them', []) + palette('Lines', None, ['Circle', 'Segment', 'Polygon'])
# phone 2: Tangent tool, step 2 needs a circle: circles glow, A picked, folded bar
svg2 = '<circle cx="206" cy="410" r="110" fill="none" stroke="color-mix(in srgb, var(--pri) 16%, transparent)" stroke-width="18"/><circle cx="206" cy="410" r="110" fill="none" stroke="var(--ter)" stroke-width="2.6"/>' + \
       '<ellipse cx="300" cy="640" rx="80" ry="40" fill="none" stroke="color-mix(in srgb, var(--pri) 16%, transparent)" stroke-width="18"/><ellipse cx="300" cy="640" rx="80" ry="40" fill="none" stroke="var(--sec)" stroke-width="2.6"/>' + \
       pt(206, 410, 'O') + pt(60, 250, 'A', ring=True) + '<line x1="0" y1="560" x2="412" y2="300" stroke="var(--onv)" stroke-width="2"/>'
ov2 = status('Tangent', 2, 2, 'Circle or conic', 'Tap a circle or conic', [('From', 'A')]) + folded('Tangent', ['Tangent', 'Circle', 'Segment'])
# phone 3: polygon with 4 corners, Close button; points glow for picking
pts = [(110, 520), (150, 300), (290, 270), (330, 470)]
svg3 = '<polyline points="' + ' '.join(f'{x},{y}' for x, y in pts) + '" fill="color-mix(in srgb, var(--sec) 14%, transparent)" stroke="var(--sec)" stroke-width="2.6" stroke-dasharray="7 6"/>' + \
       ''.join(f'<polyline points="{a[0]},{a[1]} {b[0]},{b[1]}" stroke="color-mix(in srgb, var(--pri) 35%, transparent)" stroke-width="0"/>' for a, b in zip(pts, pts[1:])) + \
       ''.join(pt(x, y, n, glow=True, ring=True) for (x, y), n in zip(pts, 'ABCD')) + pt(230, 620, 'E', glow=True)
ov3 = status('Polygon', 0, 0, '4 corners', 'Tap the corners, then the first again to close', [('Corners', 'A'), ('', 'B'), ('', 'C'), ('', 'D')], close=True) + folded('Polygon', ['Polygon', 'Tangent', 'Circle'])
extra = '''
.plot{position:absolute;top:32px;left:0}
.scard{position:absolute;top:44px;left:12px;right:12px;background:var(--chh);border-radius:24px;padding:10px 8px 10px 14px;box-shadow:0 6px 18px rgba(0,0,0,.22);z-index:5}
.srow{display:flex;align-items:center;gap:10px}
.sic{width:36px;height:36px;border-radius:12px;background:var(--pc);display:flex;align-items:center;justify-content:center;flex:none}
.st{flex:1;display:flex;flex-direction:column}.st b{font:500 16px GSF}.st small{font:500 12px GSF;color:var(--pri)}
.ib{width:40px;height:40px;display:flex;align-items:center;justify-content:center;color:var(--onv)}
.tonal{background:var(--sc);color:var(--onsc);border-radius:20px;padding:9px 14px;font:500 14px GSF}
.txt{color:var(--pri);font:500 14px GSF;padding:9px 10px}
.dots{display:flex;gap:6px;margin:6px 0 0 46px}.dots i{width:8px;height:8px;border-radius:4px;background:var(--outv)}.dots i.d{background:var(--pri)}.dots i.n{width:10px;height:10px;border-radius:5px;background:color-mix(in srgb, var(--pri) 35%, transparent)}
.instr{font:12.5px GSF;color:var(--onv);margin:4px 0 0 46px}
.chips{display:flex;gap:6px;margin:8px 0 0 46px}.chip{background:var(--sc);color:var(--onsc);border-radius:14px;padding:3px 10px;display:flex;align-items:center}.chip small{font:11px GSF;opacity:.7}.chip em{font:15px CMI}
.pal{position:absolute;left:10px;right:10px;bottom:84px;background:var(--c);border-radius:28px;padding:10px;box-shadow:0 8px 24px rgba(0,0,0,.25);z-index:5}
.tabs{display:flex;gap:6px;overflow:hidden;white-space:nowrap}.tab{font:500 13.5px GSF;color:var(--onv);padding:8px 13px;border-radius:18px;flex:none}.tab.on{background:var(--sc);color:var(--onsc)}
.tiles{display:grid;grid-template-columns:repeat(4,1fr);gap:6px;margin-top:8px}
.tile{background:var(--low);border-radius:18px;padding:10px 4px;display:flex;flex-direction:column;align-items:center;gap:4px;font:11.5px/1.15 GSF;text-align:center;color:var(--on)}
.tile.on{background:var(--pri);color:var(--onpri)}
.rl{font:500 12px GSF;color:var(--onv);margin:8px 6px 6px}
.recent{display:flex;gap:6px}
.rnd{width:44px;height:44px;border-radius:22px;background:var(--chh);display:flex;align-items:center;justify-content:center}.rnd.on{background:var(--pri)}
.fold{display:flex;align-items:center;gap:6px}.grow{flex:1}
.toolsbtn{display:flex;align-items:center;gap:6px;background:var(--sc);color:var(--onsc);border-radius:22px;padding:10px 14px;font:500 14px GSF}
.bbar{position:absolute;left:12px;right:12px;bottom:14px;display:flex;justify-content:space-between;align-items:center}
.fab{width:56px;height:56px;border-radius:18px;background:var(--pc);color:var(--onpc);display:flex;align-items:center;justify-content:center;font:28px GSF;box-shadow:0 4px 12px rgba(0,0,0,.25)}
.tb{display:flex;background:var(--chh);border-radius:28px;padding:4px 6px}.tb span{width:44px;height:48px;display:flex;align-items:center;justify-content:center;color:var(--onv);font-size:20px}
.status-bar{height:32px;display:flex;justify-content:space-between;align-items:center;padding:0 26px;font:500 14px GSF}
.allicons{display:grid;grid-template-columns:repeat(7,170px);gap:10px}
.ic{background:#fcf9ee;border-radius:18px;padding:12px;display:flex;align-items:center;gap:10px;font:13px GSF;color:#1C1C16}
'''
allicons = ''.join(f'<div class="ic light" style="background:#fcf9ee">{icon(t, size=34)}<span>{t}</span></div>' for c in cats for t in cats[c])
html = f'''<!doctype html><html><head><meta charset="utf-8"><title>x</title><style>{style}{extra}</style></head><body>
<h2>Round 35: a better Construct</h2>
<p class="note">Construct is now a mode with two parts. A status card on top shows the tool, the step it's on (dots and the step's role), what to tap next, and the picks so far as chips, with Undo, Close (polygons) and Done. A tool palette at the bottom groups the tools as GeoGebra does, each with a drawn icon: what it makes in the accent color, what it's built from in ink. Picking a tool folds the palette to a slim bar (Move, recent tools, Tools), so the graph stays clear. While building, whatever the next tap can take glows softly, and each pick gives a little haptic tick.</p>
<div class="row">
<div>{phone("light", svg1, ov1, [])}<div class="cap">Just entered: the palette open on Lines, with recent tools under it. Move (in Points) is the default: drag points, nothing is built.</div></div>
<div>{phone("light", svg2, ov2, [])}<div class="cap">Tangent, step 2 of 2: A is picked (chip, ring), and the circle and the ellipse, the things that can be tapped now, glow. The palette has folded to a bar.</div></div>
<div>{phone("dark", svg3, ov3, [])}<div class="cap">Polygon in the dark theme: four corners so far, Close appears, and existing points glow as targets.</div></div>
</div>
<h2 style="margin-top:10px">The tool icons</h2>
<div class="allicons">{allicons}</div>
<script>document.fonts.ready.then(()=>document.title='ready')</script></body></html>'''
open(os.path.join(here, 'round35.html'), 'w').write(html)

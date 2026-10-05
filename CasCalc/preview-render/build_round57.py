# Builds round57.html: italic x z w keys, centred ∮, aligned b constants, Rad/Deg icons,
# the home screen shortcuts, and the Theme colormap following the UI color.
import re, math, cmath, base64, io
from icons_svg import key_icons, table_icons
from PIL import Image
K, T = key_icons(), table_icons()
data = open('../app/src/main/java/com/example/cas/graph/ColormapData.kt').read()
def cmap(name):
    h = re.search(r'ColormapData\("[^"]*", "' + name + r'", \w+, \w+, "([0-9a-f]+)"', data).group(1)
    return [tuple(int(h[i+j:i+j+2], 16) for j in (0, 2, 4)) for i in range(0, len(h), 6)]
def theme_map(primary):
    r, g, b = [int(primary[i:i+2], 16) / 255 for i in (1, 3, 5)]
    mx, mn = max(r, g, b), min(r, g, b); d = mx - mn
    if d < 0.08: return 'Greys'
    if mx == r: h = 60 * (((g - b) / d) % 6)
    elif mx == g: h = 60 * ((b - r) / d + 2)
    else: h = 60 * ((r - g) / d + 4)
    h %= 360
    for lim, n in ((18, 'Reds'), (40, 'Oranges'), (58, 'YlOrBr'), (95, 'YlGn'), (155, 'Greens'), (195, 'BuGn'), (245, 'Blues'), (290, 'Purples'), (340, 'RdPu')):
        if h < lim: return n
    return 'Reds'
def domain(name, W=220, H=220):
    s = cmap(name); n = len(s)
    im = Image.new('RGB', (W, H)); px = im.load()
    for yy in range(H):
        for xx in range(W):
            z = complex((xx / W - .5) * 5, (.5 - yy / H) * 5)
            try: w = (z * z - 1) / (z * z + 1)
            except ZeroDivisionError: px[xx, yy] = (255, 255, 255); continue
            m = abs(w)
            light = .5 + math.atan(math.log(m) / 1.5) / math.pi if m > 0 else 0
            if m > 0:
                bb = math.log(m) / math.log(2); light *= .82 + .18 * (bb - math.floor(bb))
            t = (cmath.phase(w) + math.pi) / (2 * math.pi)
            t = .14 + .86 * t
            x = t * (n - 1); k = min(int(x), n - 2); f = x - k
            c = [s[k][i] * (1 - f) + s[k + 1][i] * f for i in range(3)]
            c = [v / 255 for v in c]
            out = [v * 2 * light if light < .5 else v + (1 - v) * (2 * light - 1) for v in c]
            px[xx, yy] = tuple(int(max(0, min(1, v)) * 255) for v in out)
    buf = io.BytesIO(); im.save(buf, 'PNG'); return base64.b64encode(buf.getvalue()).decode()

def icon(svg, w, h, ink, acc):
    return f'<svg viewBox="0 0 {w} 24" width="{h * w / 24:.1f}" height="{h}" fill="none" stroke-linecap="round" stroke-linejoin="round" style="--ink:{ink};--acc:{acc};overflow:visible">{svg}</svg>'
def key(spoken, kw=84, kh=58, bg='#E5E3D6'):
    svg, w = K[spoken]; a = w / 24
    h = max(26, min(34, kh * .62, (kw - 12) / a))
    return f'<div class="key" style="width:{max(kw, h*a+16):.0f}px;height:{kh}px;background:{bg}">{icon(svg, w, h, "#1C1C16", "#5B6133")}</div>'

sec = []
sec.append('<div class="p"><h3>Letters in math italic, like the i</h3><div class="row">' + ''.join(key(k) for k in ['i, the imaginary unit', 'x', 'z', 'w']) +
           '</div><div class="row" style="margin-top:10px"><div class="key" style="width:84px;height:58px;background:#E2E4C4">' + icon(K['x'][0], 24, 34, '#1B1D0A', '#5B6133') + '</div><span class="cap">x on the number pad</span></div></div>')
sec.append('<div class="p"><h3>Loop integral: circle centred on the ∮</h3><div class="row">' + key('integral') + key('contour integral') + '</div></div>')
sec.append('<div class="p"><h3>Constants with b: all aligned, nothing cut off</h3><div class="row">' + ''.join(key(k) for k in ['Wien wavelength displacement law constant', 'Wien frequency displacement law constant', 'Wien entropy displacement law constant']) + '</div></div>')
def seg(sel):
    out = ''
    for i, (n, d) in enumerate((('Radians', 'Radians'), ('Degrees', 'Degrees'))):
        on = (i == 0) == sel
        r = '18px 0 0 18px' if i == 0 else '0 18px 18px 0'
        out += f'<div class="seg" style="border-radius:{r};background:{"#E2E4C4" if on else "transparent"}">{"✓ " if on else ""}{icon(T[n], 24, 22, "#1B1D0A" if on else "#1C1C16", "#5B6133")}</div>'
    return f'<div class="segs">{out}</div>'
sec.append('<div class="p"><h3>Rad / Deg as pictures: one radian of a circle, and an angle with °</h3><div class="row">' + seg(True) + seg(False) + '</div></div>')
sc = ''.join(f'<div class="sc"><div class="sci">{icon(T[n], 24, 40, "#3E4F3C", "#6E7D2A")}</div><span>{l}</span></div>' for n, l in (('Converter', 'Units'), ('Mode2D', '2D graph'), ('Mode3D', '3D graph'), ('ModeComplex', 'Complex')))
sec.append(f'<div class="p"><h3>Home screen shortcuts: the app\'s own icons</h3><div class="row" style="background:#3a4a5a;padding:18px;border-radius:20px;gap:22px">{sc}</div></div>')
th = ''
for prim, lab in (('#5B6133', 'olive (now)'), ('#3A6EA5', 'blue'), ('#386A20', 'green'), ('#6750A4', 'purple'), ('#9C4146', 'red'), ('#8B5000', 'orange')):
    n = theme_map(prim)
    th += f'<div class="th"><div class="dot" style="background:{prim}"></div><img src="data:image/png;base64,{domain(n)}"><span>{lab} UI → {n}</span></div>'
sec.append(f'<div class="p wide"><h3>Complex plane: the Theme colormap (default until you change your list) follows the UI color — f(z) = (z²−1)/(z²+1)</h3><div class="row" style="flex-wrap:wrap;gap:18px">{th}</div></div>')
html = f'''<!doctype html><html><head><meta charset="utf-8"><title>x</title><style>
@font-face{{font-family:GSF;src:url(../app/src/main/res/font/google_sans_flex.ttf)}}
body{{margin:0;background:#dcd8cf;font-family:GSF;padding:28px;width:1500px;display:flex;flex-wrap:wrap;gap:20px;align-items:flex-start}}
.p{{background:#F6F4E8;border-radius:28px;padding:18px 20px;color:#1C1C16}}.p.wide{{width:1460px}}h3{{font:600 17px GSF;margin:0 0 12px}}
.row{{display:flex;gap:10px;align-items:center}}.key{{border-radius:29px;display:flex;align-items:center;justify-content:center}}.cap{{font:14px GSF;color:#47473B}}
.segs{{display:flex;border:1px solid #77786A;border-radius:18px;height:36px}}.seg{{width:76px;display:flex;align-items:center;justify-content:center;font:14px GSF}}.seg+.seg{{border-left:1px solid #77786A}}
.sc{{display:flex;flex-direction:column;align-items:center;gap:6px;color:#fff;font:13px GSF}}.sci{{width:64px;height:64px;border-radius:32px;background:#F3ECDA;display:flex;align-items:center;justify-content:center}}
.th{{display:flex;flex-direction:column;gap:6px;font:14px GSF;position:relative}}.th img{{width:220px;height:220px;border-radius:16px}}.dot{{position:absolute;top:8px;left:8px;width:22px;height:22px;border-radius:11px;border:2px solid #fff}}
</style></head><body>{"".join(sec)}<script>document.fonts.ready.then(()=>document.title='ready')</script></body></html>'''
open('round57.html', 'w').write(html)

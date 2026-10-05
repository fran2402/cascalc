# Builds round58.html: the cleaner italic x, the history card (question as large as the answer),
# Rad/Deg back as words, and the Theme colormap with the new single-hue maps.
import math, re, base64, io
from icons_svg import key_icons
from PIL import Image
import types
r57 = types.SimpleNamespace()
_src = open('build_round57.py').read()
_ns = {}
exec(_src[:_src.index('def icon')], _ns)  # just its colormap helpers
r57.cmap, r57.domain = _ns['cmap'], _ns['domain']
K = key_icons()
def oklab(l, a, b):
    l_ = l + .3963377774*a + .2158037573*b; m_ = l - .1055613458*a - .0638541728*b; s_ = l - .0894841775*a - 1.2914855480*b
    l3, m3, s3 = l_**3, m_**3, s_**3
    return [4.0767416621*l3 - 3.3077115913*m3 + .2309699292*s3, -1.2684380046*l3 + 2.6097574011*m3 - .3413193965*s3, -.0041960863*l3 - .7034186147*m3 + 1.7076147010*s3]
def ramp(h, n=256):
    out = []
    for k in range(n):
        t = k / (n - 1); l = .97 - .67 * t; c = .17 * (t / .6 if t < .6 else 1 - .35 * (t - .6) / .4)
        while True:
            rgb = oklab(l, c * math.cos(math.radians(h)), c * math.sin(math.radians(h)))
            if all(-.0005 <= v <= 1.0005 for v in rgb) or c <= 0: break
            c -= .004
        enc = lambda v: round(255 * (12.92 * v if v <= .0031308 else 1.055 * max(v, 0) ** (1 / 2.4) - .055))
        out.append(tuple(max(0, min(255, enc(min(max(v, 0), 1)))) for v in rgb))
    return out
EXTRA = {'Yellows': 100, 'Teals': 195, 'Magentas': 325, 'Pinks': 355}
orig = _ns['cmap']
_ns['cmap'] = lambda n: ramp(EXTRA[n]) if n in EXTRA else orig(n)
def theme_map(p):
    r, g, b = [int(p[i:i+2], 16) / 255 for i in (1, 3, 5)]
    mx, mn = max(r, g, b), min(r, g, b); d = mx - mn
    if d < .08: return 'Greys'
    h = (60 * (((g - b) / d) % 6) if mx == r else 60 * ((b - r) / d + 2) if mx == g else 60 * ((r - g) / d + 4)) % 360
    for lim, n in ((15, 'Reds'), (40, 'Oranges'), (66, 'Yellows'), (95, 'YlGn'), (155, 'Greens'), (195, 'Teals'), (245, 'Blues'), (290, 'Purples'), (325, 'Magentas'), (345, 'Pinks')):
        if h < lim: return n
    return 'Reds'
th = ''
for prim, lab in (('#5B6133', 'olive (now)'), ('#6D5E0F', 'yellow'), ('#006A6A', 'teal'), ('#9A25AE', 'magenta'), ('#B4235E', 'pink'), ('#3A6EA5', 'blue')):
    n = theme_map(prim)
    th += f'<div class="th"><div class="dot" style="background:{prim}"></div><img src="data:image/png;base64,{r57.domain(n, 200, 200)}"><span>{lab} UI → {n}</span></div>'
def icon(svg, w, h):
    return f'<svg viewBox="0 0 {w} 24" width="{h*w/24}" height="{h}" fill="none" stroke-linecap="round" stroke-linejoin="round" style="--ink:#1C1C16;--acc:#5B6133">{svg}</svg>'
keys = ''.join(f'<div class="key">{icon(K[k][0], 24, 34)}</div>' for k in ['i, the imaginary unit', 'x', 'z', 'w'])
def card(new, focused):
    q = '<i>x</i><sup>2</sup> − 5<i>x</i> + 6 = 0'
    a = '<i>x</i> = 2, &nbsp;<i>x</i> = 3'
    qs, qc = (25 if focused else 23, '#1C1C16') if new else (19, '#47473B')
    asz = 26 if focused else 23
    pin = '<span class="fold">Algebra</span>' if new else ''
    top = '' if new else '<div style="margin-bottom:4px"><span class="fold">Algebra</span></div>'
    acts = '<div class="acts"><span class="act t">↵ Use</span><span class="act">☰ Steps</span><span style="flex:1"></span><span class="mi">⧉</span><span class="mi">⋮</span></div>' if focused else ''
    return (f'<div class="card" style="background:{"#EBE9DC" if focused else "#F1EEE2"}">{top}<div class="qrow"><span class="m" style="font-size:{qs}px;color:{qc}">{q}</span>{pin}</div>'
            f'<div class="arow"><span class="eq" style="font-size:{asz}px">⇔</span>&nbsp;<span class="m" style="font-size:{asz}px">{a}</span></div>{acts}</div>')
html = f'''<!doctype html><html><head><meta charset="utf-8"><title>x</title><style>
@font-face{{font-family:GSF;src:url(../app/src/main/res/font/google_sans_flex.ttf)}}
@font-face{{font-family:CMR;src:url(../app/src/main/res/font/cm_main.otf)}}
@font-face{{font-family:CMI;src:url(../app/src/main/res/font/cm_italic.otf)}}
body{{margin:0;background:#dcd8cf;font-family:GSF;padding:28px;width:1380px;display:flex;flex-wrap:wrap;gap:20px;align-items:flex-start}}
.p{{background:#F6F4E8;border-radius:28px;padding:18px 20px;color:#1C1C16}}h3{{font:600 17px GSF;margin:0 0 12px}}.l{{font:500 13px GSF;opacity:.7;margin:0 0 6px}}
.row{{display:flex;gap:10px;align-items:center}}.key{{width:84px;height:58px;border-radius:29px;background:#E5E3D6;display:flex;align-items:center;justify-content:center}}
.segs{{display:flex;border:1px solid #77786A;border-radius:18px;height:36px}}.seg{{width:76px;display:flex;align-items:center;justify-content:center;font:14px GSF}}.seg+.seg{{border-left:1px solid #77786A}}
.card{{width:400px;border-radius:24px;padding:14px 16px 14px 20px;margin-bottom:10px}}.qrow{{display:flex;align-items:center;gap:8px}}.qrow .m{{flex:1}}
.m{{font-family:CMR}}.m i{{font-family:CMI;font-style:normal}}.m sup{{font-size:65%}}.arow{{display:flex;justify-content:flex-end;align-items:center;margin-top:8px}}.eq{{color:#5B6133;font-family:CMR}}
.fold{{font:500 11px GSF;background:#E2E4C4;color:#1B1D0A;border-radius:10px;padding:2px 8px}}
.acts{{display:flex;gap:8px;align-items:center;margin-top:10px}}.act{{font:500 14px GSF;border:1px solid #C8C7B5;border-radius:14px;padding:9px 14px;color:#47473B}}.act.t{{background:#E2E4C4;border-color:#E2E4C4;color:#1B1D0A}}.mi{{width:34px;text-align:center;color:#47473B}}
.th{{display:flex;flex-direction:column;gap:6px;font:14px GSF;position:relative}}.th img{{width:200px;height:200px;border-radius:16px}}.dot{{position:absolute;top:8px;left:8px;width:22px;height:22px;border-radius:11px;border:2px solid #fff}}
</style></head><body>
<div class="p"><h3>Italic letters: x cleaned up</h3><div class="row">{keys}</div></div>
<div class="p"><h3>Rad / Deg: words again</h3><div class="segs"><div class="seg" style="background:#E2E4C4;border-radius:18px 0 0 18px">✓ Rad</div><div class="seg">Deg</div></div></div>
<div class="p"><h3>History card</h3><div class="row" style="align-items:flex-start;gap:24px"><div><p class="l">before</p>{card(False, True)}{card(False, False)}</div><div><p class="l">now: the question as large as the answer, marks at its end</p>{card(True, True)}{card(True, False)}</div></div></div>
<div class="p" style="width:1340px"><h3>Theme colormap with new single-hue maps (Yellows, Teals, Magentas, Pinks) for colors matplotlib doesn't cover</h3><div class="row" style="flex-wrap:wrap;gap:16px">{th}</div></div>
<script>document.fonts.ready.then(()=>document.title='ready')</script></body></html>'''
open('round58.html', 'w').write(html)

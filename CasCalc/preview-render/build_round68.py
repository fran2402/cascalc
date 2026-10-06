# Builds round68.html: frames of the launch animation (ui/LaunchAnimation.kt) at a few moments,
# the icon's axes growing from the middle, the hexagon traced, then the curve written.
import re
src = open('../app/src/main/java/com/example/cas/ui/LaunchAnimation.kt').read()
HEX = re.search(r'HEXAGON = "([^"]+)"', src).group(1)
CURVE = re.search(r'CURVE = "([^"]+)"', src).group(1)
AXES = ["M538.54,559.05L169.6,766.19", "M538.54,559.05L910.4,761.28", "M538.54,559.05L538.54,127.17"]
frames = [0.12, 0.3, 0.5, 0.7, 0.85, 1.0]
def ease(x):
    # FastOutSlowIn, roughly (cubic-bezier(0.4, 0, 0.2, 1)).
    x = max(0.0, min(1.0, x))
    lo, hi = 0.0, 1.0
    for _ in range(40):
        m = (lo + hi) / 2
        bx = 3 * (1 - m) ** 2 * m * 0.4 + 3 * (1 - m) * m * m * 0.2 + m ** 3
        if bx < x: lo = m
        else: hi = m
    m = (lo + hi) / 2
    return 3 * (1 - m) * m * m * 1 + m ** 3
def phase(t, a, b): return ease((t - a) / (b - a))
cells = ''
for t in frames:
    parts = ''.join(f'<path d="{d}" stroke="#7F886B" stroke-width="45" stroke-linecap="round" fill="none" data-f="{phase(t, 0, 0.38)}"/>' for d in AXES)
    parts += f'<path d="{HEX}" stroke="#F3ECDA" stroke-width="55" stroke-linecap="round" fill="none" data-f="{phase(t, 0.18, 0.72)}"/>'
    parts += f'<path d="{CURVE}" stroke="#F3ECDA" stroke-width="38" stroke-linecap="round" fill="none" data-f="{phase(t, 0.45, 1.0)}"/>'
    s = 0.94 + 0.06 * ease(t)
    cells += (f'<div class="f"><svg viewBox="0 0 1080 1080" width="200" height="200" style="transform:scale({s})">{parts}</svg>'
              f'<span>{int(t * 1250)} ms</span></div>')
html = f'''<!doctype html><html><head><meta charset="utf-8"><title>x</title><style>
@font-face{{font-family:GSF;src:url(../app/src/main/res/font/google_sans_flex.ttf)}}
body{{margin:0;background:#dcd8cf;font-family:GSF;padding:24px;display:flex;gap:14px}}
.f{{background:#3E4F3C;border-radius:24px;width:220px;height:300px;display:flex;flex-direction:column;align-items:center;justify-content:center;gap:16px}}
span{{color:#F3ECDA;font-size:15px}}
</style></head><body>{cells}<script>
document.querySelectorAll('path').forEach(p=>{{const L=p.getTotalLength(),f=+p.dataset.f;p.style.strokeDasharray=L+' '+L;p.style.strokeDashoffset=L*(1-f);if(f<=0)p.style.visibility='hidden'}});
document.fonts.ready.then(()=>document.title='ready')</script></body></html>'''
open('round68.html', 'w').write(html)

# Builds round55.html: one-tone icons on tertiary, bigger key symbols, gesture hands, the
# acknowledgements rosette, raised m-constants, capital Φ, and ⊤ / upright H in the editor.
from icons_svg import key_icons, table_icons
K, T = key_icons(), table_icons()
LIGHT = dict(bg='#FCF9EE', on='#1C1C16', onv='#47473B', low='#F6F4E8', ch='#EBE9DC', chh='#E5E3D6', pri='#5B6133',
             sc='#E2E4C4', onsc='#1B1D0A', tc='#BEECDB', ontc='#002019', ter='#3C6A5A', inv='#C5CB86', eq='#5B6133', oneq='#fff')
DARK = dict(bg='#12130C', on='#E5E3D6', onv='#C8C7B5', low='#1B1C14', ch='#292A22', chh='#34352C', pri='#C5CB86',
            sc='#45483A', onsc='#E2E4C4', tc='#214E41', ontc='#BEECDB', ter='#A2D0BF', inv='#5B6133', eq='#C5CB86', oneq='#2F3300')

def icon(svg, w, h, ink, acc):
    return f'<svg viewBox="0 0 {w} 24" width="{h * w / 24:.1f}" height="{h}" fill="none" stroke-linecap="round" stroke-linejoin="round" style="--ink:{ink};--acc:{acc}">{svg}</svg>'

def key(spoken, c, bg=None, fg=None, acc=None, kw=84, kh=58, old=False):
    svg, w = K[spoken]
    aspect = w / 24
    h = 26 if old else max(26, min(34, kh * 0.62, (kw - 12) / aspect))
    bg = bg or c['chh']; fg = fg or c['on']; acc = acc or c['pri']
    return f'<div class="key" style="width:{kw}px;height:{kh}px;background:{bg}">{icon(svg, w, h, fg, acc)}</div>'

def phone_keys(c, old):
    ks = ['pi', 'e', 'square root', 'nth root', 'sin', 'inverse hyperbolic cos', 'natural log', 'factorial',
          'inverse', 'transpose', 'conjugate transpose', 'inverse normal', 'Electron mass', 'Proton mass', 'Muon mass', 'list of constants with names']
    ks = [k for k in ks if k in K]
    out = ''.join(key(k, c, old=old) for k in ks)
    return f'<div class="grid">{out}</div>'

def section(c, cls):
    p = []
    p.append(f'<div class="panel {cls}" style="background:{c["low"]};color:{c["on"]}"><h3>Key symbols: before (26dp) and now (as big as the key allows)</h3>'
             f'<div class="pair"><div><div class="lab">before</div>{phone_keys(c, True)}</div><div><div class="lab">now</div>{phone_keys(c, False)}</div></div></div>')
    # Tertiary: defined keys draw one tone.
    defined = ''.join(key(k, c, bg=c['tc'], fg=c['ontc'], acc=c['ontc']) for k in ['pi', 'Electron mass', 'Bohr magneton', 'Proton mass'] if k in K)
    normal = ''.join(key(k, c) for k in ['pi', 'Electron mass', 'Bohr magneton', 'Proton mass'] if k in K)
    p.append(f'<div class="panel {cls}" style="background:{c["low"]};color:{c["on"]}"><h3>On tertiary (a defined symbol): one tone</h3>'
             f'<div class="pair"><div><div class="lab">usual</div><div class="grid">{normal}</div></div><div><div class="lab">defined (tertiary)</div><div class="grid">{defined}</div></div></div></div>')
    # Constants with m, raised and centred.
    ms = [k for k in K if k.endswith(' mass') and k[0].isupper()][:8] + [k for k in ['Bohr magneton', 'Nuclear magneton', 'Proton-to-electron mass ratio'] if k in K]
    p.append(f'<div class="panel {cls}" style="background:{c["low"]};color:{c["on"]}"><h3>Masses and magnetons: centred on the m / μ, tails inside</h3>'
             f'<div class="grid wide">{"".join(key(k, c) for k in ms)}</div></div>')
    # Φ.
    p.append(f'<div class="panel {cls}" style="background:{c["low"]};color:{c["on"]}"><h3>Normal distribution: φ (density), Φ (distribution), Φ⁻¹ (inverse)</h3>'
             f'<div class="grid">{"".join(key(k, c) for k in ["normal density", "normal distribution function", "inverse normal", "inverse", "transpose", "conjugate transpose"])}</div></div>')
    # Gesture tips.
    def tip(name, title, text, tertiary):
        bg, fg = (c['tc'], c['ontc']) if tertiary else (c['sc'], c['onsc'])
        acc = fg if tertiary else c['pri']
        return (f'<div class="tip"><div class="tb" style="background:{bg}">{icon(T[name], 24, 22, fg, acc)}</div>'
                f'<div><b>{title}</b><br><span style="color:{c["onv"]}">{text}</span></div></div>')
    g = (tip('Tap', 'Tap a curve', 'Its point: zeros, extrema and crossings', False)
         + tip('GestureHold', 'Hold a line’s dot', 'Color, line style, points, arrows', False)
         + tip('GestureSwipe', 'Swipe a line', 'Deletes it', False)
         + tip('GestureDrag', 'Drag', 'Moves the view; pinch to zoom', False)
         + tip('Folder', 'Folders and notes', '＋ adds lines, notes and folders (tertiary: one tone)', True)
         + tip('Share', 'Export', 'PNG, JPG, SVG or PDF (tertiary: one tone)', True))
    p.append(f'<div class="panel {cls}" style="background:{c["low"]};color:{c["on"]}"><h3>How-to: gesture hands</h3>{g}</div>')
    # Menu.
    m = ''.join(f'<div class="mi">{icon(T[n], 24, 22, c["onv"], c["pri"])}<span>{t}</span></div>' for n, t in [('Settings', 'Settings'), ('Thanks', 'Acknowledgements'), ('ClearHistory', 'Clear history')])
    p.append(f'<div class="panel {cls}" style="background:{c["low"]};color:{c["on"]}"><h3>Acknowledgements: a rosette, not a heart</h3><div class="menu" style="background:{c["ch"]}">{m}</div></div>')
    # Editor.
    eq = ('<span class="cm"><i>A</i><sup class="up">⊤</sup> = <span class="br">[</span>1&nbsp;&nbsp;3<span class="br">]</span>'
          '&emsp;&emsp;<i>B</i><sup class="up">H</sup>&emsp;&emsp;<span style="opacity:.55">was <i>A</i><sup>T</sup>, <i>B</i><sup><i>H</i></sup></span></span>')
    p.append(f'<div class="panel {cls}" style="background:{c["low"]};color:{c["on"]}"><h3>In the equation: ⊤ for transpose, upright H</h3><div class="eq">{eq}</div></div>')
    return ''.join(p)

html = f'''<!doctype html><html><head><meta charset="utf-8"><title>x</title><style>
@font-face{{font-family:GSF;src:url(../app/src/main/res/font/google_sans_flex.ttf);font-weight:1 1000}}
@font-face{{font-family:CMR;src:url(../app/src/main/res/font/cm_main.otf)}}
@font-face{{font-family:CMI;src:url(../app/src/main/res/font/cm_italic.otf)}}
@font-face{{font-family:NCMM;src:url(../app/src/main/res/font/ncm_math.otf)}}
body{{margin:0;background:#dcd8cf;font-family:GSF;padding:32px}}
.cols{{display:flex;gap:28px;align-items:flex-start}}.col{{display:flex;flex-direction:column;gap:20px;width:800px}}
.panel{{border-radius:28px;padding:18px 20px}}h3{{font:600 17px GSF;margin:0 0 12px}}
.grid{{display:grid;grid-template-columns:repeat(4,84px);gap:8px}}.grid.wide{{grid-template-columns:repeat(6,84px)}}
.pair{{display:flex;gap:28px}}.lab{{font:500 13px GSF;opacity:.7;margin-bottom:6px}}
.key{{border-radius:29px;display:flex;align-items:center;justify-content:center;overflow:visible}}
.tip{{display:flex;gap:14px;align-items:center;margin:8px 0;font:15px/1.35 GSF}}.tb{{width:40px;height:40px;border-radius:20px;display:flex;align-items:center;justify-content:center;flex:none}}
.menu{{width:280px;border-radius:16px;padding:6px 0}}.mi{{display:flex;gap:14px;align-items:center;padding:10px 16px;font:15px GSF}}
.eq{{font:30px CMR;padding:8px 4px}}.eq i{{font-family:CMI;font-style:normal}}.eq sup{{font-size:20px}}.up{{font-family:NCMM,CMR}}.br{{font-size:36px}}
</style></head><body><div class="cols"><div class="col">{section(LIGHT, "light")}</div><div class="col">{section(DARK, "dark")}</div></div>
<script>document.fonts.ready.then(()=>document.title='ready')</script></body></html>'''
open('round55.html', 'w').write(html)

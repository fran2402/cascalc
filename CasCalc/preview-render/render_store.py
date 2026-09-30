# Renders store/*.svg to the PNGs Google Play asks for (fonts from the app's res/font).
import os
from playwright.sync_api import sync_playwright
here = os.path.dirname(os.path.abspath(__file__)); store = os.path.join(os.path.dirname(here), 'store')
font = 'file://' + os.path.join(os.path.dirname(here), 'app/src/main/res/font/google_sans_flex.ttf')
jobs = [('icon-512', 512, 512), ('feature-graphic', 1024, 500), ('feature-graphic-plain', 1024, 500)]
with sync_playwright() as p:
    b = p.chromium.launch(executable_path='/opt/pw-browsers/chromium-1194/chrome-linux/chrome')
    for name, w, h in jobs:
        pg = b.new_page(viewport={'width': w, 'height': h}, device_scale_factor=1)
        svg = open(os.path.join(store, name + '.svg')).read()
        page = os.path.join(here, '_store_render.html')
        open(page, 'w').write(f'<html><head><meta charset="utf-8"><style>@font-face{{font-family:GSF;src:url({font});font-weight:1 1000}}html,body{{margin:0}}</style></head><body>{svg}</body></html>')
        pg.goto('file://' + page)
        pg.evaluate("document.fonts.load('650 40px GSF','CasCalc')"); pg.wait_for_timeout(500)
        pg.screenshot(path=os.path.join(store, name + '.png'), clip={'x': 0, 'y': 0, 'width': w, 'height': h}, omit_background=False)
        pg.close()
    b.close()
os.remove(os.path.join(here, '_store_render.html'))

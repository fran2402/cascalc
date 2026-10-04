# Renders round54.html to round54.png.
import os
from playwright.sync_api import sync_playwright
here=os.path.dirname(os.path.abspath(__file__))
with sync_playwright() as p:
    b=p.chromium.launch(executable_path='/opt/pw-browsers/chromium-1194/chrome-linux/chrome')
    pg=b.new_page(viewport={'width': 1560,"height":1200},device_scale_factor=2)
    pg.on('pageerror',lambda e:print('JS error:',e))
    pg.goto('file://'+here+'/round54.html'); pg.wait_for_function("document.title=='ready'",timeout=15000); pg.wait_for_timeout(500)
    pg.screenshot(path=os.environ.get('OUT',here+'/round54.png'),full_page=True)
    b.close()

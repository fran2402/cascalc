# Renders round20.html to round20.png. OLD_FONTS: a folder with the fonts before the weight change (for the comparison).
import os
from playwright.sync_api import sync_playwright
here=os.path.dirname(os.path.abspath(__file__))
html=open(here+'/round20.html').read().replace('OLD_FONTS','file://'+os.environ.get('OLD_FONTS','../app/src/main/res/font'))
tmp=here+'/_round20_render.html'; open(tmp,'w').write(html)
with sync_playwright() as p:
    b=p.chromium.launch(executable_path='/opt/pw-browsers/chromium-1194/chrome-linux/chrome')
    pg=b.new_page(viewport={'width':1800,'height':1200},device_scale_factor=1)
    pg.on('pageerror',lambda e:print('JS error:',e))
    pg.goto('file://'+tmp); pg.wait_for_function("document.title=='ready'",timeout=15000); pg.wait_for_timeout(700)
    pg.screenshot(path=os.environ.get('OUT',here+'/round20.png'),full_page=True)
    b.close()
os.remove(tmp)

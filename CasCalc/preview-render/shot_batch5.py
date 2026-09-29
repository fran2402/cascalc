from playwright.sync_api import sync_playwright
with sync_playwright() as p:
    b=p.chromium.launch(); pg=b.new_page(viewport={'width':1600,'height':2000},device_scale_factor=2)
    pg.goto('file:///home/claude/casshot/batch5.html'); pg.wait_for_function("document.title=='ready'"); pg.wait_for_timeout(400)
    pg.screenshot(path='/home/claude/casshot/batch5.png')
    for i in range(0):
        pg.locator('.phone').nth(i).screenshot(path=f'/home/claude/casshot/phone{i+1}.png')
    b.close()

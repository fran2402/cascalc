from playwright.sync_api import sync_playwright
with sync_playwright() as p:
    b=p.chromium.launch(); pg=b.new_page(viewport={'width':1960,'height':2100},device_scale_factor=2)
    pg.on('pageerror', lambda e: print('ERR', e))
    pg.goto('file:///home/claude/casshot/letters.html'); pg.wait_for_function("document.title=='ready'"); pg.wait_for_timeout(500)
    pg.screenshot(path='/home/claude/casshot/letters.png', full_page=True)
    n=pg.locator('.phone').count()
    for i in range(n):
        pg.locator('.phone').nth(i).screenshot(path=f'/home/claude/casshot/letters{i}.png')
    print(n)
    b.close()

# Run in a folder holding the subset New Computer Modern Mono fonts (sub_Regular.otf, sub_Italic.otf,
# sub_Bold.otf, made with pyftsubset from WebCM Mono 10); writes *_sup.otf, which become
# app/src/main/res/font/ncm_mono.otf, ncm_mono_italic.otf and ncm_mono_bold.otf.
# Superscript digits (⁰ ⁴–⁹), ⁺ and ⁻ for the typewriter: each font's own digits put through the
# same scale and shift that turns its 2 into its ² (so they match ¹ ² ³), keeping the fixed width.
from fontTools.ttLib import TTFont
from fontTools.pens.boundsPen import BoundsPen
from fontTools.pens.ttGlyphPen import TTGlyphPen
from fontTools.pens.transformPen import TransformPen
NEW = {0x2070: 0x30, 0x2074: 0x34, 0x2075: 0x35, 0x2076: 0x36, 0x2077: 0x37, 0x2078: 0x38, 0x2079: 0x39, 0x207A: 0x2B, 0x207B: 0x2212}
for f in ['sub_Regular', 'sub_Italic', 'sub_Bold']:
    t = TTFont(f + '.otf'); gs = t.getGlyphSet(); cm = t.getBestCmap()
    def box(g): p = BoundsPen(gs); gs[g].draw(p); return p.bounds
    a = box(cm[0x32]); b = box(cm[0xB2])
    s = (b[3] - b[1]) / (a[3] - a[1])
    # x: keep the centre (2's centre maps to ²'s); y: 2's baseline maps to ²'s bottom.
    dx = (b[0] + b[2]) / 2 - s * (a[0] + a[2]) / 2
    dy = b[1] - s * a[1]
    glyf = t['glyf']; hmtx = t['hmtx']; order = t.getGlyphOrder()
    for u, src in NEW.items():
        name = 'uni%04X' % u
        pen = TTGlyphPen(gs)
        gs[cm[src]].draw(TransformPen(pen, (s, 0, 0, s, dx, dy)))
        g = pen.glyph(); glyf[name] = g
        g.recalcBounds(glyf)
        hmtx[name] = (hmtx[cm[src]][0], g.xMin if hasattr(g, 'xMin') else 0)
        if name not in order: order.append(name)
        for table in t['cmap'].tables:
            if table.isUnicode(): table.cmap[u] = name
    t.setGlyphOrder(order)
    t.save(f + '_sup.otf')
    print(f, round(s, 3), round(dx), round(dy))

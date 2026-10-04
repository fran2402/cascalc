# Builds the data table's typewriter fonts from New Computer Modern Mono subsets.
# Run in a folder holding sub_Regular.otf, sub_Italic.otf and sub_Bold.otf (pyftsubset of WebCM Mono 10);
# writes *_sup.otf, which become app/src/main/res/font/ncm_mono.otf, ncm_mono_italic.otf, ncm_mono_bold.otf.
#  - The bold is thickened further (outlines grown by BOLDER units, in place, keeping the fixed width).
#  - Superscript digits ⁰ ⁴–⁹, ⁺ and ⁻ are made from each font's own digits through the same scale and
#    shift that turns its 2 into its ², so they match ¹ ² ³.
#  - All superscripts get a narrower advance (SUP_WIDTH), centred, so an exponent's digits sit close.
import pathops
from fontTools.ttLib import TTFont
from fontTools.pens.boundsPen import BoundsPen
from fontTools.pens.ttGlyphPen import TTGlyphPen
from fontTools.pens.transformPen import TransformPen
from fontTools.pens.cu2quPen import Cu2QuPen
from fontTools.pens.recordingPen import RecordingPen

BOLDER = 16
SUP_WIDTH = 330
NEW = {0x2070: 0x30, 0x2074: 0x34, 0x2075: 0x35, 0x2076: 0x36, 0x2077: 0x37, 0x2078: 0x38, 0x2079: 0x39, 0x207A: 0x2B, 0x207B: 0x2212}
OLD = [0xB9, 0xB2, 0xB3]

def thicken(t, d):
    gs = t.getGlyphSet(); glyf = t['glyf']; hmtx = t['hmtx']
    for name in t.getGlyphOrder():
        path = pathops.Path(); gs[name].draw(path.getPen(glyphSet=gs))
        if path.bounds == (0, 0, 0, 0): continue
        stroke = pathops.Path(path)
        stroke.stroke(2 * d, pathops.LineCap.BUTT_CAP, pathops.LineJoin.ROUND_JOIN, 4)
        stroke.convertConicsToQuads()
        path = pathops.op(path, stroke, pathops.PathOp.UNION, fix_winding=True)
        pen = TTGlyphPen(None); path.draw(Cu2QuPen(pen, max_err=0.5, reverse_direction=False))
        g = pen.glyph(); glyf[name] = g; g.recalcBounds(glyf)
        hmtx[name] = (hmtx[name][0], getattr(g, 'xMin', 0))

def build(f, bold):
    t = TTFont(f + '.otf')
    gs = t.getGlyphSet(); cm = t.getBestCmap()
    def box(g): p = BoundsPen(gs); gs[g].draw(p); return p.bounds
    a = box(cm[0x32]); b = box(cm[0xB2])
    s = (b[3] - b[1]) / (a[3] - a[1])
    dx = (b[0] + b[2]) / 2 - s * (a[0] + a[2]) / 2
    dy = b[1] - s * a[1]
    glyf = t['glyf']; hmtx = t['hmtx']; order = t.getGlyphOrder()
    # Each source outline recorded first, so ¹ ² ³ can be redrawn from themselves.
    def recorded(g): r = RecordingPen(); gs[g].draw(r); return r
    sources = {u: recorded(cm[u]) for u in OLD}
    sources.update({u: recorded(cm[src]) for u, src in NEW.items()})
    def outline(rec, m):
        pen = TTGlyphPen(None); rec.replay(TransformPen(pen, m)); g = pen.glyph(); g.recalcBounds(glyf); return g
    for u, rec in sources.items():
        m = (1, 0, 0, 1, 0, 0) if u in OLD else (s, 0, 0, s, dx, dy)
        g = outline(rec, m)
        # Centre the ink in the narrower advance.
        shift = SUP_WIDTH / 2 - (g.xMin + g.xMax) / 2
        g = outline(rec, (m[0], m[1], m[2], m[3], m[4] + shift, m[5]))
        name = cm[u] if u in OLD else 'uni%04X' % u
        glyf[name] = g; hmtx[name] = (SUP_WIDTH, g.xMin)
        if name not in order: order.append(name)
        for table in t['cmap'].tables:
            if table.isUnicode(): table.cmap[u] = name
    t.setGlyphOrder(order)
    # Thickened last, so the superscripts thicken just as much as everything else.
    if bold: thicken(t, BOLDER)
    t.save(f + '_sup.otf')

for f in ['sub_Regular', 'sub_Italic', 'sub_Bold']: build(f, f == 'sub_Bold')

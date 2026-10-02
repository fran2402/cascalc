"""Thickens a TrueType font's outlines by d font units on each side (as FreeType's embolden does),
so New Computer Modern's subsets match the weight of the app's Computer Modern."""
import sys, pathops
from fontTools.ttLib import TTFont
from fontTools.pens.ttGlyphPen import TTGlyphPen
from fontTools.pens.cu2quPen import Cu2QuPen

def embolden(src, dst, d):
    f = TTFont(src)
    gs = f.getGlyphSet()
    glyf = f['glyf']; hmtx = f['hmtx']
    new = {}
    for name in f.getGlyphOrder():
        g = gs[name]
        path = pathops.Path()
        g.draw(path.getPen(glyphSet=gs))
        if d > 0 and not path.bounds == (0, 0, 0, 0):
            stroke = pathops.Path(path)
            stroke.stroke(2 * d, pathops.LineCap.BUTT_CAP, pathops.LineJoin.ROUND_JOIN, 4)
            stroke.convertConicsToQuads()
            path = pathops.op(path, stroke, pathops.PathOp.UNION, fix_winding=True)
        pen = TTGlyphPen(None)
        path.draw(Cu2QuPen(pen, max_err=0.5, reverse_direction=False))
        new[name] = pen.glyph()
    for name, g in new.items():
        glyf[name] = g
        adv, lsb = hmtx[name]
        # Wider by the added ink, so letters don't touch; shift right by d to keep the side bearings.
        if d > 0 and g.numberOfContours != 0:
            for i, (x, y) in enumerate(g.coordinates):
                g.coordinates[i] = (x + d, y)
            g.recalcBounds(glyf)
            hmtx[name] = (adv + 2 * d, g.xMin)
        else:
            if hasattr(g, 'recalcBounds'): g.recalcBounds(glyf)
            hmtx[name] = (adv + (2 * d if d > 0 else 0), getattr(g, 'xMin', 0))
    f['head'].glyphDataFormat = 0
    f.save(dst)

if __name__ == '__main__':
    embolden(sys.argv[1], sys.argv[2], float(sys.argv[3]))

"""Regenerates app/src/main/java/com/example/cas/ui/theme/FontCoverage.kt from the bundled fonts."""
from fontTools.ttLib import TTFont
import os
here = os.path.dirname(os.path.abspath(__file__))
res = os.path.join(here, '..', 'app', 'src', 'main', 'res', 'font')
fonts = {'GOOGLE_SANS_FLEX': 'google_sans_flex.ttf', 'ROBOTO': 'roboto.ttf', 'CM_ROMAN': 'cm_main.otf', 'CM_ITALIC': 'cm_italic.otf'}
for name, f in fonts.items():
    cmap = TTFont(os.path.join(res, f)).getBestCmap()
    print(name, len(cmap))

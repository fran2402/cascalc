"""Reads the app's two-tone icons (ui/TableIcons.kt, ui/KeyIcons.kt) into SVG strings, ink and accent
paths colored var(--ink) and var(--acc), so the mockups draw exactly what the app does."""
import re
UI = '../app/src/main/java/com/example/cas/ui/'

def ring(x, y, r, ry=None):
    ry = r if ry is None else ry
    return f"M{x-r} {y}a{r} {ry} 0 1 0 {2*r} 0a{r} {ry} 0 1 0 {-2*r} 0z"

def oval(x, y, rx, ry): return f"M{x-rx} {y}a{rx} {ry} 0 1 0 {2*rx} 0a{rx} {ry} 0 1 0 {-2*rx} 0z"

def _args(body, key, consts={}):
    m = re.search(r'\b' + key + r' = ', body)
    if not m: return []
    i = m.end(); depth = 0; j = i
    while j < len(body):
        c = body[j]
        if c == '(': depth += 1
        elif c == ')':
            if depth == 0: break
            depth -= 1
            if depth == 0 and body[j+1:j+2] in (',', ')', ' '): j += 1; break
        elif c == ',' and depth == 0: break
        j += 1
    expr = body[i:j]
    out = []
    for name in re.findall(r'\b(axes)\b', expr): out += consts.get(name, [])
    for mm in re.finditer(r'"([^"]*)"|(?:oval|ring)\(([^)]*)\)', expr):
        if mm.group(1) is not None: out.append(mm.group(1))
        else:
            nums = [float(v.strip().rstrip('f')) for v in mm.group(2).split(',')]
            out.append((oval if mm.group(0).startswith('oval') else ring)(*nums))
    return out

def _svg(stroke, thin, fill, shade, accent):
    col = lambda p: 'var(--acc)' if p in accent else 'var(--ink)'
    s = ''.join(f'<path d="{p}" fill="{col(p)}" fill-opacity=".35" stroke="none"/>' for p in shade)
    s += ''.join(f'<path d="{p}" stroke="{col(p)}" stroke-width="2"/>' for p in stroke)
    s += ''.join(f'<path d="{p}" stroke="{col(p)}" stroke-width="1.6"/>' for p in thin)
    s += ''.join(f'<path d="{p}" fill="{col(p)}" stroke="none"/>' for p in fill)
    return s

def table_icons():
    out = {}
    for line in open(UI + 'TableIcons.kt').read().split('\n'):
        m = re.match(r'\s*val (\w+): ImageVector by lazy \{ tableIcon\("\w+", (.*)$', line)
        if not m: continue
        b = m.group(2)
        out[m.group(1)] = _svg(_args(b, 'stroke'), _args(b, 'thin'), _args(b, 'fill'), _args(b, 'shade'), set(_args(b, 'accent')))
    return out

def key_icons():
    src = open(UI + 'KeyIcons.kt').read()
    consts = {'axes': ["M3 20.5h18", "M3.5 3v18"]}
    defs = {}
    for m in re.finditer(r'private val (\w+) = key\("\w+", (.*?)\)\n', src, re.S):
        defs[m.group(1)] = m.group(2)
    out = {}
    for m in re.finditer(r'"([^"]+)" to (key\("\w+", (.*?)\)),?\n|"([^"]+)" to (\w+),\n', src):
        if m.group(1): spoken, b = m.group(1), m.group(3)
        else: spoken, b = m.group(4), defs[m.group(5)]
        g = lambda k: _args(b, k, consts)
        ink, acc, it, at, inf, af, ash = g('ink'), g('accent'), g('inkThin'), g('accentThin'), g('inkFill'), g('accentFill'), g('accentShade')
        out[spoken] = _svg(ink + acc, it + at, inf + af, ash, set(acc + at + af + ash))
    return out

if __name__ == '__main__':
    t, k = table_icons(), key_icons(); print(len(t), len(k))

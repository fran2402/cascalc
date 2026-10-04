"""Builds the letter-based key icons (trigonometry, linear algebra) for ui/KeyIcons.kt from one set
of stroked letters with a shared baseline (19.5) and x-height (10), so every word sits alike.
Prints Kotlin map entries; run and paste between the markers in KeyIcons.kt."""
def f(v): return ('%.2f' % v).rstrip('0').rstrip('.')
def ring(x, y, rx, ry=None):
    ry = rx if ry is None else ry
    return f"M{f(x-rx)} {f(y)}a{f(rx)} {f(ry)} 0 1 0 {f(2*rx)} 0a{f(rx)} {f(ry)} 0 1 0 {f(-2*rx)} 0z"
# Each letter: (width, function x -> (strokes, fills)).
L = {
 'a': (5, lambda x: ([ring(x+2.5, 14.75, 2.5, 5), f"M{f(x+5)} 10v9.5"], [])),
 's': (5.6, lambda x: ([f"M{f(x+5.4)} 11.5c-.6-1.1-1.5-1.8-2.6-1.8c-1.4 0-2.4.9-2.4 2.2c0 3 5.2 1.8 5.2 5c0 1.6-1.1 2.8-2.8 2.8c-1.2 0-2.3-.6-2.9-1.8"], [])),
 'i': (1.6, lambda x: ([f"M{f(x+.8)} 10v9.5"], [ring(x+.8, 6.4, 1.25)])),
 'n': (5.4, lambda x: ([f"M{f(x)} 19.5V10", f"M{f(x)} 12.6c.9-1.7 2-2.6 3.1-2.6c1.5 0 2.3 1.1 2.3 3v6.5"], [])),
 'h': (5.4, lambda x: ([f"M{f(x)} 4.5v15", f"M{f(x)} 12.6c.9-1.7 2-2.6 3.1-2.6c1.5 0 2.3 1.1 2.3 3v6.5"], [])),
 'c': (4.6, lambda x: ([f"M{f(x+4.6)} 11.6c-.6-1.1-1.5-1.8-2.5-1.8c-1.5 0-2.5 2.2-2.5 5s1 5 2.5 5c1 0 1.9-.6 2.5-1.8"], [])),
 'o': (5, lambda x: ([ring(x+2.5, 14.75, 2.5, 5)], [])),
 't': (4.4, lambda x: ([f"M{f(x+1.5)} 5.5v11.8c0 1.4.6 2.2 1.7 2.2c.5 0 1-.2 1.3-.5", f"M{f(x)} 10h4.2"], [])),
 'd': (5, lambda x: ([ring(x+2.5, 14.75, 2.5, 5), f"M{f(x+5)} 4.5v15"], [])),
 'e': (5, lambda x: ([f"M{f(x)} 15h5c0-3.3-1.1-5.3-2.6-5.3s-2.4 2.3-2.4 5.1s1 5.1 2.6 5.1c1 0 1.8-.6 2.4-1.6"], [])),
 'r': (3.4, lambda x: ([f"M{f(x)} 19.5V10", f"M{f(x)} 13.2c.7-2 1.8-3.2 3.4-3.2"], [])),
 'f': (4, lambda x: ([f"M{f(x+4)} 5.1c-1.6-.6-2.8.2-2.8 2.2v12.2", f"M{f(x)} 10h3.6"], [])),
 'k': (4.6, lambda x: ([f"M{f(x)} 4.5v15", f"M{f(x+4.4)} 10l-4.4 4.8", f"M{f(x+1.5)} 13.2l3.1 6.3"], [])),
 'p': (5, lambda x: ([f"M{f(x)} 10v12.5", ring(x+2.5, 14.75, 2.5, 5)], [])),
 'g': (5, lambda x: ([ring(x+2.5, 14.75, 2.5, 5), f"M{f(x+5)} 10v10.6c0 2-1.2 3-2.7 3c-1 0-1.8-.4-2.3-1.1"], [])),
 'l': (2.6, lambda x: ([f"M{f(x+.8)} 4.5v12.8c0 1.4.6 2.2 1.8 2.2"], [])),
 'm': (9, lambda x: ([f"M{f(x)} 19.5V10", f"M{f(x)} 12.4c.7-1.5 1.5-2.4 2.5-2.4c1.2 0 2 .9 2 2.6v6.9", f"M{f(x+4.5)} 12.6c.7-1.6 1.5-2.6 2.5-2.6c1.2 0 2 .9 2 2.6v6.9"], [])),
 'A': (7, lambda x: ([f"M{f(x)} 19.5L{f(x+3.5)} 4.5L{f(x+7)} 19.5", f"M{f(x+1.3)} 14.3h4.4"], [])),
 'R': (6.6, lambda x: ([f"M{f(x)} 19.5V4.5h3.2a3.6 3.6 0 0 1 0 7.2H{f(x)}", f"M{f(x+3)} 11.7l3.6 7.8"], [])),
 'I': (4.4, lambda x: ([f"M{f(x+2.2)} 4.5v15", f"M{f(x)} 4.5h4.4", f"M{f(x)} 19.5h4.4"], [])),
 '!': (1.8, lambda x: ([f"M{f(x+.9)} 4.5v10.5"], [ring(x+.9, 18.9, 1.45)])),
 'x': (5, lambda x: ([f"M{f(x)} 10l5 9.5", f"M{f(x+5)} 10l-5 9.5"], [])),
 'λ': (6.4, lambda x: ([f"M{f(x)} 19.5l3.5-6.6", f"M{f(x+.6)} 4.5c1.1 0 1.9.6 2.4 1.9L{f(x+6.4)} 19.5"], [])),
 '(': (2.6, lambda x: ([f"M{f(x+2.6)} 4.5c-1.7 1.9-2.6 4.6-2.6 7.5s.9 5.6 2.6 7.5"], [])),
 ')': (2.6, lambda x: ([f"M{f(x)} 4.5c1.7 1.9 2.6 4.6 2.6 7.5s-.9 5.6-2.6 7.5"], [])),
 # Superscripts: −1, T, H, raised above the x-height.
 '⁻¹': (5.4, lambda x: ([f"M{f(x)} 6.5h2.4", f"M{f(x+3.8)} 4.4l1.6-1.2v7.6"], [])),
 'ᵀ': (4.6, lambda x: ([f"M{f(x)} 3.2h4.6", f"M{f(x+2.3)} 3.2v7.8"], [])),
 'ᴴ': (4.4, lambda x: ([f"M{f(x)} 3.2v7.8", f"M{f(x+4.4)} 3.2v7.8", f"M{f(x)} 7.1h4.4"], [])),
 # A matrix: brackets and four entries.
 'M': (12, lambda x: ([f"M{f(x+2.5)} 4.5H{f(x)}v15h2.5", f"M{f(x+9.5)} 4.5H{f(x+12)}v15h-2.5"], [ring(x+4.2, 9, 1.4), ring(x+7.8, 9, 1.4), ring(x+4.2, 15, 1.4), ring(x+7.8, 15, 1.4)])),
 # Smaller matrices for the products, as the keys' labels draw them: a square one, a row vector, a column vector.
 'Msm': (9, lambda x: ([f"M{f(x+2)} 6H{f(x)}v12h2", f"M{f(x+7)} 6h2v12h-2"], [ring(x+3.2, 9.5, 1.25), ring(x+5.8, 9.5, 1.25), ring(x+3.2, 14.5, 1.25), ring(x+5.8, 14.5, 1.25)])),
 'Mrow': (11, lambda x: ([f"M{f(x+2)} 8.5H{f(x)}v7h2", f"M{f(x+9)} 8.5h2v7h-2"], [ring(x+3.9, 12, 1.25), ring(x+7.1, 12, 1.25)])),
 'Mcol': (7, lambda x: ([f"M{f(x+2)} 5H{f(x)}v14h2", f"M{f(x+5)} 5h2v14h-2"], [ring(x+3.5, 9.5, 1.25), ring(x+3.5, 14.5, 1.25)])),
 # Products between two matrices.
 '·': (3, lambda x: ([], [ring(x+1.5, 12, 1.5)])),
 '×': (4, lambda x: ([f"M{f(x)} 10l4 4", f"M{f(x+4)} 10l-4 4"], [])),
 '∘': (4, lambda x: ([ring(x+2, 12, 1.9)], [])),
 '⊗': (5.6, lambda x: ([ring(x+2.8, 12, 2.8), f"M{f(x+.8)} 10l4 4", f"M{f(x+4.8)} 10l-4 4"], [])),
 # A vector arrow over the letter before it.
 '→': (0, lambda x: ([f"M{f(x-5.5)} 6.2h6", f"M{f(x-1.6)} 4.2l2.1 2l-2.1 2"], [])),
}
GAP = 3.0
# Letters drawn wider than their outlines above (strokes stay 2), so the words read as text, not condensed.
STRETCH = 1.3
MATRIX_STRETCH = 1.2
from fontTools.svgLib.path import parse_path
from fontTools.pens.svgPathPen import SVGPathPen
from fontTools.pens.transformPen import TransformPen
def stretch(d, x0, k):
    """Path d widened by k about x = x0 (absolute commands out)."""
    if k == 1: return d
    pen = SVGPathPen(None, ntos=lambda v: ('%.2f' % v).rstrip('0').rstrip('.'))
    parse_path(d, TransformPen(pen, (k, 0, 0, 1, x0 * (1 - k), 0)))
    return pen.getCommands()
def k_of(ch): return MATRIX_STRETCH if ch == 'M' else (1 if ch in ('·', '×', '∘', '⊗', 'Msm', 'Mrow', 'Mcol') else STRETCH)
def word(parts, W):
    """parts: list of (letters, role) with role 'ink' or 'accent'. Centred in a W-wide box."""
    seq = [(ch, role) for text, role in parts for ch in ([text] if text in L else list(text))]
    total = sum(L[ch][0] * k_of(ch) for ch, _ in seq) + GAP * (sum(1 for ch, _ in seq if L[ch][0] > 0) - 1)
    x = (W - total) / 2
    out = {'ink': [], 'accent': [], 'inkFill': [], 'accentFill': []}
    for ch, role in seq:
        s, fl = L[ch][1](x)
        k = k_of(ch)
        # The vector arrow belongs to the letter before it: stretch it about that letter.
        x0 = x - (L['x'][0] * STRETCH + GAP) if ch == '→' else x
        out[role] += [stretch(p, x0, k) for p in s]
        # Dots (an i's, a matrix's entries) widen in place without turning into ellipses.
        out[role + 'Fill'] += [stretch(p, x0, 1) if ch != 'M' else p for p in fl] if k == 1 else [shift_dot(p, x0, k) for p in fl]
        if L[ch][0] > 0: x += L[ch][0] * k + GAP
    return out
def shift_dot(d, x0, k):
    """A round dot moved to where its centre lands after widening by k about x0, still round."""
    import re
    m = re.match(r'M(-?[0-9.]+) (-?[0-9.]+)a([0-9.]+) ', d)
    x, y, r = float(m.group(1)), float(m.group(2)), float(m.group(3))
    c = x + r
    return ring(x0 + (c - x0) * k, y, r)

def width(parts):
    seq = [(ch, role) for text, role in parts for ch in ([text] if text in L else list(text))]
    total = sum(L[ch][0] * k_of(ch) for ch, _ in seq) + GAP * (sum(1 for ch, _ in seq if L[ch][0] > 0) - 1)
    # A margin either side; never narrower than the square icons.
    import math
    return max(24, math.ceil(total + 4))

def kt(spoken, name, parts, W=None):
    W = W or width(parts)
    o = word(parts, W)
    args = [f'{k} = listOf({", ".join(chr(34) + p + chr(34) for p in v)})' for k, v in o.items() if v]
    w = '' if W == 24 else f', width = {W}f'
    return f'            "{spoken}" to key("{name}"{w}, {", ".join(args)}),'
lines = []
for fn in ['sin', 'cos', 'tan']:
    F = fn.capitalize()
    lines += [kt(fn, F, [(fn, 'ink')]),
              kt(f'inverse {fn}', 'A' + F, [('a', 'accent'), (fn, 'ink')]),
              kt(f'hyperbolic {fn}', F + 'h', [(fn, 'ink'), ('h', 'accent')]),
              kt(f'inverse hyperbolic {fn}', 'A' + F + 'h', [('a', 'accent'), (fn, 'ink'), ('h', 'accent')])]
lines += [
 kt('natural log', 'Ln', [('ln', 'accent')]),
 kt('mod', 'Mod', [('mod', 'accent')]),
 kt('factorial', 'Factorial', [('n', 'ink'), ('!', 'accent')]),
 kt('previous answer', 'Ans', [('Ans', 'ink')]),
 kt('real part', 'Re', [('R', 'accent'), ('e', 'ink')]),
 kt('imaginary part', 'Im', [('I', 'accent'), ('m', 'ink')]),
 kt('residue', 'Residue', [('R', 'accent'), ('es', 'ink')]),
 kt('matrix', 'Matrix', [('M', 'accent')]),
 kt('inverse', 'Inverse', [('M', 'ink'), ('⁻¹', 'accent')]),
 kt('transpose', 'Transpose', [('M', 'ink'), ('ᵀ', 'accent')]),
 kt('conjugate transpose', 'Adjoint', [('M', 'ink'), ('ᴴ', 'accent')]),
 kt('determinant', 'Det', [('det', 'accent'), ('M', 'ink')]),
 kt('dot product', 'Dot', [('M', 'ink'), ('·', 'accent'), ('M', 'ink')]),
 kt('cross product', 'Cross', [('M', 'ink'), ('×', 'accent'), ('M', 'ink')]),
 kt('Hadamard product', 'Hadamard', [('M', 'ink'), ('∘', 'accent'), ('M', 'ink')]),
 kt('Kronecker product', 'Kronecker', [('M', 'ink'), ('⊗', 'accent'), ('M', 'ink')]),
 kt('trace', 'Trace', [('tr', 'accent'), ('M', 'ink')]),
 kt('eigenvalues', 'Eigenvalues', [('λ', 'accent')]),
 kt('eigenvectors', 'Eigenvectors', [('x', 'ink'), ('→', 'accent')]),
 kt('characteristic polynomial', 'Charpoly', [('p', 'ink'), ('(', 'ink'), ('λ', 'accent'), (')', 'ink')]),
 kt('reduced row echelon form', 'Rref', [('rref', 'accent'), ('M', 'ink')]),
 kt('rank', 'Rank', [('rk', 'accent'), ('M', 'ink')]),
]
print('\n'.join(lines))

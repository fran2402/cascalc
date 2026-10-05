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
 # Capitals (cap height 4.5, baseline 19.5).
 'G': (7, lambda x: ([f"M{f(x+7)} 7.6c-.9-2-2.3-3.1-3.9-3.1c-2 0-3.1 3.4-3.1 7.5s1.1 7.5 3.1 7.5c1.9 0 3.4-1.2 3.9-3.4V12.6H{f(x+4)}"], [])),
 'C': (7, lambda x: ([f"M{f(x+7)} 7.6c-.9-2-2.3-3.1-3.9-3.1c-2 0-3.1 3.4-3.1 7.5s1.1 7.5 3.1 7.5c1.6 0 3-1.1 3.9-3.1"], [])),
 'Λ': (7, lambda x: ([f"M{f(x)} 19.5L{f(x+3.5)} 4.5L{f(x+7)} 19.5"], [])),
 'Δ': (7.4, lambda x: ([f"M{f(x)} 19.5L{f(x+3.7)} 4.5L{f(x+7.4)} 19.5z"], [])),
 'K': (6, lambda x: ([f"M{f(x)} 4.5v15", f"M{f(x+6)} 4.5L{f(x)} 13", f"M{f(x+2.3)} 10.6L{f(x+6)} 19.5"], [])),
 # Capital phi (as in the normal distribution's Φ): a wide oval, the stem, serifs top and bottom.
 'Φ': (10, lambda x: ([ring(x+5, 12, 5, 4.2), f"M{f(x+5)} 4.5v15", f"M{f(x+2.8)} 4.5h4.4", f"M{f(x+2.8)} 19.5h4.4"], [])),
 'Z': (6.5, lambda x: ([f"M{f(x)} 4.5h6.5L{f(x)} 19.5h6.5"], [])),
 'E': (5.6, lambda x: ([f"M{f(x+5.6)} 4.5H{f(x)}v15h5.6", f"M{f(x)} 12h4.6"], [])),
 'F': (5.6, lambda x: ([f"M{f(x+5.6)} 4.5H{f(x)}v15", f"M{f(x)} 12h4.6"], [])),
 'N': (6.6, lambda x: ([f"M{f(x)} 19.5v-15l6.6 15v-15"], [])),
 'Ṁ': (8.4, lambda x: ([f"M{f(x)} 19.5v-15l4.2 9.5l4.2-9.5v15"], [])),
 'V': (7, lambda x: ([f"M{f(x)} 4.5l3.5 15l3.5-15"], [])),
 'B': (6.4, lambda x: ([f"M{f(x)} 4.5v15h3.4a3.8 3.8 0 0 0 0-7.6H{f(x)}", f"M{f(x)} 11.9h2.9a3.7 3.7 0 0 0 0-7.4H{f(x)}"], [])),
 'J': (5, lambda x: ([f"M{f(x+5)} 4.5v11c0 2.6-1.2 4-3 4c-1 0-1.8-.5-2.3-1.4"], [])),
 'W': (9.6, lambda x: ([f"M{f(x)} 4.5l2.3 15l2.5-10.5l2.5 10.5l2.3-15"], [])),
 'L': (5, lambda x: ([f"M{f(x)} 4.5v15h5"], [])),
 'S': (6.4, lambda x: ([f"M{f(x+6.2)} 7.4c-.7-1.8-1.9-2.9-3.3-2.9c-1.7 0-2.9 1.3-2.9 3.2c0 4.3 6.4 3 6.4 7.4c0 2.1-1.4 4.4-3.4 4.4c-1.6 0-2.8-1-3.4-2.8"], [])),
 'T': (6, lambda x: ([f"M{f(x)} 4.5h6", f"M{f(x+3)} 4.5v15"], [])),
 'H': (6, lambda x: ([f"M{f(x)} 4.5v15", f"M{f(x+6)} 4.5v15", f"M{f(x)} 12h6"], [])),
 'P': (6.2, lambda x: ([f"M{f(x)} 19.5v-15h3.2a3.6 3.6 0 0 1 0 7.2H{f(x)}"], [])),
 # More lower case.
 'b': (5, lambda x: ([f"M{f(x)} 4.5v15", ring(x+2.5, 14.75, 2.5, 5)], [])),
 'u': (5.4, lambda x: ([f"M{f(x)} 10v6.5c0 1.9.8 3 2.3 3c1.1 0 2.2-.9 3.1-2.6", f"M{f(x+5.4)} 10v9.5"], [])),
 'v': (5, lambda x: ([f"M{f(x)} 10l2.5 9.5l2.5-9.5"], [])),
 'y': (5.2, lambda x: ([f"M{f(x)} 10l2.6 9.5", f"M{f(x+5.2)} 10l-3.6 13"], [])),
 'ℏ': (5.4, lambda x: ([f"M{f(x)} 4.5v15", f"M{f(x)} 12.6c.9-1.7 2-2.6 3.1-2.6c1.5 0 2.3 1.1 2.3 3v6.5", f"M{f(x-1.2)} 8.4l3.6-1.6"], [])),
 # Greek.
 'σ': (7.4, lambda x: ([ring(x+3, 14.75, 3, 4.75), f"M{f(x+3)} 10h4.4"], [])),
 'α': (6.6, lambda x: ([f"M{f(x+6.6)} 10.2C{f(x+5.7)} 14.6 {f(x+4.5)} 19.6 {f(x+2.4)} 19.6S{f(x)} 17.4 {f(x)} 14.8S{f(x+1)} 9.9 {f(x+2.4)} 9.9S{f(x+5.7)} 14.9 {f(x+6.6)} 19.6"], [])),
 'μ': (5.4, lambda x: ([f"M{f(x)} 10v13", f"M{f(x)} 16.5c0 1.9.8 3 2.3 3c1.1 0 2.2-.9 3.1-2.6", f"M{f(x+5.4)} 10v9.5"], [])),
 'ε': (5, lambda x: ([f"M{f(x+4.8)} 11.3c-.6-.9-1.4-1.4-2.5-1.4c-1.4 0-2.3.9-2.3 2.2s.9 2.3 2.4 2.3h1.1", f"M{f(x+3.5)} 14.4H{f(x+2.4)}c-1.5 0-2.4 1-2.4 2.4s1.1 2.8 2.7 2.8c1.1 0 2-.5 2.6-1.4"], [])),
 'θ': (5.2, lambda x: ([ring(x+2.6, 12, 2.6, 7.5), f"M{f(x)} 12h5.2"], [])),
 'τ': (5.4, lambda x: ([f"M{f(x)} 10h5.4", f"M{f(x+2.7)} 10v7.6c0 1.3.6 1.9 1.6 1.9"], [])),
 'ν': (5, lambda x: ([f"M{f(x)} 10l2.4 9.5c1.6-2.6 2.6-5.6 2.6-9.5"], [])),
 # Digits and signs.
 '0': (5, lambda x: ([ring(x+2.5, 12, 2.5, 7.5)], [])),
 '1': (3.4, lambda x: ([f"M{f(x)} 7l2.4-2.5v15"], [])),
 '2': (5.2, lambda x: ([f"M{f(x)} 8c.4-2.2 1.6-3.5 3-3.5s2.2 1.2 2.2 3c0 4-5.2 6.5-5.2 12h5.2"], [])),
 '′': (2, lambda x: ([f"M{f(x+2)} 4l-1.4 5"], [])),
 '−': (4.6, lambda x: ([f"M{f(x)} 12h4.6"], [])),
 '/': (4.4, lambda x: ([f"M{f(x+4.4)} 4.5L{f(x)} 19.5"], [])),
 '∞': (10.8, lambda x: ([f"M{f(x+5.4)} 15c-1-1.6-2-2.4-3-2.4a2.4 2.4 0 0 0 0 4.8c1 0 2-.8 3-2.4s2-2.4 3-2.4a2.4 2.4 0 0 1 0 4.8c-1 0-2-.8-3-2.4z"], [])),
 # Superscripts: −1, T, H, raised above the x-height.
 '⁻¹': (5.4, lambda x: ([f"M{f(x)} 6.5h2.4", f"M{f(x+3.8)} 4.4l1.6-1.2v7.6"], [])),
 # The transpose mark ⊤ (as \top, wider than a T).
 'ᵀ': (6.2, lambda x: ([f"M{f(x)} 3.2h6.2", f"M{f(x+3.1)} 3.2v7.6"], [])),
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
 '⊗': (9, lambda x: ([ring(x+4.5, 12, 4.5), f"M{f(x+2.6)} 10.1l3.8 3.8", f"M{f(x+6.4)} 10.1l-3.8 3.8"], [])),
 # A vector arrow over the letter before it.
 # (drawn centred over the letter before it; see word())
 '→': (0, lambda c: ([f"M{f(c-3.2)} 6.2h6.4", f"M{f(c+1.1)} 4.1l2.1 2.1l-2.1 2.1"], [])),
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
def k_of(ch): return MATRIX_STRETCH if ch == 'M' else (STRETCH if (len(ch) == 1 and 'a' <= ch <= 'z') or ch == 'ℏ' else 1)
def word(parts, W):
    """parts: list of (letters, role) with role 'ink' or 'accent'. Centred in a W-wide box."""
    seq = [(ch, role) for text, role in parts for ch in ([text] if text in L else list(text))]
    total = sum(L[ch][0] * k_of(ch) for ch, _ in seq) + GAP * (sum(1 for ch, _ in seq if L[ch][0] > 0) - 1)
    x = (W - total) / 2
    out = {'ink': [], 'accent': [], 'inkFill': [], 'accentFill': []}
    for ch, role in seq:
        k = k_of(ch)
        x0 = x
        if ch == '→':
            # The vector arrow sits centred over the (already widened) letter before it, unstretched.
            prev = seq[seq.index((ch, role)) - 1][0]
            w = L[prev][0] * k_of(prev)
            s, fl = L[ch][1](x - GAP - w / 2)
            out[role] += s
            continue
        s, fl = L[ch][1](x)
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
 kt('matrix', 'Matrix', [('M', 'ink')]),
 kt('inverse normal', 'InvNorm', [('Φ', 'ink'), ('⁻¹', 'accent')]),
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

SMALL = 0.56
def small(ch, x, sub):
    """A small glyph (sub- or superscript) at x, its strokes thin."""
    k = k_of(ch)
    ps, fl = L[ch][1](0)
    ty = 22.8 - SMALL * 19.5 if sub else 2.6 - SMALL * 4.5
    out = []
    for d in ps + fl:
        pen = SVGPathPen(None, ntos=lambda v: ('%.2f' % v).rstrip('0').rstrip('.'))
        parse_path(d, TransformPen(pen, (SMALL * k, 0, 0, SMALL, x, ty)))
        out.append(pen.getCommands())
    return out[:len(ps)], out[len(ps):], L[ch][0] * k * SMALL
from fontTools.pens.boundsPen import ControlBoundsPen
def translate(d, dy):
    pen = SVGPathPen(None, ntos=lambda v: ('%.2f' % v).rstrip('0').rstrip('.'))
    parse_path(d, TransformPen(pen, (1, 0, 0, 1, 0, dy)))
    return pen.getCommands()
def constant(pieces):
    """pieces: [(text, sub, sup)] → Kotlin key arguments and width."""
    items = []  # (kind, ch) kind: main / sub / sup / sep
    for text, sub, sup in pieces:
        for ch in text:
            if ch in '¹²': items.append(('supd', '1' if ch == '¹' else '2'))
            elif ch in '()/': items.append(('sep', ch))
            else: items.append(('main', ch))
        if sub or sup: items.append(('script', (sub, sup)))
    # Lay out once to measure, then again centred.
    def lay(x0):
        o = {'accent': [], 'inkThin': [], 'ink': [], 'accentFill': [], 'inkFill': []}
        x = x0; prev = None
        for kind, ch in items:
            if kind == 'script':
                sub, sup = ch; w = 0
                if sup == '−1':
                    st, fl = L['⁻¹'][1](x - GAP + 3.0)
                    o['inkThin'] += st
                    w = L['⁻¹'][0] - 4.0 + 3.0
                    sup = ''
                for txt, is_sub in ((sub, True), (sup, False)):
                    # A clear gap after the letter (its stroke reaches 1 past its outline), then the small letters spaced.
                    xx = x - GAP + 4.0
                    got_st, got_fl = [], []
                    for c in txt:
                        c = 'Ṁ' if c == 'M' else c
                        st, fl, wc = small(c, xx, is_sub); got_st += st; got_fl += fl; xx += wc + 1.9
                    # A subscript with tails (p, y, g, μ) is raised just enough to keep them inside the
                    # key, rather than moving the whole constant (so every b, h, k… stays aligned).
                    if is_sub and got_st:
                        b = ControlBoundsPen(None)
                        for d in got_st + got_fl: parse_path(d, b)
                        lift = min(0.0, 22.2 - b.bounds[3])
                        if lift < 0:
                            got_st = [translate(d, lift) for d in got_st]; got_fl = [translate(d, lift) for d in got_fl]
                    o['inkThin'] += got_st; o['inkFill'] += got_fl
                    w = max(w, xx - 1.9 - (x - GAP + 4.0))
                x += w + 4.0 + 1.6; continue
            if kind == 'supd':
                st, fl, wc = small(ch, x - GAP + 3.0, False); o["inkThin"] += st; x += wc - GAP + 3.0 + 2.4; continue
            c = 'Ṁ' if ch == 'M' else ch
            k = k_of(c)
            st, fl = L[c][1](x)
            st = [stretch(d, x, k) for d in st]
            role = 'ink' if kind == 'sep' else 'accent'
            o[role] += st; o[role + 'Fill'] += fl
            x += L[c][0] * k + GAP
        return o, x - GAP - x0
    _, total = lay(0)
    import math
    W = max(24, math.ceil(total + 4))
    o, _ = lay((W - total) / 2)
    # Vertically: a constant written in short letters (m, μ, e…) is centred on them, not on the
    # baseline, so m_p and m_μ keep their subscripts' tails inside the key; nothing may poke out.
    def bounds(paths):
        b = ControlBoundsPen(None)
        for d in paths: parse_path(d, b)
        return b.bounds
    main = [ch for kind, ch in items if kind == 'main']
    top = min(bounds(L['Ṁ' if c == 'M' else c][1](0)[0])[1] for c in main) if main else 4.5
    _, y0, _, y1 = bounds([d for v in o.values() for d in v])
    dy = -2.4 if top >= 9.5 else 0
    dy = min(dy, 22.6 - y1)
    dy = max(dy, 1.4 - y0)
    if abs(dy) > 0.01:
        def move(d):
            pen = SVGPathPen(None, ntos=lambda v: ('%.2f' % v).rstrip('0').rstrip('.'))
            parse_path(d, TransformPen(pen, (1, 0, 0, 1, 0, dy)))
            return pen.getCommands()
        o = {k: [move(d) for d in v] for k, v in o.items()}
    return o, W
import re as _re
src = open('../app/src/main/java/com/example/cas/engine/Constants.kt').read()
for m in _re.finditer(r'\n    \w+\("[^"]+", listOf\((.*?)\), "([^"]+)"', src):
    pieces = []
    for pm in _re.finditer(r'Piece\("([^"]*)"((?:, \w+ = (?:"[^"]*"|\w+))*)\)', m.group(1)):
        kw = dict(_re.findall(r'(\w+) = "([^"]*)"', pm.group(2)))
        pieces.append((pm.group(1), kw.get('sub', ''), kw.get('sup', '')))
    desc = m.group(2).split(' (')[0]
    try:
        o, W = constant(pieces)
    except KeyError as e:
        import sys; print('// skipped', desc, e, file=sys.stderr); continue
    args = [f'{k} = listOf({", ".join(chr(34) + p + chr(34) for p in v)})' for k, v in o.items() if v]
    w = '' if W == 24 else f', width = {W}f'
    name = 'C' + _re.sub(r'[^A-Za-z0-9]', '', desc.title())[:28]
    lines.append(f'            "{desc}" to key("{name}"{w}, {", ".join(args)}),')

def constants_list():
    """Two constants (c, h) in the accent color, each with its name as a line beside it."""
    o = {'accent': [], 'ink': [], 'inkThin': []}
    for ch, y in (('c', 6.5), ('h', 17.5)):
        k = k_of(ch); sc = 0.6
        ps, _ = L[ch][1](0)
        mid = 12 if ch == 'h' else 14.75
        for d in ps:
            pen = SVGPathPen(None, ntos=lambda v: ('%.2f' % v).rstrip('0').rstrip('.'))
            parse_path(d, TransformPen(pen, (sc * k, 0, 0, sc, 2.2, y - sc * mid)))
            o['accent'].append(pen.getCommands())
        o['ink'].append(f"M11 {y - 1.6}h10")
        o['inkThin'].append(f"M11 {y + 2.2}h6.5")
    args = [f'{kk} = listOf({", ".join(chr(34) + p + chr(34) for p in v)})' for kk, v in o.items() if v]
    return f'            "list of constants with names" to key("ConstantList", {", ".join(args)}),'
lines.append(constants_list())
print('\n'.join(lines))

"""Builds the letter-based key icons (trigonometry, linear algebra) for ui/KeyIcons.kt from one set
of stroked letters with a shared baseline (19.5) and x-height (10), so every word sits alike.
Prints Kotlin map entries; run and paste between the markers in KeyIcons.kt."""
def f(v): return ('%.2f' % v).rstrip('0').rstrip('.')
def ring(x, y, rx, ry=None):
    ry = rx if ry is None else ry
    return f"M{f(x-rx)} {f(y)}a{f(rx)} {f(ry)} 0 1 0 {f(2*rx)} 0a{f(rx)} {f(ry)} 0 1 0 {f(-2*rx)} 0z"
# Each letter: (width, function x -> (strokes, fills)).
L = {
 'a': (5, lambda x: ([ring(x+2.5, 14.75, 2.5, 4.75), f"M{f(x+5)} 10v9.5"], [])),
 's': (4.8, lambda x: ([f"M{f(x+4.6)} 11.4c-.6-1-1.5-1.6-2.5-1.6c-1.4 0-2.4.8-2.4 2c0 2.8 5.1 1.6 5.1 4.6c0 1.4-1.1 2.3-2.7 2.3c-1.2 0-2.2-.5-2.9-1.6"], [])),
 'i': (1.6, lambda x: ([f"M{f(x+.8)} 10v9.5"], [ring(x+.8, 6.4, 1.25)])),
 'n': (5.4, lambda x: ([f"M{f(x)} 19.5V10", f"M{f(x)} 12.6c.9-1.7 2-2.6 3.1-2.6c1.5 0 2.3 1.1 2.3 3v6.5"], [])),
 'h': (5.4, lambda x: ([f"M{f(x)} 4.5v15", f"M{f(x)} 12.6c.9-1.7 2-2.6 3.1-2.6c1.5 0 2.3 1.1 2.3 3v6.5"], [])),
 'c': (4.6, lambda x: ([f"M{f(x+4.6)} 11.6c-.6-1.1-1.5-1.8-2.5-1.8c-1.5 0-2.5 2.2-2.5 5s1 5 2.5 5c1 0 1.9-.6 2.5-1.8"], [])),
 'o': (5, lambda x: ([ring(x+2.5, 14.75, 2.5, 4.75)], [])),
 't': (4.4, lambda x: ([f"M{f(x+1.5)} 5.5v11.8c0 1.4.6 2.2 1.7 2.2c.5 0 1-.2 1.3-.5", f"M{f(x)} 10h4.2"], [])),
 'd': (5, lambda x: ([ring(x+2.5, 14.75, 2.5, 4.75), f"M{f(x+5)} 4.5v15"], [])),
 'e': (5, lambda x: ([f"M{f(x)} 15h5c0-3.2-1.1-5.2-2.6-5.2s-2.4 2.2-2.4 5s1 5 2.6 5c1 0 1.8-.6 2.4-1.6"], [])),
 'r': (3.4, lambda x: ([f"M{f(x)} 19.5V10", f"M{f(x)} 13.2c.7-2 1.8-3.2 3.4-3.2"], [])),
 'f': (4, lambda x: ([f"M{f(x+4)} 5.1c-1.6-.6-2.8.2-2.8 2.2v12.2", f"M{f(x)} 10h3.6"], [])),
 'k': (4.6, lambda x: ([f"M{f(x)} 4.5v15", f"M{f(x+4.4)} 10l-4.4 4.8", f"M{f(x+1.5)} 13.2l3.1 6.3"], [])),
 'p': (5, lambda x: ([f"M{f(x)} 10v12.5", ring(x+2.5, 14.75, 2.5, 4.75)], [])),
 'x': (5, lambda x: ([f"M{f(x)} 10l5 9.5", f"M{f(x+5)} 10l-5 9.5"], [])),
 'λ': (6.4, lambda x: ([f"M{f(x)} 19.5l3.5-6.6", f"M{f(x+.6)} 4.5c1.1 0 1.9.6 2.4 1.9L{f(x+6.4)} 19.5"], [])),
 '(': (2.6, lambda x: ([f"M{f(x+2.6)} 4.5c-1.7 1.9-2.6 4.6-2.6 7.5s.9 5.6 2.6 7.5"], [])),
 ')': (2.6, lambda x: ([f"M{f(x)} 4.5c1.7 1.9 2.6 4.6 2.6 7.5s-.9 5.6-2.6 7.5"], [])),
 # Superscripts: −1, T, H, raised above the x-height.
 '⁻¹': (5.4, lambda x: ([f"M{f(x)} 6.5h2.4", f"M{f(x+3.8)} 4.4l1.6-1.2v7.6"], [])),
 'ᵀ': (4.6, lambda x: ([f"M{f(x)} 3.2h4.6", f"M{f(x+2.3)} 3.2v7.8"], [])),
 'ᴴ': (4.4, lambda x: ([f"M{f(x)} 3.2v7.8", f"M{f(x+4.4)} 3.2v7.8", f"M{f(x)} 7.1h4.4"], [])),
 # A matrix: brackets and four entries.
 'M': (11, lambda x: ([f"M{f(x+2.5)} 4.5H{f(x)}v15h2.5", f"M{f(x+8.5)} 4.5H{f(x+11)}v15h-2.5"], [ring(x+3.9, 9, 1.35), ring(x+7.1, 9, 1.35), ring(x+3.9, 15, 1.35), ring(x+7.1, 15, 1.35)])),
 # Products between two matrices.
 '·': (3, lambda x: ([], [ring(x+1.5, 12, 1.5)])),
 '×': (4, lambda x: ([f"M{f(x)} 10l4 4", f"M{f(x+4)} 10l-4 4"], [])),
 '∘': (4, lambda x: ([ring(x+2, 12, 1.9)], [])),
 '⊗': (5.6, lambda x: ([ring(x+2.8, 12, 2.8), f"M{f(x+.8)} 10l4 4", f"M{f(x+4.8)} 10l-4 4"], [])),
 # A vector arrow over the letter before it.
 '→': (0, lambda x: ([f"M{f(x-5.5)} 6.2h6", f"M{f(x-1.6)} 4.2l2.1 2l-2.1 2"], [])),
}
GAP = 2.4
def word(parts, W):
    """parts: list of (letters, role) with role 'ink' or 'accent'. Centred in a W-wide box."""
    seq = [(ch, role) for text, role in parts for ch in ([text] if text in L else list(text))]
    total = sum(L[ch][0] for ch, _ in seq) + GAP * (sum(1 for ch, _ in seq if L[ch][0] > 0) - 1)
    x = (W - total) / 2
    out = {'ink': [], 'accent': [], 'inkFill': [], 'accentFill': []}
    for ch, role in seq:
        s, fl = L[ch][1](x)
        out[role] += s; out[role + 'Fill'] += fl
        if L[ch][0] > 0: x += L[ch][0] + GAP
    return out
def kt(spoken, name, parts, W=36):
    o = word(parts, W)
    args = [f'{k} = listOf({", ".join(chr(34) + p + chr(34) for p in v)})' for k, v in o.items() if v]
    w = '' if W == 24 else f', width = {W}f'
    return f'            "{spoken}" to key("{name}"{w}, {", ".join(args)}),'
lines = []
for fn in ['sin', 'cos', 'tan']:
    F = fn.capitalize()
    lines += [kt(fn, F, [(fn, 'ink')], 24 if fn else 36),
              kt(f'inverse {fn}', 'A' + F, [('a', 'accent'), (fn, 'ink')]),
              kt(f'hyperbolic {fn}', F + 'h', [(fn, 'ink'), ('h', 'accent')]),
              kt(f'inverse hyperbolic {fn}', 'A' + F + 'h', [('a', 'accent'), (fn, 'ink'), ('h', 'accent')])]
lines += [
 kt('matrix', 'Matrix', [('M', 'accent')], 24),
 kt('inverse', 'Inverse', [('M', 'ink'), ('⁻¹', 'accent')], 24),
 kt('transpose', 'Transpose', [('M', 'ink'), ('ᵀ', 'accent')], 24),
 kt('conjugate transpose', 'Adjoint', [('M', 'ink'), ('ᴴ', 'accent')], 24),
 kt('determinant', 'Det', [('det', 'accent'), ('M', 'ink')], 36),
 kt('dot product', 'Dot', [('M', 'ink'), ('·', 'accent'), ('M', 'ink')], 30),
 kt('cross product', 'Cross', [('M', 'ink'), ('×', 'accent'), ('M', 'ink')], 30),
 kt('Hadamard product', 'Hadamard', [('M', 'ink'), ('∘', 'accent'), ('M', 'ink')], 30),
 kt('Kronecker product', 'Kronecker', [('M', 'ink'), ('⊗', 'accent'), ('M', 'ink')], 30),
 kt('trace', 'Trace', [('tr', 'accent'), ('M', 'ink')], 28),
 kt('eigenvalues', 'Eigenvalues', [('λ', 'accent')], 24),
 kt('eigenvectors', 'Eigenvectors', [('x', 'ink'), ('→', 'accent')], 24),
 kt('characteristic polynomial', 'Charpoly', [('p', 'ink'), ('(', 'ink'), ('λ', 'accent'), (')', 'ink')], 26),
 kt('reduced row echelon form', 'Rref', [('rref', 'accent'), ('M', 'ink')], 38),
 kt('rank', 'Rank', [('rk', 'accent'), ('M', 'ink')], 28),
]
print('\n'.join(lines))

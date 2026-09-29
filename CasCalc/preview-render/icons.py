# Tab icons: 24×24, 2px round strokes (Material Symbols Rounded style); optional filled parts.
ICONS = {
  # √x: a radical sign over an x
  "Roots":   {"stroke": ["M3 13l2.5-1.5L8.5 19L12.5 5H21", "M14.5 10l4.5 5.5M19 10l-4.5 5.5"], "fill": []},
  # a right triangle with its angle marked
  "Triangle":{"stroke": ["M4 19H20V5Z"], "thin": ["M9.5 19A5.5 5.5 0 0 0 8.1 15.4", "M17 19V16H20"], "fill": []},
  # a balance: both sides of an equation
  "Balance": {"stroke": ["M12 3.5V20", "M7.5 20h9", "M4 7.5h16", "M4 7.5L1.8 13", "M4 7.5L6.2 13", "M20 7.5L17.8 13", "M20 7.5L22.2 13"],
              "fill": ["M1.2 13h5.6a2.8 2.4 0 0 1-5.6 0z", "M17.2 13h5.6a2.8 2.4 0 0 1-5.6 0z"]},
  # a curve with the area under it shaded: an integral
  "Area":    {"stroke": ["M3 20h18", "M3 17C6.5 17 8.5 6 12 6s5 7.5 9 6"], "fill": [], "shade": ["M7.5 20v-6.3C9 9.5 10.3 6 12 6s3.2 3.6 5 5.6V20z"]},
  # ∇, nabla: vector calculus (with the heavier left stroke of the printed symbol)
  "Nabla": {"stroke": ["M3.5 5h17L12 20.5z"], "thin": ["M7.2 6.8l5.6 10.2"], "fill": []},
  # a 2×2 matrix
  "Matrix":  {"stroke": ["M7 4H4v16h3", "M17 4h3v16h-3"], "fill": ["M7.5 9a1.7 1.7 0 1 0 3.4 0a1.7 1.7 0 1 0-3.4 0z", "M13.1 9a1.7 1.7 0 1 0 3.4 0a1.7 1.7 0 1 0-3.4 0z", "M7.5 15a1.7 1.7 0 1 0 3.4 0a1.7 1.7 0 1 0-3.4 0z", "M13.1 15a1.7 1.7 0 1 0 3.4 0a1.7 1.7 0 1 0-3.4 0z"]},
  # an atom: physical constants
  "Atom":    {"thin": ["M2.5 12a9.5 3.6 0 1 0 19 0a9.5 3.6 0 1 0-19 0z", "M7.25 3.77A9.5 3.6 60 1 0 16.75 20.23A9.5 3.6 60 1 0 7.25 3.77z", "M7.25 20.23A9.5 3.6 -60 1 0 16.75 3.77A9.5 3.6 -60 1 0 7.25 20.23z"],
              "stroke": [], "fill": ["M10.2 12a1.8 1.8 0 1 0 3.6 0a1.8 1.8 0 1 0-3.6 0z"]},
  # |∨|: the graph of an absolute value between bars — more functions
  "Abs":     {"stroke": ["M3 4v16", "M21 4v16", "M7 7l5 9.5L17 7"], "fill": []},
  # an Argand diagram: z as a vector from 0 with its angle arg z
  "Complex": {"stroke": ["M4 20H21", "M4 20V3", "M4 20L15.5 8.5"], "thin": ["M9.2 20A5.2 5.2 0 0 0 7.68 16.32"],
              "fill": ["M13.4 8.5a2.1 2.1 0 1 0 4.2 0a2.1 2.1 0 1 0-4.2 0z"]},
  # ℂ: a double-struck C, the complex numbers
  "ComplexC": {"stroke": ["M18.5 7.2A7.8 7.8 0 1 0 18.5 16.8"], "thin": ["M9 5.6V18.4"], "fill": []},
  # --- graph toolbars ---
  # polar grid: circles and rays from the origin
  "PolarGrid": {"stroke": ["M3.5 12a8.5 8.5 0 1 0 17 0a8.5 8.5 0 1 0-17 0z"], "thin": ["M8 12a4 4 0 1 0 8 0a4 4 0 1 0-8 0z", "M12 12H20.5", "M12 12L18 6", "M12 12V3.5", "M12 12L6 6"], "fill": ["M10.8 12a1.2 1.2 0 1 0 2.4 0a1.2 1.2 0 1 0-2.4 0z"]},
  # concentric rings: contours of |f|
  "Bands": {"stroke": ["M9.5 12a2.5 2.5 0 1 0 5 0a2.5 2.5 0 1 0-5 0z"], "thin": ["M6 12a6 6 0 1 0 12 0a6 6 0 1 0-12 0z", "M2.5 12a9.5 9.5 0 1 0 19 0a9.5 9.5 0 1 0-19 0z"], "fill": []},
  # rays from a point: contours of arg f
  "Phase": {"stroke": ["M12 12H21.5", "M12 12L16.75 3.77", "M12 12L7.25 3.77", "M12 12H2.5", "M12 12L7.25 20.23", "M12 12L16.75 20.23"], "fill": []},
  # a bent grid: the image of the coordinate grid
  "Grid": {"stroke": ["M3 8c6-3 12 3 18 0", "M3 16c6-3 12 3 18 0", "M8 3c-3 6 3 12 0 18", "M16 3c-3 6 3 12 0 18"], "fill": []},
  # a loop with an anticlockwise arrow around a point: ∮ f dz
  "Loop": {"stroke": ["M19 12a7 7 0 1 0-7 7", "M9.2 16.2L12 19l-2.8 2.8"], "fill": ["M10.4 12a1.6 1.6 0 1 0 3.2 0a1.6 1.6 0 1 0-3.2 0z"]},
  # a bell curve over a small histogram: statistics
  "Stats": {"stroke": ["M3 20.5H21", "M3 18.5c3.5 0 5-12.5 9-12.5s5.5 12.5 9 12.5"], "fill": ["M7 20.5v-5.5h2.2v5.5z", "M10.9 20.5V10h2.2v10.5z", "M14.8 20.5v-5.5H17v5.5z"]},
  # the symbol builder: an x with a hat, a box for a superscript and a + to build
  "SymbolBuilder": {"stroke": ["M4 10.5l7 9.5", "M11 10.5l-7 9.5", "M4.5 7.5l3-3l3 3"], "thin": ["M14 3.5h5v5h-5z", "M15 17.5h6", "M18 14.5v6"], "fill": []},
  # Aα: Latin and Greek letters
  "Letters": {"stroke": ["M2.5 19L7 5l4.5 14", "M4.2 14h5.6", "M22 11.5c-.8 3-2.2 7.5-4.3 7.5c-1.9 0-3.2-1.6-3.2-3.8c0-2.2 1.4-3.9 3.3-3.9c2.6 0 3.2 5.1 4.7 7.7"], "fill": []},
}

def svg(name, size=24, color="currentColor"):
    d = ICONS[name]
    o = f'<svg width="{size}" height="{size}" viewBox="0 0 24 24" fill="none" stroke-linecap="round" stroke-linejoin="round">'
    for p in d.get("shade", []): o += f'<path d="{p}" fill="{color}" fill-opacity=".35" stroke="none"/>'
    for p in d["stroke"]: o += f'<path d="{p}" stroke="{color}" stroke-width="2"/>'
    for p in d.get("thin", []): o += f'<path d="{p}" stroke="{color}" stroke-width="1.6"/>'
    for p in d["fill"]: o += f'<path d="{p}" fill="{color}" stroke="none"/>'
    return o + '</svg>'

if __name__ == "__main__":
    html = '<html><body style="background:#fff;display:flex;gap:28px;padding:30px;flex-wrap:wrap">'
    for n in ICONS:
        html += f'<div style="text-align:center;font:14px sans-serif"><div style="color:#1c1c16">{svg(n,96)}</div>{n}</div>'
    open("sheet.html","w").write(html + "</body></html>")

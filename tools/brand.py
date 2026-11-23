# Regenerates the RetroHub TV brand assets.
# Usage: python tools/brand.py <Inter variable font .ttf (SIL OFL)> app/src/main/res <icon_preview.png>
import sys, math
from PIL import Image, ImageDraw, ImageFont
font_path, res = sys.argv[1], sys.argv[2]
K = 0.5523
# ---- controller geometry (viewport 48 x 32) ----
body = [("M",14,3),("H",34),("C",40.5,3,44.6,7.2,45.9,13.4),("L",47.6,22.6),("C",48.4,27,45.6,30.5,41.6,30.5),
        ("C",39.2,30.5,37.6,29.2,36.2,27.2),("L",33.6,23.5),("H",14.4),("L",11.8,27.2),("C",10.4,29.2,8.8,30.5,6.4,30.5),
        ("C",2.4,30.5,-0.4,27,0.4,22.6),("L",2.1,13.4),("C",3.4,7.2,7.5,3,14,3),("Z",)]
# Holes wind counter-clockwise (opposite to the body), so they cut out with the default nonZero
# fill rule. evenOdd would need API 24+, and minSdk is 21.
dpad = [("M",10.9,8.9),("V",11.9),("H",7.9),("V",15.1),("H",10.9),("V",18.1),("H",14.1),("V",15.1),("H",17.1),("V",11.9),("H",14.1),("V",8.9),("Z",)]
def circle(cx, cy, r):
    k = K * r
    return [("M",cx+r,cy),("C",cx+r,cy-k,cx+k,cy-r,cx,cy-r),("C",cx-k,cy-r,cx-r,cy-k,cx-r,cy),
            ("C",cx-r,cy+k,cx-k,cy+r,cx,cy+r),("C",cx+k,cy+r,cx+r,cy+k,cx+r,cy),("Z",)]
def pill(x, y, w, h):
    r = h/2; k = K*r
    return [("M",x+r,y),("C",x+r-k,y,x,y+r-k,x,y+r),("C",x,y+r+k,x+r-k,y+h,x+r,y+h),("H",x+w-r),
            ("C",x+w-r+k,y+h,x+w,y+r+k,x+w,y+r),("C",x+w,y+r-k,x+w-r+k,y,x+w-r,y),("Z",)]
holes = [dpad] + [circle(35.5,9.9,1.75), circle(39.1,13.5,1.75), circle(35.5,17.1,1.75), circle(31.9,13.5,1.75),
         pill(20.2,18.4,2.9,1.6), pill(24.9,18.4,2.9,1.6)]
def fmt(v): return ("%.2f" % v).rstrip("0").rstrip(".")
def to_d(cmds): return "".join(c[0] + ",".join(fmt(v) for v in c[1:]) for c in cmds)
path_d = to_d(body) + "".join(to_d(h) for h in holes)
def polygon(cmds, sx, ox, oy):
    pts = []; x = y = 0
    for c in cmds:
        t = c[0]
        if t in "ML": x, y = c[1], c[2]; pts.append((x, y))
        elif t == "H": x = c[1]; pts.append((x, y))
        elif t == "V": y = c[1]; pts.append((x, y))
        elif t == "C":
            x0, y0 = x, y; x1,y1,x2,y2,x3,y3 = c[1:]
            for i in range(1, 17):
                u = i/16; a = (1-u)**3; b = 3*(1-u)**2*u; cc = 3*(1-u)*u*u; d = u**3
                pts.append((a*x0+b*x1+cc*x2+d*x3, a*y0+b*y1+cc*y2+d*y3))
            x, y = x3, y3
    return [(ox + px*sx, oy + py*sx) for px, py in pts]
# ---- vector drawables ----
open(res + "/drawable/ic_controller.xml", "w").write(f'''<?xml version="1.0" encoding="utf-8"?>
<!-- RetroHub TV mark: white controller. Generated; edit brand.py rather than this file. -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="48dp" android:height="32dp"
    android:viewportWidth="48" android:viewportHeight="32">
    <path android:fillColor="#FFFFFFFF"
        android:pathData="{path_d}" />
</vector>
''')
# (The launcher icon is generated further down as adaptive + legacy mipmaps.)
# (The TV banner is generated at the end, on the same gradient as the launcher icon.)
import os
# preview of the icon too
prev = Image.new("RGB", (216, 216), (0, 0, 0)); pd = ImageDraw.Draw(prev)
s2 = 2.5; o2x = (216 - 48*s2)/2; o2y = (216 - 32*s2)/2
pd.polygon(polygon(body, s2, o2x, o2y), fill=(255,255,255))
for h in holes: pd.polygon(polygon(h, s2, o2x, o2y), fill=(0,0,0))
prev.save(sys.argv[3])
print("axes:", [a["name"] for a in ImageFont.truetype(font_path, 10).get_variation_axes()])

# ---- launcher icons: controller + "RetroHub" wordmark on black ----
# Adaptive icon (API 26+): 108dp canvas, launchers mask it to a circle/squircle, and only the
# central 66dp is guaranteed visible, so the mark is kept inside that safe zone.
# Legacy icons (API 21-25): 48dp rounded square and circle.
def draw_mark(img, cx, cy, width_px):
    d = ImageDraw.Draw(img)
    sx = width_px / 48.0
    ox, oy = cx - 24 * sx, cy - 16 * sx
    d.polygon(polygon(body, sx, ox, oy), fill=(255, 255, 255, 255))
    for h in holes: d.polygon(polygon(h, sx, ox, oy), fill=(0, 0, 0, 0))  # transparent: background shows through

def word(img, cx, top, size_px):
    d = ImageDraw.Draw(img)
    f = font(700, size_px)
    w = d.textlength("RetroHub", font=f)
    d.text((cx - w / 2, top), "RetroHub", font=f, fill=(255, 255, 255, 255))

def icon_art(px, canvas_dp, safe_dp):
    """White controller centred in a square of px pixels, using safe_dp of canvas_dp."""
    S = 4  # supersample
    big = px * S
    img = Image.new("RGBA", (big, big), (0, 0, 0, 0))
    mark_w = big * safe_dp / canvas_dp * 0.86
    draw_mark(img, big / 2, big / 2, mark_w)
    return img.resize((px, px), Image.LANCZOS)

# Same palette as the Settings screen background (SettingsScreen.AuroraBackground).
AURORA_STOPS = [(0.0, (0x2A, 0x0F, 0x5C)), (0.45, (0x11, 0x1B, 0x4E)), (1.0, (0x04, 0x23, 0x2E))]
PINK, CYAN = (0xE0, 0x45, 0x7B), (0x22, 0xD3, 0xEE)

def aurora(n):
    """Diagonal violet -> navy -> teal, pink glow top-right, cyan glow bottom-left."""
    import math
    img = Image.new("RGB", (n, n))
    px = img.load()
    def lerp(a, b, t): return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))
    for y in range(n):
        for x in range(n):
            t = (x + y) / (2.0 * (n - 1))
            for (o0, c0), (o1, c1) in zip(AURORA_STOPS, AURORA_STOPS[1:]):
                if t <= o1:
                    c = lerp(c0, c1, (t - o0) / (o1 - o0)); break
            dp = math.hypot(x - n * 1.05, y + n * 0.05) / (n * 0.95)
            gp = max(0.0, 1 - dp) * 0.45
            dc = math.hypot(x + n * 0.05, y - n * 1.05) / (n * 0.9)
            gc = max(0.0, 1 - dc) * 0.25
            c = lerp(c, PINK, gp); c = lerp(c, CYAN, gc)
            px[x, y] = c
    return img

densities = {"mdpi": 1, "hdpi": 1.5, "xhdpi": 2, "xxhdpi": 3, "xxxhdpi": 4}
for name, scale in densities.items():
    d = res + "/mipmap-" + name
    os.makedirs(d, exist_ok=True)
    # adaptive foreground: transparent 108dp, art inside the 66dp safe zone (a little margin: 60dp)
    icon_art(int(108 * scale), 108, 60).save(d + "/ic_launcher_foreground.png", optimize=True)
    aurora(int(108 * scale)).save(d + "/ic_launcher_background.png", optimize=True)
    # legacy square + round
    n = int(48 * scale)
    for shape in ("square", "round"):
        S = 4
        base = Image.new("RGBA", (n * S, n * S), (0, 0, 0, 0))
        mask = Image.new("L", (n * S, n * S), 0)
        md = ImageDraw.Draw(mask)
        inset = n * S * (0.04 if shape == "square" else 0)
        if shape == "square":
            md.rounded_rectangle([inset, inset, n * S - inset, n * S - inset], radius=n * S * 0.22, fill=255)
        else:
            md.ellipse([inset, inset, n * S - inset, n * S - inset], fill=255)
        grad = aurora(n * S).convert("RGBA")
        base.paste(grad, (0, 0), mask)
        base = base.resize((n, n), Image.LANCZOS)
        art = icon_art(n, 48, 34 if shape == "round" else 38)
        base.alpha_composite(art)
        base.save(d + ("/ic_launcher.png" if shape == "square" else "/ic_launcher_round.png"), optimize=True)

os.makedirs(res + "/mipmap-anydpi-v26", exist_ok=True)
for nm in ("ic_launcher", "ic_launcher_round"):
    open(res + "/mipmap-anydpi-v26/" + nm + ".xml", "w").write('''<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@mipmap/ic_launcher_background" />
    <foreground android:drawable="@mipmap/ic_launcher_foreground" />
    <monochrome android:drawable="@drawable/ic_launcher_monochrome" />
</adaptive-icon>
''')
# Themed-icon (Android 13+) layer: controller only, inside the safe zone.
open(res + "/drawable/ic_launcher_monochrome.xml", "w").write(f'''<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp" android:height="108dp"
    android:viewportWidth="108" android:viewportHeight="108">
    <group android:scaleX="0.95" android:scaleY="0.95" android:translateX="31.2" android:translateY="38.8">
        <path android:fillColor="#FFFFFFFF" android:pathData="{path_d}" />
    </group>
</vector>
''')
prev2 = aurora(432).convert("RGBA")
prev2.alpha_composite(Image.open(res + "/mipmap-xxxhdpi/ic_launcher_foreground.png"))
prev2.save(sys.argv[3].replace(".png", "_adaptive.png"))

# ---- TV launcher banner (xhdpi 640x360): exactly like the app icon, controller only ----
def aurora_rect(w, h):
    """Same gradient and glows as aurora(), stretched to a w x h rectangle."""
    import math
    img = Image.new("RGB", (w, h))
    px = img.load()
    def lerp(a, b, t): return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))
    for y in range(h):
        for x in range(w):
            t = (x / (w - 1) + y / (h - 1)) / 2.0
            for (o0, c0), (o1, c1) in zip(AURORA_STOPS, AURORA_STOPS[1:]):
                if t <= o1:
                    c = lerp(c0, c1, (t - o0) / (o1 - o0)); break
            dp = math.hypot(x - w * 1.05, y + h * 0.05) / (h * 0.95)
            gp = max(0.0, 1 - dp) * 0.45
            dc = math.hypot(x + w * 0.05, y - h * 1.05) / (h * 0.9)
            gc = max(0.0, 1 - dc) * 0.25
            c = lerp(c, PINK, gp); c = lerp(c, CYAN, gc)
            px[x, y] = c
    return img

BW, BH, SS = 640, 360, 2
banner = aurora_rect(BW * SS, BH * SS).convert("RGBA")
mark = Image.new("RGBA", (BW * SS, BH * SS), (0, 0, 0, 0))
# Same proportion as the icon: controller ~48% of the short side's height band.
draw_mark(mark, BW * SS / 2, BH * SS / 2, BH * SS * 0.48 * 48 / 32 * 0.75)
banner.alpha_composite(mark)
os.makedirs(res + "/drawable-xhdpi", exist_ok=True)
banner.resize((BW, BH), Image.LANCZOS).convert("RGB").save(res + "/drawable-xhdpi/banner.png", optimize=True)

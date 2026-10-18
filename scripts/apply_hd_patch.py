#!/usr/bin/env python3
"""Apply an experimental HD renderer to a clean, pinned J2ME Loader checkout.

Only the open-source runtime is modified. The original MIDlet/JAR is not copied.
Every patch anchor is verified before files are written.
"""
from pathlib import Path
import sys

if len(sys.argv) != 2:
    raise SystemExit("Usage: python3 scripts/apply_hd_patch.py /path/to/J2ME-Loader")

project = Path(__file__).resolve().parents[1]
root = Path(sys.argv[1]).resolve()
base = root / "app/src/main/java/javax/microedition/lcdui"
paths = {
    "Image.java": base / "Image.java",
    "Graphics.java": base / "Graphics.java",
    "Canvas.java": base / "Canvas.java",
    "CanvasWrapper.java": base / "graphics/CanvasWrapper.java",
}
hd_java = base / "CrusoeHD.java"
if hd_java.exists():
    raise SystemExit("Already patched; refusing to patch twice.")
for name, path in paths.items():
    if not path.is_file():
        raise SystemExit(f"Missing upstream source: {path}")
sources = {name: path.read_text(encoding="utf-8") for name, path in paths.items()}

def replace_once(name: str, before: str, after: str) -> None:
    original = sources[name]
    count = original.count(before)
    if count != 1:
        raise RuntimeError(f"{name}: expected one exact anchor, found {count}: {before[:85]!r}")
    sources[name] = original.replace(before, after, 1)

replace_once("Image.java",
    "\tprivate Bitmap bitmap;\n",
    "\tprivate Bitmap bitmap;\n\tprivate int crusoeScale = 1;\n\tprivate String crusoeResourceName;\n")

replace_once("Image.java",
    "\tpublic Bitmap getBitmap() {\n",
    """\t/** Backing pixels are larger than the emulated logical viewport. */
\tstatic Image createCrusoeRenderTarget(int logicalWidth, int logicalHeight, int argb) {
\t\t// Guard against oversized user-defined virtual screen dimensions.
\t\tint scale = CrusoeHD.isEnabled() && (long) logicalWidth * logicalHeight <= 360L * 640L\n\t\t\t\t? CrusoeHD.SCALE : 1;
\t\tBitmap backing = Bitmap.createBitmap(logicalWidth * scale, logicalHeight * scale,
\t\t\t\tBitmap.Config.ARGB_8888);
\t\tif (argb != 0) backing.eraseColor(argb);
\t\tImage result = new Image(backing);
\t\tresult.bounds.right = logicalWidth;
\t\tresult.bounds.bottom = logicalHeight;
\t\tresult.crusoeScale = scale;
\t\treturn result;
\t}
\tint getCrusoeScale() { return crusoeScale; }\n\tString getCrusoeResourceName() { return crusoeResourceName; }
\tpublic Rect getCrusoeNativeBounds() {
\t\treturn crusoeScale == 1 ? bounds : new Rect(0, 0,
\t\t\t\tbounds.right * crusoeScale, bounds.bottom * crusoeScale);
\t}

\tpublic Bitmap getBitmap() {
""")

replace_once("Image.java",
    """\t\tif (b == null) {
\t\t\tthrow new IOException("Can't decode image: " + resname);
\t\t}
\t\treturn new Image(b);
\t}""",
    """\t\tif (b == null) {
\t\t\tthrow new IOException("Can't decode image: " + resname);
\t\t}
\t\tImage result = new Image(b);
\t\tresult.crusoeResourceName = resname;
\t\treturn result;
\t}""")

replace_once("Image.java",
    "\t\tdst.getSingleGraphics().getCanvas().drawBitmap(bitmap, bounds, bounds, null);",
    """\t\tRect src = crusoeScale == 1 ? bounds : new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
\t\tdst.getSingleGraphics().getCanvas().drawBitmap(bitmap, src, bounds, null);""")
replace_once("Image.java",
    "\t\tdst.getSingleGraphics().getCanvas().drawBitmap(bitmap, bounds, r, null);",
    """\t\tRect src = crusoeScale == 1 ? bounds : new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
\t\tdst.getSingleGraphics().getCanvas().drawBitmap(bitmap, src, r, null);""")
replace_once("Image.java",
    "\tvoid setSize(int width, int height) {\n",
    """\tvoid setSize(int width, int height) {
\t\tif (crusoeScale != 1) {
\t\t\tint pixelWidth = width * crusoeScale;
\t\t\tint pixelHeight = height * crusoeScale;
\t\t\tif (pixelWidth > bitmap.getWidth() || pixelHeight > bitmap.getHeight()) {
\t\t\t\tBitmap previous = bitmap;
\t\t\t\tbitmap = Bitmap.createBitmap(Math.max(pixelWidth, previous.getWidth()),
\t\t\t\t\t\tMath.max(pixelHeight, previous.getHeight()), Bitmap.Config.ARGB_8888);
\t\t\t\tnew android.graphics.Canvas(bitmap).drawBitmap(previous, 0, 0, null);
\t\t\t\tgraphics = null;
\t\t\t}
\t\t\tbounds.right = width;
\t\t\tbounds.bottom = height;
\t\t\tif (graphics != null) graphics.reset(0, 0, width, height);
\t\t\treturn;
\t\t}
""")
replace_once("Graphics.java",
    "\t\tcanvas = new Canvas(image.getBitmap());\n\t\tcanvas.save();",
    """\t\tcanvas = new Canvas(image.getBitmap());
\t\tif (image.getCrusoeScale() > 1) canvas.scale(image.getCrusoeScale(), image.getCrusoeScale());
\t\tcanvas.save();""")
replace_once("Graphics.java",
    "\t\tcanvas.restoreToCount(1);\n\t\tcanvas.save();",
    """\t\tcanvas.restoreToCount(1);
\t\tif (image.getCrusoeScale() > 1) canvas.scale(image.getCrusoeScale(), image.getCrusoeScale());
\t\tcanvas.save();""")
replace_once("Graphics.java",
    "\t\tRect srcR = rect;\n\t\tRectF dstR = rectF;\n\t\tfloat dx;",
    """\t\tBitmap hdReplacement = CrusoeHD.get(image, x_src, y_src, width, height);
\t\tRect srcR = rect;
\t\tRectF dstR = rectF;
\t\tfloat dx;""")
replace_once("Graphics.java",
    "\t\tsrcR.set(x_src, y_src, x_src + width, y_src + height);\n\n\t\tMatrix matrix = new Matrix();",
    """\t\tif (hdReplacement == null) srcR.set(x_src, y_src, x_src + width, y_src + height);
\t\telse srcR.set(0, 0, hdReplacement.getWidth(), hdReplacement.getHeight());

\t\tMatrix matrix = new Matrix();""")

s = sources["Graphics.java"]
start = s.index("\tpublic void drawRegion(")
end = s.index("\tpublic void drawRGB(", start)
region = s[start:end]
original_call = "canvas.drawBitmap(image.getBitmap(), srcR, dstR, null);"
assert region.count(original_call) == 2, "Upstream drawRegion changed: expected 2 bitmap operations"
region = region.replace(original_call,
    "canvas.drawBitmap(hdReplacement != null ? hdReplacement : image.getBitmap(), srcR, dstR, null);")
sources["Graphics.java"] = s[:start] + region + s[end:]

replace_once("Canvas.java",
    "\t\t\t\toffscreen = Image.createImage(width, maxHeight, 0);\n\t\t\t\toffscreenCopy = Image.createImage(width, maxHeight, 0);",
    """\t\t\t\toffscreen = Image.createCrusoeRenderTarget(width, maxHeight, 0);
\t\t\t\toffscreenCopy = Image.createCrusoeRenderTarget(width, maxHeight, 0);""")
replace_once("Canvas.java",
    "\t\t\tfloat th = (float) height / offscreen.getBitmap().getHeight();\n\t\t\tfloat tw = (float) width / offscreen.getBitmap().getWidth();",
    """\t\t\tfloat th = (float) (height * offscreen.getCrusoeScale()) / offscreen.getBitmap().getHeight();
\t\t\tfloat tw = (float) (width * offscreen.getCrusoeScale()) / offscreen.getBitmap().getWidth();""")
replace_once("CanvasWrapper.java",
    "\t\tcanvas.drawBitmap(bitmap, image.getBounds(), dst, imgPaint);",
    "\t\tcanvas.drawBitmap(bitmap, image.getCrusoeNativeBounds(), dst, imgPaint);")

# Write only after every source anchor passed. The repo's Java file is our own code.
for name, path in paths.items():
    path.write_text(sources[name], encoding="utf-8")
hd_java.write_text((project / "renderer/CrusoeHD.java").read_text(encoding="utf-8"),
                  encoding="utf-8")
print("Applied experimental HD renderer to five open-source runtime files.")
print("No game JAR/JAD or proprietary assets were copied.")

"""Build small antialiased nine-slice UI sprites and restore the OFL font. Stdlib only."""
import base64
import lzma
import json
import math
from pathlib import Path
import struct
import zlib
ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'src/main/resources/assets/orbit'

def chunk(kind, payload):
    return struct.pack('>I', len(payload)) + kind + payload + struct.pack('>I', zlib.crc32(kind + payload) & 0xffffffff)

def png(path, size, rgba):
    raw = b''.join(b'\0' + bytes(rgba[y*size*4:(y+1)*size*4]) for y in range(size))
    path.write_bytes(b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('>IIBBBBB', size, size, 8, 6, 0, 0, 0)) + chunk(b'IDAT', zlib.compress(raw, 9)) + chunk(b'IEND', b''))

def distance(x, y, size, radius, margin=0):
    qx = abs(x-size/2) - (size/2-margin-radius)
    qy = abs(y-size/2) - (size/2-margin-radius)
    return math.hypot(max(qx, 0), max(qy, 0)) + min(max(qx, qy), 0) - radius

sprites = ASSETS / 'textures/gui/sprites/ui'
sprites.mkdir(parents=True, exist_ok=True)
for kind in ('surface', 'border', 'shadow', 'surface_small', 'border_small', 'window', 'window_shade', 'surface_medium', 'border_medium'):
    size = 64
    base_kind = kind.removesuffix('_small').removesuffix('_medium')
    radius = 3 if kind.endswith('_small') else 6 if kind.endswith('_medium') else 10
    data = bytearray()
    for y in range(size):
        for x in range(size):
            alpha = 0.0
            for sy in range(4):
                for sx in range(4):
                    px, py = x+(sx+.5)/4, y+(sy+.5)/4
                    d = distance(px, py, size, radius, 6 if kind == 'shadow' else 0)
                    if kind == 'shadow': a = .28 * math.exp(-max(0, d)**2/12)
                    elif base_kind == 'border': a = 1.0 if -1 <= d <= 0 else 0.0
                    else: a = 1.0 if d <= 0 else 0.0
                    alpha += a/16
            if kind == 'window_shade': alpha *= (y / (size - 1)) ** 1.35
            shade = round(255 - 22*y/(size-1)) if base_kind == 'surface' else round(255 - 186*y/(size-1)) if kind == 'window' else 255
            data.extend((0, 0, 0, round(alpha*255)) if kind == 'shadow' else (shade, shade, shade, round(alpha*255)))
    target = sprites / (kind + '.png')
    png(target, size, data)
    target.with_suffix('.png.mcmeta').write_text(json.dumps({'gui': {'scaling': {'type': 'nine_slice', 'width': size, 'height': size, 'border': 16 if kind == 'shadow' else radius, 'stretch_inner': True}}}, indent=2)+'\n')
font = ASSETS / 'font'
font.mkdir(exist_ok=True)
(font/'license.txt').write_text((ROOT/'tools/assets/LICENSE').read_text())
(font/'ui.ttf').write_bytes(base64.b64decode((ROOT/'tools/assets/orbit-ui.ttf.b64').read_bytes()))
(font/'ui.json').write_text(json.dumps({'providers':[{'type':'ttf','file':'orbit:ui.ttf','shift':[0,1],'size':10,'oversample':3.0},{'type':'reference','id':'minecraft:default'}]}, indent=2)+'\n')
(font/'ui_bold.ttf').write_bytes(lzma.decompress(base64.b64decode((ROOT/'tools/assets/orbit-ui-bold.ttf.xz.b64').read_bytes())))
(font/'ui_bold.json').write_text(json.dumps({'providers':[{'type':'ttf','file':'orbit:ui_bold.ttf','shift':[0,1],'size':10,'oversample':3.0},{'type':'reference','id':'minecraft:default'}]},indent=2)+'\n')
print('Generated 9 antialiased nine-slice sprites and Orbit UI font (Latin + Cyrillic).')

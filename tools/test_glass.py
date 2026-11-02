"""Resource and layout checks only. Does not run the Minecraft renderer."""
from pathlib import Path
import json
import struct
import subprocess
import sys
import zlib
ROOT = Path(__file__).resolve().parents[1]
subprocess.run([sys.executable, str(ROOT/'tools/generate_ui.py')], check=True)
ASSETS = ROOT/'src/main/resources/assets/orbit'
for kind in ('surface', 'border', 'shadow', 'surface_small', 'border_small'):
    path = ASSETS/f'textures/gui/sprites/ui/{kind}.png'
    data = path.read_bytes()
    assert data[:8] == b'\x89PNG\r\n\x1a\n'
    assert struct.unpack('>IIBB',data[16:26]) == (64,64,8,6)
    pos=8; compressed=b''
    while pos<len(data):
        n=struct.unpack('>I',data[pos:pos+4])[0]
        tag=data[pos+4:pos+8]; payload=data[pos+8:pos+8+n]
        assert zlib.crc32(tag+payload)&0xffffffff == struct.unpack('>I',data[pos+8+n:pos+12+n])[0]
        if tag==b'IDAT': compressed+=payload
        pos+=n+12
    raw=zlib.decompress(compressed)
    alpha=[raw[y*257+1+x*4+3] for y in range(64) for x in range(64)]
    assert min(alpha)==0
    assert any(0<a<255 for a in alpha), 'No antialiased edge pixels'
    if kind!='shadow': assert max(alpha)==255
    meta=json.loads(path.with_suffix('.png.mcmeta').read_text())['gui']['scaling']
    assert meta['type']=='nine_slice' and meta['stretch_inner'] is True
    assert meta['border']*2 <= 32 # Smallest shadow target: 20 + 12.
font=ASSETS/'font/ui.ttf'
assert font.read_bytes()[:4]==b'\0\1\0\0'

# Validate the Unicode cmap rather than trusting that the TTF file merely exists.
font_data=font.read_bytes()
num_tables=struct.unpack_from('>H',font_data,4)[0]
tables={}
for i in range(num_tables):
    offset=12+i*16
    tag=font_data[offset:offset+4].decode('ascii')
    tables[tag]=struct.unpack_from('>II',font_data,offset+8)
cmap_base=tables['cmap'][0]
cmap_count=struct.unpack_from('>H',font_data,cmap_base+2)[0]
cmap4=None
for i in range(cmap_count):
    platform,encoding,relative=struct.unpack_from('>HHI',font_data,cmap_base+4+i*8)
    sub=cmap_base+relative
    if struct.unpack_from('>H',font_data,sub)[0]==4 and platform in (0,3): cmap4=sub
assert cmap4 is not None, 'No Unicode BMP character map'
segments=struct.unpack_from('>H',font_data,cmap4+6)[0]//2
def glyph_index(cp):
    for i in range(segments):
        end=struct.unpack_from('>H',font_data,cmap4+14+i*2)[0]
        start=struct.unpack_from('>H',font_data,cmap4+16+segments*2+i*2)[0]
        if start<=cp<=end:
            delta=struct.unpack_from('>h',font_data,cmap4+16+segments*4+i*2)[0]
            range_pos=cmap4+16+segments*6+i*2
            ro=struct.unpack_from('>H',font_data,range_pos)[0]
            if not ro:return (cp+delta)&65535
            glyph=struct.unpack_from('>H',font_data,range_pos+ro+(cp-start)*2)[0]
            return (glyph+delta)&65535 if glyph else 0
    return 0
for cp in list(range(0x20,0x7f))+list(range(0x410,0x450))+[0x401,0x451,0x2026]:
    assert glyph_index(cp)>0, f'Missing UI character U+{cp:04X}'

assert (ASSETS/'font/license.txt').exists()
providers=json.loads((ASSETS/'font/ui.json').read_text())['providers']
assert providers[0]['type']=='ttf' and providers[0]['oversample']>=2
# Minecraft prefixes font/ when resolving the provider's file identifier.
namespace, resource=providers[0]['file'].split(':',1)
resolved=ROOT/'src/main/resources/assets'/namespace/'font'/resource
assert resolved.is_file(), f'TTF resource not found by Minecraft: {resolved}'
assert resource=='ui.ttf', 'Do not duplicate the font/ prefix'
assert providers[1] == {'type':'reference','id':'minecraft:default'}
source=(ROOT/'src/client/java/dev/alacka/orbit/menu/GlassMenu.java').read_text()
assert 'blitSprite' in source and 'radius, int fill, int border' in source
assert '0xb8ca3657' not in source # Previous red profile accent removed.
for w in (320,360,426,640,854):
    for h in (240,260,300,360,480):
        pw=min(360,w-24);ph=min(216,h-16);left=(w-pw)//2;top=(h-ph)//2
        inner=pw-24
        assert inner-142>=100
        assert top+167+9 <= top+ph-27
        assert left>=12 and top>=8
print('PASS: antialiased RGBA sprites/CRCs, nine-slice metadata, font path/Latin+Cyrillic cmap/license and 25 Profiles layouts. In-game test required.')

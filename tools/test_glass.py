"""Stdlib asset/font checks. Runtime and geometry have their own separate tests."""
from pathlib import Path
import json,struct,subprocess,sys,zlib
ROOT=Path(__file__).resolve().parents[1]
subprocess.run([sys.executable,str(ROOT/'tools/generate_ui.py')],check=True)
ASSETS=ROOT/'src/main/resources/assets/orbit'
SPRITES=('surface','border','shadow','surface_small','border_small','surface_medium','border_medium','window','window_shade')
for kind in SPRITES:
    path=ASSETS/f'textures/gui/sprites/ui/{kind}.png';data=path.read_bytes()
    assert data[:8]==b'\x89PNG\r\n\x1a\n' and struct.unpack('>IIBB',data[16:26])==(64,64,8,6)
    pos=8;compressed=b''
    while pos<len(data):
        n=struct.unpack('>I',data[pos:pos+4])[0];tag=data[pos+4:pos+8];payload=data[pos+8:pos+8+n]
        assert zlib.crc32(tag+payload)&0xffffffff==struct.unpack('>I',data[pos+8+n:pos+12+n])[0]
        if tag==b'IDAT':compressed+=payload
        pos+=n+12
    raw=zlib.decompress(compressed);alpha=[raw[y*257+1+x*4+3] for y in range(64) for x in range(64)]
    assert min(alpha)==0 and any(0<a<255 for a in alpha)
    if kind!='shadow':assert max(alpha)==255
    meta=json.loads(path.with_suffix('.png.mcmeta').read_text())['gui']['scaling']
    assert meta['type']=='nine_slice' and meta['stretch_inner'] is True and meta['border']*2<=32

def validate_font(path):
    data=path.read_bytes();assert data[:4]==b'\0\1\0\0'
    tables={}
    for i in range(struct.unpack_from('>H',data,4)[0]):
        off=12+i*16;tables[data[off:off+4].decode('ascii')]=struct.unpack_from('>II',data,off+8)
    base=tables['name'][0]
    count,strings=struct.unpack_from('>HH',data,base+2)
    for i in range(count):
        platform,encoding,language,name_id,length,relative=struct.unpack_from('>HHHHHH',data,base+6+i*12)
        if name_id in (1,4,6,16,18):
            raw=data[base+strings+relative:base+strings+relative+length]
            label=raw.decode('utf-16-be' if platform in (0,3) else 'mac_roman')
            assert label.startswith('Orbit'),(path.name,name_id,label)
    base=tables['cmap'][0];cmap=None
    for i in range(struct.unpack_from('>H',data,base+2)[0]):
        platform,encoding,relative=struct.unpack_from('>HHI',data,base+4+i*8);sub=base+relative
        if struct.unpack_from('>H',data,sub)[0]==4 and platform in (0,3):cmap=sub
    assert cmap is not None
    count=struct.unpack_from('>H',data,cmap+6)[0]//2
    def glyph(cp):
        for i in range(count):
            end=struct.unpack_from('>H',data,cmap+14+i*2)[0];start=struct.unpack_from('>H',data,cmap+16+count*2+i*2)[0]
            if not start<=cp<=end:continue
            delta=struct.unpack_from('>h',data,cmap+16+count*4+i*2)[0];rp=cmap+16+count*6+i*2;ro=struct.unpack_from('>H',data,rp)[0]
            if not ro:return (cp+delta)&65535
            g=struct.unpack_from('>H',data,rp+ro+(cp-start)*2)[0];return (g+delta)&65535 if g else 0
        return 0
    for cp in list(range(0x20,0x7f))+list(range(0x410,0x450))+[0x401,0x451,0x2026]:assert glyph(cp)>0,(path.name,f'U+{cp:04X}')
for face in ('ui','ui_bold'):
    d=json.loads((ASSETS/f'font/{face}.json').read_text());provider=d['providers'][0]
    assert provider['type']=='ttf' and provider['oversample']>=2
    ns,res=provider['file'].split(':',1);assert not res.startswith('font/')
    validate_font(ROOT/'src/main/resources/assets'/ns/'font'/res)
    assert d['providers'][1]=={'type':'reference','id':'minecraft:default'}
assert (ASSETS/'font/license.txt').exists()
source=(ROOT/'src/client/java/dev/alacka/orbit/menu/GlassMenu.java').read_text()
assert 'blitSprite' in source and 'radius, int fill, int border' in source
assert '0xb8ca3657' not in source
print('PASS: 9 antialiased RGBA/nine-slice sprites, CRCs, regular+bold Latin/Cyrillic cmap, Orbit derivative names, OFL license and fallback providers.')

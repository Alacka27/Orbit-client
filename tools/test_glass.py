"""Read-only checks for retained 0.4.7 resources and font coverage; not runtime geometry QA."""
from pathlib import Path
import hashlib,json,struct
from check_ui_resources import check_resource
ROOT=Path(__file__).resolve().parents[1]
ASSETS=ROOT/'src/main/resources/assets/orbit'
expected=json.loads((ROOT/'tools/assets/orbit047-resource-hashes.json').read_text())
for name,digest in expected.items():
    path=ROOT/'src/main/resources'/name
    assert path.exists(),name
    check_resource(name,path.read_bytes(),digest)
assert not list((ASSETS/'textures/gui/sprites/ui').glob('*.png')),'Legacy raster sprites must not replace vector panels'

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
    validate_font(ASSETS/'font'/f'{face}.ttf')
fonts=sorted((ASSETS/'font').glob('*.json'))
assert len(fonts)==18
for file in fonts:
    providers=json.loads(file.read_text())['providers']
    assert providers[0]['type']=='ttf' and 1<=providers[0]['oversample']<=8,file
    ns,res=providers[0]['file'].split(':',1)
    assert not res.startswith('font/')
    assert (ROOT/'src/main/resources'/f'assets/{ns}/font/{res}').exists()
    assert providers[-1]=={'type':'reference','id':'minecraft:default'},file
assert (ASSETS/'font/license.txt').exists()
source=(ROOT/'src/client/java/dev/alacka/orbit/menu/GlassMenu.java').read_text()
assert 'ShapeMath.fill' in source and 'SmoothShapes.mesh' in source
assert 'radius, int fill, int border' in source
print(f'PASS: {len(expected)-2} byte-identical 0.4.7 UI resources, no Orbit vanilla key labels, 18 font descriptions, Latin/Cyrillic cmap, OFL and fallback providers; vector panel path retained.')

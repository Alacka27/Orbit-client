"""Asset/layout checks only; not a Minecraft renderer or runtime test."""
from pathlib import Path
import json
import struct
import subprocess
import sys
import zlib

root=Path(__file__).resolve().parents[1]
subprocess.run([sys.executable,str(root/'tools/generate_icon.py')],check=True)
meta=json.loads((root/'src/main/resources/fabric.mod.json').read_text())
icon=root/'src/main/resources'/meta['icon']
data=icon.read_bytes()
assert data[:8]==b'\x89PNG\r\n\x1a\n'
assert struct.unpack('>IIBB',data[16:26])==(512,512,8,6)
position=8;compressed=b''
while position<len(data):
 length=struct.unpack('>I',data[position:position+4])[0]
 kind=data[position+4:position+8];payload=data[position+8:position+8+length]
 assert zlib.crc32(kind+payload)&0xffffffff==struct.unpack('>I',data[position+8+length:position+12+length])[0]
 if kind==b'IDAT':compressed+=payload
 position+=12+length
raw=zlib.decompress(compressed)
assert len(raw)==512*(1+512*4)
alpha=[]
for y in range(512):
 row=raw[y*(1+512*4):(y+1)*(1+512*4)]
 assert row[0]==0
 alpha.extend(row[4::4])
assert min(alpha)==0 and max(alpha)==255
source=(root/'src/client/java/dev/alacka/orbit/menu/OrbitMenu.java').read_text()
assert 'logoSize = compact ? 52 : 82' in source
assert 'int h = compact ? 22 : 28' in source
cases=0
for w in (320,360,426,640,854,1280):
 for h in (240,260,299,300,360,480,720):
  compact=h<300;logo=52 if compact else 82;button=22 if compact else 28
  total=logo+39+button*3+12
  top=max(8,(h-total-44)//2)
  last=top+logo+39+2*(button+6)+button
  assert last+12<=h-44,(w,h,last)
  assert min(220,w-48)>=200
  assert (w-min(220,w-48))//2>=12
  cases+=1
print('PASS: PNG signature, size, CRC, transparency, metadata path and',cases,'layout cases. In-game test required.')

"""Original layered cloud wallpaper inspired by the user's palette, without reference UI/branding."""
import math
import random
import struct
import zlib
from pathlib import Path
W,H=1600,900
rows=[]
for y in range(H):
 t=y/(H-1)
 # Violet at the top, a softer lavender sky near the bottom.
 c=(int(91+43*t),int(10+51*t),int(171+31*t))
 rows.append(bytearray(bytes(c)*W))
rng=random.Random(4712)
# Heightfields are unions of circular cloud lobes, drawn from far to near.
for base,radius,color in [(160,125,(123,57,178)),(315,116,(144,81,191)),(480,102,(163,107,202)),(660,116,(185,141,216)),(805,108,(207,177,229))]:
 circles=[]
 x=-radius
 while x<W+radius:
  rad=radius*rng.uniform(.65,1.35)
  circles.append((x,base+rng.uniform(-42,30),rad))
  x+=radius*rng.uniform(.9,1.35)
 for x in range(W):
  top=base+radius*.35
  for cx,cy,rad in circles:
   dx=x-cx
   if abs(dx)<rad:top=min(top,cy-math.sqrt(rad*rad-dx*dx))
  y0=max(0,int(top));rgb=bytes(color)
  for y in range(y0,H): rows[y][x*3:x*3+3]=rgb
# Calm unified dim so white controls remain readable; no blur claim.
for y,row in enumerate(rows):
 for x in range(W):
  # A subtle dark-violet edge vignette, baked into the image.
  dx=(x-W/2)/(W/2);dy=(y-H/2)/(H/2)
  mul=.82-.12*min(1,(dx*dx+dy*dy)/2)
  p=x*3
  row[p]=int(row[p]*mul);row[p+1]=int(row[p+1]*mul);row[p+2]=int(row[p+2]*mul)
def chunk(k,d):return struct.pack('>I',len(d))+k+d+struct.pack('>I',zlib.crc32(k+d)&0xffffffff)
raw=b''.join(b'\x00'+row for row in rows)
png=b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',struct.pack('>IIBBBBB',W,H,8,2,0,0,0))+chunk(b'IDAT',zlib.compress(raw,9))+chunk(b'IEND',b'')
target=Path(__file__).resolve().parents[1]/'src/main/resources/assets/orbit/clouds.png'
target.parent.mkdir(parents=True,exist_ok=True);target.write_bytes(png)
print('Generated original cloud wallpaper:',W,H,len(png),'bytes')

"""Original Orbit pixel-Earth icon, generated without external libraries or Minecraft assets."""
import math
import struct
import zlib
from pathlib import Path

SIZE = 512
SCALE = SIZE / 256
pixels = bytearray(SIZE * SIZE * 4)
def color(hexvalue):
    return tuple(bytes.fromhex(hexvalue.lstrip('#'))) + (255,)
def rect(x0,y0,x1,y1,c):
    x0,y0,x1,y1=[int(round(v*SCALE)) for v in (x0,y0,x1,y1)]
    x0=max(0,x0);y0=max(0,y0);x1=min(SIZE,x1);y1=min(SIZE,y1)
    row=bytes(c)*max(0,x1-x0)
    for y in range(y0,y1): pixels[(y*SIZE+x0)*4:(y*SIZE+x1)*4]=row

def circle(x,y,r,c):
    for yy in range(int((y-r)*SCALE),int((y+r)*SCALE)+1):
        if not 0<=yy<SIZE:continue
        d=(yy/SCALE-y)
        dx=math.sqrt(max(0,r*r-d*d))
        rect(x-dx,yy/SCALE,x+dx,(yy+1)/SCALE,c)

def line(a,b,width,c):
    length=math.hypot(b[0]-a[0],b[1]-a[1]);steps=max(1,int(length*SCALE*1.5))
    for i in range(steps+1):
        t=i/steps;circle(a[0]+(b[0]-a[0])*t,a[1]+(b[1]-a[1])*t,width/2,c)

def polygon(points,c):
    pts=[(x*SCALE,y*SCALE) for x,y in points]
    for y in range(max(0,int(min(p[1] for p in pts))),min(SIZE,int(max(p[1] for p in pts))+1)):
        hits=[]
        for i,(x1,y1) in enumerate(pts):
            x2,y2=pts[(i+1)%len(pts)]
            if (y1<=y<y2) or (y2<=y<y1):hits.append(x1+(y-y1)*(x2-x1)/(y2-y1))
        hits.sort()
        for j in range(0,len(hits)-1,2): rect(hits[j]/SCALE,y/SCALE,hits[j+1]/SCALE,(y+1)/SCALE,c)

def orbit(t):
    x=110*math.cos(t);y=37*math.sin(t);a=.43
    return (128+x*math.cos(a)-y*math.sin(a),128+x*math.sin(a)+y*math.cos(a))
def arc(start,end):
    points=[orbit(start+(end-start)*i/160) for i in range(161)]
    for width,hexcolor in [(6,'0b316c'),(3.4,'3899ff'),(1,'a1d9ff')]:
        for a,b in zip(points,points[1:]):line(a,b,width,color(hexcolor))

arc(math.pi,math.pi*2)
# Square ocean, with a pixel border and no isometric cube sides.
rect(71,67,185,181,color('081d43'))
rect(74,70,182,178,color('2876d4'))
rect(78,74,178,174,color('1c62bd'))
rect(78,74,178,80,color('398ee1'))
# Original stepped continents on a 6px grid.
land='''
.####.......#....
######.....####.
#######...######
.######..#######
..####....######
...##....######.
....#.....###...
.....##...####..
.....###...###..
......###..##...
......###.......
......##.....##.
......#.....####
............###.
'''.strip().splitlines()
for row,text in enumerate(land):
    for col,char in enumerate(text):
        if char=='#':
            x=80+col*6;y=82+row*6
            rect(x,y,x+6,y+6,color('35be69'))
            if (row*7+col*3)%9==0:rect(x,y,x+6,y+6,color('70d18b'))
# Polar ice and water highlights.
rect(104,77,128,81,color('b8e3e7'));rect(80,169,117,173,color('7fbdcf'))
rect(152,117,158,121,color('3990dc'));rect(85,143,97,146,color('4c9fe6'))
arc(0,math.pi)
star=orbit(.57)
points=[]
for i in range(10):
    a=-math.pi/2+i*math.pi/5;r=13 if i%2==0 else 5.5
    points.append((star[0]+math.cos(a)*r,star[1]+math.sin(a)*r))
polygon(points,color('916320'))
points2=[(star[0]+(x-star[0])*.84,star[1]+(y-star[1])*.84) for x,y in points]
polygon(points2,color('ffd16e'))
circle(star[0]-2,star[1]-2,1.5,color('fff1b7'))

def chunk(kind,data):return struct.pack('>I',len(data))+kind+data+struct.pack('>I',zlib.crc32(kind+data)&0xffffffff)
raw=b''.join(b'\x00'+pixels[y*SIZE*4:(y+1)*SIZE*4] for y in range(SIZE))
png=b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',struct.pack('>IIBBBBB',SIZE,SIZE,8,6,0,0,0))+chunk(b'IDAT',zlib.compress(raw,9))+chunk(b'IEND',b'')
root=Path(__file__).resolve().parents[1]
target=root/'src/main/resources/assets/orbit/icon.png'
target.parent.mkdir(parents=True,exist_ok=True);target.write_bytes(png)
print('Generated',target.relative_to(root),f'({SIZE}x{SIZE}, RGBA, {len(png)} bytes)')

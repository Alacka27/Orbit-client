import math

def inset(y,h,r):
    distance=r-y-.5 if y<r else y-(h-r)+.5 if y>=h-r else 0
    return 0 if distance==0 else max(0,math.ceil(r-math.sqrt(max(0,r*r-distance*distance))))
count=0
for w in (1,2,16,104,150,220,500):
 for h in (1,2,16,20,22,28,240):
  r=min(10,w//2,h//2)
  for row in range(h):
   outer=inset(row,h,r)
   assert 0<=outer<=w//2
   if row not in (0,h-1):
    inner=inset(row-1,h-2,max(0,r-1))+1
    assert inner>=outer,(w,h,row,outer,inner)
   assert inset(row,h,r)==inset(h-1-row,h,r)
  count+=1
print('PASS:',count,'rounded-panel geometry cases. Native renderer still needs in-game testing.')

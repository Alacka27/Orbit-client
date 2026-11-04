package dev.alacka.orbit.menu;
/** Profile rows never overlap the editor, notices or footer, even at 320x180 GUI pixels. */
public record ProfileListLayout(int x,int y,int width,int height,int rows,int rowY,int rowHeight,int step) {
    public static ProfileListLayout of(int sw,int sh){
        int w=Math.min(460,sw-24),h=Math.min(304,sh-20);boolean tiny=h<200;
        int ry=tiny?41:47,rh=tiny?30:37,step=tiny?36:42;
        int rows=Math.max(1,Math.min(3,(h-87-ry+step-rh-3)/step));
        return new ProfileListLayout((sw-w)/2,(sh-h)/2,w,h,rows,ry,rh,step);
    }
    public int editY(){return y+height-87;}
    public int noticeY(){return y+height-59;}
    public int footerY(){return y+height-30;}
}

package dev.alacka.orbit.menu;
/** Measured from the actual vanilla buttons, including server-provided dialog actions. */
public record PauseLayout(int x,int y,int width,int height,int buttonHeight,int step) {
    public static PauseLayout of(int sw,int sh,int count){
        int rows=3+Math.max(0,(count-3+1)/2),h=Math.min(sh-16,rows*24+64),w=Math.min(sw-24,300);
        int bh=Math.min(20,Math.max(12,(h-58)/Math.max(1,rows)-4));
        return new PauseLayout((sw-w)/2,(sh-h)/2,w,h,bh,bh+4);
    }
}

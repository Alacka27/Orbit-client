package dev.alacka.orbit;
import dev.alacka.orbit.menu.ClickMenuLayout;
public final class ClickMenuLayoutSelfTest {
    public static void main(String[] args) {
        int count = 0;
        for (int w : new int[]{320, 360, 427, 640, 854, 1280, 1920}) {
            for (int h : new int[]{180, 240, 270, 300, 360, 480, 720, 1080}) {
                var b = ClickMenuLayout.of(w, h);
                assert b.x() >= 10 && b.y() >= 10;
                assert b.x()+b.width() <= w-10 && b.y()+b.height() <= h-10;
                assert b.tabHeight() >= 14 && b.contentWidth() >= 168;
                assert b.tabY(4)+b.tabHeight() <= b.footerY()-2 : "tabs overlap player block";
                assert b.footerY()+32 <= b.y()+b.height()-8;
                assert b.contentX() > b.x()+b.sidebar();
                assert b.contentX()+b.contentWidth() == b.x()+b.width()-16;
                assert b.x()+b.width()-92 > b.x()+b.sidebar();
                count++;
            }
        }
        System.out.println("PASS: " + count + " responsive ClickGUI layouts");
    }
}

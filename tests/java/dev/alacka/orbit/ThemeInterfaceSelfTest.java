package dev.alacka.orbit;

import dev.alacka.orbit.config.ClientSettings;
import dev.alacka.orbit.config.HudPrefs;
import dev.alacka.orbit.config.OrbitConfig;
import dev.alacka.orbit.hud.HudLayout;
import dev.alacka.orbit.menu.ModuleGrid;
import dev.alacka.orbit.menu.UiPalette;
import java.awt.Color;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

public final class ThemeInterfaceSelfTest {
    private static int checks;
    private static void check(boolean value, String message) {
        checks++;
        if (!value) throw new AssertionError(message);
    }
    public static void main(String[] args) throws Exception {
        persistence();
        palette();
        grid();
        System.out.println("PASS themes, transparent chat and continuous grid: " + checks + " assertions");
    }
    private static void persistence() throws Exception {
        Path dir=Files.createTempDirectory("orbit-theme-test-");
        try {
            Path file=dir.resolve("settings.properties");
            check(!ClientSettings.load(file).transparentChat(),"new profiles retain ordinary chat");
            ClientSettings value=ClientSettings.DEFAULT.withTransparentChat(true);
            check(value.theme(0x42b983,60,75,false).transparentChat(),"theme edit preserves chat");
            check(value.combat(value.combat()).transparentChat(),"combat builder preserves chat");
            check(value.support(value.support()).transparentChat(),"inventory builder preserves chat");
            for(String name:new String[]{"NoFall","Jesus","Trajectories","KillAura","TriggerBot","AutoTotem","AutoArmor","AutoConsume","ElytraSwap"})
                check(value.module(name,true).transparentChat(),"module switch preserves chat");
            var placement=new HudLayout.Placement(HudLayout.Anchor.TOP_LEFT,48,64);
            HudPrefs hud=HudPrefs.DEFAULT.withPlacement("status",placement);
            ClientSettings.saveTogether(file,value,hud);
            check(ClientSettings.load(file).equals(value),"transparent chat survives disk round trip");
            check(HudPrefs.load(file).equals(hud),"theme save retains dragged HUD position");
            ClientSettings.saveTogether(file,null,hud.withOpacity(85));
            check(ClientSettings.load(file).equals(value),"HUD save retains chat preference");
            var named=new OrbitConfig(value,hud,true,12);
            check(OrbitConfig.fromProperties(named.toProperties("test")).equals(named),"named config includes chat and placement");
            var old=named.toProperties("old");old.remove("theme.transparentChat");
            check(!OrbitConfig.fromProperties(old).settings().transparentChat(),"older named configs use ordinary chat");
            old.setProperty("theme.transparentChat","perhaps");
            try { OrbitConfig.fromProperties(old);throw new AssertionError("invalid named boolean accepted"); }
            catch(IllegalArgumentException expected){checks++;}
            var properties=ClientSettings.read(file);properties.remove("chat.transparent");
            ClientSettings.store(file,properties,"old profile");
            check(!ClientSettings.load(file).transparentChat(),"older profile remains compatible");
            properties.setProperty("chat.transparent","perhaps");
            ClientSettings.store(file,properties,"malformed profile");
            byte[] before=Files.readAllBytes(file);
            try { value.save(file);throw new AssertionError("malformed profile overwritten"); }
            catch(IOException expected){checks++;}
            check(java.util.Arrays.equals(before,Files.readAllBytes(file)),"malformed profile is preserved byte-for-byte");
        } finally {
            try(var paths=Files.walk(dir)){for(Path p:paths.sorted(Comparator.reverseOrder()).toList())Files.delete(p);}
        }
    }
    private static void palette() {
        int base=ClientSettings.DEFAULT.accent();
        for(int alpha:new int[]{0,1,48,128,255}) for(int source:new int[]{0x481579,0x6c338e,0xb29bc0,0xffffff,0,0x303030}) {
            int argb=alpha<<24|source;
            check(UiPalette.cloudColor(argb,base)==argb,"default cloud pixels are unchanged");
            for(int accent:new int[]{0x6488e8,0x42b983,0xe87a9d,0xffffff,0,0x999999})
                check(UiPalette.cloudColor(argb,accent)>>>24==alpha,"cloud recolour retains alpha");
        }
        int source=0xff481579;
        int green=UiPalette.cloudColor(source,0x42b983);
        float[] hsv=Color.RGBtoHSB(green>>16&255,green>>8&255,green&255,null);
        check(hsv[0]>.2f&&hsv[0]<.5f,"green theme changes the cloud hue");
        int gray=UiPalette.cloudColor(source,0xaaaaaa);
        check((gray>>16&255)==(gray>>8&255)&&(gray>>8&255)==(gray&255),"neutral theme has no leftover purple");
        for(int accent:new int[]{0x6488e8,0x42b983,0xffffff,0}) {
            var prefs=ClientSettings.DEFAULT.theme(accent,55,70,true);
            int window=UiPalette.tint(773328934,false,prefs);
            check(window>>>24==55*255/100,"window transparency follows settings");
            if(accent!=base)check(window!=UiPalette.tint(773328934,false,ClientSettings.DEFAULT),"window tint follows theme");
            check(UiPalette.tint(0xfff3f3f3,false,prefs)==0xfff3f3f3,"neutral text remains legible");
        }
    }
    private static void grid() {
        for(int width=80;width<=650;width+=7) for(int height:new int[]{1,28,64,100,174,400}) for(int count:new int[]{0,1,8,25,100}) {
            var g=ModuleGrid.of(width,height,count);
            check(g.columns()>=1&&g.columns()<=3,"one to three responsive columns");
            check(g.cardWidth()>0,"positive card width");
            check(g.clampScroll(-100)==0,"scroll clamps at start");
            check(g.clampScroll(1e8)==g.maxScroll(),"scroll clamps at end");
            if(count>0) {
                check(g.x(count-1)+g.cardWidth()<=width,"last column remains inside viewport");
                check(g.y(count-1)+g.cardHeight()-g.maxScroll()<=height,"last row reachable by scrolling");
            } else check(g.contentHeight()==0&&g.maxScroll()==0,"empty category has no phantom scroll");
        }
        check(ModuleGrid.of(496,174,8).columns()==3,"normal RShift fits three columns");
        check(ModuleGrid.of(196,84,8).columns()==1,"small windows use one scrollable column");
        check(ModuleGrid.of(496,174,8).rows()==3,"module list has rows, not pages");
    }
}
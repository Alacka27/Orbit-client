package dev.alacka.orbit;

import dev.alacka.orbit.config.ClientSettings;
import dev.alacka.orbit.menu.ThemeLayout;
import dev.alacka.orbit.menu.UiPalette;
import dev.alacka.orbit.modules.Ballistics;
import java.nio.file.*;
import java.util.*;

public final class ModulesSettingsSelfTest {
    private static int checks;
    private static void check(boolean b,String label) {checks++;if(!b)throw new AssertionError(label);}
    private static void near(double a,double b) {check(Math.abs(a-b)<1e-8,"physics "+a+" != "+b);}
    public static void main(String[] args) throws Exception {
        Path dir=Files.createTempDirectory("orbit-settings-test-");
        try {
            Path a=dir.resolve(UUID.randomUUID()+".properties"), b=dir.resolve(UUID.randomUUID()+".properties");
            var base=ClientSettings.load(a);check(base.equals(ClientSettings.DEFAULT),"safe defaults");
            check(!base.noFall()&&!base.jesus()&&!base.trajectories(),"modules opt-in");
            var custom=base.theme(0x00ff80,35,80,false).module("NoFall",true).module("Jesus",true).module("Trajectories",true);
            custom.save(a);check(ClientSettings.load(a).equals(custom),"roundtrip");
            base.save(b);check(ClientSettings.load(b).equals(base),"identity isolation");
            Files.writeString(a,"\nexternal=value\n",StandardOpenOption.APPEND);custom.save(a);
            check(Files.readString(a).contains("external=value"),"unknown keys preserved");
            for (String invalid: List.of("accent=bad","accent=1234567","windowOpacity=86","buttonOpacity=0","animations=maybe","NoFall=wat","Jesus=no","Trajectories=1","accent=\\uQQQQ")) {
                Files.writeString(a,invalid);String before=Files.readString(a);boolean refused=false;
                try{custom.save(a);}catch(java.io.IOException e){refused=true;}
                check(refused,"invalid config refused: "+invalid);check(before.equals(Files.readString(a)),"invalid file unchanged");
            }
            boolean unknown=false;try{base.module("Fake",true);}catch(IllegalArgumentException e){unknown=true;}check(unknown,"unknown module refused");
            for(int w:new int[]{320,427,640,960,1920}) for(int h:new int[]{180,200,240,300,360,540,1080}) {
                var l=ThemeLayout.of(w,h);
                check(l.x()>=0&&l.y()>=0&&l.x()+l.width()<=w&&l.y()+l.height()<=h,"theme fits");
                check(l.opacityY()+2*l.step()+l.controlHeight()<=l.noticeY(),"controls avoid footer notice");
            }
            for(int color:new int[]{0,0xffffff,0xff0000,0x00ff00,0x0000ff,0xffff00,0x945cda,0xff9900}) {
                var p=base.theme(color,18,60,true);
                check(UiPalette.tint(0x2e181026,true,p)>>>24==45,"window alpha");
                check(UiPalette.tint(0xffff0000,false,p)==0xffff0000,"warnings unchanged");
                check(UiPalette.tint(0xffffffff,false,p)==0xffffffff,"text unchanged");
                int rgb=UiPalette.tint(0xac7846b7,true,p)&0xffffff;
                double lum=channel(rgb>>16)*.2126+channel(rgb>>8&255)*.7152+channel(rgb&255)*.0722;
                check(lum<=.18,"solid surface label contrast");
            }
            var v=new Ballistics.Vector(1,2,3);
            var arrow=Ballistics.nextVelocity(v,.99,.05,false);near(arrow.x(),.99);near(arrow.y(),1.93);near(arrow.z(),2.97);
            var thrown=Ballistics.nextVelocity(v,.99,.03,true);near(thrown.y(),1.9503);
            var water=Ballistics.nextVelocity(v,.8,.03,true);near(water.x(),.8);near(water.y(),1.576);
            System.out.println("Modules/settings checks: "+checks);
        } finally {try(var files=Files.walk(dir)){for(Path p:files.sorted(Comparator.reverseOrder()).toList())Files.delete(p);}}
    }
    private static double channel(int b){double c=b/255.;return c<=.04045?c/12.92:Math.pow((c+.055)/1.055,2.4);}
}

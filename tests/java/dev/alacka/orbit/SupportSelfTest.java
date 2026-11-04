package dev.alacka.orbit;

import dev.alacka.orbit.config.*;
import dev.alacka.orbit.menu.*;
import dev.alacka.orbit.modules.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

public final class SupportSelfTest {
    private static int checks;
    private static final List<String> MODULES=List.of("AutoTotem","AutoArmor","AutoConsume","ElytraSwap");
    private static void check(boolean value,String message) {
        checks++; if(!value) throw new AssertionError(message);
    }
    private static void refused(Runnable action,String message) {
        boolean failed=false;
        try { action.run(); } catch(IllegalArgumentException|NullPointerException e) { failed=true; }
        check(failed,message);
    }
    public static void main(String[] args) throws Exception {
        settings(); files(); slotsAndScores(); gate(); consumption(); layouts();
        System.out.println("Support/config/slot/ownership/layout checks: "+checks);
    }
    private static SupportSettings custom() {
        var d=SupportSettings.DEFAULT;
        return d.withTotem(d.totem().enabled(true).healthOnly(true).health(6).delay(12).hotbarFirst(false))
                .withArmor(d.armor().enabled(true).delay(13).protection(false))
                .withConsume(d.consume().enabled(true).option("food",false).option("potions",true).option("gapples",true)
                        .option("safeOnly",false).option("stopOnDamage",false).health(7).hunger(9).dangerRadius(15).delay(17))
                .withElytra(d.elytra().enabled(true).delay(11).protectFlight(false));
    }
    private static void settings() {
        var d=SupportSettings.DEFAULT;
        check(d.equals(SupportSettings.read(new Properties())),"legacy defaults");
        for(String module:MODULES) check(!d.enabled(module),"opt-in "+module);
        check(d.consume().food()&&!d.consume().potions()&&!d.consume().gapples(),"valuable consumables opt-in");
        check(d.consume().safeOnly()&&d.consume().stopOnDamage()&&d.elytra().protectFlight(),"safe defaults");
        Properties p=new Properties(); p.setProperty("foreign","kept"); custom().write(p);
        check(SupportSettings.read(p).equals(custom()),"all fields roundtrip");
        check(p.getProperty("foreign").equals("kept"),"unknown keys retained");
        for(String module:MODULES) {
            var on=d.enabled(module,true);
            for(String other:MODULES) check(on.enabled(other)==other.equals(module),"isolated toggle");
            check(on.enabled(module,false).equals(d),"toggle off");
            var merged=d.settingsFrom(module,custom());
            check(!merged.enabled(module),"settings cannot enable module");
            var expected=switch(module) {
                case "AutoTotem" -> d.withTotem(custom().totem().enabled(false));
                case "AutoArmor" -> d.withArmor(custom().armor().enabled(false));
                case "AutoConsume" -> d.withConsume(custom().consume().enabled(false));
                case "ElytraSwap" -> d.withElytra(custom().elytra().enabled(false));
                default -> throw new AssertionError(module);
            };
            check(merged.equals(expected),"isolated settings merge");
            check(custom().settingsFrom(module,d).enabled(module,false).settingsFrom(module,custom()).enabled(module,true).equals(custom()),"reset merges back");
        }
        for(String bad:List.of("totem.enabled=yes","totem.health=NaN","totem.health=Infinity","totem.health=0","totem.health=21",
                "totem.delay=0","totem.delay=21","totem.hotbarFirst=1","armor.delay=-1","armor.delay=41","armor.protection=maybe",
                "consume.food=yes","consume.potions=2","consume.gapples=on","consume.safeOnly=0","consume.stopOnDamage=no",
                "consume.health=0","consume.health=21","consume.hunger=0","consume.hunger=21","consume.hunger=2.5",
                "consume.dangerRadius=NaN","consume.dangerRadius=1","consume.dangerRadius=17","consume.delay=0","consume.delay=41",
                "elytra.delay=0","elytra.delay=41","elytra.protectFlight=on")) {
            String[] kv=bad.split("=",2); Properties invalid=new Properties(); invalid.setProperty("support."+kv[0],kv[1]);
            refused(()->SupportSettings.read(invalid),"reject "+bad);
        }
        refused(()->d.enabled("KillAura"),"wrong family");
        refused(()->d.enabled("AutoEat",true),"no accidental alias");
        refused(()->d.settingsFrom("NotReal",d),"invalid dialog target");
        refused(()->d.consume().option("eatEverything",true),"invalid submodule");
        for(int i=1;i<=20;i++) {
            check(d.totem().health(i).health()==i,"health boundary");
            check(d.totem().delay(i).delay()==i,"totem delay");
            check(d.consume().hunger(i).hunger()==i,"hunger");
        }
        for(int i=1;i<=40;i++) {
            check(d.armor().delay(i).delay()==i,"armor delay");
            check(d.consume().delay(i).delay()==i,"consume delay");
            check(d.elytra().delay(i).delay()==i,"swap delay");
        }
    }
    private static void files() throws Exception {
        Path dir=Files.createTempDirectory("orbit-support-");
        try {
            Path file=dir.resolve("settings.properties");
            Files.writeString(file,"accent=945cda\nwindowOpacity=18\nbuttonOpacity=60\nforeign=retained\ncombat.aura.enabled=true\n");
            var old=ClientSettings.load(file);
            check(old.support().equals(SupportSettings.DEFAULT)&&old.combat().aura().enabled(),"old profile migration");
            var next=old.support(custom());
            check(next.theme(0xa15be0,55,70,false).module("KillAura",false).module("Jesus",true).support().equals(custom()),"old edits retain support");
            for(String module:MODULES) check(next.module(module,false).support().equals(custom().enabled(module,false)),"ClientSettings toggle");
            next.save(file);
            check(ClientSettings.load(file).equals(next),"atomic per-profile roundtrip");
            check(ClientSettings.read(file).getProperty("foreign").equals("retained"),"profile unknown keys");
            var named=new OrbitConfig(next,HudPrefs.DEFAULT,true,12);
            Properties exported=named.toProperties("0.4.9");
            check(OrbitConfig.fromProperties(exported).equals(named),"named config full roundtrip");
            exported.keySet().removeIf(k->k.toString().startsWith("support."));
            check(OrbitConfig.fromProperties(exported).settings().support().equals(SupportSettings.DEFAULT),"old named config OFF");
            check(new ClientSettings(0x945cda,18,60,true,false,false,false).support().equals(SupportSettings.DEFAULT),"7-arg ctor");
            check(new ClientSettings(0x945cda,18,60,true,false,false,false,CombatSettings.DEFAULT).support().equals(SupportSettings.DEFAULT),"8-arg ctor");
            Properties invalid=ClientSettings.read(file); invalid.setProperty("support.consume.delay","0");
            ClientSettings.store(file,invalid,"invalid test"); byte[] before=Files.readAllBytes(file);
            boolean failure=false; try { next.save(file); } catch(IOException e) { failure=true; }
            check(failure,"corrupt profile refused");
            check(Arrays.equals(before,Files.readAllBytes(file)),"corrupt profile not overwritten");
        } finally {
            try(var paths=Files.walk(dir)) {
                for(Path p:paths.sorted(Comparator.reverseOrder()).toList()) Files.delete(p);
            }
        }
    }
    private static void slotsAndScores() {
        Set<Integer> slots=new HashSet<>();
        for(int i=0;i<36;i++) {
            int mapped=SupportMath.menuSlot(i);
            check(mapped>=9&&mapped<=44,"player main inventory range");
            check(slots.add(mapped),"bijective mapping");
            check(mapped==(i<9?36+i:i),"hotbar/menu mapping");
        }
        check(slots.size()==36,"all main inventory slots");
        check(SupportMath.armorSlot("HEAD")==5&&SupportMath.armorSlot("CHEST")==6
                &&SupportMath.armorSlot("LEGS")==7&&SupportMath.armorSlot("FEET")==8,"native armor slots");
        for(int invalid:new int[]{-1,36,40,45,99}) refused(()->SupportMath.menuSlot(invalid),"invalid source");
        refused(()->SupportMath.armorSlot("OFFHAND"),"no armor/offhand confusion");
        double leather=SupportMath.armorScore(3,0,0,1,true),diamond=SupportMath.armorScore(8,2,0,1,true);
        check(diamond>leather,"base armor comparison");
        check(SupportMath.armorScore(6,0,4,1,true)>diamond,"general protection counted");
        check(SupportMath.armorScore(6,0,4,1,false)<diamond,"protection switch");
        check(SupportMath.armorScore(8,2,0,1,true)>SupportMath.armorScore(8,2,0,.1,true),"durability tie-break");
        check(SupportMath.armorScore(Double.NaN,0,0,1,true)==Double.NEGATIVE_INFINITY,"NaN armor");
        check(SupportMath.armorScore(1,-1,0,1,true)==Double.NEGATIVE_INFINITY,"negative toughness");
        check(SupportMath.armorScore(1,0,0,Double.POSITIVE_INFINITY,true)==Double.NEGATIVE_INFINITY,"infinite durability");
        check(SupportMath.lowHealth(6,2,8)&&!SupportMath.lowHealth(6,3,8),"absorption included");
        check(!SupportMath.lowHealth(0,0,8)&&!SupportMath.lowHealth(Double.NaN,0,8),"dead/invalid HP");
    }
    private static void gate() {
        var gate=new SupportGate();
        check(gate.claim(0,SupportGate.Action.ARMOR,6),"first armor action");
        check(!gate.claim(0,SupportGate.Action.TOTEM,2),"one full operation per tick");
        check(gate.claim(1,SupportGate.Action.TOTEM,2),"emergency totem not held by armor cooldown");
        check(!gate.claim(2,SupportGate.Action.TOTEM,2),"totem own delay");
        check(!gate.claim(1,SupportGate.Action.ELYTRA,4),"no time reversal");
        check(gate.claim(2,SupportGate.Action.CONSUME,8),"other category can proceed next tick");
        check(!gate.claim(5,SupportGate.Action.ARMOR,6),"armor cooldown retained across other actions");
        check(gate.claim(6,SupportGate.Action.ARMOR,6),"armor ready");
        check(gate.claim(1000,SupportGate.Action.ELYTRA,4),"idle does not lose action");
        check(!gate.claim(1000,SupportGate.Action.CONSUME,8),"no idle burst");
        gate.reset(); check(gate.claim(0,SupportGate.Action.TOTEM,2),"world reset");
        refused(()->gate.claim(-1,SupportGate.Action.TOTEM,2),"negative tick");
        refused(()->gate.claim(1,SupportGate.Action.TOTEM,0),"invalid delay");
        refused(()->gate.claim(1,null,2),"invalid category");
        for(int delay=1;delay<=40;delay++) {
            gate.reset(); check(gate.claim(0,SupportGate.Action.ARMOR,delay),"delay first");
            for(int tick=1;tick<delay;tick++) check(!gate.claim(tick,SupportGate.Action.ARMOR,delay),"full delay");
            check(gate.claim(delay,SupportGate.Action.ARMOR,delay),"ready exact boundary");
        }
    }
    private static void consumption() {
        var session=new ConsumeSession();
        check(session.observe(0,0,20,false,true,false,false,true)==ConsumeSession.Stop.NONE,"inactive");
        for(int original=0;original<9;original++) for(int owned=0;owned<9;owned++) {
            session.begin(10,original,owned,32,20);
            check(session.active()&&session.ownedSlot()==owned,"start ownership");
            check(session.observe(10,owned,20,false,true,false,false,true)==ConsumeSession.Stop.NONE,"allow initial use tick");
            check(session.observe(11,owned,20,true,true,false,false,true)==ConsumeSession.Stop.NONE,"normal use");
            check(session.restoreSlot(owned)==original,"restore automation-owned slot");
            int manual=(owned+1)%9;
            check(session.observe(12,manual,20,false,true,false,false,true)==ConsumeSession.Stop.SLOT,"manual slot interrupts");
            check(session.restoreSlot(manual)==manual,"never undo scroll/number key");
            session.clear(); check(!session.active()&&session.restoreSlot(owned)==owned,"released ownership");
        }
        session.begin(0,2,5,32,20);
        check(session.observe(1,5,20,true,false,false,false,true)==ConsumeSession.Stop.CONTEXT,"menu/disabled/focus");
        check(session.observe(2,5,20,true,true,true,false,true)==ConsumeSession.Stop.DANGER,"unsafe");
        check(session.observe(3,5,20,true,true,false,true,true)==ConsumeSession.Stop.DAMAGE,"absorbed damage flag");
        check(session.observe(4,5,19,true,true,false,false,true)==ConsumeSession.Stop.DAMAGE,"health loss");
        check(session.observe(5,5,18,true,true,false,true,false)==ConsumeSession.Stop.NONE,"opt-out damage interruption");
        check(session.observe(6,5,20,false,true,false,false,true)==ConsumeSession.Stop.FINISHED,"normal completion");
        check(session.observe(52,5,20,true,true,false,false,true)==ConsumeSession.Stop.TIMEOUT,"bounded stuck use");
        refused(()->session.begin(1,1,1,1,20),"no overlapping owner");
        session.clear();
        refused(()->session.begin(0,-1,0,32,20),"invalid original");
        refused(()->session.begin(0,0,9,32,20),"invalid selected");
        refused(()->session.begin(0,0,1,0,20),"invalid duration");
        refused(()->session.begin(0,0,1,32,Double.NaN),"invalid health");
    }
    private static void layouts() {
        for(int w:new int[]{214,320,427,640,960}) for(int h:new int[]{150,180,200,240,400,540}) {
            var box=ModuleSettingsLayout.of(w,h);
            check(box.x()>=0&&box.y()>=0&&box.x()+box.width()<=w&&box.y()+box.height()<=h,"settings bounds");
            for(int controls:new int[]{2,4,9}) {
                int shown=Math.min(controls,box.rows());
                check(box.pages(controls)*box.rows()>=controls,"all controls paginated");
                check(box.controlY(shown-1)+box.rowHeight()<box.helpY(),"no controls/help overlap");
                check(box.helpY()<box.footerY()&&box.footerY()+20<=h,"help/footer bounds");
            }
        }
        for(int width:new int[]{100,180,260,390,510}) for(int height=28;height<=420;height++) {
            var grid=ModuleGrid.of(width,height,8);
            check(grid.columns()*grid.rows()>=8,"all eight PvP cards reachable");
            check(grid.columns()<=3&&grid.columns()>=1,"responsive column count");
            check(grid.contentHeight()-grid.maxScroll()<=height,"continuous content scrolls into view");
            check(grid.y(7)+grid.cardHeight()-grid.maxScroll()<=height,"last row reachable");
        }
    }
}
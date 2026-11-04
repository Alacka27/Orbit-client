package dev.alacka.orbit;

import dev.alacka.orbit.config.*;
import dev.alacka.orbit.config.CombatSettings.*;
import dev.alacka.orbit.menu.*;
import dev.alacka.orbit.modules.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

public final class CombatSelfTest {
    private static int checks;
    private static void check(boolean value,String message) {
        checks++; if (!value) throw new AssertionError(message);
    }
    private static void near(double a,double b) { check(Math.abs(a-b)<1e-5,a+" != "+b); }
    private static void refused(Runnable action,String message) {
        boolean failed=false;
        try { action.run(); } catch (IllegalArgumentException | NullPointerException e) { failed=true; }
        check(failed,message);
    }
    public static void main(String[] args) throws Exception {
        persistence();
        scheduler();
        selection();
        layouts();
        System.out.println("Combat/config/layout checks: "+checks);
    }

    private static void persistence() throws Exception {
        CombatSettings d=CombatSettings.DEFAULT;
        check(!d.aura().enabled()&&!d.trigger().enabled(),"both modules opt-in");
        check(d.equals(CombatSettings.read(new Properties())),"old settings fallback");
        CombatSettings custom=d.withAura(d.aura().enabled(true).range(4.7).priority(Priority.HEALTH)
                .maxTargets(3).rotation(Rotation.NONE).rotationStep(45).targets(Targets.MOBS)
                .ignoreTeam(false).onlyWeapon(false).pauseUsingItem(false))
                .withTrigger(d.trigger().enabled(true).mode(AttackMode.CPS).cps(13).range(2.4)
                        .targets(Targets.ALL).ignoreTeam(false).onlyWeapon(false).pauseUsingItem(false));
        Properties props=new Properties();props.setProperty("external","retained");
        custom.write(props);
        check(custom.equals(CombatSettings.read(props)),"all combat fields roundtrip");
        check(props.getProperty("external").equals("retained"),"combat write preserves unknown keys");
        check(d.aura().range()==3.0&&!d.aura().enabled(),"defaults remain immutable");

        for (String bad:List.of("aura.range=NaN","aura.range=Infinity","aura.range=0.9","aura.range=6.1",
                "aura.enabled=yes","aura.priority=FAST","aura.rotation=SILENT","aura.maxTargets=0",
                "aura.maxTargets=6","aura.rotationStep=4","aura.rotationStep=91","aura.targets=FAKE",
                "trigger.mode=GRIM","trigger.cps=0","trigger.cps=21","trigger.cps=1.5",
                "trigger.range=-1","trigger.range=NaN","trigger.enabled=1","trigger.onlyWeapon=maybe")) {
            String[] item=bad.split("=",2);Properties invalid=new Properties();
            invalid.setProperty("combat."+item[0],item[1]);
            refused(()->CombatSettings.read(invalid),"reject "+bad);
        }
        Path dir=Files.createTempDirectory("orbit-combat-");
        try {
            Path file=dir.resolve("profile.properties");
            Files.writeString(file,"accent=945cda\nwindowOpacity=18\nbuttonOpacity=60\nexternal=kept\n");
            ClientSettings old=ClientSettings.load(file);
            check(old.combat().equals(d),"0.4.5/0.4.7 profile migration");
            ClientSettings next=old.combat(custom).module("NoFall",true).theme(0xa15be0,55,70,false);
            check(next.combat().equals(custom),"theme and old module edits retain combat");
            next.save(file);
            check(ClientSettings.load(file).equals(next),"profile atomic roundtrip");
            check(ClientSettings.read(file).getProperty("external").equals("kept"),"unknown profile key retained");
            check(!next.module("KillAura",false).combat().aura().enabled(),"aura toggle");
            check(!next.module("TriggerBot",false).combat().trigger().enabled(),"trigger toggle");
            check(next.module("AttackAura",false).equals(next.module("KillAura",false)),"AttackAura alias");
            check(new ClientSettings(0x945cda,18,60,true,false,false,false).combat().equals(d),"legacy constructor");
            Properties malformed=ClientSettings.read(file);
            malformed.setProperty("combat.trigger.cps","0");
            ClientSettings.store(file,malformed,"invalid test");
            byte[] before=Files.readAllBytes(file);
            boolean failure=false;
            try { next.save(file); } catch (IOException e) { failure=true; }
            check(failure,"invalid profile refused");
            check(Arrays.equals(before,Files.readAllBytes(file)),"invalid file not overwritten");

            OrbitConfig export=new OrbitConfig(next,HudPrefs.DEFAULT,true,12.0);
            check(OrbitConfig.fromProperties(export.toProperties("0.4.8")).equals(export),"named config includes combat");
            Properties legacy=export.toProperties("0.4.7");
            legacy.keySet().removeIf(k->k.toString().startsWith("combat."));
            check(OrbitConfig.fromProperties(legacy).settings().combat().equals(d),"old named configs disable new modules");
        } finally {
            try (var files=Files.walk(dir)) {
                for (Path p:files.sorted(Comparator.reverseOrder()).toList()) Files.delete(p);
            }
        }
    }

    private static void scheduler() {
        CombatScheduler gate=new CombatScheduler();
        check(!gate.ready(1,.9f,AttackMode.COOLDOWN),"wait for full cooldown");
        check(!gate.ready(1,Float.NaN,AttackMode.COOLDOWN),"NaN cooldown refused");
        check(gate.ready(1,1f,AttackMode.COOLDOWN),"full cooldown");
        gate.attacked(1,10);
        check(!gate.ready(1,1f,AttackMode.COOLDOWN),"second module blocked this tick");
        check(!gate.ready(1,1f,AttackMode.CPS),"CPS also shares budget");
        check(!gate.ready(2,.1f,AttackMode.CPS),"CPS delay");
        check(gate.ready(3,.1f,AttackMode.CPS),"CPS does not pretend to be 1.9 cooldown");
        for (int cps=1;cps<=20;cps++) {
            gate.reset();int attacks=0;
            for (long tick=0;tick<2000;tick++) {
                if (gate.ready(tick,0,AttackMode.CPS)) {
                    gate.attacked(tick,cps);attacks++;
                    check(!gate.ready(tick,1,AttackMode.COOLDOWN),"no duplicate for CPS "+cps);
                    check(!gate.ready(tick,1,AttackMode.CPS),"no duplicate CPS hit");
                }
            }
            check(Math.abs(attacks-cps*100)<=1,"correct average "+cps+" CPS: "+attacks);
        }
        gate.reset();gate.attacked(0,1);
        check(gate.ready(10000,0,AttackMode.CPS),"resume after long idle");
        gate.attacked(10000,1);
        check(!gate.ready(10001,0,AttackMode.CPS),"no catch-up burst after idle");
        check(gate.ready(10020,0,AttackMode.CPS),"new regular interval");
    }

    private static void selection() {
        near(CombatMath.wrapDegrees(270),-90);near(CombatMath.wrapDegrees(-181),179);
        near(CombatMath.stepYaw(179,-179,15),181);
        near(CombatMath.stepYaw(-179,179,15),-181);
        near(CombatMath.stepPitch(80,100,30),90);
        near(CombatMath.stepPitch(-80,-100,30),-90);
        near(CombatMath.angles(0,0,1).yaw(),0);
        near(CombatMath.angles(1,0,0).yaw(),-90);
        for (int a=-720;a<=720;a+=3) for(int b=-180;b<=180;b+=13) {
            float next=CombatMath.stepYaw(a,b,20);
            check(Math.abs(next-a)<=20.0001,"bounded yaw step");
            check(Math.abs(CombatMath.wrapDegrees(b-next))<=Math.abs(CombatMath.wrapDegrees(b-a))+1e-4,"approach target");
        }
        List<CombatMath.Candidate> candidates=List.of(new CombatMath.Candidate(1,4,80,10.),
                new CombatMath.Candidate(2,1,120,20.),new CombatMath.Candidate(3,9,5,5.),
                new CombatMath.Candidate(4,2,1,null));
        check(ids(candidates,Priority.CLOSEST).equals(List.of(2,4,1,3)),"closest priority");
        check(ids(candidates,Priority.ANGLE).equals(List.of(4,3,1,2)),"angle priority");
        check(ids(candidates,Priority.HEALTH).equals(List.of(3,1,2,4)),"known HP only; unknown last");
        var players=List.of(new CombatMath.Candidate(7,4,5,null),new CombatMath.Candidate(8,1,40,null));
        check(ids(players,Priority.HEALTH).equals(List.of(8,7)),"unknown player health falls back to distance");
        check(CombatMath.cycleIndex(List.of(),null,null)==-1,"empty pool");
        check(CombatMath.cycleIndex(List.of(1,2,3),2,null)==1,"retain aim until attack");
        check(CombatMath.cycleIndex(List.of(1,2,3),null,2)==2,"advance after attack");
        check(CombatMath.cycleIndex(List.of(1,2,3),null,3)==0,"wrap target pool");
        check(CombatMath.cycleIndex(List.of(1),9,9)==0,"removed target");
    }
    private static List<Integer> ids(List<CombatMath.Candidate> values,Priority priority) {
        return values.stream().sorted(CombatMath.order(priority)).map(CombatMath.Candidate::id).toList();
    }

    private static void layouts() {
        for(int w:new int[]{320,360,427,640,960,1920}) for(int h:new int[]{180,200,240,266,300,400,540,1080})
            for(int dx:new int[]{-10000,0,10000}) for(int dy:new int[]{-10000,0,10000}) {
                var box=ModuleSettingsLayout.of(w,h,dx,dy);
                check(box.x()>=0&&box.y()>=0&&box.x()+box.width()<=w&&box.y()+box.height()<=h,"settings fit");
                check(box.controlY(box.rows()-1)+box.rowHeight()<=box.helpY()-5,"controls above help");
                check(box.helpY()+9<=box.footerY()-5,"help above footer");
                check(box.footerY()+20<=box.y()+box.height(),"footer inside panel");
                check(box.pages(9)*box.rows()>=9&&box.pages(7)*box.rows()>=7,"all options reachable");
                var click=ClickMenuLayout.of(w,h,dx,dy);
                int room=Math.max(1,click.height()-76);
                var grid=ModuleGrid.of(click.contentWidth()-8,room,4);
                check(grid.contentHeight()-grid.maxScroll()<=room,"continuous list fits viewport after scroll");
                check(grid.cardHeight()>=28,"usable cards");
                check(grid.rows()*grid.columns()>=4,"all modules reachable without pages");
                check(grid.x(3)+grid.cardWidth()<=click.contentWidth()-8,"last column in bounds");
            }
        check(ModuleGrid.of(300,100,0).rows()==0,"empty category");
    }
}
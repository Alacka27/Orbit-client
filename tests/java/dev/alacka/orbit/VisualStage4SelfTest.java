package dev.alacka.orbit;

import dev.alacka.orbit.config.*;
import dev.alacka.orbit.modules.VisualLedger;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

public final class VisualStage4SelfTest {
    private static int checks;
    private static void check(boolean condition, String reason) {
        checks++; if (!condition) throw new AssertionError(reason);
    }
    private static void reject(Runnable action) {
        try { action.run(); throw new AssertionError("Invalid preferences accepted"); }
        catch (IllegalArgumentException expected) { checks++; }
    }
    public static void main(String[] args) throws Exception {
        preferences(); persistence(); controls(); reportedHealth(); observations(); boundedHistory(); colors();
        System.out.println("PASS visual stage 4 and internal controls: " + checks + " assertions");
    }
    private static void reportedHealth() {
        check(Double.isNaN(VisualLedger.declaredHealthScore(true,null)),"missing scoreboard entry is unknown, not zero");
        check(Double.isNaN(VisualLedger.declaredHealthScore(false,null)),"missing ordinary score is unknown");
        for (int value : new int[]{-1,0,1,7,20,4096,4097,Integer.MAX_VALUE,Integer.MIN_VALUE}) {
            double read=VisualLedger.declaredHealthScore(true,value);
            check(value>=0&&value<=4096 ? read==value : Double.isNaN(read),"declared heart-score bounds");
            check(Double.isNaN(VisualLedger.declaredHealthScore(false,value)),"ordinary integer scores never become HP");
        }
    }
    private static void preferences() {
        var defaults=VisualSettings.DEFAULT;
        check(!defaults.anyEnabled(),"new visual modules opt in");
        check(VisualSettings.MODULES.size()==7,"seven new visual modules, no duplicate trajectories");
        for (String module:VisualSettings.MODULES) {
            check(!defaults.enabled(module),"default off");
            var enabled=defaults.enabled(module,true);
            check(enabled.enabled(module),"toggle on");
            check(enabled.enabled(module,false).equals(defaults),"toggle off canonicalizes");
            var p=new Properties();enabled.write(p);
            check(VisualSettings.read(p).equals(enabled),"visual property codec");
            check(ClientSettings.DEFAULT.module(module,true).visual().equals(enabled),"registry settings mapping");
        }
        var mutable=new HashMap<String,Boolean>();mutable.put("TargetHUD",true);
        var immutable=new VisualSettings(mutable,32,4,8,24,true,false,true,true);
        mutable.clear();check(immutable.enabled("TargetHUD"),"immutable module map");
        try { immutable.modules().clear(); throw new AssertionError("mutable preferences"); }
        catch (UnsupportedOperationException expected) { checks++; }
        reject(()->defaults.enabled("Zoom",false));
        reject(()->defaults.number("range",7));reject(()->defaults.number("range",65));
        reject(()->defaults.number("numberTicks",11));reject(()->defaults.number("particleCount",33));
        reject(()->defaults.number("targetSeconds",0));reject(()->defaults.option("unknown",true));
        reject(()->new VisualSettings(Map.of("Unknown",true),32,4,8,24,true,false,true,true));
        var draft=defaults.enabled("HitParticles",true).number("targetSeconds",7).number("range",48).option("healing",false);
        var rebased=defaults.enabled("TargetHUD",true).settingsFrom("TargetHUD",draft);
        check(rebased.enabled("TargetHUD")&&!rebased.enabled("HitParticles"),"dialog never overwrites enabled states");
        check(rebased.targetSeconds()==7&&rebased.range()==48&&rebased.healing(),"dialog rebases only own options");
        var all=defaults;
        for(String module:VisualSettings.MODULES)all=all.enabled(module,true);
        var settings=ClientSettings.DEFAULT.withVisual(all);
        check(settings.theme(0x52bd8d,50,70,false).visual().equals(all),"theme preserves visual settings");
        check(settings.withBinds(BindPrefs.DEFAULT.with("NoFall",75)).visual().equals(all),"binds preserve visual");
        check(settings.combat(CombatSettings.DEFAULT).visual().equals(all),"combat preserves visual");
        check(settings.support(SupportSettings.DEFAULT).visual().equals(all),"items preserve visual");
        check(settings.withTransparentChat(true).visual().equals(all),"chat preserves visual");
        check(settings.module("NoFall",true).visual().equals(all),"old module preserves visual");
    }
    private static void persistence() throws Exception {
        Path dir=Files.createTempDirectory("orbit-visual-stage4-");
        try {
            Path a=dir.resolve("a.properties"),b=dir.resolve("b.properties");
            var settings=ClientSettings.DEFAULT.withVisual(VisualSettings.DEFAULT.enabled("TargetHUD",true)
                    .enabled("DamageNumbers",true).number("range",48).option("healing",false))
                    .withBinds(BindPrefs.DEFAULT.with("Menu",297).with("HitParticles",1003));
            ClientSettings.saveTogether(a,settings,HudPrefs.DEFAULT);
            check(ClientSettings.load(a).equals(settings),"complete per-profile roundtrip");
            ClientSettings.saveTogether(a,null,HudPrefs.DEFAULT.withOpacity(80));
            check(ClientSettings.load(a).equals(settings),"HUD save preserves all new state");
            check(ClientSettings.load(b).equals(ClientSettings.DEFAULT),"profiles isolated");
            var config=new OrbitConfig(settings,HudPrefs.DEFAULT,false,12);
            check(config.freeLook(),"legacy constructor cannot disable FreeLook");
            var props=config.toProperties("test");
            check(!props.containsKey("module.FreeLook")&&!props.containsKey("module.Zoom"),"built-in switches no longer exported");
            check(OrbitConfig.fromProperties(props).equals(config),"named config roundtrip");
            props.setProperty("module.FreeLook","false");props.setProperty("module.Zoom","false");
            check(OrbitConfig.fromProperties(props).freeLook()&&OrbitConfig.fromProperties(props).settings().zoom(),"legacy switches ignored");
            var legacy=ClientSettings.read(a);legacy.setProperty("Zoom","false");
            legacy.setProperty("bind.FreeLook","-1");legacy.setProperty("bind.Zoom","-1");
            ClientSettings.store(a,legacy,"legacy");
            check(ClientSettings.load(a).zoom(),"old false cannot switch Zoom off");
            check(!ClientSettings.load(a).binds().keys().containsKey("Zoom"),"old unbound built-in migrates to default");
            settings.save(a);
            check(!ClientSettings.read(a).containsKey("Zoom"),"obsolete field removed on explicit save");
            for(String invalid:List.of("0","65","NaN")) {
                var bad=ClientSettings.read(a);bad.setProperty("visual.range",invalid);ClientSettings.store(a,bad,"invalid");
                byte[] bytes=Files.readAllBytes(a);
                try { settings.save(a);throw new AssertionError("invalid file overwritten"); }
                catch(IOException expected) { checks++; }
                check(Arrays.equals(bytes,Files.readAllBytes(a)),"malformed file preserved byte-for-byte");
                Files.delete(a);settings.save(a);
            }
        } finally {
            try(var paths=Files.walk(dir)) { for(Path p:paths.sorted(Comparator.reverseOrder()).toList())Files.delete(p); }
        }
    }
    private static void controls() {
        check(BindPrefs.defaultCode("Menu")==344&&BindPrefs.defaultCode("FreeLook")==342
                &&BindPrefs.defaultCode("Zoom")==67&&BindPrefs.defaultCode("ElytraSwap")==71,"internal default controls");
        for(String builtin:List.of("Menu","FreeLook","Zoom"))reject(()->BindPrefs.DEFAULT.with(builtin,BindPrefs.NONE));
        reject(()->BindPrefs.DEFAULT.with("Menu",BindPrefs.MOUSE));
        check(BindPrefs.DEFAULT.with("NoFall",BindPrefs.NONE).keys().get("NoFall")==-1,"ordinary modules may be unbound");
        var p=new Properties();p.setProperty("bind.Zoom","-1");p.setProperty("bind.FreeLook","-1");p.setProperty("bind.Menu","-1");
        check(BindPrefs.read(p).equals(BindPrefs.DEFAULT),"legacy none controls migrate safely");
        reject(()->ClientSettings.DEFAULT.module("FreeLook",false));
        reject(()->ClientSettings.DEFAULT.module("Zoom",false));
        check(new ClientSettings(0x945cda,50,70,true,false,false,false,CombatSettings.DEFAULT,
                SupportSettings.DEFAULT,false,false,BindPrefs.DEFAULT).zoom(),"legacy API cannot disable built-in");
    }
    private static void observations() {
        var ledger=new VisualLedger();var id=UUID.randomUUID();
        check(Double.isNaN(ledger.observe(id,20,true,1)),"first sample is not a damage event");
        check(ledger.observe(id,14,true,2)==6,"observed damage");
        check(ledger.observe(id,17,true,3)==-3,"observed healing");
        check(Double.isNaN(ledger.observe(id,0,false,4)),"unknown health not a fabricated death");
        check(ledger.trackedHealth()==0,"unknown health erases baseline");
        check(Double.isNaN(ledger.observe(id,20,true,5)),"returning knowledge needs a baseline");
        check(Double.isNaN(ledger.observe(id,10,true,100)),"stale observations not displayed as damage");
        check(Double.isNaN(ledger.observe(id,Double.NaN,true,101)),"NaN is unknown");
        check(Double.isNaN(ledger.observe(id,-1,true,102)),"negative HP invalid");
        check(Double.isNaN(ledger.observe(id,5000,true,103)),"unbounded HP invalid");
        check(ledger.critical(id,200),"new actual critical event");
        check(!ledger.critical(id,201),"local/server duplicate critical coalesced");
        check(ledger.isCritical(id,203),"recent critical color");
        check(!ledger.isCritical(id,205),"critical marker expires");
        check(ledger.pop(id,1)==1&&ledger.pop(id,1)==1,"same-tick pop not double counted");
        check(ledger.pop(id,2)==2,"next observed pop counts");
        ledger.clearPops();check(ledger.pops(id)==0,"manual pop reset");
    }
    private static void boundedHistory() {
        var ledger=new VisualLedger();
        for(int i=0;i<2000;i++) {
            UUID id=new UUID(0,i);
            ledger.observe(id,20,true,i);ledger.pop(id,i);ledger.critical(id,i);
            check(ledger.trackedHealth()<=VisualLedger.LIMIT&&ledger.trackedPops()<=VisualLedger.LIMIT,"bounded entity histories");
        }
        check(ledger.pops(new UUID(0,0))==0,"old histories evicted");
        ledger.prune(3000);check(ledger.trackedHealth()==0,"health TTL");
        UUID id=UUID.randomUUID();
        for(int i=0;i<12000;i++)check(ledger.pop(id,4000+i)<=9999,"bounded counter");
        check(ledger.pops(id)==9999,"counter clamp");
        ledger.clear();check(ledger.trackedHealth()==0&&ledger.trackedPops()==0,"disconnect/profile reset");
    }
    private static void colors() {
        for(int t=0;t<60;t++) {
            double alpha=VisualLedger.alpha(t,60);
            check(alpha>=0&&alpha<=1,"finite alpha");
        }
        check(VisualLedger.alpha(60,60)==0&&VisualLedger.alpha(-1,60)==0&&VisualLedger.alpha(Double.NaN,60)==0,"expiry/invalid alpha");
        for(int rgb:List.of(0xff000000,0xff945cda,0xffffffff,0x809e59de)) {
            int near=VisualLedger.gradient(rgb,0),far=VisualLedger.gradient(rgb,1);
            check((near&0xff000000)==(rgb&0xff000000)&&(far&0xff000000)==(rgb&0xff000000),"gradient preserves alpha");
            for(int shift:new int[]{0,8,16})check((near>>>shift&255)>=(far>>>shift&255),"near-to-far brightness");
            check(VisualLedger.gradient(rgb,-100)==near&&VisualLedger.gradient(rgb,100)==far,"gradient clamps");
        }
    }
}
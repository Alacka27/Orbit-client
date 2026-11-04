"""Check packaging and retain the delivered 0.4.7 UI resources; runtime QA is separate."""
from pathlib import Path
import hashlib,json,struct,zipfile
from check_ui_resources import check_resource
R=Path(__file__).resolve().parents[1]
version=next(s.split('=',1)[1] for s in (R/'gradle.properties').read_text().splitlines() if s.startswith('version='))
jar=R/'build/libs'/f'orbit-{version}.jar'
expected=json.loads((R/'src/client/resources/orbit.client.mixins.json').read_text())
assets=json.loads((R/'tools/assets/orbit047-resource-hashes.json').read_text())

def class_links(data):
    """Read interfaces and method references without depending on a decompiler."""
    count=struct.unpack_from('>H',data,8)[0]
    pool=[None]*count
    pos=10
    i=1
    while i<count:
        tag=data[pos]; pos+=1
        if tag==1:
            size=struct.unpack_from('>H',data,pos)[0]; pos+=2
            pool[i]=(tag,data[pos:pos+size].decode('utf-8',errors='replace')); pos+=size
        elif tag in (7,8,16,19,20):
            pool[i]=(tag,struct.unpack_from('>H',data,pos)[0]); pos+=2
        elif tag in (9,10,11,12,17,18):
            pool[i]=(tag,struct.unpack_from('>HH',data,pos)); pos+=4
        elif tag in (3,4):
            pos+=4
        elif tag in (5,6):
            pos+=8; i+=1
        elif tag==15:
            pos+=3
        else:
            raise AssertionError(f'Unknown class constant tag: {tag}')
        i+=1
    def text(index): return pool[index][1]
    def class_name(index): return text(pool[index][1])
    pos+=6  # access_flags, this_class, super_class
    interface_count=struct.unpack_from('>H',data,pos)[0]; pos+=2
    interfaces={class_name(struct.unpack_from('>H',data,pos+2*i)[0]) for i in range(interface_count)}
    methods=set()
    for entry in pool:
        if entry and entry[0] in (10,11):
            owner,signature=entry[1]
            name,descriptor=pool[signature][1]
            methods.add((class_name(owner),text(name),text(descriptor)))
    return interfaces,methods

with zipfile.ZipFile(jar) as z:
    names=set(z.namelist()); info=json.loads(z.read('fabric.mod.json'))
    assert info['id']=='orbit' and info['version']==version and info['depends']['minecraft']=='1.21.11'
    assert json.loads(z.read('orbit.client.mixins.json'))==expected
    assert expected['required'] and expected['compatibilityLevel']=='JAVA_21'
    assert 'dev/alacka/orbit/hud/HudEditorScreen.class' not in names, 'obsolete HUD editor'
    assert not any(n in expected['client'] for n in ('ChatLayoutMixin','ChatInputLayoutMixin','ChatSuggestionsLayoutMixin')), 'chat must keep native coordinates'
    for n in expected['client']: assert f'dev/alacka/orbit/mixin/{n}.class' in names,n
    for n in ('OrbitClient','menu/ModuleCard','menu/ModuleHelp','menu/ModuleInfoScreen','hud/HudGeometry',
              'menu/ModuleSettingsScreen','menu/ModuleSettingsLayout','menu/BindScreen','input/ModuleBinds',
              'config/BindPrefs','input/HoldGate','input/PressQueue','zoom/ZoomController','zoom/ZoomState',
              'menu/OrbitControlsScreen','menu/VisualSettingsScreen','config/VisualSettings',
              'modules/VisualModules','modules/VisualLedger','hud/VisualHud',
              'menu/ConfigsScreen','hud/HudChatDrag','menu/ThemeBackdrop','config/CombatSettings','config/SupportSettings',
              'menu/SupportSettingsScreen','accounts/AccountSessions','accounts/AuthProtocol',
              'accounts/AuthHttp','accounts/ElySessionService','accounts/SecretEditBox',
              'accounts/ElyLoginScreen','accounts/MicrosoftLoginScreen','profiles/ProfilesScreen','modules/InventoryModules','modules/UseKeyLease',
              'modules/SupportMath','modules/SupportGate','modules/ConsumeSession',
              'modules/CombatModules','modules/CombatScheduler','modules/CombatMath','modules/Trajectories'):
        assert f'dev/alacka/orbit/{n}.class' in names,n
    for name,digest in assets.items():
        assert name in names, name
        check_resource(name,z.read(name),digest)
    assert hashlib.sha256(z.read('assets/orbit/auth/ely-textures-public-key.der')).hexdigest() == '37b6c5f379f8538e74a8750b51273d11051e758be0205b1ea71e4fd88aba1ce0'
    assert not any(n.endswith(('accounts.properties','profiles.properties')) for n in names), 'local account data bundled'
    fonts=sorted(n for n in assets if n.startswith('assets/orbit/font/') and n.endswith('.json'))
    assert len(fonts)==18
    for name in fonts:
        providers=json.loads(z.read(name))['providers']
        assert providers[-1]=={'type':'reference','id':'minecraft:default'},name
        for p in providers:
            if p['type']=='ttf':
                ns,path=p['file'].split(':',1)
                assert f'assets/{ns}/font/{path}' in names,(name,path)
    for name,size in ((info['icon'],(512,512)),('assets/orbit/clouds.png',(1600,900))):
        data=z.read(name)
        assert data[:8]==b'\x89PNG\r\n\x1a\n' and struct.unpack('>II',data[16:24])==size
    assert not any(n.startswith('assets/orbit/textures/gui/sprites/ui/') for n in names),'obsolete 0.4.6 sprites'
    classes=[n for n in names if n.startswith('dev/alacka/orbit/') and n.endswith('.class')]
    windows=[]
    for n in classes:
        data=z.read(n)
        magic,minor,major=struct.unpack('>IHH',data[:8])
        assert magic==0xCAFEBABE and major==65,(n,major)
        interfaces,methods=class_links(data)
        if 'dev/alacka/orbit/menu/OrbitWindow' in interfaces:
            windows.append(n)
        if n in ('dev/alacka/orbit/menu/ModuleInfoScreen.class','dev/alacka/orbit/menu/BindScreen.class',
                 'dev/alacka/orbit/menu/ModuleSettingsScreen.class','dev/alacka/orbit/menu/SupportSettingsScreen.class',
                 'dev/alacka/orbit/accounts/AccountFormScreen.class','dev/alacka/orbit/accounts/ElyLoginScreen.class',
                 'dev/alacka/orbit/accounts/MicrosoftLoginScreen.class','dev/alacka/orbit/profiles/ProfilesScreen.class'):
            # Screen renders its background before render(); MenuBackdropMixin owns it.
            # Calling it again from these settings caused the reported in-world crash.
            for owner,name,descriptor in methods:
                assert not (owner=='dev/alacka/orbit/menu/GlassMenu' and name=='backdrop'),f'duplicate background in {n}'
                assert not (owner in ('net/minecraft/client/gui/GuiGraphics','net/minecraft/class_332')
                            and name in ('blurBeforeThisStratum','method_71278')),f'direct window blur in {n}'
    assert 'dev/alacka/orbit/menu/ModuleSettingsScreen.class' in windows
    assert 'dev/alacka/orbit/menu/SupportSettingsScreen.class' in windows
    assert 'dev/alacka/orbit/menu/ModuleInfoScreen.class' in windows
print(f'PASS: {jar.name}, {len(classes)} Java 21 classes, {len(expected["client"])} mixins, 18 fonts, {len(assets)-2} byte-identical 0.4.7 resources and no Orbit vanilla key labels; settings/account background regression guards; pinned Ely public key; no local account files.')

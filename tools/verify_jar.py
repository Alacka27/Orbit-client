"""Verify a self-contained remapped Orbit artifact, not a substitute for Minecraft runtime QA."""
from pathlib import Path
import json,struct,zipfile
R=Path(__file__).resolve().parents[1]
version=next(s.split('=',1)[1] for s in (R/'gradle.properties').read_text().splitlines() if s.startswith('version='))
jar=R/'build/libs'/f'orbit-{version}.jar'
expected=json.loads((R/'src/client/resources/orbit.client.mixins.json').read_text())
with zipfile.ZipFile(jar) as z:
    names=set(z.namelist()); info=json.loads(z.read('fabric.mod.json'))
    assert info['id']=='orbit' and info['version']==version and info['depends']['minecraft']=='1.21.11'
    assert json.loads(z.read('orbit.client.mixins.json'))==expected
    assert expected['required'] and expected['compatibilityLevel']=='JAVA_21'
    for n in expected['client']:assert f'dev/alacka/orbit/mixin/{n}.class' in names,n
    for n in ('OrbitClient','menu/GameUiTheme','menu/HudTheme','menu/ModuleCard','menu/ProfileListLayout','freelook/FreeLookController','modules/Trajectories','profiles/ProfileStore'):
        assert f'dev/alacka/orbit/{n}.class' in names,n
    for name,size in ((info['icon'],(512,512)),('assets/orbit/clouds.png',(1600,900))):
        data=z.read(name);assert data[:8]==b'\x89PNG\r\n\x1a\n' and struct.unpack('>II',data[16:24])==size
    for sprite in ('surface','border','shadow','surface_small','border_small','surface_medium','border_medium','window','window_shade'):
        for suffix in ('.png','.png.mcmeta'):assert 'assets/orbit/textures/gui/sprites/ui/'+sprite+suffix in names
    for face in ('ui','ui_bold'):
        d=json.loads(z.read(f'assets/orbit/font/{face}.json'));ns,path=d['providers'][0]['file'].split(':',1)
        assert f'assets/{ns}/font/{path}' in names
        assert d['providers'][1]=={'type':'reference','id':'minecraft:default'}
    assert 'assets/orbit/font/license.txt' in names
    for n in names:
        if n.startswith('dev/alacka/orbit/') and n.endswith('.class'):
            magic,minor,major=struct.unpack('>IHH',z.read(n)[:8]);assert magic==0xCAFEBABE and major==65,(n,major)
print(f'PASS: {jar.name}, Java 21 bytecode, {len(expected["client"])} mixins, 9 sprites, clouds and both font providers.')

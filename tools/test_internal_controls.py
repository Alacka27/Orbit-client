"""Regression guard: Orbit controls must never enter Minecraft's Controls registry."""
from pathlib import Path
import json
import re

root = Path(__file__).resolve().parents[1]
java = root / "src/client/java/dev/alacka/orbit"
for path in java.rglob("*.java"):
    text = path.read_text()
    assert "KeyBindingHelper" not in text, ("Vanilla binding helper is forbidden", path)
    assert not re.search(r"new\s+KeyMapping\s*\(", text), ("Vanilla binding construction is forbidden", path)
for lang in ("en_us", "ru_ru"):
    data = json.loads((root / f"src/main/resources/assets/orbit/lang/{lang}.json").read_text())
    assert not any(k.startswith("key.orbit.") for k in data), lang
registry = (java / "modules/Modules.java").read_text()
for builtin in ("FreeLook", "Zoom", "Menu"):
    assert f'new Modules.Module("{builtin}"' not in registry, builtin
for visual in ("TargetHUD", "DamageNumbers", "HitParticles", "CriticalHitEffect",
               "ShieldStatus", "TotemCounter", "SeeInvisibles"):
    assert f'new Modules.Module("{visual}"' in registry, visual
click = (java / "menu/OrbitClickScreen.java").read_text()
assert 'new OrbitControlsScreen(this)' in click
assert 'case General' not in click
assert 'setEnabled' not in click
events = (java / "mixin/VisualEventsMixin.java").read_text()
assert "cancellable" not in events and ".cancel(" not in events and ".send(" not in events
ghost = (java / "mixin/SeeInvisiblesMixin.java").read_text()
assert "state.isInvisibleToPlayer = false" in ghost and ".setInvisible(" not in ghost
assert "setInvisible(" not in (java / "modules/VisualModules.java").read_text()
print("PASS internal controls, always-available built-ins and visual-only observation guards")
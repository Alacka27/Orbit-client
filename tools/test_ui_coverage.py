"""Reference mapping sanity check, not an assertion that every screen was runtime tested."""
from pathlib import Path
import json
R=Path(__file__).resolve().parents[1]
rows=json.loads((R/'docs/ui-coverage.json').read_text())
assert len(rows)==48 and len({r['id'] for r in rows})==48
for row in rows:assert all(row.get(k) for k in ('id','reference','presentation','native'))
ui=(R/'src/client/java/dev/alacka/orbit/menu/GameUiTheme.java').read_text()
assert 'slot.x =' not in ui and 'slot.y =' not in ui and 'sendPacket' not in ui
assert 'slot.isActive()' in ui and 'getClass().getPackageName()' in ui
assert 'style != null' in (R/'src/client/java/dev/alacka/orbit/mixin/TooltipSkinMixin.java').read_text()
chat=(R/'src/client/java/dev/alacka/orbit/mixin/ChatGlassMixin.java').read_text()
assert '@Shadow' not in chat,'Avoid ambiguous multi-target field remapping'
print('PASS: 48 reference mappings, vanilla-container guard, native slots, custom tooltip preservation and safe chat remapping.')

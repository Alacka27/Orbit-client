"""Restore the user-supplied Orbit logo prepared with transparent padding (no third-party packages)."""
import base64
import hashlib
from pathlib import Path
import struct

root = Path(__file__).resolve().parents[1]
source = root / 'tools/assets/orbit-logo.png.b64'
png = base64.b64decode(source.read_text(encoding='ascii').strip(), validate=True)
assert png[:8] == b'\x89PNG\r\n\x1a\n', 'Invalid logo PNG'
assert struct.unpack('>IIBB', png[16:26]) == (512, 512, 8, 6), 'Expected 512x512 RGBA logo'
assert hashlib.sha256(png).hexdigest() == '38d107f9e9ad87a12290ed75cd6186601de642488e992c199fb1362c6213b972', 'Unexpected logo data'
target = root / 'src/main/resources/assets/orbit/icon.png'
target.parent.mkdir(parents=True, exist_ok=True)
target.write_bytes(png)
print('Restored user-supplied logo:', target.relative_to(root), f'({len(png)} bytes)')

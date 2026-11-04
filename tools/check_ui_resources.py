"""Keep 0.4.7 art/font bytes; the only removed translations are obsolete vanilla key labels."""
import hashlib
import json

REMOVED_LEGACY_KEYS = {
    "assets/orbit/lang/en_us.json": {"key.orbit.freelook": "Orbit: FreeLook (hold)", "key.orbit.menu": "Orbit: open menu"},
    "assets/orbit/lang/ru_ru.json": {"key.orbit.freelook": "Orbit: свободный обзор (удерживать)", "key.orbit.menu": "Orbit: открыть меню"},
}


def check_resource(name, data, digest):
    if name in REMOVED_LEGACY_KEYS:
        actual = json.loads(data)
        assert not any(key.startswith("key.orbit.") for key in actual), "Vanilla control label returned: " + name
        # Reconstruct the original serialization, not a newly generated baseline.
        # Its hash must still match the unmodified 0.4.7 manifest.
        original = (json.dumps({**REMOVED_LEGACY_KEYS[name], **actual}, ensure_ascii=False, indent=2) + "\n").encode()
        assert hashlib.sha256(original).hexdigest() == digest, "Old translation changed: " + name
    else:
        assert hashlib.sha256(data).hexdigest() == digest, "0.4.7 UI resource changed: " + name
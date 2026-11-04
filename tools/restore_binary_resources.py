"""Restore pinned binary resources from reviewable text sources in a clean Git checkout."""
from pathlib import Path
import argparse
import base64
import hashlib
import lzma
import os
import tempfile

ROOT = Path(__file__).resolve().parents[1]
RESOURCES = (
    ("font/ui.ttf", "tools/assets/orbit-ui.ttf.b64", False,
     "e68e471e4d9f6e59933414a2063d13abee3484e11a3a496daa36626fce13f9e1"),
    ("font/ui_bold.ttf", "tools/assets/orbit-ui-bold.ttf.xz.b64", True,
     "a513cd1c01a7a2f0121958859edd705341d4691c16f01048c8c4dcbb1931626e"),
    ("auth/ely-textures-public-key.der", "tools/assets/ely-textures-public-key.der.b64", False,
     "37b6c5f379f8538e74a8750b51273d11051e758be0205b1ea71e4fd88aba1ce0"),
)


def restore(destination):
    for name, source, compressed, digest in RESOURCES:
        encoded = "".join((ROOT / source).read_text(encoding="ascii").split())
        data = base64.b64decode(encoded, validate=True)
        if compressed:
            data = lzma.decompress(data)
        if hashlib.sha256(data).hexdigest() != digest:
            raise ValueError("Pinned resource hash mismatch: " + source)
        target = destination / name
        if target.exists() and target.read_bytes() == data:
            continue
        target.parent.mkdir(parents=True, exist_ok=True)
        handle, temp = tempfile.mkstemp(prefix="orbit-resource-", dir=target.parent)
        try:
            with os.fdopen(handle, "wb") as stream:
                stream.write(data)
            os.replace(temp, target)
        finally:
            if os.path.exists(temp):
                os.unlink(temp)
    print("Restored/verified 2 original fonts and the pinned public Ely texture key.")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--destination", type=Path,
                        default=ROOT / "src/main/resources/assets/orbit")
    restore(parser.parse_args().destination)
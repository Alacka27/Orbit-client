"""Verify exact target versions using public Fabric metadata; does not test gameplay."""
import json
import os
from pathlib import Path
import urllib.request
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[2]
props = {}
for line in (ROOT / 'gradle.properties').read_text().splitlines():
    if '=' in line and not line.lstrip().startswith('#'):
        key, value = line.split('=', 1)
        props[key.strip()] = value.strip()

def fetch(url):
    request = urllib.request.Request(url, headers={'User-Agent': 'Orbit-dependency-check/0.1'})
    with urllib.request.urlopen(request, timeout=60) as response:
        return response.read()

mc = props['minecraft_version']
loader = props['loader_version']
api = props['fabric_api_version']
games = json.loads(fetch('https://meta.fabricmc.net/v2/versions/game'))
if not any(item['version'] == mc for item in games):
    raise SystemExit(f'Minecraft {mc} is not listed by Fabric metadata')
loaders = json.loads(fetch(f'https://meta.fabricmc.net/v2/versions/loader/{mc}'))
if not any(item['loader']['version'] == loader for item in loaders):
    raise SystemExit(f'Fabric Loader {loader} is not listed for Minecraft {mc}')
metadata = ET.fromstring(fetch('https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/maven-metadata.xml'))
if api not in [element.text for element in metadata.findall('./versioning/versions/version')]:
    raise SystemExit(f'Fabric API {api} is not listed in Fabric Maven')

report = ('## Orbit: dependency metadata verified\n\n'
          f'- Minecraft: `{mc}`\n- Fabric Loader: `{loader}`\n'
          f'- Fabric API: `{api}`\n- Loom: `{props["loom_version"]}` (upstream snapshot; build verification follows)\n'
          '- Gradle: `9.8.2` (upstream example wrapper version)\n'
          '- Java target: `21`\n\n'
          'This checks published metadata only. Compilation, in-game startup and Baritone compatibility are separate checks.\n')
print(report)
if os.environ.get('GITHUB_STEP_SUMMARY'):
    with open(os.environ['GITHUB_STEP_SUMMARY'], 'a') as output:
        output.write(report)

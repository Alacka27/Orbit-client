# Orbit project rules

- All Orbit hotkeys and settings belong inside Orbit. Never register an Orbit `KeyMapping`,
  use `KeyBindingHelper`, or add entries to Minecraft Controls / Miscellaneous.
- FreeLook and Zoom are always-available built-ins, not switchable modules. Their hold keys
  are editable in Orbit → Settings → Управление Orbit. Keep a usable binding for each.
- Keep native Minecraft controls, chat, menus, focus-loss and release-to-rearm behavior intact.
- New visual/combat/item modules are opt-in. Do not fabricate health, shield cooldown,
  durability or remaining totems for other players. Unknown data must stay unknown.
- Back up before changes. Test in an isolated dev directory and disposable worlds, never
  in the user's main `.minecraft` or real saves. A successful build is not an in-game test.
- Do not publish account/session files, credentials, worlds, caches, test harnesses or logs.
- Preserve the established Violet Glass appearance and real Orbit UI Bold font.
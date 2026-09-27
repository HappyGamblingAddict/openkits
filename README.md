# Kits

A kit plugin for Paper / Folia. Players claim kits from a GUI, each kit has its own
cooldown, and everyone can decide where the items land in their own inventory.

## What it does

- **Kit GUI** (`/kits`) — one icon per kit, left-click to claim, right-click to see
  what's inside first. Locked kits are shown greyed out so players know what exists.
- **Per-kit cooldowns** — set per kit, e.g. `12h` or `1d6h`. Optional; `none` means
  no cooldown.
- **Own layout** (`/kitlayouts`) — pick a kit and drag the items around so they land
  where you want them. Only rearranging, so nothing can be duplicated or stolen.
- **Coin shop** (`/coinshop`) — kits with a price can be bought once with coins.
  Players earn coins per kill (configurable in `config.yml`).
- **Admin commands** (`/kit`) — create kits from your inventory, edit contents in a
  GUI, arrange where they show up, set icons, cooldowns, permissions, prices.
- Fully translatable via `messages.yml` (MiniMessage formatting).
- Works on **Paper and Folia**.

## Commands

| Command | Permission | What it does |
| --- | --- | --- |
| `/kits` | `kits.use` | Open the kit menu |
| `/kitlayouts` | `kits.use` | Change your personal kit layouts |
| `/coinshop` | `kits.use` | Buy kits with coins |
| `/coins` | `kits.use` | Check your balance (admins: `give`/`take`/`set`) |
| `/kit` | `kits.admin` | Manage kits |

## Build

```
mvn package
```

The jar ends up in `target/Kits-1.0.0.jar`. Requires Java 21.

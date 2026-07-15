# XercaPaint — Update Notes

A survival-focused update that turns painting into a real resource loop, spreads paintings across the world, and adds server-side control. All new mechanics are **off or non-invasive by default** — nothing changes for existing worlds unless you opt in.

---

## ✨ New Features

### 🎨 Paint costs dye (per-colour charge)
Painting is no longer free. The palette now stores a **separate paint charge for each of the 16 basic colours**.

- Refill a colour by crafting the palette together with that **dye** — orange dye refills only orange, blue only blue, and so on (`+256` per dye, up to `4096` per colour by default).
- While painting, each pixel consumes charge from the colour it uses. **Mixed/custom colours** are estimated against the basic colours available on your palette and consume them proportionally.
- The palette tooltip shows the remaining paint for every unlocked colour.
- Fully **configurable** and **disabled by default** (see Configuration). Creative mode never consumes paint.

### 🖼️ Paintings in world generation
Canvases, easels and palettes can now be found while exploring:

- **Cartographer houses** (villages): a palette (~40%) and an easel (~25%).
- **Woodland mansions**: a large canvas (~50%).

Implemented as Global Loot Modifiers, so they cleanly add to the target chests only.

### 💰 Villager & wandering trader trades
- **Cartographer**: blank canvas (novice), easel (apprentice), large canvas (journeyman) for emeralds.
- **Wandering trader**: painter's palette.
- Toggleable via config.

### 🏆 Advancements
A proper advancement branch with toasts:
**The Artist's Way** (craft a palette) → **Blank Canvas** (get a canvas) → **At the Easel** (get an easel).

### 🔒 Protected (waxed) paintings
Wax a **signed** painting with a **honeycomb** in the crafting grid to lock it. Waxed paintings can no longer be edited on an easel — perfect for preserving finished artwork on shared servers. The tooltip marks them as *Protected*.

### ⚙️ Server configuration
A new server config file (`xercapaint-server.toml`) with grouped sections:

- **`[painting]`** — `dyeCostEnabled` (default `false`), `chargePerDye`, `maxCharge`.
- **`[import]`** — `importEnabled`, `importRequiresOp`, `importOpLevel` (gate `/paintimport` on public servers).
- **`[trades]`** — `villagerTradesEnabled`.

> Note: server configs are **per-world** (`saves/<world>/serverconfig/` in singleplayer, `world/serverconfig/` on a dedicated server). The file in the global `config/` folder is only the template for new worlds.

---

## 🐛 Bug Fixes

- **Fixed dedicated-server crash**: client-only packet handlers (e.g. `CloseGuiPacketHandler`) are no longer loaded on the server. Packet registration is now split by physical side.
- **Fixed world-load crash**: the config was being read before it was loaded during world creation; all config access now safely falls back to defaults until the config is available.
- **Fixed broken drop loot**: paintings/easels/palettes were dropping from *every* block due to an invalid loot condition id (`minecraft:loot_table_id` → `neoforge:loot_table_id`). Drops are now correctly restricted to their target chests.

---

## ➕ New Recipes

- **Palette from wooden slabs** — an alternative to the planks recipe:
  ```
  slab slab slab
  slab slab
  ```
- **Wax canvas** — signed canvas + honeycomb → protected canvas.

---

## 🔧 Under the Hood

- The 16 basic palette colours now live in a single shared source (`PaletteUtil.BASIC_COLORS`) used by both client rendering and server-side charge accounting, guaranteeing they always match.
- Crafting dyes onto a palette now preserves its other data (custom colours and existing charges) instead of resetting it.

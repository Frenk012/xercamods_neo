# XercaPaint — Update Notes

## 1.4.0 — Glass paintings, painted frames & dye-cost fixes

### ✨ New (ported from the original mod's 2.0 update)
- **Glass canvases** (small, long, tall, large) for **transparent paintings**. Craft with 8 sticks around a glass pane; combine them like regular canvases. Right-click erases back to fully transparent; empty glass shows a checkerboard in the editor and a glass frame in the world.
- **Paintable frames (canvas sides)**: toggle them with the small button next to the canvas holder and paint the edges of your canvas. Works on regular and glass canvases, on walls, easels and in hand.
- **Signed paintings stack up to 16**.
- Help tooltips in the painting GUI are now **translated**; the painting GUI no longer **blurs the world** behind it.
- New/updated translations (Italian, Polish, Icelandic and more Spanish locales added).

### 🎨 Dye cost (thanks to **trygve555**, PR #1)
- Dye charge is now spent **per painted pixel**, scaled by brush **opacity**, and tracked live in the GUI.
- You can no longer paint with a colour that has run out; the palette shows **how much paint is left** in each slot.
- No colour is selected when opening the GUI (the brush is shown clean), and the selected colour no longer carries over between canvases.
- Custom colours that can no longer be mixed from your remaining paint are cleared (only when dye cost is enabled).
- New server option `[editor] allowErase` to disable right-click erasing.
- Default `maxCharge` is now `1024`.

### 🐛 Fixes
- Server now **validates** the paint charges reported by the client: a palette can only lose paint while painting (prevents infinite-paint cheats and malformed packets).
- Erasing no longer needs a selected colour and **no longer consumes dye**.
- Signed or waxed canvases can no longer be overwritten by edit packets; the easel is released correctly when an edit is rejected.
- **Breaking a large/long/tall painting from any of its blocks now drops the painting** (previously only the bottom-left block dropped it, the others destroyed it).
- **Waxed (protected)** state and painted frames are now kept when a painting is placed on a wall and broken again.
- Paintings re-request their image from the server when a newer version exists (the request cache was never cleared).
- Recipe-book unlock advancements were in the wrong folder for 1.21 and never loaded; recipes now unlock properly.
- Import/export (`/paintexport`, `/paintimport`) keep glass material and painted frames.

---

## 1.3.0

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

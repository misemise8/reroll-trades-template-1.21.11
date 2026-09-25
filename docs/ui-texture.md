# Trade panel texture

The GUI uses `common/src/main/resources/assets/reroll-trades/textures/gui/trade_panel.png` (1254 × 1254, opaque RGB PNG). It is rendered behind the controls with a light gray tint to keep text readable. Texture filtering is disabled in its `.mcmeta` file.

Created with the built-in image generation tool, then copied unchanged into the project. The first output was a translucent overlay; the second pass made the surface opaque.

## Initial prompt

Use case: stylized-concept. Asset type: production texture for a Minecraft inventory-style GUI panel. Generate a seamless, flat, front-facing square tile of very subtle light warm-gray stone/paper pixel texture. Nearly uniform light gray (#c6c6c6), extremely low contrast tiny irregular square flecks within #bebebe to #cccccc. Coherent crisp pixel art, not photographic noise, no lighting gradient, no vignette, no cracks, no bricks, no framing, no border, no text, no symbols, no objects. The entire image is the material swatch edge-to-edge, tileable in both axes. Restrained vanilla Minecraft inventory aesthetic; dark UI text will be drawn over it, so texture must remain quiet and light. Output a square PNG.

## Final edit prompt

Edit this texture into an OPAQUE game UI background tile. Fill EVERY pixel of the entire square with a solid light gray base (#c6c6c6); there must be NO transparent or translucent pixels anywhere. Keep only very subtle gray pixel flecks and fine pixelated stone grain above this base, with low contrast in the #bdbdbd–#cfcfcf range. Flat evenly lit 2D pixel-art material, similar to a quiet vanilla Minecraft inventory panel. No text, no border, no framing, no gradients. This is a full rectangular opaque surface, not a transparent overlay or a cutout. Retain the gray background across the entire canvas and all four corners. 1024 by 1024 square.

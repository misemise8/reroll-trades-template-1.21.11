# Trade panel texture

The panel uses Minecraft's `minecraft:textures/gui/container/generic_54.png` at runtime. No copy of the vanilla asset is distributed with the mod.

The 176 by 222 chest frame is drawn in nine regions: fixed four-pixel corners, four-pixel edges that stretch only along their length, and a blank header sample for the interior. This fits the editor bounds without stretching the bevel or bringing chest inventory slots into the background. Transparent corner pixels retain the vanilla silhouette.

Item icons use the same texture's 18 by 18 inset slot at (7, 17), with a 16 by 16 item and a subtle hover/focus highlight. GUI scaling remains controlled by Minecraft.

The previous generated grain texture is no longer used or packaged. Resource packs that replace the vanilla chest texture also affect this panel; custom packs must retain the vanilla texture layout.

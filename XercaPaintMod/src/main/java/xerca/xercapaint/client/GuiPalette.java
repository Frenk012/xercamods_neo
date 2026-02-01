package xerca.xercapaint.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import xerca.xercapaint.packets.PaletteUpdatePacket;
import net.neoforged.neoforge.network.PacketDistributor;

@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
public class GuiPalette extends BasePalette {

    protected GuiPalette(@NotNull ItemStack paletteStack, Component title) {
        super(title, paletteStack);
    }

    @Override
    public void init() {
        paletteX = paletteXs[paletteXs.length - 1];
        paletteY = paletteYs[paletteYs.length - 1];
        if (paletteX == -1000 || paletteY == -1000) {
            paletteX = 140;
            paletteY = 40;
        }
        updatePalettePos(0, 0);
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float f) {
        super.render(guiGraphics, mouseX, mouseY, f);

        renderCursor(guiGraphics, mouseX, mouseY);
    }

    private void renderCursor(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (isCarryingColor) {
            // Apply color tint using RenderSystem (NeoForge 1.21.1 doesn't support tinted blit with int color)
            float r = ((carriedColor.rgbVal() >> 16) & 0xFF) / 255.0f;
            float g = ((carriedColor.rgbVal() >> 8) & 0xFF) / 255.0f;
            float b = (carriedColor.rgbVal() & 0xFF) / 255.0f;
            com.mojang.blaze3d.systems.RenderSystem.setShaderColor(r, g, b, 1.0f);
            guiGraphics.blit(paletteTextures, mouseX - brushSpriteSize / 2, mouseY - brushSpriteSize / 2, brushSpriteX + brushSpriteSize, brushSpriteY, dropSpriteWidth, brushSpriteSize, 256, 256);
            com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

        } else if (isCarryingWater) {
            // Apply water color tint
            float r = ((waterColor.rgbVal() >> 16) & 0xFF) / 255.0f;
            float g = ((waterColor.rgbVal() >> 8) & 0xFF) / 255.0f;
            float b = (waterColor.rgbVal() & 0xFF) / 255.0f;
            com.mojang.blaze3d.systems.RenderSystem.setShaderColor(r, g, b, 1.0f);
            guiGraphics.blit(paletteTextures, mouseX - brushSpriteSize / 2, mouseY - brushSpriteSize / 2, brushSpriteX + brushSpriteSize, brushSpriteY, dropSpriteWidth, brushSpriteSize, 256, 256);
            com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        }
    }

    @Override
    public boolean mouseDragged(double posX, double posY, int mouseButton, double deltaX, double deltaY) {
        if (isCarryingPalette) {
            boolean ret = super.mouseDragged(posX, posY, mouseButton, deltaX, deltaY);
            updatePalettePos(deltaX, deltaY);
            return ret;
        }
        return super.mouseDragged(posX, posY, mouseButton, deltaX, deltaY);
    }

    private void updatePalettePos(double deltaX, double deltaY) {
        paletteX += deltaX;
        paletteY += deltaY;

        paletteXs[paletteXs.length - 1] = paletteX;
        paletteYs[paletteYs.length - 1] = paletteY;
    }

    @Override
    public void removed() {
        if (paletteDirty) {
            PacketDistributor.sendToServer(new PaletteUpdatePacket(customColors));
        }
    }
}
package xerca.xercapaint.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.item.ClampedItemPropertyFunction;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import xerca.xercapaint.Mod;
import xerca.xercapaint.block_entity.BlockEntities;
import xerca.xercapaint.entity.Entities;
import xerca.xercapaint.entity.EntityEasel;
import xerca.xercapaint.item.ItemCanvas;
import xerca.xercapaint.item.ItemPalette;
import xerca.xercapaint.item.Items;

@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = Mod.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ModClient {
    public static final ModelLayerLocation EASEL_MAIN_LAYER = new ModelLayerLocation(Mod.id("easel"), "main");
    public static final ModelLayerLocation EASEL_CANVAS_LAYER = new ModelLayerLocation(Mod.id("easel"), "canvas");
    public static CanvasItemRenderer CANVAS_ITEM_RENDERER;

    public static void showCanvasGui(EntityEasel easel, ItemStack palette) {
        showCanvasGui(easel, palette, Minecraft.getInstance());
    }

    public static void showCanvasGui(EntityEasel easel, ItemStack paletteStack, Minecraft minecraft) {
        ItemStack canvasStack = easel.getItem();
        if ((canvasStack.getOrDefault(Items.CANVAS_GENERATION.get(), 0) > 0) || paletteStack.isEmpty()) {
            minecraft.setScreen(new GuiCanvasView(canvasStack,
                    Component.translatable("item.xercapaint.item_canvas"),
                    ((ItemCanvas) canvasStack.getItem()).getCanvasType(), easel));
        } else {
            minecraft.setScreen(new GuiCanvasEdit(minecraft.player, canvasStack, paletteStack,
                    Component.translatable("item.xercapaint.item_canvas"),
                    ((ItemCanvas) canvasStack.getItem()).getCanvasType(), easel));
        }
    }

    public static void showCanvasGui(Player player) {
        final ItemStack heldItem = player.getMainHandItem();
        final ItemStack offhandItem = player.getOffhandItem();
        final Minecraft minecraft = Minecraft.getInstance();

        if (heldItem.isEmpty() || (minecraft.player != null && !minecraft.player.getGameProfile().getId().equals(player.getGameProfile().getId()))) {
            return;
        }

        if (heldItem.getItem() instanceof ItemCanvas) {
            if (offhandItem.isEmpty() || !(offhandItem.getItem() instanceof ItemPalette) || (heldItem.getOrDefault(Items.CANVAS_GENERATION.get(), 0) > 0)) {
                minecraft.setScreen(new GuiCanvasView(heldItem, Component.translatable("item.xercapaint.item_canvas"), ((ItemCanvas) heldItem.getItem()).getCanvasType(), null));
            } else {
                minecraft.setScreen(new GuiCanvasEdit(minecraft.player, heldItem, offhandItem, Component.translatable("item.xercapaint.item_canvas"), ((ItemCanvas) heldItem.getItem()).getCanvasType(), null));
            }
        } else if (heldItem.getItem() instanceof ItemPalette) {
            if (offhandItem.isEmpty() || !(offhandItem.getItem() instanceof ItemCanvas)) {
                minecraft.setScreen(new GuiPalette(heldItem, Component.translatable("item.xercapaint.item_palette")));
            } else {
                if (offhandItem.getOrDefault(Items.CANVAS_GENERATION.get(), 0) > 0) {
                    minecraft.setScreen(new GuiCanvasView(offhandItem, Component.translatable("item.xercapaint.item_canvas"), ((ItemCanvas) offhandItem.getItem()).getCanvasType(), null));
                } else {
                    minecraft.setScreen(new GuiCanvasEdit(minecraft.player, offhandItem, heldItem, Component.translatable("item.xercapaint.item_canvas"), ((ItemCanvas) offhandItem.getItem()).getCanvasType(), null));
                }
            }
        }
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            // Register item properties for model predicates
            ClampedItemPropertyFunction drawn = (itemStack, level, livingEntity, i) -> {
                boolean hasPixels = itemStack.get(Items.CANVAS_PIXELS.get()) != null;
                return hasPixels ? 1.0F : 0.0f;
            };
            ClampedItemPropertyFunction colors = (stack, worldIn, entityIn, i) ->
                    ((float) ItemPalette.basicColorCount(stack)) / 16.0F;

            ItemProperties.register(Items.ITEM_CANVAS.get(), Mod.id("drawn"), drawn);
            ItemProperties.register(Items.ITEM_CANVAS_LARGE.get(), Mod.id("drawn"), drawn);
            ItemProperties.register(Items.ITEM_CANVAS_LONG.get(), Mod.id("drawn"), drawn);
            ItemProperties.register(Items.ITEM_CANVAS_TALL.get(), Mod.id("drawn"), drawn);
            ItemProperties.register(Items.ITEM_PALETTE.get(), Mod.id("colors"), colors);

            Mod.LOGGER.info("XercaPaint: Registered item properties for canvas and palette");
        });
    }

    @SubscribeEvent
    public static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        Mod.LOGGER.info("XercaPaint: Registering client extensions for canvas items");

        IClientItemExtensions canvasExtensions = new IClientItemExtensions() {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (CANVAS_ITEM_RENDERER == null) {
                    Mod.LOGGER.info("XercaPaint: Creating CanvasItemRenderer");
                    CANVAS_ITEM_RENDERER = new CanvasItemRenderer();
                }
                return CANVAS_ITEM_RENDERER;
            }
        };

        event.registerItem(canvasExtensions,
            Items.ITEM_CANVAS.get(),
            Items.ITEM_CANVAS_LARGE.get(),
            Items.ITEM_CANVAS_LONG.get(),
            Items.ITEM_CANVAS_TALL.get()
        );

        Mod.LOGGER.info("XercaPaint: Client extensions registered successfully");
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(Entities.CANVAS.get(), RenderEntityCanvas::new);
        event.registerEntityRenderer(Entities.EASEL.get(), RenderEntityEasel::new);
        event.registerBlockEntityRenderer(BlockEntities.CANVAS.get(), RenderBlockCanvas::new);
    }

    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(EASEL_MAIN_LAYER, EaselModel::createBodyLayer);
        event.registerLayerDefinition(EASEL_CANVAS_LAYER, EaselModel::createBodyLayer);
    }
}

package xerca.xercapaint;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xerca.xercapaint.block.Blocks;
import xerca.xercapaint.block_entity.BlockEntities;
import xerca.xercapaint.entity.Entities;
import xerca.xercapaint.item.Items;
import xerca.xercapaint.packets.*;

@net.neoforged.fml.common.Mod(Mod.MODID)
public class Mod {
    public static final String MODID = "xercapaint";
    public static final Logger LOGGER = LogManager.getLogger(MODID);

    public Mod(IEventBus modEventBus, ModContainer modContainer) {
        // Register deferred registries
        Items.ITEMS.register(modEventBus);
        Items.DATA_COMPONENT_TYPES.register(modEventBus);
        Items.RECIPE_SERIALIZERS.register(modEventBus);
        Items.CREATIVE_MODE_TABS.register(modEventBus);
        Blocks.BLOCKS.register(modEventBus);
        BlockEntities.BLOCK_ENTITIES.register(modEventBus);
        Entities.ENTITY_TYPES.register(modEventBus);
        SoundEvents.SOUND_EVENTS.register(modEventBus);

        // Register mod event handlers
        modEventBus.addListener(this::onRegisterPayloads);

        // Register game event handlers
        NeoForge.EVENT_BUS.register(this);
    }

    private void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(MODID).versioned("1.0.0");

        // Server-to-Client packets (S2C)
        if (FMLEnvironment.dist.isClient()) {
            // On client: register with actual handlers
            xerca.xercapaint.client.ClientPacketHandler.registerClientPackets(registrar);
        } else {
            // On server: register with no-op handlers (server only sends these, never receives)
            registrar.playToClient(CloseGuiPacket.TYPE, CloseGuiPacket.STREAM_CODEC, (packet, context) -> {});
            registrar.playToClient(ExportPaintingPacket.TYPE, ExportPaintingPacket.STREAM_CODEC, (packet, context) -> {});
            registrar.playToClient(ImportPaintingPacket.TYPE, ImportPaintingPacket.STREAM_CODEC, (packet, context) -> {});
            registrar.playToClient(OpenGuiPacket.TYPE, OpenGuiPacket.STREAM_CODEC, (packet, context) -> {});
            registrar.playToClient(PictureSendPacket.TYPE, PictureSendPacket.STREAM_CODEC, (packet, context) -> {});
        }

        // Client-to-Server packets (C2S)
        registrar.playToServer(CanvasUpdatePacket.TYPE, CanvasUpdatePacket.STREAM_CODEC, CanvasUpdatePacketHandler::handle);
        registrar.playToServer(CanvasMiniUpdatePacket.TYPE, CanvasMiniUpdatePacket.STREAM_CODEC, CanvasMiniUpdatePacketHandler::handle);
        registrar.playToServer(EaselLeftPacket.TYPE, EaselLeftPacket.STREAM_CODEC, EaselLeftPacketHandler::handle);
        registrar.playToServer(ImportPaintingSendPacket.TYPE, ImportPaintingSendPacket.STREAM_CODEC, ImportPaintingSendPacketHandler::handle);
        registrar.playToServer(PaletteUpdatePacket.TYPE, PaletteUpdatePacket.STREAM_CODEC, PaletteUpdatePacketHandler::handle);
        registrar.playToServer(PictureRequestPacket.TYPE, PictureRequestPacket.STREAM_CODEC, PictureRequestPacketHandler::handle);
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        CommandImport.register(event.getDispatcher());
        CommandExport.register(event.getDispatcher());
    }

    public static void sendToClient(ServerPlayer player, CustomPacketPayload packet) {
        PacketDistributor.sendToPlayer(player, packet);
    }

    public static ResourceLocation id(String location) {
        return ResourceLocation.fromNamespaceAndPath(MODID, location);
    }

    public static ResourceKey<Item> itemKey(String location) {
        return ResourceKey.create(Registries.ITEM, Mod.id(location));
    }
}

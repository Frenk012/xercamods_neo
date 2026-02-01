package xerca.xercamusic.common;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xerca.xercamusic.common.block.Blocks;
import xerca.xercamusic.common.entity.Entities;
import xerca.xercamusic.common.item.Items;
import xerca.xercamusic.common.loot.LootModifiers;
import xerca.xercamusic.common.packets.clientbound.*;
import xerca.xercamusic.common.packets.serverbound.*;
import xerca.xercamusic.common.tile_entity.BlockEntities;

import javax.annotation.Nullable;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

@net.neoforged.fml.common.Mod(Mod.MODID)
public class Mod {
    public static final String MODID = "xercamusic";
    public static final Logger LOGGER = LogManager.getLogger();
    public static final int MAX_NOTES_IN_PACKET = 5000;

    public Mod(IEventBus modEventBus, ModContainer modContainer) {
        // Register deferred registries
        Blocks.BLOCKS.register(modEventBus);
        BlockEntities.BLOCK_ENTITIES.register(modEventBus);
        Items.ITEMS.register(modEventBus);
        Items.DATA_COMPONENT_TYPES.register(modEventBus);
        Items.RECIPE_SERIALIZERS.register(modEventBus);
        Items.CREATIVE_MODE_TABS.register(modEventBus);
        Entities.ENTITY_TYPES.register(modEventBus);
        SoundEvents.SOUND_EVENTS.register(modEventBus);
        Triggers.TRIGGERS.register(modEventBus);
        LootModifiers.LOOT_MODIFIER_SERIALIZERS.register(modEventBus);

        // Register mod event handlers
        modEventBus.addListener(this::onCommonSetup);
        modEventBus.addListener(this::onRegisterPayloads);

        // Register game event handlers
        NeoForge.EVENT_BUS.register(this);
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            // Initialize sound events on instruments
            SoundEvents.initInstrumentSounds();
        });
    }

    private void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(MODID).versioned("1.0.0");

        // Server-bound packets (Client -> Server)
        registrar.playToServer(MusicUpdatePacket.TYPE, MusicUpdatePacket.STREAM_CODEC, MusicUpdatePacketHandler::handle);
        registrar.playToServer(MusicEndedPacket.TYPE, MusicEndedPacket.STREAM_CODEC, MusicEndedPacketHandler::handle);
        registrar.playToServer(ImportMusicSendPacket.TYPE, ImportMusicSendPacket.STREAM_CODEC, ImportMusicSendPacketHandler::handle);
        registrar.playToServer(MusicDataRequestPacket.TYPE, MusicDataRequestPacket.STREAM_CODEC, MusicDataRequestPacketHandler::handle);
        registrar.playToServer(SingleNotePacket.TYPE, SingleNotePacket.STREAM_CODEC, SingleNotePacketHandler::handle);
        registrar.playToServer(SendNotesPartToServerPacket.TYPE, SendNotesPartToServerPacket.STREAM_CODEC, SendNotesPartToServerPacketHandler::handle);

        // Client-bound packets (Server -> Client)
        registrar.playToClient(ExportMusicPacket.TYPE, ExportMusicPacket.STREAM_CODEC, ExportMusicPacketHandler::handle);
        registrar.playToClient(ImportMusicPacket.TYPE, ImportMusicPacket.STREAM_CODEC, ImportMusicPacketHandler::handle);
        registrar.playToClient(MusicBoxUpdatePacket.TYPE, MusicBoxUpdatePacket.STREAM_CODEC, MusicBoxUpdatePacketHandler::handle);
        registrar.playToClient(MusicDataResponsePacket.TYPE, MusicDataResponsePacket.STREAM_CODEC, MusicDataResponsePacketHandler::handle);
        registrar.playToClient(NotesPartAckFromServerPacket.TYPE, NotesPartAckFromServerPacket.STREAM_CODEC, NotesPartAckFromServerPacketHandler::handle);
        registrar.playToClient(SingleNoteClientPacket.TYPE, SingleNoteClientPacket.STREAM_CODEC, SingleNoteClientPacketHandler::handle);
        registrar.playToClient(TripleNoteClientPacket.TYPE, TripleNoteClientPacket.STREAM_CODEC, TripleNoteClientPacketHandler::handle);
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        CommandImport.register(event.getDispatcher());
        CommandExport.register(event.getDispatcher());
    }

    public static void sendToClient(ServerPlayer player, CustomPacketPayload packet) {
        PacketDistributor.sendToPlayer(player, packet);
    }

    @Nullable
    public static <T> T onlyCallOnClient(Supplier<Callable<T>> toRun) throws Exception {
        if (FMLLoader.getDist() == Dist.CLIENT) {
            return toRun.get().call();
        }
        return null;
    }

    public static void onlyRunOnClient(Supplier<Runnable> toRun) {
        if (FMLLoader.getDist() == Dist.CLIENT) {
            toRun.get().run();
        }
    }

    public static ResourceLocation id(String location) {
        return ResourceLocation.fromNamespaceAndPath(MODID, location);
    }
}

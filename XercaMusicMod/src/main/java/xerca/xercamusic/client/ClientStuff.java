package xerca.xercamusic.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;
import xerca.xercamusic.common.Mod;
import xerca.xercamusic.common.SoundEvents;
import xerca.xercamusic.common.entity.Entities;
import xerca.xercamusic.common.item.IItemInstrument;
import xerca.xercamusic.common.item.ItemMusicSheet;
import xerca.xercamusic.common.item.Items;
import xerca.xercamusic.common.packets.serverbound.MusicEndedPacket;

import java.util.UUID;

@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = Mod.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientStuff {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        // Register game event handlers
        NeoForge.EVENT_BUS.register(ClientEvents.class);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(Entities.MUSIC_SPIRIT.get(), RenderNothing::new);
    }

    public static void showMusicGui() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            ItemStack heldItem = player.getMainHandItem();
            if (!heldItem.isEmpty() && heldItem.getItem() instanceof ItemMusicSheet) {
                player.playSound(SoundEvents.OPEN_SCROLL.get(), 1.0f, 0.8f + player.level().random.nextFloat() * 0.4f);
                UUID id = heldItem.get(Items.SHEET_ID.get());
                int version = heldItem.getOrDefault(Items.SHEET_VERSION.get(), -1);
                if (id != null && version >= 0) {
                    MusicManagerClient.checkMusicDataAndRun(id, version, () -> Minecraft.getInstance().setScreen(new GuiMusicSheet(player, heldItem, Component.translatable("item.xercamusic.music_sheet"))));
                } else {
                    Minecraft.getInstance().setScreen(new GuiMusicSheet(player, heldItem, Component.translatable("item.xercamusic.music_sheet")));
                }
            }
        }
    }

    public static void showInstrumentGui() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            ItemStack heldItem = player.getMainHandItem();
            if (!heldItem.isEmpty() && heldItem.getItem() instanceof IItemInstrument) {
                Minecraft.getInstance().setScreen(new GuiInstrument(player, (IItemInstrument) heldItem.getItem(), Component.translatable("item.xercamusic.instrument_gui"), null));
            }
        }
    }

    public static void showInstrumentGui(IItemInstrument instrument, BlockPos blockInsPos) {
        LocalPlayer player = Minecraft.getInstance().player;
        Minecraft.getInstance().setScreen(new GuiInstrument(player, instrument, Component.translatable("item.xercamusic.instrument_gui"), blockInsPos));
    }

    public static NoteSound playNote(SoundEvent event, double x, double y, double z, float volume, float pitch, byte lengthTicks) {
        return playNote(event, x, y, z, SoundSource.PLAYERS, volume, pitch, lengthTicks);
    }

    public static NoteSound playNote(SoundEvent event, double x, double y, double z, float volume, float pitch) {
        return playNote(event, x, y, z, SoundSource.PLAYERS, volume, pitch, (byte) -1);
    }

    public static void playNoteTE(SoundEvent event, double x, double y, double z, float volume, float pitch, byte lengthTicks) {
        playNote(event, x, y, z, SoundSource.RECORDS, volume, pitch, lengthTicks);
    }

    public static NoteSound playNote(SoundEvent event, double x, double y, double z, SoundSource category, float volume, float pitch, byte lengthTicks) {
        NoteSound sound = new NoteSound(event, category, (float) x, (float) y, (float) z, volume, pitch, lengthTicks);
        Minecraft.getInstance().getSoundManager().play(sound);
        return sound;
    }

    public static void endMusic(int spiritID, int playerID) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && playerID == player.getId()) {
            MusicEndedPacket pack = new MusicEndedPacket(spiritID);
            sendToServer(pack);
        }
    }

    public static void sendToServer(CustomPacketPayload packet) {
        PacketDistributor.sendToServer(packet);
    }
}

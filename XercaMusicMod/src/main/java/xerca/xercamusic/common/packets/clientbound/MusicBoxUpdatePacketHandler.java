package xerca.xercamusic.common.packets.clientbound;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import xerca.xercamusic.common.item.Items;
import xerca.xercamusic.common.tile_entity.TileEntityMusicBox;

public class MusicBoxUpdatePacketHandler {
    private static void processMessage(MusicBoxUpdatePacket msg) {
        Level world = Minecraft.getInstance().level;
        if (world == null || !world.hasChunkAt(msg.pos())) {  // NOSONAR
            return;
        }

        BlockEntity te = world.getBlockEntity(msg.pos());
        if (te instanceof TileEntityMusicBox tileEntityMusicBox) {

            if (msg.sheetSent()) {
                if (msg.noSheet()) {
                    tileEntityMusicBox.removeSheetStack();
                } else {
                    ItemStack sheetStack = new ItemStack(Items.MUSIC_SHEET.get());
                    sheetStack.set(Items.SHEET_ID.get(), msg.sheetId());
                    sheetStack.set(Items.SHEET_VERSION.get(), msg.version());
                    sheetStack.set(Items.SHEET_BPS.get(), msg.bps());
                    sheetStack.set(Items.SHEET_LENGTH.get(), msg.length());
                    sheetStack.set(Items.SHEET_VOLUME.get(), msg.volume());
                    tileEntityMusicBox.setSheetStack(sheetStack, false);
                }
            }

            if (!msg.instrumentId().isEmpty()) {
                tileEntityMusicBox.setInstrument(BuiltInRegistries.ITEM.get(ResourceLocation.parse(msg.instrumentId())));
            } else {
                tileEntityMusicBox.removeInstrument();
            }
        }
    }

    public static void handle(MusicBoxUpdatePacket packet, IPayloadContext context) {
        if (packet != null) {
            context.enqueueWork(() -> processMessage(packet));
        }
    }
}

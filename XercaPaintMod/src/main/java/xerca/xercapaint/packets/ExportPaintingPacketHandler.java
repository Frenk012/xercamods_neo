package xerca.xercapaint.packets;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import xerca.xercapaint.CommandExport;

public class ExportPaintingPacketHandler {
    private static void processMessage(ExportPaintingPacket msg) {
        Minecraft m = Minecraft.getInstance();
        if (m.player != null) {
            if (CommandExport.doExport(m.player, msg.canvasId())) {
                m.player.displayClientMessage(Component.translatable("xercapaint.export.success", msg.canvasId()).withStyle(ChatFormatting.GREEN), false);
            } else {
                m.player.displayClientMessage(Component.translatable("xercapaint.export.fail", msg.canvasId()).withStyle(ChatFormatting.RED), false);
            }
        }
    }

    public static void handle(ExportPaintingPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> processMessage(packet));
    }
}

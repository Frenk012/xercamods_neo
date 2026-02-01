package xerca.xercapaint.packets;

import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class CloseGuiPacketHandler {
    private static void processMessage() {
        Minecraft.getInstance().setScreen(null);
    }

    public static void handle(CloseGuiPacket packet, IPayloadContext context) {
        context.enqueueWork(CloseGuiPacketHandler::processMessage);
    }
}

package xerca.xercapaint.packets;

import net.neoforged.neoforge.network.handling.IPayloadContext;
import xerca.xercapaint.entity.EntityCanvas;

public class PictureSendPacketHandler {
    private static void processMessage(PictureSendPacket msg) {
        EntityCanvas.PICTURES.put(msg.canvasId(), new EntityCanvas.Picture(msg.version(), msg.pixels()));
        if (!EntityCanvas.PICTURE_REQUESTS.contains(msg.canvasId())) {
            EntityCanvas.PICTURE_REQUESTS.remove(msg.canvasId());
        }
    }

    public static void handle(PictureSendPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> processMessage(packet));
    }
}

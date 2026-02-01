package xerca.xercapaint.client;

import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import xerca.xercapaint.packets.*;

public class ClientPacketHandler {
    public static void registerClientPackets(PayloadRegistrar registrar) {
        // Server-to-Client packets (S2C) - handlers run only on client
        registrar.playToClient(CloseGuiPacket.TYPE, CloseGuiPacket.STREAM_CODEC, CloseGuiPacketHandler::handle);
        registrar.playToClient(ExportPaintingPacket.TYPE, ExportPaintingPacket.STREAM_CODEC, ExportPaintingPacketHandler::handle);
        registrar.playToClient(ImportPaintingPacket.TYPE, ImportPaintingPacket.STREAM_CODEC, ImportPaintingPacketHandler::handle);
        registrar.playToClient(OpenGuiPacket.TYPE, OpenGuiPacket.STREAM_CODEC, OpenGuiPacketHandler::handle);
        registrar.playToClient(PictureSendPacket.TYPE, PictureSendPacket.STREAM_CODEC, PictureSendPacketHandler::handle);
    }
}

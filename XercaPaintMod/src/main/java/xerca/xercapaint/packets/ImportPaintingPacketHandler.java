package xerca.xercapaint.packets;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.io.IOException;
import java.nio.file.Path;

public class ImportPaintingPacketHandler {

    private static void processMessage(ImportPaintingPacket msg) {
        String filename = msg.canvasId() + ".paint";
        String filepath = "paintings/" + filename;
        try {
            CompoundTag tag = NbtIo.read(Path.of(filepath));
            PacketDistributor.sendToServer(new ImportPaintingSendPacket(tag));
        } catch (IOException e) {
            e.printStackTrace();
            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null) {
                player.displayClientMessage(Component.translatable("xercapaint.import.fail.4", filepath).withStyle(ChatFormatting.RED), false);
            }
        }
    }

    public static void handle(ImportPaintingPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> processMessage(packet));
    }
}

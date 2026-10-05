package xerca.xercapaint;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import xerca.xercapaint.item.ItemCanvas;
import xerca.xercapaint.item.ItemPalette;
import xerca.xercapaint.item.Items;
import xerca.xercapaint.packets.ImportPaintingPacket;

import java.util.Arrays;

public class CommandImport {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("paintimport")
                        .requires(source -> Config.importEnabled()
                                && (!Config.importRequiresOp() || source.hasPermission(Config.importOpLevel())))
                        .then(Commands.argument("name", StringArgumentType.word())
                                .executes((p) -> paintImport(p.getSource(), StringArgumentType.getString(p, "name"))))
        );
    }

    private static int paintImport(CommandSourceStack stack, String name) {
        Mod.LOGGER.debug("Paint import called. name: {}", name);

        ImportPaintingPacket pack = new ImportPaintingPacket(name);
        try {
            ServerPlayer player = stack.getPlayerOrException();
            PacketDistributor.sendToPlayer(player, pack);
        } catch (CommandSyntaxException e) {
            Mod.LOGGER.debug("Command executor is not a player");
            e.printStackTrace();
            return 0;
        }

        return 1;
    }

    public static void doImport(CompoundTag tag, ServerPlayer player) {
        // Sanitizing
        if (!tag.contains("name", 8)) {
            player.sendSystemMessage(Component.translatable("xercapaint.import.fail.5").withStyle(ChatFormatting.RED));
            Mod.LOGGER.warn("Broken paint file");
            return;
        }
        String canvasId = tag.getString("name");
        if (!canvasId.matches("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}_[0-9]+$")) {
            player.sendSystemMessage(Component.translatable("xercapaint.import.fail.5").withStyle(ChatFormatting.RED));
            Mod.LOGGER.warn("Broken paint file");
            return;
        }
        if ((tag.contains("author", 8) && !tag.contains("title", 8)) ||
                (!tag.contains("author", 8) && tag.contains("title", 8))) {
            player.sendSystemMessage(Component.translatable("xercapaint.import.fail.5").withStyle(ChatFormatting.RED));
            Mod.LOGGER.warn("Broken paint file");
            return;
        }
        if (tag.contains("title", 8) && tag.getString("title").length() > 16) {
            tag.putString("title", tag.getString("title").substring(0, 16));
        }
        if (tag.contains("author", 8) && tag.getString("author").length() > 16) {
            tag.putString("author", tag.getString("author").substring(0, 16));
        }
        if (!tag.contains("v", 3)) {
            tag.putInt("v", 1);
        }

        byte canvasType = tag.getByte("ct");
        boolean importedGlass = tag.getBoolean("glass");
        tag.remove("ct");
        tag.remove("glass");
        if (tag.getInt("generation") > 0 && tag.getInt("generation") < 3) {
            tag.putInt("generation", tag.getInt("generation") + 1);
        }

        ItemStack itemStack;
        boolean doAddItem = false;
        if (player.isCreative()) {
            CanvasType type = CanvasType.fromByte(canvasType);
            if (type == null) {
                Mod.LOGGER.error("Invalid canvas type");
                return;
            }
            itemStack = new ItemStack(ItemCanvas.canvasItemFor(type, importedGlass));
            doAddItem = true;
        } else {
            ItemStack mainhand = player.getMainHandItem();
            ItemStack offhand = player.getOffhandItem();

            if (!(mainhand.getItem() instanceof ItemCanvas) || (mainhand.get(Items.CANVAS_PIXELS.get()) != null || mainhand.get(Items.CANVAS_ID.get()) != null)) {
                player.sendSystemMessage(Component.translatable("xercapaint.import.fail.1").withStyle(ChatFormatting.RED));
                return;
            }
            CanvasType type = CanvasType.fromByte(canvasType);
            if (type == null) {
                return;
            }
            ItemCanvas heldCanvas = (ItemCanvas) mainhand.getItem();
            if (heldCanvas.getCanvasType() != type) {
                Component typeName = ItemCanvas.canvasItemFor(type, importedGlass).getName(ItemStack.EMPTY);
                player.sendSystemMessage(Component.translatable("xercapaint.import.fail.2", typeName).withStyle(ChatFormatting.RED));
                return;
            }
            if (heldCanvas.isGlass() != importedGlass) {
                Component typeName = ItemCanvas.canvasItemFor(type, importedGlass).getName(ItemStack.EMPTY);
                player.sendSystemMessage(Component.translatable("xercapaint.import.fail.material", typeName).withStyle(ChatFormatting.RED));
                return;
            }
            if (!ItemPalette.isFull(offhand)) {
                player.sendSystemMessage(Component.translatable("xercapaint.import.fail.3").withStyle(ChatFormatting.RED));
                return;
            }
            itemStack = mainhand;
        }

        itemStack.set(Items.CANVAS_VERSION.get(), tag.getInt("v"));
        itemStack.set(Items.CANVAS_ID.get(), canvasId);
        itemStack.set(Items.CANVAS_PIXELS.get(), Arrays.stream(tag.getIntArray("pixels")).boxed().toList());
        itemStack.set(Items.CANVAS_GENERATION.get(), tag.getInt("generation"));
        if (tag.contains("title", 8) && tag.contains("author", 8)) {
            itemStack.set(Items.CANVAS_TITLE.get(), tag.getString("title"));
            itemStack.set(Items.CANVAS_AUTHOR.get(), tag.getString("author"));
        }
        if (tag.contains("sidePixels")) {
            int[] sidePixels = tag.getIntArray("sidePixels");
            CanvasType importedType = ((ItemCanvas) itemStack.getItem()).getCanvasType();
            if (sidePixels.length == CanvasSides.count(importedType)) {
                itemStack.set(Items.CANVAS_SIDES_ACTIVE.get(), tag.getBoolean("sidesActive"));
                itemStack.set(Items.CANVAS_SIDE_PIXELS.get(), Arrays.stream(sidePixels).boxed().toList());
            }
        }
        ItemCanvas.updateStackSize(itemStack);
        if (doAddItem) {
            player.addItem(itemStack);
        }

        player.sendSystemMessage(Component.translatable("xercapaint.import.success").withStyle(ChatFormatting.GREEN));
    }
}

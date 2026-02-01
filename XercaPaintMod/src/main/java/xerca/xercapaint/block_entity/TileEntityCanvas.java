package xerca.xercapaint.block_entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import xerca.xercapaint.CanvasType;
import xerca.xercapaint.entity.EntityCanvas;
import xerca.xercapaint.item.Items;
import xerca.xercapaint.packets.PictureRequestPacket;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.Arrays;

public class TileEntityCanvas extends BlockEntity {
    private String canvasId = "";
    private int version = 0;
    private int[] pixels;
    private String title = "";
    private String author = "";
    private int generation = 0;
    private int rotation = 0;
    private CanvasType canvasType = CanvasType.SMALL;

    // Multi-block support: master position (null if this is the master)
    private BlockPos masterPos = null;
    private boolean isMaster = true;

    public TileEntityCanvas(BlockPos pos, BlockState state) {
        super(BlockEntities.CANVAS.get(), pos, state);
    }

    public void loadFromStack(ItemStack stack, CanvasType type, int rotation) {
        this.canvasType = type;
        this.rotation = rotation;

        String id = stack.get(Items.CANVAS_ID.get());
        if (id != null) {
            this.canvasId = id;
        }

        this.version = stack.getOrDefault(Items.CANVAS_VERSION.get(), 0);

        var pixelList = stack.get(Items.CANVAS_PIXELS.get());
        if (pixelList != null) {
            this.pixels = pixelList.stream().mapToInt(i -> i).toArray();
            // Also store in the shared picture cache
            EntityCanvas.PICTURES.put(canvasId, new EntityCanvas.Picture(version, pixels));
        }

        String stackTitle = stack.get(Items.CANVAS_TITLE.get());
        if (stackTitle != null) {
            this.title = stackTitle;
        }

        String stackAuthor = stack.get(Items.CANVAS_AUTHOR.get());
        if (stackAuthor != null) {
            this.author = stackAuthor;
        }

        this.generation = stack.getOrDefault(Items.CANVAS_GENERATION.get(), 0);

        setChanged();
    }

    public ItemStack getCanvasItem() {
        ItemStack stack;
        switch (canvasType) {
            case LARGE -> stack = new ItemStack(Items.ITEM_CANVAS_LARGE.get());
            case LONG -> stack = new ItemStack(Items.ITEM_CANVAS_LONG.get());
            case TALL -> stack = new ItemStack(Items.ITEM_CANVAS_TALL.get());
            default -> stack = new ItemStack(Items.ITEM_CANVAS.get());
        }

        if (!canvasId.isEmpty()) {
            stack.set(Items.CANVAS_ID.get(), canvasId);
            stack.set(Items.CANVAS_VERSION.get(), version);
        }

        if (pixels != null && pixels.length > 0) {
            stack.set(Items.CANVAS_PIXELS.get(), Arrays.stream(pixels).boxed().toList());
        }

        if (!title.isEmpty() && !author.isEmpty()) {
            stack.set(Items.CANVAS_TITLE.get(), title);
            stack.set(Items.CANVAS_AUTHOR.get(), author);
            stack.set(Items.CANVAS_GENERATION.get(), generation);
        }

        return stack;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString("canvasId", canvasId);
        tag.putInt("version", version);
        tag.putByte("canvasType", (byte) canvasType.ordinal());
        tag.putByte("rotation", (byte) rotation);
        tag.putString("title", title);
        tag.putString("author", author);
        tag.putInt("generation", generation);
        tag.putBoolean("isMaster", isMaster);
        if (masterPos != null) {
            tag.putInt("masterX", masterPos.getX());
            tag.putInt("masterY", masterPos.getY());
            tag.putInt("masterZ", masterPos.getZ());
        }
        if (pixels != null) {
            tag.putIntArray("pixels", pixels);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        canvasId = tag.getString("canvasId");
        version = tag.getInt("version");
        canvasType = CanvasType.fromByte(tag.getByte("canvasType"));
        if (canvasType == null) canvasType = CanvasType.SMALL;
        rotation = tag.getByte("rotation");
        title = tag.getString("title");
        author = tag.getString("author");
        generation = tag.getInt("generation");
        isMaster = tag.getBoolean("isMaster");
        if (tag.contains("masterX")) {
            masterPos = new BlockPos(tag.getInt("masterX"), tag.getInt("masterY"), tag.getInt("masterZ"));
        } else {
            masterPos = null;
        }
        if (tag.contains("pixels")) {
            pixels = tag.getIntArray("pixels");
            // Store in shared picture cache
            if (!canvasId.isEmpty() && pixels.length > 0) {
                EntityCanvas.Picture existing = EntityCanvas.PICTURES.get(canvasId);
                if (existing == null || existing.version() < version) {
                    EntityCanvas.PICTURES.put(canvasId, new EntityCanvas.Picture(version, pixels));
                }
            }
        }
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public @NotNull CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }

    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        loadAdditional(tag, registries);

        // Client-side: request picture if not cached
        if (level != null && level.isClientSide && !canvasId.isEmpty() && version > 0) {
            EntityCanvas.Picture picture = EntityCanvas.PICTURES.get(canvasId);
            if (picture == null || picture.version() < version) {
                if (!EntityCanvas.PICTURE_REQUESTS.contains(canvasId)) {
                    EntityCanvas.PICTURE_REQUESTS.add(canvasId);
                    PacketDistributor.sendToServer(new PictureRequestPacket(canvasId));
                }
            }
        }
    }

    // Getters
    public String getCanvasId() {
        return canvasId;
    }

    public int getVersion() {
        return version;
    }

    public int[] getPixels() {
        return pixels;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public int getGeneration() {
        return generation;
    }

    public int getRotation() {
        return rotation;
    }

    public void setRotation(int rotation) {
        this.rotation = rotation;
    }

    public CanvasType getCanvasType() {
        return canvasType;
    }

    public int getWidth() {
        return CanvasType.getWidth(canvasType);
    }

    public int getHeight() {
        return CanvasType.getHeight(canvasType);
    }

    public boolean hasTitle() {
        return !title.isEmpty();
    }

    // Multi-block support methods
    public boolean isMaster() {
        return isMaster;
    }

    public BlockPos getMasterPos() {
        return masterPos;
    }

    public void setAsPart(BlockPos masterPos, CanvasType type, int rotation) {
        this.isMaster = false;
        this.masterPos = masterPos;
        this.canvasType = type;
        this.rotation = rotation;
        setChanged();
    }

    /**
     * Gets the master block entity, or this block entity if this is the master.
     * Returns null if the master block no longer exists.
     */
    @Nullable
    public TileEntityCanvas getMaster() {
        if (isMaster) {
            return this;
        }
        if (masterPos != null && level != null) {
            BlockEntity be = level.getBlockEntity(masterPos);
            if (be instanceof TileEntityCanvas master) {
                return master;
            }
        }
        return null;
    }
}

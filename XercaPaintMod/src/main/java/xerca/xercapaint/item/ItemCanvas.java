package xerca.xercapaint.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.StringUtil;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.lwjgl.system.NonnullDefault;
import xerca.xercapaint.CanvasType;
import xerca.xercapaint.block.BlockCanvas;
import xerca.xercapaint.block.Blocks;
import xerca.xercapaint.block_entity.TileEntityCanvas;
import xerca.xercapaint.client.ModClient;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

@NonnullDefault
public class ItemCanvas extends Item {
    private final CanvasType canvasType;

    ItemCanvas(CanvasType canvasType, String name) {
        super(new Item.Properties().stacksTo(1));
        this.canvasType = canvasType;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level worldIn, Player playerIn, @Nonnull InteractionHand hand) {
        if (worldIn.isClientSide) {
            ModClient.showCanvasGui(playerIn);
        }
        return InteractionResultHolder.success(playerIn.getItemInHand(hand));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        BlockPos blockpos = context.getClickedPos();
        Direction direction = context.getClickedFace();
        BlockPos pos = blockpos.relative(direction);
        Player player = context.getPlayer();
        ItemStack itemstack = context.getItemInHand();
        if (player != null) {
            if (!this.mayPlace(player, direction, itemstack, pos)) {
                if (context.getLevel().isClientSide) {
                    ModClient.showCanvasGui(player);
                }
            } else {
                Level world = context.getLevel();

                String canvasId = itemstack.get(Items.CANVAS_ID.get());
                List<Integer> canvasPixels = itemstack.get(Items.CANVAS_PIXELS.get());
                if (canvasId == null || canvasPixels == null) {
                    if (context.getLevel().isClientSide) {
                        ModClient.showCanvasGui(player);
                    }
                    return InteractionResult.SUCCESS;
                }

                int rotation = getRotation(direction, blockpos, player);

                if (!world.isClientSide) {
                    // Check if there's enough space for the canvas
                    if (!canPlaceCanvas(world, pos, direction, canvasType)) {
                        return InteractionResult.FAIL;
                    }

                    // Place canvas block instead of spawning entity
                    BlockState canvasState = Blocks.CANVAS.get().defaultBlockState()
                            .setValue(BlockCanvas.FACING, direction);

                    // Check if position is valid
                    if (world.getBlockState(pos).canBeReplaced() && canvasState.canSurvive(world, pos)) {
                        // Place all blocks for multi-block canvas
                        placeMultiBlockCanvas(world, pos, direction, canvasType, canvasState, itemstack, rotation);

                        world.playSound(null, pos, SoundEvents.PAINTING_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
                        itemstack.shrink(1);
                    }
                }
            }
        }
        return InteractionResult.SUCCESS;
    }

    private static int getRotation(Direction direction, BlockPos blockpos, Player player) {
        int rotation = 0;
        if (direction.getAxis() == Direction.Axis.Y) {
            double xDiff = blockpos.getX() - player.getX();
            double zDiff = blockpos.getZ() - player.getZ();
            if (Math.abs(xDiff) > Math.abs(zDiff)) {
                if (xDiff > 0) {
                    rotation = 1;
                } else {
                    rotation = 3;
                }
            } else {
                if (zDiff > 0) {
                    rotation = 2;
                }
            }
            if (direction == Direction.DOWN && Math.abs(xDiff) < Math.abs(zDiff)) {
                rotation += 2;
            }
        }
        return rotation;
    }

    public static boolean hasTitle(@Nonnull ItemStack stack) {
        return !StringUtil.isNullOrEmpty(stack.get(Items.CANVAS_TITLE.get()));
    }

    public static Component getFullLabel(@Nonnull ItemStack stack) {
        String labelString = "";
        Component title = getCustomTitle(stack);
        if (title != null) {
            labelString += (title.getString() + " ");
        }
        String author = stack.get(Items.CANVAS_AUTHOR.get());

        if (!StringUtil.isNullOrEmpty(author)) {
            labelString += (Component.translatable("canvas.byAuthor", author)).getString() + " ";
        }

        int generation = stack.getOrDefault(Items.CANVAS_GENERATION.get(), 0);
        MutableComponent label = Component.literal(labelString);
        if (generation == 1) {
            label.withStyle(ChatFormatting.YELLOW);
        } else if (generation >= 3) {
            label.withStyle(ChatFormatting.GRAY);
        }
        return label;
    }

    @Nullable
    public static Component getCustomTitle(@Nonnull ItemStack stack) {
        String s = stack.get(Items.CANVAS_TITLE.get());
        if (!StringUtil.isNullOrEmpty(s)) {
            return Component.literal(s);
        }
        return null;
    }

    @Nonnull
    @Override
    public Component getName(@Nonnull ItemStack stack) {
        Component comp = getCustomTitle(stack);
        if (comp != null) {
            return comp;
        }
        return super.getName(stack);
    }

    @Override
    @net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        List<Integer> pixels = stack.get(Items.CANVAS_PIXELS.get());
        if (pixels != null) {
            String author = stack.get(Items.CANVAS_AUTHOR.get());

            if (!StringUtil.isNullOrEmpty(author)) {
                tooltipComponents.add(Component.translatable("canvas.byAuthor", author));
            }

            int generation = stack.getOrDefault(Items.CANVAS_GENERATION.get(), 0);
            // generation = 0 means empty, 1 means original, more means copy
            if (generation > 0) {
                tooltipComponents.add((Component.translatable("canvas.generation." + (generation - 1))).withStyle(ChatFormatting.GRAY));
            }
            // Feature 10: mark protected (waxed) paintings.
            if (stack.getOrDefault(Items.CANVAS_WAXED.get(), false)) {
                tooltipComponents.add(Component.translatable("canvas.waxed").withStyle(ChatFormatting.GOLD));
            }
        } else {
            tooltipComponents.add(Component.translatable("canvas.empty").withStyle(ChatFormatting.GRAY));
        }
    }

    @Override
    @net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
    public boolean isFoil(ItemStack stack) {
        return stack.getOrDefault(Items.CANVAS_GENERATION.get(), 0) > 0;
    }

    public int getWidth() {
        return CanvasType.getWidth(canvasType);
    }

    public int getHeight() {
        return CanvasType.getHeight(canvasType);
    }

    public CanvasType getCanvasType() {
        return canvasType;
    }

    protected boolean mayPlace(Player playerIn, Direction directionIn, ItemStack itemStackIn, BlockPos posIn) {
        if (canvasType == CanvasType.SMALL) {
            return Level.isInSpawnableBounds(posIn) && playerIn.mayUseItemAt(posIn, directionIn, itemStackIn);
        } else {
            return !directionIn.getAxis().isVertical() && playerIn.mayUseItemAt(posIn, directionIn, itemStackIn);
        }
    }

    /**
     * Place all blocks for a multi-block canvas.
     * The master block is at pos, secondary blocks reference the master.
     */
    private void placeMultiBlockCanvas(Level world, BlockPos pos, Direction facing, CanvasType canvasType,
                                        BlockState canvasState, ItemStack itemstack, int rotation) {
        int widthBlocks = CanvasType.getWidth(canvasType) / 16;
        int heightBlocks = CanvasType.getHeight(canvasType) / 16;
        Direction leftDir = facing.getCounterClockWise();

        // Place all blocks
        for (int h = 0; h < heightBlocks; h++) {
            for (int w = 0; w < widthBlocks; w++) {
                BlockPos blockPos = pos.relative(leftDir, w).above(h);
                world.setBlock(blockPos, canvasState, 3);

                if (world.getBlockEntity(blockPos) instanceof TileEntityCanvas canvas) {
                    if (h == 0 && w == 0) {
                        // This is the master block
                        canvas.loadFromStack(itemstack, canvasType, rotation);
                    } else {
                        // This is a secondary block - reference the master
                        canvas.setAsPart(pos, canvasType, rotation);
                    }
                    world.sendBlockUpdated(blockPos, canvasState, canvasState, 3);
                }
            }
        }
    }

    /**
     * Check if there's enough space to place the canvas.
     * The canvas extends to the left (counter-clockwise) and up from the placement position.
     */
    private static boolean canPlaceCanvas(Level world, BlockPos pos, Direction facing, CanvasType canvasType) {
        int widthBlocks = CanvasType.getWidth(canvasType) / 16;
        int heightBlocks = CanvasType.getHeight(canvasType) / 16;

        // Get the direction to extend horizontally (counter-clockwise from facing)
        Direction leftDir = facing.getCounterClockWise();

        // Check all positions the canvas will occupy
        for (int h = 0; h < heightBlocks; h++) {
            for (int w = 0; w < widthBlocks; w++) {
                // Calculate position: extend left and up from the base position
                BlockPos checkPos = pos
                        .relative(leftDir, w)
                        .above(h);

                // Check if the position is within world bounds
                if (!Level.isInSpawnableBounds(checkPos)) {
                    return false;
                }

                // Check if the position can be replaced (air or replaceable block)
                if (!world.getBlockState(checkPos).canBeReplaced()) {
                    return false;
                }

                // Check if there's a solid block behind to support the canvas
                BlockPos supportPos = checkPos.relative(facing.getOpposite());
                if (!world.getBlockState(supportPos).isFaceSturdy(world, supportPos, facing)) {
                    return false;
                }
            }
        }

        return true;
    }
}

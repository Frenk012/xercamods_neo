package xerca.xercapaint.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import xerca.xercapaint.CanvasType;
import xerca.xercapaint.block_entity.TileEntityCanvas;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

public class BlockCanvas extends Block implements EntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    // Thin shapes for each facing direction
    private static final VoxelShape SHAPE_NORTH = Block.box(0, 0, 15, 16, 16, 16);
    private static final VoxelShape SHAPE_SOUTH = Block.box(0, 0, 0, 16, 16, 1);
    private static final VoxelShape SHAPE_WEST = Block.box(15, 0, 0, 16, 16, 16);
    private static final VoxelShape SHAPE_EAST = Block.box(0, 0, 0, 1, 16, 16);
    private static final VoxelShape SHAPE_UP = Block.box(0, 0, 0, 16, 1, 16);
    private static final VoxelShape SHAPE_DOWN = Block.box(0, 15, 0, 16, 16, 16);

    public BlockCanvas(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getClickedFace();
        return this.defaultBlockState().setValue(FACING, facing);
    }

    @Override
    public @NotNull VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case NORTH -> SHAPE_NORTH;
            case SOUTH -> SHAPE_SOUTH;
            case WEST -> SHAPE_WEST;
            case EAST -> SHAPE_EAST;
            case UP -> SHAPE_UP;
            case DOWN -> SHAPE_DOWN;
        };
    }

    @Override
    public @NotNull RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        BlockPos supportPos = pos.relative(facing.getOpposite());
        return level.getBlockState(supportPos).isFaceSturdy(level, supportPos, facing);
    }

    @Override
    public @NotNull BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                            LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        Direction facing = state.getValue(FACING);
        if (direction == facing.getOpposite() && !state.canSurvive(level, pos)) {
            // Remove all connected blocks when support is lost
            if (level instanceof Level realLevel && !realLevel.isClientSide) {
                removeConnectedBlocks(realLevel, pos, state);
            }
            return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    protected @NotNull InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                                         Player player, BlockHitResult hitResult) {
        if (level.getBlockEntity(pos) instanceof TileEntityCanvas canvas) {
            // Get the master block for rotation
            TileEntityCanvas master = canvas.getMaster();
            if (master == null) {
                return InteractionResult.PASS;
            }

            CanvasType canvasType = master.getCanvasType();
            // Only small and large canvases can be rotated
            if (canvasType == CanvasType.SMALL || canvasType == CanvasType.LARGE) {
                if (!level.isClientSide) {
                    int newRotation = (master.getRotation() + 1) % 4;
                    master.setRotation(newRotation);
                    master.setChanged();
                    BlockPos masterPos = master.getBlockPos();
                    BlockState masterState = level.getBlockState(masterPos);
                    level.sendBlockUpdated(masterPos, masterState, masterState, Block.UPDATE_ALL);
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    public @NotNull List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        BlockEntity be = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (be instanceof TileEntityCanvas canvas) {
            // Only master block drops the item
            if (canvas.isMaster()) {
                return Collections.singletonList(canvas.getCanvasItem());
            }
        }
        return Collections.emptyList();
    }

    @Override
    public @NotNull BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide) {
            level.playSound(null, pos, SoundEvents.PAINTING_BREAK, SoundSource.BLOCKS, 1.0F, 1.0F);
            // Remove all connected canvas blocks
            removeConnectedBlocks(level, pos, state);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    /**
     * Remove all blocks that are part of the same multi-block canvas.
     */
    private void removeConnectedBlocks(Level level, BlockPos brokenPos, BlockState state) {
        if (!(level.getBlockEntity(brokenPos) instanceof TileEntityCanvas canvas)) {
            return;
        }

        // Find the master block position
        BlockPos masterPos = canvas.isMaster() ? brokenPos : canvas.getMasterPos();
        if (masterPos == null) {
            return;
        }

        // Get canvas info from master or current block
        TileEntityCanvas master = canvas.isMaster() ? canvas : canvas.getMaster();
        if (master == null) {
            return;
        }

        CanvasType canvasType = master.getCanvasType();
        Direction facing = state.getValue(FACING);
        int widthBlocks = CanvasType.getWidth(canvasType) / 16;
        int heightBlocks = CanvasType.getHeight(canvasType) / 16;
        Direction leftDir = facing.getCounterClockWise();

        // Remove all blocks except the one being broken (that one is handled normally)
        for (int h = 0; h < heightBlocks; h++) {
            for (int w = 0; w < widthBlocks; w++) {
                BlockPos blockPos = masterPos.relative(leftDir, w).above(h);
                if (!blockPos.equals(brokenPos)) {
                    // Check if it's still a canvas block before removing
                    if (level.getBlockState(blockPos).getBlock() == this) {
                        level.removeBlock(blockPos, false);
                    }
                }
            }
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityCanvas(pos, state);
    }
}

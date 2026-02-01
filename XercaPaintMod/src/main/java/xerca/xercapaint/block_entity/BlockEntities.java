package xerca.xercapaint.block_entity;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;
import xerca.xercapaint.Mod;
import xerca.xercapaint.block.Blocks;

import java.util.function.Supplier;

public class BlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, Mod.MODID);

    public static final Supplier<BlockEntityType<TileEntityCanvas>> CANVAS = BLOCK_ENTITIES.register("canvas",
            () -> BlockEntityType.Builder.of(TileEntityCanvas::new, Blocks.CANVAS.get()).build(null));
}

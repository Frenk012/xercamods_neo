package xerca.xercapaint.block;

import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import xerca.xercapaint.Mod;

public class Blocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Mod.MODID);

    public static final DeferredBlock<BlockCanvas> CANVAS = BLOCKS.register("canvas",
            () -> new BlockCanvas(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(0.5f)
                    .sound(SoundType.WOOD)
                    .noOcclusion()
                    .noCollission()));
}

package xerca.xercamusic.common.tile_entity;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import xerca.xercamusic.common.Mod;
import xerca.xercamusic.common.block.Blocks;

import java.util.function.Supplier;

public class BlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, Mod.MODID);

    public static final Supplier<BlockEntityType<TileEntityMetronome>> METRONOME = BLOCK_ENTITIES.register("metronome",
            () -> BlockEntityType.Builder.of(TileEntityMetronome::new, Blocks.BLOCK_METRONOME.get()).build(null));

    public static final Supplier<BlockEntityType<TileEntityMusicBox>> MUSIC_BOX = BLOCK_ENTITIES.register("music_box",
            () -> BlockEntityType.Builder.of(TileEntityMusicBox::new, Blocks.MUSIC_BOX.get()).build(null));
}

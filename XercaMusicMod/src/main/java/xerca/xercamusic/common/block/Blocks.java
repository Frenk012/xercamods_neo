package xerca.xercamusic.common.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import xerca.xercamusic.common.Mod;

public class Blocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Mod.MODID);

    public static final DeferredBlock<BlockMetronome> BLOCK_METRONOME = BLOCKS.register("block_metronome",
            () -> new BlockMetronome(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .ignitedByLava()
                    .instrument(NoteBlockInstrument.BASS)
                    .strength(2.f, 6.f)
                    .sound(SoundType.WOOD)));

    public static final DeferredBlock<BlockMusicBox> MUSIC_BOX = BLOCKS.register("music_box",
            () -> new BlockMusicBox(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .ignitedByLava()
                    .instrument(NoteBlockInstrument.BASS)
                    .strength(2.f, 6.f)
                    .sound(SoundType.WOOD)
                    .isRedstoneConductor((blockState, blockGetter, blockPos) -> false)));

    public static final DeferredBlock<BlockPiano> PIANO = BLOCKS.register("piano", BlockPiano::new);

    public static final DeferredBlock<BlockDrums> DRUM_KIT = BLOCKS.register("drum_kit", BlockDrums::new);
}

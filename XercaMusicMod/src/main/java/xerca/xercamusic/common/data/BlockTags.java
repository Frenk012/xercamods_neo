package xerca.xercamusic.common.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import xerca.xercamusic.common.Mod;
import xerca.xercamusic.common.block.Blocks;

import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;

public class BlockTags extends BlockTagsProvider {
    public BlockTags(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider,
                     @Nullable ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, Mod.MODID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(net.minecraft.tags.BlockTags.MINEABLE_WITH_AXE)
                .add(Blocks.BLOCK_METRONOME.get())
                .add(Blocks.MUSIC_BOX.get())
                .add(Blocks.DRUM_KIT.get())
                .add(Blocks.PIANO.get());
    }
}

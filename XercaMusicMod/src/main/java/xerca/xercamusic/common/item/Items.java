package xerca.xercamusic.common.item;

import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import xerca.xercamusic.common.Mod;
import xerca.xercamusic.common.block.Blocks;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public final class Items {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Mod.MODID);
    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENT_TYPES =
            DeferredRegister.create(BuiltInRegistries.DATA_COMPONENT_TYPE, Mod.MODID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(BuiltInRegistries.RECIPE_SERIALIZER, Mod.MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Mod.MODID);

    // Instruments
    public static final DeferredItem<ItemInstrument> HARP_MC = ITEMS.register("harp_mc",
            () -> new ItemInstrument(-1, 0, 7, new Item.Properties()));
    public static final DeferredItem<ItemInstrument> GUITAR = ITEMS.register("guitar",
            () -> new ItemInstrument(0, 0, 6));
    public static final DeferredItem<ItemInstrument> LYRE = ITEMS.register("lyre",
            () -> new ItemInstrument(1, 1, 5));
    public static final DeferredItem<ItemInstrument> BANJO = ITEMS.register("banjo",
            () -> new ItemInstrument(2, 0, 4));
    public static final DeferredItem<ItemInstrument> DRUM = ITEMS.register("drum",
            () -> new ItemInstrument(3, 1, 4));
    public static final DeferredItem<ItemInstrument> CYMBAL = ITEMS.register("cymbal",
            () -> new ItemInstrument(4, 0, 4));
    public static final DeferredItem<ItemBlockInstrument> DRUM_KIT = ITEMS.register("drum_kit",
            () -> new ItemBlockInstrument(5, 0, 7, Blocks.DRUM_KIT.get()));
    public static final DeferredItem<ItemInstrument> XYLOPHONE = ITEMS.register("xylophone",
            () -> new ItemInstrument(6, 0, 5));
    public static final DeferredItem<ItemInstrument> TUBULAR_BELL = ITEMS.register("tubular_bell",
            () -> new ItemInstrument(7, 1, 4));
    public static final DeferredItem<ItemInstrument> SANSULA = ITEMS.register("sansula",
            () -> new ItemInstrument(8, 1, 5));
    public static final DeferredItem<ItemInstrument> VIOLIN = ITEMS.register("violin",
            () -> new ItemInstrument(9, 1, 5));
    public static final DeferredItem<ItemInstrument> CELLO = ITEMS.register("cello",
            () -> new ItemInstrument(10, 0, 6));
    public static final DeferredItem<ItemInstrument> FLUTE = ITEMS.register("flute",
            () -> new ItemInstrument(11, 1, 6));
    public static final DeferredItem<ItemInstrument> SAXOPHONE = ITEMS.register("saxophone",
            () -> new ItemInstrument(12, 0, 4));
    public static final DeferredItem<ItemInstrument> GOD = ITEMS.register("god",
            () -> new ItemInstrument(13, 0, 5));
    public static final DeferredItem<ItemBlockInstrument> PIANO = ITEMS.register("piano",
            () -> new ItemBlockInstrument(14, 0, 7, Blocks.PIANO.get()));
    public static final DeferredItem<ItemInstrument> OBOE = ITEMS.register("oboe",
            () -> new ItemInstrument(15, 0, 4));
    public static final DeferredItem<ItemInstrument> REDSTONE_GUITAR = ITEMS.register("redstone_guitar",
            () -> new ItemInstrument(16, 0, 5));
    public static final DeferredItem<ItemInstrument> FRENCH_HORN = ITEMS.register("french_horn",
            () -> new ItemInstrument(17, 0, 5));
    public static final DeferredItem<ItemInstrument> BASS_GUITAR = ITEMS.register("bass_guitar",
            () -> new ItemInstrument(18, 1, 4));

    // Other items
    public static final DeferredItem<ItemMusicSheet> MUSIC_SHEET = ITEMS.register("music_sheet", ItemMusicSheet::new);
    public static final DeferredItem<BlockItem> MUSIC_BOX = ITEMS.register("music_box",
            () -> new BlockItem(Blocks.MUSIC_BOX.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> METRONOME = ITEMS.register("metronome",
            () -> new BlockItem(Blocks.BLOCK_METRONOME.get(), new Item.Properties()));

    // Data Components
    public static final Supplier<DataComponentType<Byte>> SHEET_BPS = DATA_COMPONENT_TYPES.register("sheet_bps",
            () -> DataComponentType.<Byte>builder().persistent(Codec.BYTE).build());
    public static final Supplier<DataComponentType<Integer>> SHEET_LENGTH = DATA_COMPONENT_TYPES.register("sheet_length",
            () -> DataComponentType.<Integer>builder().persistent(Codec.INT).build());
    public static final Supplier<DataComponentType<Integer>> SHEET_VERSION = DATA_COMPONENT_TYPES.register("sheet_version",
            () -> DataComponentType.<Integer>builder().persistent(Codec.INT).networkSynchronized(ByteBufCodecs.INT).build());
    public static final Supplier<DataComponentType<Byte>> SHEET_PREV_INSTRUMENT = DATA_COMPONENT_TYPES.register("sheet_prev_instrument",
            () -> DataComponentType.<Byte>builder().persistent(Codec.BYTE).build());
    public static final Supplier<DataComponentType<Boolean>> SHEET_PREV_INSTRUMENT_LOCKED = DATA_COMPONENT_TYPES.register("sheet_prev_instrument_locked",
            () -> DataComponentType.<Boolean>builder().persistent(Codec.BOOL).build());
    public static final Supplier<DataComponentType<Byte>> SHEET_HIGHLIGHT_INTERVAL = DATA_COMPONENT_TYPES.register("sheet_highlight_interval",
            () -> DataComponentType.<Byte>builder().persistent(Codec.BYTE).build());
    public static final Supplier<DataComponentType<Float>> SHEET_VOLUME = DATA_COMPONENT_TYPES.register("sheet_volume",
            () -> DataComponentType.<Float>builder().persistent(Codec.FLOAT).build());
    public static final Supplier<DataComponentType<UUID>> SHEET_ID = DATA_COMPONENT_TYPES.register("sheet_id",
            () -> DataComponentType.<UUID>builder().persistent(UUIDUtil.STRING_CODEC).networkSynchronized(UUIDUtil.STREAM_CODEC).build());
    public static final Supplier<DataComponentType<String>> SHEET_TITLE = DATA_COMPONENT_TYPES.register("sheet_title",
            () -> DataComponentType.<String>builder().persistent(Codec.STRING).build());
    public static final Supplier<DataComponentType<String>> SHEET_AUTHOR = DATA_COMPONENT_TYPES.register("sheet_author",
            () -> DataComponentType.<String>builder().persistent(Codec.STRING).build());
    public static final Supplier<DataComponentType<Integer>> SHEET_GENERATION = DATA_COMPONENT_TYPES.register("sheet_generation",
            () -> DataComponentType.<Integer>builder().persistent(ExtraCodecs.NON_NEGATIVE_INT).build());

    // Recipe Serializers
    public static final Supplier<RecipeSerializer<RecipeNoteCloning>> CRAFTING_SPECIAL_NOTECLONING =
            RECIPE_SERIALIZERS.register("crafting_special_notecloning",
                    () -> new SimpleCraftingRecipeSerializer<>(RecipeNoteCloning::new));

    // Creative Mode Tab
    public static final Supplier<CreativeModeTab> MUSIC_TAB = CREATIVE_MODE_TABS.register("music_tab",
            () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(Items.GUITAR.get()))
                    .displayItems((params, output) -> {
                        output.accept(MUSIC_SHEET.get());
                        output.accept(GUITAR.get());
                        output.accept(LYRE.get());
                        output.accept(BANJO.get());
                        output.accept(DRUM.get());
                        output.accept(CYMBAL.get());
                        output.accept(DRUM_KIT.get());
                        output.accept(XYLOPHONE.get());
                        output.accept(TUBULAR_BELL.get());
                        output.accept(SANSULA.get());
                        output.accept(VIOLIN.get());
                        output.accept(CELLO.get());
                        output.accept(FLUTE.get());
                        output.accept(SAXOPHONE.get());
                        output.accept(GOD.get());
                        output.accept(PIANO.get());
                        output.accept(OBOE.get());
                        output.accept(FRENCH_HORN.get());
                        output.accept(REDSTONE_GUITAR.get());
                        output.accept(BASS_GUITAR.get());
                        output.accept(MUSIC_BOX.get());
                        output.accept(METRONOME.get());
                    })
                    .title(Component.translatable("itemGroup.xercamusic.music_tab"))
                    .build());

    // Instrument list for convenience (populated at runtime)
    public static List<IItemInstrument> getInstruments() {
        return List.of(
                (IItemInstrument) GUITAR.get(), (IItemInstrument) LYRE.get(), (IItemInstrument) BANJO.get(),
                (IItemInstrument) DRUM.get(), (IItemInstrument) CYMBAL.get(), (IItemInstrument) DRUM_KIT.get(),
                (IItemInstrument) XYLOPHONE.get(), (IItemInstrument) TUBULAR_BELL.get(), (IItemInstrument) SANSULA.get(),
                (IItemInstrument) VIOLIN.get(), (IItemInstrument) CELLO.get(), (IItemInstrument) FLUTE.get(),
                (IItemInstrument) SAXOPHONE.get(), (IItemInstrument) GOD.get(), (IItemInstrument) PIANO.get(),
                (IItemInstrument) OBOE.get(), (IItemInstrument) REDSTONE_GUITAR.get(), (IItemInstrument) FRENCH_HORN.get(),
                (IItemInstrument) BASS_GUITAR.get()
        );
    }
}

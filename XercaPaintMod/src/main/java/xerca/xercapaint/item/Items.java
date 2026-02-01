package xerca.xercapaint.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import xerca.xercapaint.CanvasType;
import xerca.xercapaint.Mod;
import xerca.xercapaint.item.crafting.RecipeCanvasCloning;
import xerca.xercapaint.item.crafting.RecipeCraftPalette;
import xerca.xercapaint.item.crafting.RecipeFillPalette;
import xerca.xercapaint.item.crafting.RecipeTaglessShaped;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

public final class Items {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Mod.MODID);
    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENT_TYPES =
            DeferredRegister.create(BuiltInRegistries.DATA_COMPONENT_TYPE, Mod.MODID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(BuiltInRegistries.RECIPE_SERIALIZER, Mod.MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Mod.MODID);

    // Items
    public static final DeferredItem<ItemPalette> ITEM_PALETTE = ITEMS.register("item_palette",
            () -> new ItemPalette("item_palette"));
    public static final DeferredItem<ItemCanvas> ITEM_CANVAS = ITEMS.register("item_canvas",
            () -> new ItemCanvas(CanvasType.SMALL, "item_canvas"));
    public static final DeferredItem<ItemCanvas> ITEM_CANVAS_LARGE = ITEMS.register("item_canvas_large",
            () -> new ItemCanvas(CanvasType.LARGE, "item_canvas_large"));
    public static final DeferredItem<ItemCanvas> ITEM_CANVAS_LONG = ITEMS.register("item_canvas_long",
            () -> new ItemCanvas(CanvasType.LONG, "item_canvas_long"));
    public static final DeferredItem<ItemCanvas> ITEM_CANVAS_TALL = ITEMS.register("item_canvas_tall",
            () -> new ItemCanvas(CanvasType.TALL, "item_canvas_tall"));
    public static final DeferredItem<ItemEasel> ITEM_EASEL = ITEMS.register("item_easel",
            () -> new ItemEasel("item_easel"));

    // Data Components
    public static final Supplier<DataComponentType<List<Integer>>> CANVAS_PIXELS = DATA_COMPONENT_TYPES.register("canvas_pixels",
            () -> DataComponentType.<List<Integer>>builder().persistent(Codec.list(Codec.INT)).networkSynchronized(ByteBufCodecs.fromCodec(Codec.list(Codec.INT))).build());
    public static final Supplier<DataComponentType<Integer>> CANVAS_VERSION = DATA_COMPONENT_TYPES.register("canvas_version",
            () -> DataComponentType.<Integer>builder().persistent(ExtraCodecs.NON_NEGATIVE_INT).networkSynchronized(ByteBufCodecs.VAR_INT).build());
    public static final Supplier<DataComponentType<String>> CANVAS_ID = DATA_COMPONENT_TYPES.register("canvas_id",
            () -> DataComponentType.<String>builder().persistent(ExtraCodecs.NON_EMPTY_STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8).build());
    public static final Supplier<DataComponentType<String>> CANVAS_TITLE = DATA_COMPONENT_TYPES.register("canvas_title",
            () -> DataComponentType.<String>builder().persistent(Codec.STRING).build());
    public static final Supplier<DataComponentType<String>> CANVAS_AUTHOR = DATA_COMPONENT_TYPES.register("canvas_author",
            () -> DataComponentType.<String>builder().persistent(Codec.STRING).build());
    public static final Supplier<DataComponentType<Integer>> CANVAS_GENERATION = DATA_COMPONENT_TYPES.register("canvas_generation",
            () -> DataComponentType.<Integer>builder().persistent(ExtraCodecs.NON_NEGATIVE_INT).build());
    public static final Supplier<DataComponentType<BasicColors>> PALETTE_BASIC_COLORS = DATA_COMPONENT_TYPES.register("palette_basic_colors",
            () -> DataComponentType.<BasicColors>builder().persistent(BasicColors.CODEC).networkSynchronized(BasicColors.STREAM_CODEC).build());
    public static final Supplier<DataComponentType<ItemPalette.ComponentCustomColor>> PALETTE_CUSTOM_COLORS = DATA_COMPONENT_TYPES.register("palette_custom_colors",
            () -> DataComponentType.<ItemPalette.ComponentCustomColor>builder().persistent(ItemPalette.ComponentCustomColor.CODEC).build());

    // Recipe Serializers
    public static final Supplier<RecipeSerializer<RecipeCraftPalette>> CRAFTING_SPECIAL_PALETTE_CRAFTING =
            RECIPE_SERIALIZERS.register("crafting_special_palette_crafting",
                    () -> new SimpleCraftingRecipeSerializer<>(RecipeCraftPalette::new));
    public static final Supplier<RecipeSerializer<RecipeFillPalette>> CRAFTING_SPECIAL_PALETTE_FILLING =
            RECIPE_SERIALIZERS.register("crafting_special_palette_filling",
                    () -> new SimpleCraftingRecipeSerializer<>(RecipeFillPalette::new));
    public static final Supplier<RecipeSerializer<RecipeCanvasCloning>> CRAFTING_SPECIAL_CANVAS_CLONING =
            RECIPE_SERIALIZERS.register("crafting_special_canvas_cloning",
                    () -> new SimpleCraftingRecipeSerializer<>(RecipeCanvasCloning::new));
    public static final Supplier<RecipeSerializer<RecipeTaglessShaped>> CRAFTING_TAGLESS_SHAPED =
            RECIPE_SERIALIZERS.register("crafting_tagless_shaped", RecipeTaglessShaped.TaglessSerializer::new);

    // Creative Mode Tab
    public static final Supplier<CreativeModeTab> PAINT_TAB = CREATIVE_MODE_TABS.register("paint_tab",
            () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(Items.ITEM_PALETTE.get()))
                    .displayItems((params, output) -> {
                        ItemStack fullPalette = new ItemStack(ITEM_PALETTE.get());
                        fullPalette.set(PALETTE_BASIC_COLORS.get(), BasicColors.full());

                        output.accept(ITEM_PALETTE.get());
                        output.accept(fullPalette);
                        output.accept(ITEM_CANVAS.get());
                        output.accept(ITEM_CANVAS_LONG.get());
                        output.accept(ITEM_CANVAS_TALL.get());
                        output.accept(ITEM_CANVAS_LARGE.get());
                        output.accept(ITEM_EASEL.get());
                    })
                    .title(Component.translatable("itemGroup.xercapaint.paint_tab"))
                    .build());

    /**
     * Wrapper record for byte[] that implements equals and hashCode correctly.
     * Required for NeoForge 1.21+ data components.
     */
    public record BasicColors(byte[] colors) {
        public static final int SIZE = 16;

        public static final Codec<BasicColors> CODEC = Codec.BYTE_BUFFER.xmap(
                byteBuffer -> new BasicColors(byteBuffer.array().clone()),
                basicColors -> ByteBuffer.wrap(basicColors.colors.clone())
        );

        public static final StreamCodec<ByteBuf, BasicColors> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public BasicColors decode(ByteBuf buf) {
                byte[] bytes = new byte[SIZE];
                buf.readBytes(bytes);
                return new BasicColors(bytes);
            }

            @Override
            public void encode(ByteBuf buf, BasicColors value) {
                buf.writeBytes(value.colors);
            }
        };

        public BasicColors {
            if (colors.length != SIZE) {
                throw new IllegalArgumentException("BasicColors must have exactly " + SIZE + " elements, got " + colors.length);
            }
        }

        public static BasicColors empty() {
            return new BasicColors(new byte[SIZE]);
        }

        public static BasicColors full() {
            byte[] colors = new byte[SIZE];
            Arrays.fill(colors, (byte) 1);
            return new BasicColors(colors);
        }

        public byte get(int index) {
            return colors[index];
        }

        public BasicColors withSet(int index, byte value) {
            byte[] newColors = colors.clone();
            newColors[index] = value;
            return new BasicColors(newColors);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            BasicColors that = (BasicColors) o;
            return Arrays.equals(colors, that.colors);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(colors);
        }

        @Override
        public String toString() {
            return "BasicColors" + Arrays.toString(colors);
        }
    }
}

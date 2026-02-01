package xerca.xercamusic.common.item;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

@MethodsReturnNonnullByDefault
public class RecipeNoteCloning extends CustomRecipe {
    public RecipeNoteCloning(CraftingBookCategory category) {
        super(category);
    }

    /**
     * Used to check if a recipe matches current crafting inventory
     */
    @Override
    public boolean matches(CraftingInput inv, @NotNull Level worldIn) {
        ItemStack orgNote = ItemStack.EMPTY;
        ItemStack freshNote = ItemStack.EMPTY;

        for (int j = 0; j < inv.size(); ++j) {
            ItemStack item = inv.getItem(j);
            if (!item.isEmpty()) {
                if (item.getItem() == Items.MUSIC_SHEET.get() && item.getOrDefault(Items.SHEET_GENERATION.get(), 0) > 0) {
                    if (!orgNote.isEmpty()) {
                        return false;
                    }

                    orgNote = item;
                } else if (item.getItem() == Items.MUSIC_SHEET.get() && ItemMusicSheet.isEmptySheet(item)) {
                    if (!freshNote.isEmpty()) {
                        return false;
                    }

                    freshNote = item;
                }
            }
        }

        return !orgNote.isEmpty() && !freshNote.isEmpty();
    }

    @Override
    public ItemStack assemble(@NotNull CraftingInput inv, HolderLookup.@NotNull Provider registries) {
        ItemStack orgNote = ItemStack.EMPTY;
        ItemStack freshNote = ItemStack.EMPTY;

        for (int j = 0; j < inv.size(); ++j) {
            ItemStack item = inv.getItem(j);
            if (!item.isEmpty()) {
                if (item.getItem() == Items.MUSIC_SHEET.get() && item.getOrDefault(Items.SHEET_GENERATION.get(), 0) > 0) {
                    if (!orgNote.isEmpty()) {
                        return ItemStack.EMPTY;
                    }

                    orgNote = item;
                } else if (item.getItem() == Items.MUSIC_SHEET.get() && ItemMusicSheet.isEmptySheet(item)) {
                    if (!freshNote.isEmpty()) {
                        return ItemStack.EMPTY;
                    }

                    freshNote = item;
                }
            }
        }

        int gen = orgNote.getOrDefault(Items.SHEET_GENERATION.get(), 0);
        if (!orgNote.isEmpty() && !freshNote.isEmpty() && ItemMusicSheet.isEmptySheet(freshNote) && gen < 3 && gen > 0) {
            ItemStack resultStack = new ItemStack(Items.MUSIC_SHEET.get());
            resultStack.set(Items.SHEET_GENERATION.get(), gen + 1);
            resultStack.set(Items.SHEET_ID.get(), orgNote.get(Items.SHEET_ID.get()));
            resultStack.set(Items.SHEET_VERSION.get(), orgNote.get(Items.SHEET_VERSION.get()));
            resultStack.set(Items.SHEET_LENGTH.get(), orgNote.get(Items.SHEET_LENGTH.get()));
            resultStack.set(Items.SHEET_BPS.get(), orgNote.get(Items.SHEET_BPS.get()));
            resultStack.set(Items.SHEET_PREV_INSTRUMENT.get(), orgNote.get(Items.SHEET_PREV_INSTRUMENT.get()));
            resultStack.set(Items.SHEET_VOLUME.get(), orgNote.get(Items.SHEET_VOLUME.get()));
            resultStack.set(Items.SHEET_AUTHOR.get(), orgNote.get(Items.SHEET_AUTHOR.get()));
            resultStack.set(Items.SHEET_TITLE.get(), orgNote.get(Items.SHEET_TITLE.get()));
            return resultStack;
        } else {
            return ItemStack.EMPTY;
        }
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput inv) {
        NonNullList<ItemStack> itemStacks = NonNullList.withSize(inv.size(), ItemStack.EMPTY);

        for (int i = 0; i < itemStacks.size(); ++i) {
            ItemStack itemStack = inv.getItem(i);
            if (itemStack.getItem() == Items.MUSIC_SHEET.get() && itemStack.getOrDefault(Items.SHEET_GENERATION.get(), 0) > 0) {
                ItemStack copy = itemStack.copy();
                copy.setCount(1);
                itemStacks.set(i, copy);
                break;
            }
        }

        return itemStacks;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return Items.CRAFTING_SPECIAL_NOTECLONING.get();
    }

    /**
     * Used to determine if this recipe can fit in a grid of the given width/height
     */
    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width >= 3 && height >= 3;
    }
}
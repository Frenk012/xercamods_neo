package xerca.xercapaint.item.crafting;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import xerca.xercapaint.item.ItemCanvas;

/**
 * Feature 10: wax a signed painting with a honeycomb to make it permanently un-editable.
 * Signed (generation &gt; 0) canvas + honeycomb -&gt; same canvas marked waxed.
 */
@MethodsReturnNonnullByDefault
public class RecipeWaxCanvas extends CustomRecipe {
    public RecipeWaxCanvas(CraftingBookCategory craftingBookCategory) {
        super(craftingBookCategory);
    }

    private static boolean isWaxableCanvas(ItemStack stack) {
        return stack.getItem() instanceof ItemCanvas
                && stack.getOrDefault(xerca.xercapaint.item.Items.CANVAS_GENERATION.get(), 0) > 0
                && !stack.getOrDefault(xerca.xercapaint.item.Items.CANVAS_WAXED.get(), false);
    }

    @Override
    public boolean matches(CraftingInput inv, @NotNull Level worldIn) {
        ItemStack canvas = ItemStack.EMPTY;
        boolean honeycomb = false;

        for (int j = 0; j < inv.size(); ++j) {
            ItemStack stack = inv.getItem(j);
            if (stack.isEmpty()) {
                continue;
            }
            if (isWaxableCanvas(stack)) {
                if (!canvas.isEmpty()) {
                    return false;
                }
                canvas = stack;
            } else if (stack.is(Items.HONEYCOMB)) {
                if (honeycomb) {
                    return false;
                }
                honeycomb = true;
            } else {
                return false;
            }
        }

        return !canvas.isEmpty() && honeycomb;
    }

    @Override
    public ItemStack assemble(CraftingInput inv, @NotNull HolderLookup.Provider provider) {
        ItemStack canvas = ItemStack.EMPTY;

        for (int j = 0; j < inv.size(); ++j) {
            ItemStack stack = inv.getItem(j);
            if (!stack.isEmpty() && isWaxableCanvas(stack)) {
                canvas = stack;
            }
        }

        if (canvas.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack result = canvas.copy();
        result.setCount(1);
        result.set(xerca.xercapaint.item.Items.CANVAS_WAXED.get(), true);
        return result;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<RecipeWaxCanvas> getSerializer() {
        return xerca.xercapaint.item.Items.CRAFTING_SPECIAL_WAX_CANVAS.get();
    }
}

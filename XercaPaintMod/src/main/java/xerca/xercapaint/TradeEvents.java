package xerca.xercapaint;

import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;
import net.neoforged.neoforge.event.village.WandererTradesEvent;

import java.util.List;

/**
 * Feature 3: villagers and the wandering trader deal in painting supplies.
 */
public final class TradeEvents {
    private TradeEvents() {
    }

    private static VillagerTrades.ItemListing sell(net.minecraft.world.item.Item result, int count, int emeralds,
                                                   int maxUses, int xp) {
        return (trader, random) -> new MerchantOffer(
                new ItemCost(Items.EMERALD, emeralds),
                new ItemStack(result, count),
                maxUses, xp, 0.05F);
    }

    @SubscribeEvent
    public static void onVillagerTrades(VillagerTradesEvent event) {
        if (!Config.villagerTradesEnabled()) {
            return;
        }
        if (event.getType() == VillagerProfession.CARTOGRAPHER) {
            var trades = event.getTrades();
            // Novice: blank canvas for an emerald.
            trades.get(1).add(sell(xerca.xercapaint.item.Items.ITEM_CANVAS.get(), 1, 1, 16, 2));
            // Apprentice: an easel.
            trades.get(2).add(sell(xerca.xercapaint.item.Items.ITEM_EASEL.get(), 1, 3, 12, 5));
            // Journeyman: a larger canvas.
            trades.get(3).add(sell(xerca.xercapaint.item.Items.ITEM_CANVAS_LARGE.get(), 1, 4, 12, 10));
        }
    }

    @SubscribeEvent
    public static void onWandererTrades(WandererTradesEvent event) {
        if (!Config.villagerTradesEnabled()) {
            return;
        }
        List<VillagerTrades.ItemListing> generic = event.getGenericTrades();
        generic.add(sell(xerca.xercapaint.item.Items.ITEM_PALETTE.get(), 1, 2, 8, 1));
    }
}

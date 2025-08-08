package com.samxel.mendingremover;

import com.mojang.logging.LogUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AnvilUpdateEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.village.VillagerTradesEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Mod(Mendingremover.MODID)
public class Mendingremover {

    public static final String MODID = "mendingremover";
    private static final Logger LOGGER = LogUtils.getLogger();

    public Mendingremover() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::commonSetup);
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("MendingRemover mod initialized - Mending disabled");
    }

    @SubscribeEvent
    public void onVillagerTrades(VillagerTradesEvent event) {
        if (event.getType() == VillagerProfession.LIBRARIAN) {
            LOGGER.debug("Processing Librarian trades");
            for (int level = 1; level <= 5; level++) {
                List<VillagerTrades.ItemListing> trades = event.getTrades().get(level);
                if (trades != null) {
                    Iterator<VillagerTrades.ItemListing> iterator = trades.iterator();
                    while (iterator.hasNext()) {
                        VillagerTrades.ItemListing trade = iterator.next();
                        try {
                            var offer = trade.getOffer(null, RandomSource.create());
                            if (offer != null) {
                                ItemStack result = offer.getResult();
                                if (result.getItem() == Items.ENCHANTED_BOOK) {
                                    ListTag enchantments = EnchantedBookItem.getEnchantments(result);
                                    for (int i = 0; i < enchantments.size(); i++) {
                                        CompoundTag tag = enchantments.getCompound(i);
                                        if (tag.getString("id").equals(Enchantments.MENDING.getFullname(0).toString())) {
                                            iterator.remove();
                                            LOGGER.info("Removed Mending book trade from Librarian (level " + level + ")");
                                            break;
                                        }
                                    }
                                }
                            }
                        } catch (Exception ignored) {
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onAnvilUpdate(AnvilUpdateEvent event) {
        ItemStack right = event.getRight();
        if (!right.isEmpty() && right.getItem() == Items.ENCHANTED_BOOK) {
            Map<Enchantment, Integer> enchantments = EnchantmentHelper.getEnchantments(right);
            if (enchantments.containsKey(Enchantments.MENDING)) {
                event.setCanceled(true);
                LOGGER.debug("Blocked anvil operation with Mending book");
            }
        }
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        removeMendingFromInventory(player);
    }

    @SubscribeEvent
    public void onLivingUpdate(LivingEvent.LivingTickEvent event) {
        if (event.getEntity() instanceof Player player && event.getEntity().tickCount % 200 == 0) {
            removeMendingFromInventory(player);
        }
    }

    @SubscribeEvent
    public void onPlayerOpenContainer(PlayerContainerEvent.Open event) {
        Player player = event.getEntity();
        AbstractContainerMenu container = event.getContainer();
        removeMendingFromInventory(player);
        for (int i = 0; i < container.slots.size(); i++) {
            Slot slot = container.slots.get(i);
            ItemStack stack = slot.getItem();
            if (hasMending(stack)) {
                removeMending(stack);
                if (isOnlyMending(stack)) {
                    slot.set(ItemStack.EMPTY);
                }
                LOGGER.debug("Removed Mending from container slot " + i);
            }
        }
    }

    @SubscribeEvent
    public void onItemFished(net.minecraftforge.event.entity.player.ItemFishedEvent event) {
        if (!event.getDrops().isEmpty()) {
            ItemStack fishedItem = event.getDrops().get(0);
            if (hasMending(fishedItem)) {
                removeMending(fishedItem);
                if (isOnlyMending(fishedItem)) {
                    event.getDrops().clear();
                }
                LOGGER.debug("Prevented Mending book from being fished");
            }
        }
    }

    private void removeMendingFromInventory(Player player) {
        Inventory inventory = player.getInventory();
        boolean removed = false;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty() && hasMending(stack)) {
                removeMending(stack);
                if (isOnlyMending(stack)) {
                    inventory.setItem(i, ItemStack.EMPTY);
                }
                removed = true;
            }
        }
        if (removed) {
            LOGGER.debug("Removed Mending from player " + player.getName().getString() + "'s inventory");
        }
    }

    private boolean hasMending(ItemStack stack) {
        if (stack.isEmpty()) return false;
        Map<Enchantment, Integer> enchantments = EnchantmentHelper.getEnchantments(stack);
        return enchantments.containsKey(Enchantments.MENDING);
    }

    private boolean isOnlyMending(ItemStack stack) {
        if (stack.isEmpty()) return false;
        Map<Enchantment, Integer> enchantments = EnchantmentHelper.getEnchantments(stack);
        return enchantments.size() == 1 && enchantments.containsKey(Enchantments.MENDING);
    }

    @SubscribeEvent
    public void onLivingHurt(net.minecraftforge.event.entity.living.LivingHurtEvent event) {
        if (event.getEntity() instanceof Player player) {
            removeMendingFromInventory(player);
        }
    }

    private void removeMending(ItemStack stack) {
        Map<Enchantment, Integer> enchantments = EnchantmentHelper.getEnchantments(stack);
        if (enchantments.remove(Enchantments.MENDING) != null) {

            CompoundTag tag = stack.getTag();
            if (tag != null) {
                if (tag.contains("StoredEnchantments")) {
                    ListTag storedEnchantments = tag.getList("StoredEnchantments", 10);
                    storedEnchantments.removeIf(enchantmentTag -> {
                        CompoundTag compound = (CompoundTag) enchantmentTag;
                        return compound.getString("id").equals("minecraft:mending");
                    });

                    if (storedEnchantments.isEmpty()) {
                        tag.remove("StoredEnchantments");
                    }
                }

                if (tag.contains("Enchantments")) {
                    ListTag normalEnchantments = tag.getList("Enchantments", 10);
                    normalEnchantments.removeIf(enchantmentTag -> {
                        CompoundTag compound = (CompoundTag) enchantmentTag;
                        return compound.getString("id").equals("minecraft:mending");
                    });

                    if (normalEnchantments.isEmpty()) {
                        tag.remove("Enchantments");
                    }
                }

                if (!tag.contains("Enchantments") && !tag.contains("StoredEnchantments")) {
                    tag.remove("RepairCost");
                    tag.remove("ench"); // Legacy enchantment tag
                }

                if (tag.isEmpty()) {
                    stack.setTag(null);
                }
            }

            if (!enchantments.isEmpty()) {
                EnchantmentHelper.setEnchantments(enchantments, stack);
            }
        }
    }

}
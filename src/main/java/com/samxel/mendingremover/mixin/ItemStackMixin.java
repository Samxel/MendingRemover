package com.samxel.mendingremover.mixin;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = ItemStack.class, priority = 1001)
public abstract class ItemStackMixin {

    @Redirect(method = "hasFoil", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/Item;isFoil(Lnet/minecraft/world/item/ItemStack;)Z"))
    private boolean customFoilCheck(Item item, ItemStack stack) {

        boolean originalFoil = item.isFoil(stack);

        if (hasOnlyMendingEnchantments(stack)) {
            return false; // Kein Glint wenn nur Mending
        }

        return originalFoil;
    }

    private boolean hasOnlyMendingEnchantments(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null) return false;

        boolean hasEnchantments = false;
        boolean hasNonMendingEnchants = false;

        if (tag.contains("Enchantments", 9)) {
            ListTag enchants = tag.getList("Enchantments", 10);
            hasEnchantments = !enchants.isEmpty();

            for (int i = 0; i < enchants.size(); i++) {
                CompoundTag enchant = enchants.getCompound(i);
                String enchantId = enchant.getString("id");

                if (!"minecraft:mending".equals(enchantId)) {
                    hasNonMendingEnchants = true;
                    break;
                }
            }
        }

        if (tag.contains("StoredEnchantments", 9)) {
            ListTag storedEnchants = tag.getList("StoredEnchantments", 10);
            hasEnchantments = hasEnchantments || !storedEnchants.isEmpty();

            for (int i = 0; i < storedEnchants.size(); i++) {
                CompoundTag enchant = storedEnchants.getCompound(i);
                String enchantId = enchant.getString("id");

                if (!"minecraft:mending".equals(enchantId)) {
                    hasNonMendingEnchants = true;
                    break;
                }
            }
        }

        return hasEnchantments && !hasNonMendingEnchants;
    }
}
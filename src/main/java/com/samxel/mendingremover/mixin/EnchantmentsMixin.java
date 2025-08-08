package com.samxel.mendingremover.mixin;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;


@Mixin(Enchantments.class)
public class EnchantmentsMixin {

    @Redirect(
            method = "<clinit>",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/enchantment/Enchantments;register(Ljava/lang/String;Lnet/minecraft/world/item/enchantment/Enchantment;)Lnet/minecraft/world/item/enchantment/Enchantment;"
            )
    )
    private static Enchantment cancelMendingRegister(String name, Enchantment enchantment) {
        if ("mending".equals(name)) {
            return null; // Mending wird nicht registriert
        }

        return Registry.register(BuiltInRegistries.ENCHANTMENT, name, enchantment);
    }
}
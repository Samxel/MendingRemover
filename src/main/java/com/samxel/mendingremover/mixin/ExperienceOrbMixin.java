package com.samxel.mendingremover.mixin;

import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ExperienceOrb.class, priority = 1001)
public class ExperienceOrbMixin {

    @Inject(method = "repairPlayerItems", at = @At("HEAD"), cancellable = true)
    private void preventMendingRepair(Player player, int repairAmount, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(repairAmount);
    }
}
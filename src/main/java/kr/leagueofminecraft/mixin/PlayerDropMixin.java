package kr.leagueofminecraft.mixin;

import kr.leagueofminecraft.champion.darius.DariusSkills;
import kr.leagueofminecraft.champion.yone.YoneSkills;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerDropMixin {
    @Inject(method = "drop(Lnet/minecraft/world/item/ItemStack;Z)Lnet/minecraft/world/entity/item/ItemEntity;",
            at = @At("HEAD"), cancellable = true)
    private void darius$preventWeaponDrop(ItemStack stack, boolean randomThrow,
                                          CallbackInfoReturnable<ItemEntity> cir) {
        if (DariusSkills.isDariusWeapon(stack) || YoneSkills.isYoneWeapon(stack)) cir.setReturnValue(null);
    }
}

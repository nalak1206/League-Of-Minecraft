package kr.darius.skills.combat;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Shared server-side damage entry point for champion skills and item effects. */
public final class CombatEngine {
    public enum DamageKind { PHYSICAL, MAGIC }
    public enum KnockbackPolicy { VANILLA, PRESERVE_MOVEMENT }

    private CombatEngine() {}

    public static boolean deal(ServerPlayer attacker, LivingEntity target, float amount,
                               DamageKind kind, KnockbackPolicy knockbackPolicy) {
        DamageSource source = switch (kind) {
            case PHYSICAL -> attacker.damageSources().playerAttack(attacker);
            case MAGIC -> attacker.damageSources().magic();
        };
        return deal(attacker, target, source, amount, knockbackPolicy, true);
    }

    public static boolean deal(ServerPlayer attacker, LivingEntity target, DamageSource source, float amount,
                               KnockbackPolicy knockbackPolicy, boolean resetInvulnerability) {
        if (amount <= 0.0f || !attacker.isAlive() || !target.isAlive() || attacker == target) return false;

        Vec3 movement = target.getDeltaMovement();
        if (resetInvulnerability) target.invulnerableTime = 0;
        boolean damaged = target.hurtServer(attacker.level(), source, amount);
        if (resetInvulnerability) target.invulnerableTime = 0;
        if (knockbackPolicy == KnockbackPolicy.PRESERVE_MOVEMENT) target.setDeltaMovement(movement);
        return damaged;
    }
}

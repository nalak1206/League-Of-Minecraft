package kr.leagueofminecraft.champion.darius;

import com.mojang.math.Transformation;
import kr.leagueofminecraft.mixin.DisplayAccessor;
import kr.leagueofminecraft.mixin.ItemDisplayAccessor;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Darius-only visual effects and display construction. */
final class DariusVfx {
    static final DustParticleOptions BLACK_DUST = new DustParticleOptions(0x08060C, 2.2f);
    static final DustParticleOptions PURPLE_DUST = new DustParticleOptions(0x4A126B, 2.0f);
    static final DustParticleOptions MAGENTA_DUST = new DustParticleOptions(0x96105F, 1.8f);

    private DariusVfx() {}

    static void maceShockwave(ServerLevel level, LivingEntity center, ServerPlayer attacker, boolean knockback) {
        level.sendParticles(BLACK_DUST, center.getX(), center.getY() + 0.15, center.getZ(), 80, 2.1, 0.15, 2.1, 0.18);
        level.sendParticles(PURPLE_DUST, center.getX(), center.getY() + 0.25, center.getZ(), 55, 1.8, 0.20, 1.8, 0.14);
        if (!knockback) return;
        for (LivingEntity nearby : level.getEntitiesOfClass(LivingEntity.class,
                center.getBoundingBox().inflate(3.5), e -> e != center && e != attacker && e.isAlive())) {
            Vec3 push = nearby.position().subtract(center.position()).multiply(1, 0, 1);
            if (push.lengthSqr() < 0.01) push = new Vec3(0.1, 0, 0);
            push = push.normalize().scale(1.15);
            nearby.setDeltaMovement(push.x, 0.38, push.z);
        }
    }

    static void guillotineChargeVfx(ServerLevel level, ServerPlayer player, LivingEntity target) {
        level.sendParticles(BLACK_DUST, target.getX(), target.getY() + 2.5, target.getZ(), 65, 1.8, 2.2, 1.8, 0.10);
        level.sendParticles(PURPLE_DUST, target.getX(), target.getY() + 2.4, target.getZ(), 50, 1.6, 2.0, 1.6, 0.08);
        level.sendParticles(MAGENTA_DUST, player.getX(), player.getY() + 0.8, player.getZ(), 45, 1.0, 1.4, 1.0, 0.09);
    }

    static Display.ItemDisplay createDragonHeadDisplay(ServerLevel level, LivingEntity target, double y) {
        Display.ItemDisplay head = new Display.ItemDisplay(EntityTypes.ITEM_DISPLAY, level);
        ((ItemDisplayAccessor) head).darius$setItemStack(new ItemStack(Items.DRAGON_HEAD));
        ((DisplayAccessor) head).darius$setTransformation(new Transformation(
                new Vector3f(), new Quaternionf(), new Vector3f(5.0f, 5.0f, 5.0f), new Quaternionf()));
        head.setGlowingTag(true);
        head.setPos(target.getX(), y, target.getZ());
        level.addFreshEntity(head);
        return head;
    }

    static void faceDragonHead(Display.ItemDisplay head, Vec3 targetPosition) {
        Vec3 direction = targetPosition.subtract(head.position());
        double horizontal = Math.sqrt(direction.x * direction.x + direction.z * direction.z);
        float yaw = (float) (Math.atan2(direction.x, direction.z) + Math.PI);
        float pitch = (float) Math.atan2(direction.y, horizontal);
        Quaternionf rotation = new Quaternionf().rotateY(yaw).rotateX(pitch);
        ((DisplayAccessor) head).darius$setTransformation(new Transformation(
                new Vector3f(), rotation, new Vector3f(5.0f, 5.0f, 5.0f), new Quaternionf()));
    }

    static void guillotineImpactVfx(ServerLevel level, LivingEntity target) {
        double x = target.getX(), y = target.getY(), z = target.getZ();
        level.sendParticles(BLACK_DUST, x, y + 1.0, z, 140, 2.8, 2.3, 2.8, 0.24);
        level.sendParticles(PURPLE_DUST, x, y + 1.2, z, 105, 2.5, 2.1, 2.5, 0.20);
        level.sendParticles(MAGENTA_DUST, x, y + 1.0, z, 75, 2.2, 2.0, 2.2, 0.16);
        for (int claw = -1; claw <= 1; claw++) for (int i = -18; i <= 18; i++) {
            double t = i / 7.0;
            double px = x + t, py = y + 2.4 - t * 0.55 + claw * 0.48, pz = z + claw * 0.65;
            level.sendParticles(PURPLE_DUST, px, py, pz, 3, 0.08, 0.08, 0.08, 0.025);
            level.sendParticles(MAGENTA_DUST, px, py, pz, 2, 0.06, 0.06, 0.06, 0.018);
        }
        for (int i = 0; i < 18; i++) {
            double py = y + i * 0.32;
            level.sendParticles(BLACK_DUST, x, py, z, 8, 0.55, 0.14, 0.55, 0.08);
            level.sendParticles(PURPLE_DUST, x, py, z, 5, 0.42, 0.11, 0.42, 0.05);
            level.sendParticles(MAGENTA_DUST, x, py, z, 3, 0.30, 0.08, 0.30, 0.03);
        }
    }

    static void clawMarks(ServerLevel level, LivingEntity target, int stacks) {
        for (int claw = 0; claw < Math.min(3, stacks); claw++) for (int i = 0; i < 7; i++) {
            double x = target.getX() - 0.35 + claw * 0.35 + i * 0.035;
            double y = target.getY() + 1.75 - i * 0.12;
            level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, x, y, target.getZ() + 0.45, 1, 0, 0, 0, 0);
        }
    }

    static void wolfSigil(ServerLevel level, LivingEntity target, double scale, boolean burst) {
        double[][] points = {{-.70,.70},{-.45,1.05},{-.20,.72},{.20,.72},{.45,1.05},{.70,.70},
                {-.58,.35},{-.36,.05},{-.22,-.35},{0,-.58},{.22,-.35},{.36,.05},{.58,.35},
                {-.27,.28},{.27,.28},{0,-.08}};
        double baseY = target.getY() + target.getBbHeight() + 1.0;
        for (double[] p : points) level.sendParticles(ParticleTypes.WITCH,
                target.getX() + p[0] * scale, baseY + p[1] * scale, target.getZ(),
                burst ? 3 : 1, 0.03, 0.03, 0.03, 0.01);
    }
}

package kr.darius.skills;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Forces the local Yone client to display the server-selected attack hand. */
public record YoneAttackAnimationPayload(boolean offhand) implements CustomPacketPayload {
    public static final Type<YoneAttackAnimationPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath("darius_skills", "yone_attack_animation"));
    public static final StreamCodec<RegistryFriendlyByteBuf, YoneAttackAnimationPayload> CODEC =
            StreamCodec.composite(ByteBufCodecs.BOOL, YoneAttackAnimationPayload::offhand,
                    YoneAttackAnimationPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

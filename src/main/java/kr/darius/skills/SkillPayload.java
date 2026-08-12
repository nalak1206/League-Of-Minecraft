package kr.darius.skills;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;

public record SkillPayload(int skill) implements CustomPacketPayload {
    public static final Type<SkillPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("darius_skills", "cast"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SkillPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, SkillPayload::skill, SkillPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

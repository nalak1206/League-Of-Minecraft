package kr.leagueofminecraft.network;

import kr.leagueofminecraft.ModConstants;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client request for non-combat League UI actions. */
public record UiActionPayload(int action) implements CustomPacketPayload {
    public static final int OPEN_SHOP = 1;
    public static final int OPEN_INVENTORY = 2;
    public static final int RECALL = 3;

    public static final Type<UiActionPayload> TYPE = new Type<>(ModConstants.id("ui_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf, UiActionPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, UiActionPayload::action, UiActionPayload::new);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}

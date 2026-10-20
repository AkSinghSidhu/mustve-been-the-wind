package com.windsmoke.net;

import com.windsmoke.WindSmoke;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Server to client: the current wind, so particles bend the same way. */
public record WindPayload(float angle, float strength) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<WindPayload> TYPE =
		new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(WindSmoke.MOD_ID, "wind"));

	public static final StreamCodec<RegistryFriendlyByteBuf, WindPayload> CODEC = StreamCodec.composite(
		ByteBufCodecs.FLOAT, WindPayload::angle,
		ByteBufCodecs.FLOAT, WindPayload::strength,
		WindPayload::new
	);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}

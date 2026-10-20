package com.windsmoke.net;

import com.windsmoke.WindSmoke;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Client-to-server request from the in-game wind debug screen. This is never
 * accepted from unprivileged players; see WindSmoke's receiver.
 */
public record WindControlPayload(float angle, float strength, boolean release) implements CustomPacketPayload {
	public static final Type<WindControlPayload> TYPE = new Type<>(
		Identifier.fromNamespaceAndPath(WindSmoke.MOD_ID, "wind_control")
	);
	public static final StreamCodec<RegistryFriendlyByteBuf, WindControlPayload> CODEC = StreamCodec.composite(
		ByteBufCodecs.FLOAT,
		WindControlPayload::angle,
		ByteBufCodecs.FLOAT,
		WindControlPayload::strength,
		ByteBufCodecs.BOOL,
		WindControlPayload::release,
		WindControlPayload::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}

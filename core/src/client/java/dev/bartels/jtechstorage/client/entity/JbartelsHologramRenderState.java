package dev.bartels.jtechstorage.client.entity;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.network.chat.Component;

public class JbartelsHologramRenderState extends ArmedEntityRenderState {
	/** Speech bubble (already wrapped). */
	public final List<Component> bubble = new ArrayList<>();
	/** No player nearby: hologram dimmed, head lowered. */
	public boolean standby;
}

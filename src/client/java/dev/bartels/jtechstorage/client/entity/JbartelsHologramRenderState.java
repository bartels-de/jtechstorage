package dev.bartels.jtechstorage.client.entity;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.network.chat.Component;

public class JbartelsHologramRenderState extends ArmedEntityRenderState {
	/** Sprechblase (bereits umbrochen). */
	public final List<Component> bubble = new ArrayList<>();
	/** Kein Spieler in der Nähe: Hologramm gedimmt, Kopf gesenkt. */
	public boolean standby;
}

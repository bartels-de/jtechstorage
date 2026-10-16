package dev.bartels.jtechstorage.block;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import dev.bartels.jtechstorage.ModAdvancements;
import dev.bartels.jtechstorage.ModContent;
import dev.bartels.jtechstorage.entity.Phoenix;
import dev.bartels.jtechstorage.mail.PhoenixPost;
import dev.bartels.jtechstorage.network.StorageNetwork;

/**
 * Phönix-Horst: Post-Adresse eines Netzwerks. Der Phönix wohnt hier, nimmt Pakete aus dem Lager mit und bringt sie
 * zu anderen Horsten. Ankommende Pakete wandern ins Lager; was nicht passt, bleibt im Posteingang.
 */
public class PhoenixNestBlockEntity extends LinkedPanelBlockEntity {
	public static final int SHIPMENT_SLOTS = 9;
	public static final int INBOX_SLOTS = 9;
	public static final int MAX_NAME_LENGTH = 16;
	/** Dauerauftrag: einmal pro Minecraft-Tag. */
	public static final long DAILY_INTERVAL = 24000L;

	private final SimpleContainer shipment = new SimpleContainer(SHIPMENT_SLOTS) {
		@Override
		public void setChanged() {
			super.setChanged();
			PhoenixNestBlockEntity.this.setChanged();
		}
	};
	private final SimpleContainer inbox = new SimpleContainer(INBOX_SLOTS) {
		@Override
		public void setChanged() {
			super.setChanged();
			PhoenixNestBlockEntity.this.setChanged();
		}
	};

	private String name = "";
	private PhoenixPost.Accept accept = PhoenixPost.Accept.ALL;
	private @Nullable GlobalPos destination;
	private boolean daily;
	private long lastDaily = Long.MIN_VALUE;

	public PhoenixNestBlockEntity(BlockPos pos, BlockState state) {
		super(ModContent.PHOENIX_NEST_BLOCK_ENTITY, pos, state);
	}

	public SimpleContainer getShipment() {
		return shipment;
	}

	public SimpleContainer getInbox() {
		return inbox;
	}

	public String getName() {
		return name;
	}

	public void setName(String value) {
		String cleaned = value.replaceAll("[\\p{Cntrl}§]", "").trim();
		name = cleaned.length() > MAX_NAME_LENGTH ? cleaned.substring(0, MAX_NAME_LENGTH) : cleaned;
		setChanged();
		register();
	}

	public PhoenixPost.Accept getAccept() {
		return accept;
	}

	public void setAccept(PhoenixPost.Accept value) {
		accept = value;
		setChanged();
		register();
	}

	public @Nullable GlobalPos getDestination() {
		return destination;
	}

	public void setDestination(@Nullable GlobalPos pos) {
		destination = pos;
		setChanged();
	}

	public boolean isDaily() {
		return daily;
	}

	public void setDaily(boolean value) {
		daily = value;

		if (value) {
			lastDaily = Long.MIN_VALUE;
		}

		setChanged();
	}

	public @Nullable GlobalPos globalPos() {
		return level == null ? null : GlobalPos.of(level.dimension(), worldPosition);
	}

	/** Name des Teams (aus dem verknüpften Netzwerk). */
	public String teamName() {
		StorageTerminalBlockEntity terminal = terminal();
		return terminal != null ? terminal.teamLabel().getString() : "?";
	}

	/** Anzeigename dieses Horsts. */
	public String label() {
		String team = teamName();
		return name.isEmpty() ? team : team + " · " + name;
	}

	// ------------------------------------------------------------------
	// Adressbuch
	// ------------------------------------------------------------------

	/** Trägt den Horst ins Adressbuch ein bzw. aktualisiert ihn. */
	public void register() {
		if (!(level instanceof ServerLevel serverLevel)) {
			return;
		}

		StorageTerminalBlockEntity terminal = terminal();

		if (terminal == null) {
			return; // nicht verknüpft oder Terminal nicht geladen – alter Eintrag bleibt
		}

		List<UUID> allowed = new ArrayList<>(terminal.getMembers().keySet());

		if (terminal.getOwnerId() != null) {
			allowed.add(terminal.getOwnerId());
		}

		PhoenixPost.get(serverLevel.getServer()).putNest(GlobalPos.of(level.dimension(), worldPosition),
				new PhoenixPost.NestInfo(name, terminal.teamLabel().getString(), terminal.getTeamRgb(), accept.ordinal(), allowed));
	}

	@Override
	public void setLink(BlockPos pos) {
		super.setLink(pos);
		register();
	}

	public static void serverTick(Level level, BlockPos pos, BlockState state, PhoenixNestBlockEntity nest) {
		long time = level.getGameTime();

		if (time % 100 == 13) {
			nest.register();
		}

		if (time % 20 == 5) {
			nest.flushInbox();
		}

		if (nest.daily && time % 40 == 9 && nest.destination != null
				&& (nest.lastDaily == Long.MIN_VALUE || time - nest.lastDaily >= DAILY_INTERVAL)) {
			StorageTerminalBlockEntity terminal = nest.terminal();
			UUID ownerId = terminal != null ? terminal.getOwnerId() : null;
			ServerPlayer owner = ownerId != null && level instanceof ServerLevel serverLevel
					? serverLevel.getServer().getPlayerList().getPlayer(ownerId) : null;

			// Daueraufträge laufen im Namen des Besitzers; er muss dafür nicht online sein
			if (nest.send(owner, ownerId, true) == SendResult.SENT) {
				nest.lastDaily = time;
				nest.setChanged();
			}
		}
	}

	/** Posteingang ins Lager räumen (sobald Platz ist). */
	private void flushInbox() {
		if (inbox.isEmpty()) {
			return;
		}

		StorageTerminalBlockEntity terminal = terminal();

		if (terminal == null) {
			return;
		}

		StorageNetwork network = terminal.buildNetwork();
		boolean changed = false;

		for (int i = 0; i < inbox.getContainerSize(); i++) {
			ItemStack stack = inbox.getItem(i);

			if (!stack.isEmpty()) {
				int before = stack.getCount();
				ItemStack rest = network.insert(stack.copy());
				inbox.setItem(i, rest);
				changed |= rest.getCount() != before;
			}
		}

		if (changed) {
			inbox.setChanged();
		}
	}

	// ------------------------------------------------------------------
	// Senden & Empfangen
	// ------------------------------------------------------------------

	public enum SendResult {
		SENT, NOT_LINKED, NO_DESTINATION, NO_PHOENIX, PHOENIX_AWAY, EMPTY, NOTHING_IN_STORAGE
	}

	/**
	 * Packt die Bestellliste aus dem Lager und schickt den Phönix los.
	 *
	 * @param player   wer verschickt (für Meldungen; bei Daueraufträgen evtl. offline → null)
	 * @param senderId in wessen Namen
	 */
	public SendResult send(@Nullable ServerPlayer player, @Nullable UUID senderId, boolean quiet) {
		if (!(level instanceof ServerLevel serverLevel)) {
			return SendResult.NOT_LINKED;
		}

		StorageTerminalBlockEntity terminal = terminal();

		if (terminal == null || senderId == null) {
			return SendResult.NOT_LINKED;
		}

		PhoenixPost post = PhoenixPost.get(serverLevel.getServer());

		if (destination == null || post.nest(destination) == null) {
			return SendResult.NO_DESTINATION;
		}

		Phoenix phoenix = Phoenix.findHome(serverLevel, worldPosition);

		if (phoenix == null) {
			return SendResult.NO_PHOENIX;
		}

		if (phoenix.isAway() || phoenix.isDeparting()) {
			return SendResult.PHOENIX_AWAY;
		}

		if (shipment.isEmpty()) {
			return SendResult.EMPTY;
		}

		StorageNetwork network = terminal.buildNetwork();
		List<ItemStack> items = new ArrayList<>();

		for (int i = 0; i < shipment.getContainerSize(); i++) {
			ItemStack wanted = shipment.getItem(i);

			if (!wanted.isEmpty()) {
				ItemStack got = network.extract(wanted.copyWithCount(1), wanted.getCount());

				if (!got.isEmpty()) {
					items.add(got);
				}
			}
		}

		if (items.isEmpty()) {
			return SendResult.NOTHING_IN_STORAGE;
		}

		GlobalPos self = GlobalPos.of(level.dimension(), worldPosition);
		String senderName = player != null ? player.getPlainTextName() : terminal.getOwnerName();
		int flight = post.send(serverLevel.getServer(), self, destination, senderId, senderName, label(), items);
		phoenix.depart(flight * 2);
		PhoenixPost.NestInfo target = post.nest(destination);
		String targetLabel = target != null ? target.label() : "?";
		terminal.broadcast(StorageTerminalBlockEntity.Channel.MAIL, Component.translatable("jtechstorage.mail.sent",
				senderName, targetLabel, PhoenixPost.summary(items), flight / 20));

		if (player != null) {
			ModAdvancements.grant(player, "phoenix_mail");

			if (!quiet) {
				player.sendOverlayMessage(Component.translatable("jtechstorage.mail.sent.short", targetLabel, flight / 20));
			}
		}

		return SendResult.SENT;
	}

	/** Ein Paket kommt an: ins Lager, Rest in den Posteingang, notfalls auf den Boden. */
	public void receive(PhoenixPost.Parcel parcel) {
		if (!(level instanceof ServerLevel serverLevel)) {
			return;
		}

		StorageTerminalBlockEntity terminal = terminal();
		StorageNetwork network = terminal != null ? terminal.buildNetwork() : null;

		for (ItemStack stack : parcel.items()) {
			ItemStack rest = network != null ? network.insert(stack.copy()) : stack.copy();

			if (!rest.isEmpty()) {
				rest = inbox.addItem(rest);
			}

			if (!rest.isEmpty()) {
				Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 1.0, worldPosition.getZ() + 0.5, rest);
			}
		}

		inbox.setChanged();
		Phoenix.spawnMessenger(serverLevel, worldPosition);
		level.playSound(null, worldPosition, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.8f, 1.4f);
		serverLevel.sendParticles(ParticleTypes.FLAME, worldPosition.getX() + 0.5, worldPosition.getY() + 0.8,
				worldPosition.getZ() + 0.5, 30, 0.4, 0.3, 0.4, 0.04);

		if (terminal != null) {
			terminal.broadcast(StorageTerminalBlockEntity.Channel.MAIL, parcel.returning()
					? Component.translatable("jtechstorage.mail.returned", PhoenixPost.summary(parcel.items()))
					: Component.translatable("jtechstorage.mail.arrived", parcel.fromLabel(), parcel.senderName(),
							PhoenixPost.summary(parcel.items())));
		}
	}

	/** Beim Abbauen: aus dem Adressbuch austragen, Posteingang fallen lassen. */
	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		super.preRemoveSideEffects(pos, state);

		if (level instanceof ServerLevel serverLevel) {
			PhoenixPost.get(serverLevel.getServer()).removeNest(GlobalPos.of(level.dimension(), pos));
			Containers.dropContents(level, pos, inbox);
		}
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putString("NestName", name);
		output.putString("Accept", accept.name());
		output.putBoolean("Daily", daily);
		output.putLong("LastDaily", lastDaily);

		if (destination != null) {
			output.store("Destination", GlobalPos.CODEC, destination);
		}

		for (int i = 0; i < SHIPMENT_SLOTS; i++) {
			if (!shipment.getItem(i).isEmpty()) {
				output.store("Ship" + i, ItemStack.CODEC, shipment.getItem(i));
			}
		}

		for (int i = 0; i < INBOX_SLOTS; i++) {
			if (!inbox.getItem(i).isEmpty()) {
				output.store("Inbox" + i, ItemStack.CODEC, inbox.getItem(i));
			}
		}
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		name = input.getStringOr("NestName", "");

		try {
			accept = PhoenixPost.Accept.valueOf(input.getStringOr("Accept", "ALL"));
		} catch (IllegalArgumentException e) {
			accept = PhoenixPost.Accept.ALL;
		}

		daily = input.getBooleanOr("Daily", false);
		lastDaily = input.getLongOr("LastDaily", Long.MIN_VALUE);
		destination = input.read("Destination", GlobalPos.CODEC).orElse(null);

		for (int i = 0; i < SHIPMENT_SLOTS; i++) {
			shipment.setItem(i, input.read("Ship" + i, ItemStack.CODEC).orElse(ItemStack.EMPTY));
		}

		for (int i = 0; i < INBOX_SLOTS; i++) {
			inbox.setItem(i, input.read("Inbox" + i, ItemStack.CODEC).orElse(ItemStack.EMPTY));
		}
	}

	/** Für Fehlermeldungen beim Senden. */
	public static Component message(SendResult result) {
		return Component.translatable("jtechstorage.mail.result." + result.name().toLowerCase(java.util.Locale.ROOT))
				.withStyle(result == SendResult.SENT ? ChatFormatting.GREEN : ChatFormatting.RED);
	}
}

package dev.bartels.jtechstorage.companion;

import org.jspecify.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import dev.bartels.jtechstorage.ModAdvancements;
import dev.bartels.jtechstorage.ModSounds;

/**
 * Geodenhüter (v0.25): lebt in einer Amethyst-Geode und schenkt dem ersten Spieler ohne Begleiter einen von drei
 * Begleitern (Mantis, Pilz oder Rabe – zufällig, je Hüter fest). Annehmen oder ablehnen per Klick im Chat. Lehnt man ab,
 * verschwindet er. Sonst verschwindet er nie von selbst (v0.27.3), lässt sich töten und in Booten/Loren transportieren.
 * <p>
 * Händler (v0.27): Wie ein Dorfbewohner tauscht er Amethyst-Splitter gegen Leckerlis, Amethyst-Kerne und Geoden-Schätze.
 * Mit jedem Handel sammelt er Erfahrung und schaltet neue Angebote frei (Stufen 1–5, je Stufe zwei zufällige Angebote),
 * der Vorrat füllt sich alle 10 Minuten wieder auf. Wer einen Begleiter angenommen oder mit ihm gehandelt hat, bei dem
 * bleibt er: Er lässt sich dann dauerhaft in seiner Geode nieder.
 */
public class GeodeKeeper extends PathfinderMob implements net.minecraft.world.item.trading.Merchant {
	public static final String[] SPECIES = {"mantis", "shroom", "raven"};
	private static final EntityDataAccessor<Integer> DATA_SPECIES = SynchedEntityData.defineId(GeodeKeeper.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> DATA_OFFER = SynchedEntityData.defineId(GeodeKeeper.class, EntityDataSerializers.INT);
	private int age;
	private int vanishIn;
	/** Bleibt für immer (hat einen Begleiter verschenkt oder gehandelt). */
	private boolean settled;
	private net.minecraft.world.item.trading.@Nullable MerchantOffers offers;
	private @Nullable Player tradingPlayer;
	private int merchantXp;
	private int merchantLevel = 1;
	private int restockIn = RESTOCK;
	private static final int RESTOCK = 20 * 60 * 10;

	public GeodeKeeper(EntityType<? extends GeodeKeeper> type, Level level) {
		super(type, level);
		setPersistenceRequired();
	}

	public static AttributeSupplier.Builder createAttributes() {
		return createMobAttributes().add(Attributes.MAX_HEALTH, 40.0).add(Attributes.MOVEMENT_SPEED, 0.0).add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_SPECIES, 0);
		builder.define(DATA_OFFER, 0);
	}

	public void setSpecies(int species) {
		entityData.set(DATA_SPECIES, Math.floorMod(species, SPECIES.length));
	}

	public String species() {
		return SPECIES[Math.floorMod(entityData.get(DATA_SPECIES), SPECIES.length)];
	}

	/** Für die Animation: >0, solange er seine Hand anbietend ausstreckt. */
	public int getOfferTicks() {
		return entityData.get(DATA_OFFER);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 10.0f, 1.0f));
		goalSelector.addGoal(2, new RandomLookAroundGoal(this));
	}

	@Override
	public void aiStep() {
		super.aiStep();

		if (!(level() instanceof ServerLevel level)) {
			if (tickCount % 6 == 0) {
				level().addParticle(ParticleTypes.END_ROD, getX() + (random.nextDouble() - 0.5) * 1.2, getY() + 2.2 + random.nextDouble() * 0.4,
						getZ() + (random.nextDouble() - 0.5) * 1.2, 0, 0.01, 0);
			}

			return;
		}

		int offer = getOfferTicks();

		if (offer > 0) {
			entityData.set(DATA_OFFER, offer - 1);
		}

		if (vanishIn > 0) {
			if (--vanishIn == 0) {
				vanish(level);
			}

			return;
		}

		if (tradingPlayer != null && (!tradingPlayer.isAlive() || tradingPlayer.distanceToSqr(this) > 16 * 16
				|| !(tradingPlayer.containerMenu instanceof net.minecraft.world.inventory.MerchantMenu))) {
			setTradingPlayer(null);
		}

		if (offers != null && --restockIn <= 0) {
			restockIn = RESTOCK;

			for (net.minecraft.world.item.trading.MerchantOffer trade : offers) {
				trade.resetUses();
			}
		}

		if (!settled) {
			age++;
		}
	}

	private void vanish(ServerLevel level) {
		level.sendParticles(ParticleTypes.WITCH, getX(), getY() + 1.0, getZ(), 40, 0.4, 0.8, 0.4, 0.05);
		level.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 1.0, getZ(), 20, 0.4, 0.8, 0.4, 0.05);
		playSound(ModSounds.KEEPER_VANISH, 1.0f, 1.0f);
		discard();
	}

	public boolean isLeaving() {
		return vanishIn > 0;
	}

	// ------------------------------------------------------------------
	// Angebot
	// ------------------------------------------------------------------

	@Override
	protected InteractionResult mobInteract(Player player, InteractionHand hand) {
		if (hand != InteractionHand.MAIN_HAND) {
			return InteractionResult.PASS;
		}

		if (player instanceof ServerPlayer serverPlayer && !isLeaving()) {
			// Shift + Rechtsklick oder schon ein Begleiter: handeln, sonst das Begleiter-Angebot
			if (player.isSecondaryUseActive() || CompanionData.get(serverPlayer.level().getServer()).hasCompanion(player.getUUID())) {
				startTrading(serverPlayer);
			} else {
				offer(serverPlayer);
			}
		}

		return InteractionResult.SUCCESS;
	}

	private void say(ServerPlayer player, Component text) {
		player.sendSystemMessage(Component.translatable("jtechstorage.keeper.says", getDisplayName(), text).withStyle(ChatFormatting.LIGHT_PURPLE));
	}

	public void offer(ServerPlayer player) {
		CompanionData data = CompanionData.get(player.level().getServer());
		lookAt(player, 30.0f, 30.0f);
		playSound(ModSounds.KEEPER_GREET, 1.0f, 0.9f + random.nextFloat() * 0.2f);

		if (data.hasCompanion(player.getUUID())) {
			startTrading(player);
			return;
		}

		entityData.set(DATA_OFFER, 60);
		String species = species();
		say(player, Component.translatable("jtechstorage.keeper.offer." + species));
		player.sendSystemMessage(Component.translatable("jtechstorage.companion.about." + species).withStyle(ChatFormatting.GRAY));
		MutableComponent accept = Component.translatable("jtechstorage.keeper.accept").withStyle(style -> style.withColor(ChatFormatting.GREEN)
				.withBold(true).withClickEvent(new ClickEvent.RunCommand("/jtechstorage companion accept"))
				.withHoverEvent(new HoverEvent.ShowText(Component.translatable("jtechstorage.keeper.accept.hover"))));
		MutableComponent decline = Component.translatable("jtechstorage.keeper.decline").withStyle(style -> style.withColor(ChatFormatting.RED)
				.withBold(true).withClickEvent(new ClickEvent.RunCommand("/jtechstorage companion decline"))
				.withHoverEvent(new HoverEvent.ShowText(Component.translatable("jtechstorage.keeper.decline.hover"))));
		player.sendSystemMessage(Component.literal("   ").append(accept).append(Component.literal("    ")).append(decline));
		player.sendSystemMessage(Component.translatable("jtechstorage.keeper.trade_hint").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
	}

	/** Spieler nimmt an: der Begleiter erscheint, der Hüter verabschiedet sich. */
	public void accept(ServerPlayer player) {
		ServerLevel level = (ServerLevel) level();
		CompanionData data = CompanionData.get(level.getServer());

		if (isLeaving() || data.hasCompanion(player.getUUID())) {
			return;
		}

		Companion companion = CompanionContent.create(species(), level);

		if (companion == null) {
			return;
		}

		companion.snapTo(getX() + getLookAngle().x, getY(), getZ() + getLookAngle().z, getYRot(), 0.0f);
		companion.tame(player);
		companion.refreshStage();
		companion.setHealth(companion.getMaxHealth());
		level.addFreshEntity(companion);
		data.set(player.getUUID(), companion.getUUID(), companion.species());
		level.sendParticles(ParticleTypes.END_ROD, companion.getX(), companion.getY() + 0.5, companion.getZ(), 30, 0.4, 0.5, 0.4, 0.05);
		level.sendParticles(ParticleTypes.HAPPY_VILLAGER, companion.getX(), companion.getY() + 0.5, companion.getZ(), 10, 0.4, 0.5, 0.4, 0.0);
		companion.playSound(ModSounds.COMPANION_EVOLVE, 1.0f, 1.4f);
		say(player, Component.translatable("jtechstorage.keeper.farewell_accept", companion.getDisplayName()));
		player.sendSystemMessage(Component.translatable("jtechstorage.companion.howto").withStyle(ChatFormatting.GRAY));
		ModAdvancements.grant(player, "companion");
		say(player, Component.translatable("jtechstorage.keeper.stays"));
		settled = true;
	}

	public void decline(ServerPlayer player) {
		if (isLeaving()) {
			return;
		}

		say(player, Component.translatable("jtechstorage.keeper.farewell_decline"));
		vanishIn = 40;
	}

	// ------------------------------------------------------------------
	// Handel (v0.27) – wie bei Dorfbewohnern
	// ------------------------------------------------------------------

	private record Trade(net.minecraft.world.level.ItemLike cost, int costCount, net.minecraft.world.level.@Nullable ItemLike costB, int costBCount,
			net.minecraft.world.level.ItemLike result, int resultCount, int maxUses, int xp) {
		net.minecraft.world.item.trading.MerchantOffer offer() {
			return new net.minecraft.world.item.trading.MerchantOffer(new net.minecraft.world.item.trading.ItemCost(cost, costCount),
					costB == null ? java.util.Optional.empty() : java.util.Optional.of(new net.minecraft.world.item.trading.ItemCost(costB, costBCount)),
					new net.minecraft.world.item.ItemStack(result, resultCount), maxUses, xp, 0.05f);
		}
	}

	private static final net.minecraft.world.level.ItemLike SHARD = net.minecraft.world.item.Items.AMETHYST_SHARD;

	/** Angebote je Stufe; je Stufe kommen zwei zufällige dazu (Stufe 1: drei). */
	private static java.util.List<java.util.List<Trade>> tiers() {
		return java.util.List.of(
				java.util.List.of(
						new Trade(SHARD, 6, null, 0, CompanionContent.RESONANCE_TREAT, 2, 12, 2),
						new Trade(net.minecraft.world.item.Items.CALCITE, 16, null, 0, SHARD, 3, 16, 2),
						new Trade(net.minecraft.world.item.Items.SMOOTH_BASALT, 16, null, 0, SHARD, 3, 16, 2),
						new Trade(SHARD, 4, null, 0, net.minecraft.world.item.Items.AMETHYST_CLUSTER, 1, 16, 1),
						new Trade(net.minecraft.world.item.Items.EMERALD, 1, null, 0, SHARD, 6, 16, 2)),
				java.util.List.of(
						new Trade(SHARD, 12, null, 0, net.minecraft.world.item.Items.EXPERIENCE_BOTTLE, 3, 12, 5),
						new Trade(SHARD, 8, null, 0, net.minecraft.world.item.Items.TINTED_GLASS, 4, 12, 5),
						new Trade(SHARD, 10, null, 0, net.minecraft.world.item.Items.LARGE_AMETHYST_BUD, 2, 12, 5),
						new Trade(net.minecraft.world.item.Items.GLOW_BERRIES, 12, null, 0, SHARD, 4, 16, 5),
						new Trade(SHARD, 16, null, 0, net.minecraft.world.item.Items.NAME_TAG, 1, 6, 5)),
				java.util.List.of(
						new Trade(SHARD, 24, null, 0, dev.bartels.jtechstorage.ModContent.AMETHYST_DRAUGHT, 1, 6, 10),
						new Trade(SHARD, 20, net.minecraft.world.item.Items.EMERALD, 2, net.minecraft.world.item.Items.SPYGLASS, 1, 4, 10),
						new Trade(SHARD, 16, null, 0, CompanionContent.RESONANCE_TREAT, 8, 8, 10),
						new Trade(net.minecraft.world.item.Items.ECHO_SHARD, 1, null, 0, SHARD, 16, 8, 10)),
				java.util.List.of(
						// v0.36: Amethyst-/Funk-Kern gibt es nur noch vom Boss
						new Trade(SHARD, 32, null, 0, dev.bartels.jtechstorage.depths.DepthsContent.GLOW_CRYSTAL, 4, 6, 20),
						new Trade(SHARD, 24, net.minecraft.world.item.Items.EMERALD, 2, dev.bartels.jtechstorage.depths.DepthsContent.CRYSTAL_LANTERN, 4, 6, 20),
						new Trade(SHARD, 32, net.minecraft.world.item.Items.EMERALD, 8, net.minecraft.world.item.Items.EXPERIENCE_BOTTLE, 16, 4, 20)),
				java.util.List.of(
						new Trade(SHARD, 64, net.minecraft.world.item.Items.DIAMOND, 4, net.minecraft.world.item.Items.BUDDING_AMETHYST, 1, 2, 30),
						new Trade(SHARD, 64, net.minecraft.world.item.Items.EMERALD_BLOCK, 2, net.minecraft.world.item.Items.TOTEM_OF_UNDYING, 1, 1, 30)));
	}

	private static boolean bossOnly(net.minecraft.world.item.ItemStack stack) {
		return stack.is(dev.bartels.jtechstorage.ModContent.AMETHYST_CORE) || stack.is(dev.bartels.jtechstorage.ModContent.WIRELESS_CORE)
				|| stack.getItem() instanceof dev.bartels.jtechstorage.item.UpgradeItem;
	}

	private void addTier(int tier) {
		if (offers == null || tier < 1 || tier > 5) {
			return;
		}

		java.util.List<Trade> pool = new java.util.ArrayList<>(tiers().get(tier - 1));
		java.util.Collections.shuffle(pool, new java.util.Random(random.nextLong()));

		for (int i = 0; i < Math.min(tier == 1 ? 3 : 2, pool.size()); i++) {
			offers.add(pool.get(i).offer());
		}
	}

	@Override
	public net.minecraft.world.item.trading.MerchantOffers getOffers() {
		if (offers == null) {
			offers = new net.minecraft.world.item.trading.MerchantOffers();

			for (int tier = 1; tier <= merchantLevel; tier++) {
				addTier(tier);
			}
		}

		return offers;
	}

	private void startTrading(ServerPlayer player) {
		if (tradingPlayer != null && tradingPlayer != player) {
			say(player, Component.translatable("jtechstorage.keeper.busy"));
			return;
		}

		if (getOffers().isEmpty()) {
			return;
		}

		lookAt(player, 30.0f, 30.0f);
		playSound(ModSounds.KEEPER_GREET, 1.0f, 1.1f);
		setTradingPlayer(player);
		openTradingScreen(player, getDisplayName(), merchantLevel);
	}

	@Override
	public void setTradingPlayer(@Nullable Player player) {
		tradingPlayer = player;
	}

	@Override
	public @Nullable Player getTradingPlayer() {
		return tradingPlayer;
	}

	@Override
	public void overrideOffers(net.minecraft.world.item.trading.MerchantOffers newOffers) {
	}

	@Override
	public void notifyTrade(net.minecraft.world.item.trading.MerchantOffer offer) {
		offer.increaseUses();
		ambientSoundTime = -getAmbientSoundInterval();
		merchantXp += offer.getXp();
		settled = true;       // wer handelt, hat einen Freund gefunden

		if (level() instanceof ServerLevel level) {
			if (offer.shouldRewardExp()) {
				net.minecraft.world.entity.ExperienceOrb.award(level, position().add(0, 0.5, 0), 3 + random.nextInt(4));
			}

			level.sendParticles(ParticleTypes.WAX_ON, getX(), getY() + 1.6, getZ(), 6, 0.3, 0.3, 0.3, 0.0);
		}

		// Stufenaufstieg wie beim Dorfbewohner
		if (net.minecraft.world.entity.npc.villager.VillagerData.canLevelUp(merchantLevel)
				&& merchantXp >= net.minecraft.world.entity.npc.villager.VillagerData.getMaxXpPerLevel(merchantLevel)) {
			merchantLevel++;
			addTier(merchantLevel);

			if (level() instanceof ServerLevel level) {
				level.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 1.2, getZ(), 25, 0.4, 0.6, 0.4, 0.05);
				playSound(ModSounds.COMPANION_LEVEL, 1.0f, 1.2f);
			}

			if (tradingPlayer instanceof ServerPlayer player) {
				// Menü mit den neuen Angeboten neu öffnen
				player.closeContainer();
				setTradingPlayer(player);
				openTradingScreen(player, getDisplayName(), merchantLevel);
			}
		}
	}

	@Override
	public void notifyTradeUpdated(net.minecraft.world.item.ItemStack stack) {
		if (!level().isClientSide() && ambientSoundTime > -getAmbientSoundInterval() + 20) {
			ambientSoundTime = -getAmbientSoundInterval();
			playSound(ModSounds.KEEPER_AMBIENT, 0.6f, stack.isEmpty() ? 0.8f : 1.2f);
		}
	}

	@Override
	public int getVillagerXp() {
		return merchantXp;
	}

	@Override
	public void overrideXp(int xp) {
	}

	@Override
	public boolean showProgressBar() {
		return true;
	}

	@Override
	public SoundEvent getNotifyTradeSound() {
		return ModSounds.KEEPER_GREET;
	}

	@Override
	public boolean isClientSide() {
		return level().isClientSide();
	}

	@Override
	public boolean stillValid(Player player) {
		return tradingPlayer == player && isAlive() && !isLeaving() && player.distanceToSqr(this) < 8 * 8;
	}

	// v0.27.3: verwundbar, verschwindet nie von selbst; „schiebbar“ nur, damit Boote und Loren ihn aufnehmen –
	// Spieler und Mobs schubsen ihn trotzdem nicht weg.

	@Override
	public boolean isPushable() {
		return true;
	}

	@Override
	public void push(net.minecraft.world.entity.Entity entity) {
	}

	@Override
	public boolean removeWhenFarAway(double distance) {
		return false;
	}

	@Override
	protected @Nullable SoundEvent getAmbientSound() {
		return ModSounds.KEEPER_AMBIENT;
	}

	@Override
	public int getAmbientSoundInterval() {
		return 300;
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putInt("Species", entityData.get(DATA_SPECIES));
		output.putInt("KeeperAge", age);
		output.putBoolean("Settled", settled);
		output.putInt("MerchantXp", merchantXp);
		output.putInt("MerchantLevel", merchantLevel);
		output.putInt("Restock", restockIn);

		if (offers != null) {
			output.store("Offers", net.minecraft.world.item.trading.MerchantOffers.CODEC, offers);
		}
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		setSpecies(input.getIntOr("Species", 0));
		age = input.getIntOr("KeeperAge", 0);
		settled = input.getBooleanOr("Settled", false);
		merchantXp = input.getIntOr("MerchantXp", 0);
		merchantLevel = Math.clamp(input.getIntOr("MerchantLevel", 1), 1, 5);
		restockIn = input.getIntOr("Restock", RESTOCK);
		offers = input.read("Offers", net.minecraft.world.item.trading.MerchantOffers.CODEC).orElse(null);

		if (offers != null) {
			// v0.36: Kerne und Upgrades gibt es nicht mehr beim Hüter – alte Angebote aus bestehenden Welten entfernen
			offers.removeIf(offer -> bossOnly(offer.getResult()) || bossOnly(offer.getCostA()) || bossOnly(offer.getCostB()));
		}
	}

	/** Erzeugt einen Hüter mit zufälligem Angebot. */
	public static @Nullable GeodeKeeper spawn(ServerLevel level, double x, double y, double z) {
		GeodeKeeper keeper = CompanionContent.GEODE_KEEPER.create(level, EntitySpawnReason.EVENT);

		if (keeper == null) {
			return null;
		}

		keeper.setSpecies(level.getRandom().nextInt(SPECIES.length));
		keeper.snapTo(x, y, z, level.getRandom().nextFloat() * 360.0f, 0.0f);
		level.addFreshEntity(keeper);
		level.sendParticles(ParticleTypes.WITCH, x, y + 1.0, z, 30, 0.4, 0.8, 0.4, 0.05);
		return keeper;
	}
}

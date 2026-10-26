package dev.bartels.jtechstorage.entity;

import dev.bartels.jtechstorage.api.JtsHooks;
import java.util.List;

import net.minecraft.world.damagesource.DamageSource;
import org.jspecify.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.StructureTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import dev.bartels.jtechstorage.ModAdvancements;
import dev.bartels.jtechstorage.ModSounds;
import dev.bartels.jtechstorage.wolf.WolfContent;

/**
 * Moon wolf (v0.26): the alpha. Appears wild on a hill near a base on full-moon nights and vanishes at
 * dawn – win it with an amethyst bone. Leads the pack (crystal wolves nearby fight
 * stronger), senses structures (right-click with compass), accompanies on request (bone) and in a resonance storm lets out its
 * moon howl: storm creatures nearby freeze briefly, the pack grows stronger.
 */
public class MoonWolf extends CrystalWolf {
	private static final EntityDataAccessor<Boolean> DATA_WILD = SynchedEntityData.defineId(MoonWolf.class, EntityDataSerializers.BOOLEAN);
	private static final int STORM_HOWL_COOLDOWN = 20 * 20;
	private static final int SCENT_COOLDOWN = 20 * 10;
	private static final List<Scent> SCENTS = List.of(
			new Scent(StructureTags.VILLAGE, "village"), new Scent(StructureTags.MINESHAFT, "mineshaft"),
			new Scent(StructureTags.RUINED_PORTAL, "ruined_portal"), new Scent(StructureTags.ON_TREASURE_MAPS, "treasure"),
			new Scent(StructureTags.ON_BURIED_TRIAL_CHAMBERS_MAPS, "trial_chambers"), new Scent(StructureTags.ON_ANCIENT_CITY_MAPS, "ancient_city"));

	private int stormHowlCooldown;
	private int scentCooldown;
	private int scentIndex;
	private boolean stormSeen;

	private record Scent(TagKey<Structure> tag, String key) {
	}

	public MoonWolf(EntityType<? extends MoonWolf> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return createMobAttributes()
				.add(Attributes.MAX_HEALTH, 60.0)
				.add(Attributes.ATTACK_DAMAGE, 8.0)
				.add(Attributes.MOVEMENT_SPEED, 0.34)
				.add(Attributes.ARMOR, 8.0)
				.add(Attributes.KNOCKBACK_RESISTANCE, 0.6)
				.add(Attributes.FOLLOW_RANGE, 32.0);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_WILD, false);
	}

	/** Wild = not yet won (appears at full moon, vanishes at dawn). */
	public boolean isWild() {
		return entityData.get(DATA_WILD);
	}

	public void makeWild() {
		entityData.set(DATA_WILD, true);
	}

	@Override
	protected String unlinkedStatus() {
		return "jtechstorage.moon_wolf.status.unlinked";
	}

	@Override
	protected String linkAdvancement() {
		return "moon_wolf";
	}

	@Override
	protected SoundEvent howlSound() {
		return ModSounds.MOON_WOLF_HOWL;
	}

	/** Howls often at full moon, otherwise rarely. */
	@Override
	protected int howlChance() {
		return level() instanceof ServerLevel server && server.getMoonBrightness(blockPosition()) >= 0.99f ? 600 : 4000;
	}

	@Override
	protected boolean canRest() {
		return !isWild() && super.canRest();
	}

	@Override
	protected Item packedItem() {
		return WolfContent.MOON_WOLF_ITEM;
	}

	@Override
	public boolean shouldShowName() {
		return !isWild();
	}

	@Override
	public void aiStep() {
		super.aiStep();

		if (!(level() instanceof ServerLevel level) || isHologram()) {
			if (level().isClientSide() && !isHologram() && random.nextInt(4) == 0) {
				level().addParticle(ParticleTypes.END_ROD, getRandomX(0.9), getY() + 1.2 + random.nextDouble() * 0.6, getRandomZ(0.9), 0, 0.02, 0);
			}
			return;
		}

		if (scentCooldown > 0) {
			scentCooldown--;
		}

		if (stormHowlCooldown > 0) {
			stormHowlCooldown--;
		}

		if (isWild()) {
			wildTick(level);
			return;
		}

		boolean storm = tickCount % 20 == 7 && JtsHooks.stormActive.test(level, blockPosition());

		if (storm && !stormSeen) {
			stormSeen = true;
			startHowl();   // the moon wolf senses the storm first
		} else if (tickCount % 20 == 7 && !storm) {
			stormSeen = false;
		}

		if (storm && stormHowlCooldown == 0 && getTarget() != null) {
			moonHowl(level);
		}
	}

	/** Wild: sits on its hill, howls at the moon and vanishes at dawn. */
	private void wildTick(ServerLevel level) {
		getNavigation().stop();
		setTarget(null);

		if (getHowlTicks() == 0 && random.nextInt(160) == 0) {
			startHowl();
		}

		long time = Math.floorMod(level.getDefaultClockTime(), 24000L);

		if (time > 23000L || time < 12500L) {
			level.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 1.0, getZ(), 40, 0.6, 0.8, 0.6, 0.05);
			level.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 0.6, getZ(), 15, 0.5, 0.4, 0.5, 0.02);
			discard();
		}
	}

	/** Moon howl: storm creatures nearby freeze briefly, wolves grow stronger. */
	private void moonHowl(ServerLevel level) {
		stormHowlCooldown = STORM_HOWL_COOLDOWN;
		startHowl();
		level.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 1.6, getZ(), 60, 4.0, 1.0, 4.0, 0.08);

		for (LivingEntity enemy : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(12),
				e -> e instanceof Enemy && e.isAlive() && !(e instanceof NetworkHelperMob))) {
			enemy.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 4), this);
			enemy.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0), this);
		}

		for (CrystalWolf wolf : level.getEntitiesOfClass(CrystalWolf.class, getBoundingBox().inflate(16), w -> !w.isHologram())) {
			wolf.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 200, 0), this);
			wolf.addEffect(new MobEffectInstance(MobEffects.SPEED, 200, 0), this);
		}
	}

	// ------------------------------------------------------------------
	// Interaction
	// ------------------------------------------------------------------

	@Override
	protected InteractionResult mobInteract(Player player, InteractionHand hand) {
		if (isWild()) {
			ItemStack held = player.getItemInHand(hand);

			if (level().isClientSide()) {
				return held.is(WolfContent.AMETHYST_BONE) ? InteractionResult.SUCCESS : InteractionResult.PASS;
			}

			if (held.is(WolfContent.AMETHYST_BONE)) {
				held.consume(1, player);
				entityData.set(DATA_WILD, false);
				setOwner(player);
				home = blockPosition();
				setPersistenceRequired();
				startHowl();
				ServerLevel level = (ServerLevel) level();
				level.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 1.0, getZ(), 50, 0.6, 0.8, 0.6, 0.1);
				level.sendParticles(ParticleTypes.HEART, getX(), getY() + 1.6, getZ(), 6, 0.5, 0.3, 0.5, 0.0);
				player.sendSystemMessage(Component.translatable("jtechstorage.moon_wolf.tamed").withStyle(ChatFormatting.AQUA));

				if (player instanceof ServerPlayer serverPlayer) {
					ModAdvancements.grant(serverPlayer, "moon_wolf");
				}

				return InteractionResult.SUCCESS;
			}

			player.sendOverlayMessage(Component.translatable("jtechstorage.moon_wolf.wild"));
			return InteractionResult.SUCCESS;
		}

		return super.mobInteract(player, hand);
	}

	@Override
	protected InteractionResult interactWith(Player player, InteractionHand hand) {
		ItemStack held = player.getItemInHand(hand);

		if (held.is(Items.COMPASS) && level() instanceof ServerLevel level) {
			scent(level, player);
			return InteractionResult.SUCCESS;
		}

		return super.interactWith(player, hand);
	}

	/** Scent: next structure in turn (village, mineshaft, ruined portal, treasure, trial chamber, ancient city). */
	private void scent(ServerLevel level, Player player) {
		if (scentCooldown > 0) {
			player.sendOverlayMessage(Component.translatable("jtechstorage.moon_wolf.scent.cooldown"));
			return;
		}

		scentCooldown = SCENT_COOLDOWN;
		Scent scent = SCENTS.get(scentIndex % SCENTS.size());
		scentIndex++;
		BlockPos found = level.findNearestMapStructure(scent.tag(), blockPosition(), 48, false);
		Component what = Component.translatable("jtechstorage.moon_wolf.scent." + scent.key());

		if (found == null) {
			player.sendSystemMessage(Component.translatable("jtechstorage.moon_wolf.scent.none", what).withStyle(ChatFormatting.GRAY));
			return;
		}

		Vec3 dir = Vec3.atCenterOf(found).subtract(position());
		int distance = (int) Math.sqrt(dir.x * dir.x + dir.z * dir.z);
		String compass = direction(dir);
		startHowl();
		player.sendSystemMessage(Component.translatable("jtechstorage.moon_wolf.scent.found", getDisplayName(), what, distance,
				Component.translatable("jtechstorage.direction." + compass)).withStyle(ChatFormatting.AQUA));

		// Glowing trail in that direction
		Vec3 step = new Vec3(dir.x, 0, dir.z).normalize();

		for (int i = 1; i <= 12; i++) {
			Vec3 p = position().add(step.scale(i * 0.8)).add(0, 1.0, 0);
			level.sendParticles(ParticleTypes.END_ROD, p.x, p.y, p.z, 2, 0.05, 0.05, 0.05, 0.0);
		}
	}

	private static String direction(Vec3 dir) {
		double angle = Math.toDegrees(Math.atan2(dir.x, -dir.z));   // 0 = north, clockwise
		String[] names = {"north", "northeast", "east", "southeast", "south", "southwest", "west", "northwest"};
		return names[(int) Math.floorMod(Math.round(angle / 45.0), 8L)];
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		return !isWild() && super.hurtServer(level, source, amount);
	}

	@Override
	public boolean removeWhenFarAway(double distance) {
		return false;
	}

	@Override
	protected @Nullable SoundEvent helperAmbientSound() {
		return isWild() ? null : ModSounds.MOON_WOLF_AMBIENT;
	}

	@Override
	protected @Nullable SoundEvent helperSleepSound() {
		return ModSounds.MOON_WOLF_SLEEP;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return ModSounds.MOON_WOLF_HURT;
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putBoolean("Wild", isWild());
		output.putInt("ScentIndex", scentIndex);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		entityData.set(DATA_WILD, input.getBooleanOr("Wild", false));
		scentIndex = input.getIntOr("ScentIndex", 0);
	}
}

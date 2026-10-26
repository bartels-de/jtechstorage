package dev.bartels.jtechstorage.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

import dev.bartels.jtechstorage.end.EndContent;

/** Crystal web of the Great Amethyst Spider: slows heavily and briefly spins a web at the impact point. */
public class WebShot extends ThrowableItemProjectile {
	public WebShot(EntityType<? extends WebShot> type, Level level) {
		super(type, level);
	}

	public WebShot(Level level, LivingEntity thrower) {
		super(EndContent.WEB_SHOT, thrower, level, new ItemStack(Items.COBWEB));
	}

	@Override
	protected Item getDefaultItem() {
		return Items.COBWEB;
	}

	@Override
	protected double getDefaultGravity() {
		return 0.02;
	}

	@Override
	public void tick() {
		super.tick();

		if (level().isClientSide() && tickCount % 2 == 0) {
			level().addParticle(ParticleTypes.END_ROD, getX(), getY(), getZ(), 0.0, 0.0, 0.0);
		}
	}

	@Override
	protected void onHitEntity(EntityHitResult result) {
		super.onHitEntity(result);
		Entity target = result.getEntity();

		if (target instanceof AmethystSpider || !(level() instanceof ServerLevel level)) {
			return;
		}

		Entity owner = getOwner();
		target.hurtServer(level, damageSources().thrown(this, owner), 6.0f);

		if (target instanceof LivingEntity living) {
			living.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 80, 3));
			living.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, 80, 1));
		}

		web(level, target.blockPosition());
	}

	@Override
	protected void onHitBlock(BlockHitResult result) {
		super.onHitBlock(result);

		if (level() instanceof ServerLevel level) {
			web(level, result.getBlockPos().relative(result.getDirection()));
		}
	}

	private void web(ServerLevel level, BlockPos pos) {
		if (getOwner() instanceof AmethystSpider spider) {
			spider.placeTemporaryWeb(level, pos);
		}
	}

	@Override
	protected void onHit(HitResult result) {
		super.onHit(result);

		if (level() instanceof ServerLevel level) {
			level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, Items.COBWEB), getX(), getY(), getZ(),
					14, 0.3, 0.3, 0.3, 0.08);
			level.playSound(null, getX(), getY(), getZ(), SoundEvents.WOOL_BREAK, SoundSource.HOSTILE, 1.0f, 0.6f);
			discard();
		}
	}
}

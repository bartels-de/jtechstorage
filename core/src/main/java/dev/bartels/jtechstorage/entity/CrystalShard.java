package dev.bartels.jtechstorage.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

import dev.bartels.jtechstorage.ModContent;

/** Crystal throw by Peter Chel Parker against flying monsters. Only hits monsters. */
public class CrystalShard extends ThrowableItemProjectile {
	public static final float DAMAGE = 10.0f;
	private float damage = DAMAGE;

	public CrystalShard(EntityType<? extends CrystalShard> type, Level level) {
		super(type, level);
	}

	public CrystalShard(Level level, LivingEntity thrower) {
		super(ModContent.CRYSTAL_SHARD, thrower, level, new ItemStack(Items.AMETHYST_SHARD));
	}

	/** Damage per hit (shard rain from Cheldrogan: less than Peter's throw). */
	public CrystalShard withDamage(float damage) {
		this.damage = damage;
		return this;
	}

	@Override
	protected Item getDefaultItem() {
		return Items.AMETHYST_SHARD;
	}

	@Override
	protected double getDefaultGravity() {
		return 0.008;
	}

	@Override
	protected void onHitEntity(EntityHitResult result) {
		super.onHitEntity(result);
		Entity target = result.getEntity();

		if (!(level() instanceof ServerLevel level)) {
			return;
		}

		Entity owner = getOwner();

		if (owner instanceof Enemy) {
			// spat by monsters (Vein Crawler, v0.31): hits everything except monsters, burning shards ignite things
			if (!(target instanceof Enemy) && target.hurtServer(level, damageSources().thrown(this, owner), damage) && isOnFire()) {
				target.igniteForSeconds(3.0f);
			}
		} else if (target instanceof Enemy) {
			target.hurtServer(level, damageSources().thrown(this, owner), damage);
		}
	}

	@Override
	protected void onHit(HitResult result) {
		super.onHit(result);

		if (level() instanceof ServerLevel level) {
			level.sendParticles(ParticleTypes.WITCH, getX(), getY(), getZ(), 12, 0.2, 0.2, 0.2, 0.05);
			level.sendParticles(ParticleTypes.END_ROD, getX(), getY(), getZ(), 6, 0.1, 0.1, 0.1, 0.05);
			level.playSound(null, getX(), getY(), getZ(), SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.NEUTRAL, 1.0f, 1.4f);
			discard();
		}
	}
}

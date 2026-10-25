package dev.bartels.jtechstorage;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

/** Eigene Sounds der Mod (synthetisiert, Dateien unter assets/jtechstorage/sounds, Liste in sounds.json). */
public final class ModSounds {
	public static final SoundEvent SPRITE_AMBIENT = register("sprite.ambient");
	public static final SoundEvent SPRITE_WORK = register("sprite.work");
	public static final SoundEvent SPRITE_SLEEP = register("sprite.sleep");
	public static final SoundEvent MOTH_SLEEP = register("moth.sleep");
	public static final SoundEvent BEETLE_SLEEP = register("beetle.sleep");
	public static final SoundEvent BEETLE_STEP = register("beetle.step");
	public static final SoundEvent ASHLEY_SLEEP = register("ashley.sleep");
	public static final SoundEvent ASHLEY_STEP = register("ashley.step");
	public static final SoundEvent GRIFFIN_FLAP = register("griffin.flap");
	public static final SoundEvent GRIFFIN_STEP = register("griffin.step");
	public static final SoundEvent GRIFFIN_SLEEP = register("griffin.sleep");
	public static final SoundEvent CHEL_SLEEP = register("chel.sleep");
	public static final SoundEvent FOX_SLEEP = register("fox.sleep");
	public static final SoundEvent PHOENIX_FLAP = register("phoenix.flap");
	public static final SoundEvent PHOENIX_SLEEP = register("phoenix.sleep");
	public static final SoundEvent MOTH_AMBIENT = register("moth.ambient");
	public static final SoundEvent MOTH_ALARM = register("moth.alarm");
	public static final SoundEvent GRIFFIN_AMBIENT = register("griffin.ambient");
	public static final SoundEvent GRIFFIN_UNLOAD = register("griffin.unload");
	public static final SoundEvent BEETLE_AMBIENT = register("beetle.ambient");
	public static final SoundEvent BEETLE_CHARGE = register("beetle.charge");
	public static final SoundEvent CHEL_AMBIENT = register("chel.ambient");
	public static final SoundEvent CHEL_CRAFT = register("chel.craft");
	public static final SoundEvent CHEL_LAUGH = register("chel.laugh");
	public static final SoundEvent PETER_AMBIENT = register("peter.ambient");
	public static final SoundEvent PETER_ATTACK = register("peter.attack");
	public static final SoundEvent PETER_TRANSFORM = register("peter.transform");
	public static final SoundEvent PETER_STEP = register("peter.step");
	public static final SoundEvent PETER_DOZE = register("peter.doze");
	public static final SoundEvent ASHLEY_AMBIENT = register("ashley.ambient");
	public static final SoundEvent ASHLEY_SNEEZE = register("ashley.sneeze");
	public static final SoundEvent ASHLEY_KICKED = register("ashley.kicked");
	public static final SoundEvent PHOENIX_AMBIENT = register("phoenix.ambient");
	public static final SoundEvent PHOENIX_DEPART = register("phoenix.depart");
	public static final SoundEvent AMETHYST_PHOENIX_AMBIENT = register("amethyst_phoenix.ambient");
	public static final SoundEvent AMETHYST_PHOENIX_DEPART = register("amethyst_phoenix.depart");
	public static final SoundEvent AMETHYST_PHOENIX_EVOLVE = register("amethyst_phoenix.evolve");
	public static final SoundEvent AMETHYST_PHOENIX_FLAP = register("amethyst_phoenix.flap");
	public static final SoundEvent AMETHYST_PHOENIX_SLEEP = register("amethyst_phoenix.sleep");
	public static final SoundEvent FOX_AMBIENT = register("fox.ambient");
	public static final SoundEvent FOX_DIG = register("fox.dig");
	public static final SoundEvent FOX_FOUND = register("fox.found");
	public static final SoundEvent ROBOT_AMBIENT = register("robot.ambient");
	public static final SoundEvent ROBOT_HURT = register("robot.hurt");
	public static final SoundEvent ROBOT_DEATH = register("robot.death");
	public static final SoundEvent ROBOT_STEP = register("robot.step");
	public static final SoundEvent ROBOT_AWAKEN = register("robot.awaken");
	public static final SoundEvent ROBOT_IDLE = register("robot.idle");
	public static final SoundEvent ROBOT_LASER_CHARGE = register("robot.laser_charge");
	public static final SoundEvent ROBOT_LASER = register("robot.laser");
	public static final SoundEvent ROBOT_SLAM = register("robot.slam");
	public static final SoundEvent SPIDER_AMBIENT = register("spider.ambient");
	public static final SoundEvent SPIDER_HURT = register("spider.hurt");
	public static final SoundEvent SPIDER_DEATH = register("spider.death");
	public static final SoundEvent SPIDER_STEP = register("spider.step");
	public static final SoundEvent SPIDER_SHRIEK = register("spider.shriek");
	public static final SoundEvent SPIDER_WEB = register("spider.web");
	public static final SoundEvent SPIDER_AWAKEN = register("spider.awaken");
	public static final SoundEvent SPIDER_LURK = register("spider.lurk");
	public static final SoundEvent STORM_START = register("storm.start");
	public static final SoundEvent STORM_WAVE = register("storm.wave");
	public static final SoundEvent STORM_END = register("storm.end");
	public static final SoundEvent SHARDLING_AMBIENT = register("shardling.ambient");
	public static final SoundEvent SHARDLING_HURT = register("shardling.hurt");
	public static final SoundEvent SHARDLING_DEATH = register("shardling.death");
	public static final SoundEvent SHARDLING_STEP = register("shardling.step");
	public static final SoundEvent SHARDLING_ATTACK = register("shardling.attack");
	public static final SoundEvent HERALD_AMBIENT = register("herald.ambient");
	public static final SoundEvent HERALD_HURT = register("herald.hurt");
	public static final SoundEvent HERALD_DEATH = register("herald.death");
	public static final SoundEvent HERALD_CHARGE = register("herald.charge");
	public static final SoundEvent HERALD_BOLT = register("herald.bolt");
	public static final SoundEvent HERALD_SUMMON = register("herald.summon");
	public static final SoundEvent CRYSTAL_WOLF_AMBIENT = register("crystal_wolf.ambient");
	public static final SoundEvent CRYSTAL_WOLF_GROWL = register("crystal_wolf.growl");
	public static final SoundEvent CRYSTAL_WOLF_HOWL = register("crystal_wolf.howl");
	public static final SoundEvent CRYSTAL_WOLF_HURT = register("crystal_wolf.hurt");
	public static final SoundEvent CRYSTAL_WOLF_STEP = register("crystal_wolf.step");
	public static final SoundEvent CRYSTAL_WOLF_SLEEP = register("crystal_wolf.sleep");
	public static final SoundEvent MOON_WOLF_AMBIENT = register("moon_wolf.ambient");
	public static final SoundEvent MOON_WOLF_HOWL = register("moon_wolf.howl");
	public static final SoundEvent MOON_WOLF_HURT = register("moon_wolf.hurt");
	public static final SoundEvent MOON_WOLF_SLEEP = register("moon_wolf.sleep");
	public static final SoundEvent CHELDROGAN_AMBIENT = register("cheldrogan.ambient");
	public static final SoundEvent CHELDROGAN_HURT = register("cheldrogan.hurt");
	public static final SoundEvent CHELDROGAN_CLAW = register("cheldrogan.claw");
	public static final SoundEvent CHELDROGAN_BREATH = register("cheldrogan.breath");
	public static final SoundEvent CHELDROGAN_ROAR = register("cheldrogan.roar");
	public static final SoundEvent CHELDROGAN_FLAP = register("cheldrogan.flap");
	public static final SoundEvent CHELDROGAN_TRANSFORM = register("cheldrogan.transform");
	public static final SoundEvent CHELDROGAN_STEP = register("cheldrogan.step");
	public static final SoundEvent CHELDROGAN_SLEEP = register("cheldrogan.sleep");
	public static final SoundEvent CHELDROGAN_RAIN = register("cheldrogan.rain");
	public static final SoundEvent CHELDROGAN_CRYSTAL = register("cheldrogan.crystal");
	public static final SoundEvent COMPANION_LEVEL = register("companion.level");
	public static final SoundEvent COMPANION_EVOLVE = register("companion.evolve");
	public static final SoundEvent COMPANION_COMMAND = register("companion.command");
	public static final SoundEvent MANTIS_AMBIENT = register("mantis.ambient");
	public static final SoundEvent MANTIS_HURT = register("mantis.hurt");
	public static final SoundEvent MANTIS_ATTACK = register("mantis.attack");
	public static final SoundEvent SHROOM_AMBIENT = register("shroom.ambient");
	public static final SoundEvent SHROOM_HURT = register("shroom.hurt");
	public static final SoundEvent SHROOM_HEAL = register("shroom.heal");
	public static final SoundEvent RAVEN_AMBIENT = register("raven.ambient");
	public static final SoundEvent RAVEN_HURT = register("raven.hurt");
	public static final SoundEvent RAVEN_PICKUP = register("raven.pickup");
	public static final SoundEvent RAVEN_DEPART = register("raven.depart");
	public static final SoundEvent RAVEN_RETURN = register("raven.return");
	public static final SoundEvent RAVEN_ATTACK = register("raven.attack");
	public static final SoundEvent SHROOM_ATTACK = register("shroom.attack");
	public static final SoundEvent KEEPER_AMBIENT = register("keeper.ambient");
	public static final SoundEvent KEEPER_GREET = register("keeper.greet");
	public static final SoundEvent KEEPER_VANISH = register("keeper.vanish");
	public static final SoundEvent JBARTELS_APPEAR = register("jbartels.appear");
	public static final SoundEvent JBARTELS_TALK = register("jbartels.talk");
	public static final SoundEvent JBARTELS_VANISH = register("jbartels.vanish");
	// Kristalltiefe (v0.33): Umgebungsklänge und Quokka
	public static final SoundEvent DEPTHS_LOOP = register("ambient.depths.loop");
	public static final SoundEvent DEPTHS_LOOP_WATER = register("ambient.depths.water");
	public static final SoundEvent DEPTHS_LOOP_RUMBLE = register("ambient.depths.rumble");
	public static final SoundEvent DEPTHS_LOOP_WIND = register("ambient.depths.wind");
	public static final SoundEvent DEPTHS_LOOP_GROTTO = register("ambient.depths.grotto");
	public static final SoundEvent DEPTHS_CHIME = register("ambient.depths.chime");
	public static final SoundEvent DEPTHS_DRIP = register("ambient.depths.drip");
	public static final SoundEvent DEPTHS_EMBER = register("ambient.depths.ember");
	public static final SoundEvent DEPTHS_MOOD = register("ambient.depths.mood");
	public static final SoundEvent QUOKKA_AMBIENT = register("quokka.ambient");
	public static final SoundEvent QUOKKA_HURT = register("quokka.hurt");
	public static final SoundEvent QUOKKA_DEATH = register("quokka.death");
	public static final SoundEvent QUOKKA_EAT = register("quokka.eat");
	public static final SoundEvent QUOKKA_CURE = register("quokka.cure");

	private ModSounds() {
	}

	private static SoundEvent register(String name) {
		Identifier id = JTechStorage.id(name);
		return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
	}

	/** Lädt die Klasse (Registrierung über die statischen Felder). */
	public static void init() {
	}
}

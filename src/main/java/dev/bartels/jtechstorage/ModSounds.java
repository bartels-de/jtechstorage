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
	public static final SoundEvent ASHLEY_AMBIENT = register("ashley.ambient");
	public static final SoundEvent ASHLEY_SNEEZE = register("ashley.sneeze");
	public static final SoundEvent ASHLEY_KICKED = register("ashley.kicked");
	public static final SoundEvent PHOENIX_AMBIENT = register("phoenix.ambient");
	public static final SoundEvent PHOENIX_DEPART = register("phoenix.depart");
	public static final SoundEvent AMETHYST_PHOENIX_AMBIENT = register("amethyst_phoenix.ambient");
	public static final SoundEvent AMETHYST_PHOENIX_DEPART = register("amethyst_phoenix.depart");
	public static final SoundEvent AMETHYST_PHOENIX_EVOLVE = register("amethyst_phoenix.evolve");
	public static final SoundEvent FOX_AMBIENT = register("fox.ambient");
	public static final SoundEvent FOX_DIG = register("fox.dig");
	public static final SoundEvent FOX_FOUND = register("fox.found");
	public static final SoundEvent ROBOT_AMBIENT = register("robot.ambient");
	public static final SoundEvent ROBOT_HURT = register("robot.hurt");
	public static final SoundEvent ROBOT_DEATH = register("robot.death");
	public static final SoundEvent ROBOT_STEP = register("robot.step");
	public static final SoundEvent ROBOT_AWAKEN = register("robot.awaken");
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
	public static final SoundEvent CHELDROGAN_AMBIENT = register("cheldrogan.ambient");
	public static final SoundEvent CHELDROGAN_HURT = register("cheldrogan.hurt");
	public static final SoundEvent CHELDROGAN_CLAW = register("cheldrogan.claw");
	public static final SoundEvent CHELDROGAN_BREATH = register("cheldrogan.breath");
	public static final SoundEvent CHELDROGAN_ROAR = register("cheldrogan.roar");
	public static final SoundEvent CHELDROGAN_FLAP = register("cheldrogan.flap");
	public static final SoundEvent CHELDROGAN_TRANSFORM = register("cheldrogan.transform");

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

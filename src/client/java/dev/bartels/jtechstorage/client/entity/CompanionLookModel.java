package dev.bartels.jtechstorage.client.entity;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;

/**
 * Begleiter-Modell mit Anpassung (v0.25.4): enthält alle Körperteil-Varianten und die Ausrüstung. Die Varianten werden in
 * {@code setupAnim} über den Render-State ein- und ausgeblendet; die Ausrüstung zeichnet eine eigene Modell-Instanz je
 * Platz (Halsband, Amulett, Panzerung) mit der Textur des gewählten Materials.
 */
public interface CompanionLookModel {
	/** Diese Instanz zeichnet nur noch die Ausrüstung des Platzes {@code slot} (0 Halsband, 1 Amulett, 2 Panzerung). */
	void equipmentPass(int slot);

	/** Überträgt die Bewegung (Abweichung von der Grundpose) eines Teils auf seine Variante. */
	static void follow(ModelPart src, ModelPart dst) {
		PartPose a = src.getInitialPose();
		PartPose b = dst.getInitialPose();
		dst.x = b.x() + src.x - a.x();
		dst.y = b.y() + src.y - a.y();
		dst.z = b.z() + src.z - a.z();
		dst.xRot = b.xRot() + src.xRot - a.xRot();
		dst.yRot = b.yRot() + src.yRot - a.yRot();
		dst.zRot = b.zRot() + src.zRot - a.zRot();
	}
}

package pe.erp.sofas.ui.three.parts.concrete;

import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import pe.erp.sofas.ui.three.parts.BaseSofaPart;

/**
 * Base o armazón de asiento: caja redondeada tapizada que forma la tarima del sofá.
 * Altura típica: 18 cm (incluyendo altura de patas).
 */

public class BaseSeatFramePart extends BaseSofaPart {

    private final PhongMaterial material;

    private final double altoPatas;


    public BaseSeatFramePart(double ancho, double fondo, double altoPatas, PhongMaterial material) {
        super("Base de asiento", ancho, fondo, 18);  // ancho, fondo, alto
        this.material = material == null ? materialPorDefecto() : material;
        this.altoPatas = altoPatas;
    }


    @Override
    public Group build() {
        Group g = new Group();
        // Armazón: caja redondeada con radio 3 cm
        agregar(g, cajaRedondeada(ancho, alto, fondo, 3, material), 0, alto / 2, 0);
        return g;
    }
}

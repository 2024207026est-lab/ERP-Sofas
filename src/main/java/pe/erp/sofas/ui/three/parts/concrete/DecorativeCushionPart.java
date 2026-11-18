package pe.erp.sofas.ui.three.parts.concrete;

import javafx.scene.Group;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.transform.Rotate;
import pe.erp.sofas.ui.three.parts.BaseSofaPart;

/**
 * Cojín decorativo: pequeños cojines cuadrados o rectangulares inclinados
 * que se colocan sobre el respaldo o el asiento para mayor comodidad visual.
 * Altura típica: 12 cm, ancho variable (40-50 cm).
 */

public class DecorativeCushionPart extends BaseSofaPart {

    private final PhongMaterial material;


    public DecorativeCushionPart(double ancho, PhongMaterial material) {
        super("Cojín decorativo", ancho, ancho, 12);  // Cuadrado, bajo
        this.material = material == null ? materialPorDefecto() : material;
    }


    @Override
    public Group build() {
        Group g = new Group();

        // Cojín con radio 5 cm
        var cojin = cajaRedondeada(ancho, alto, fondo, 5, material);

        // Inclinación: 12° para darle dinamismo
        cojin.getTransforms().add(new Rotate(-12, Rotate.X_AXIS));
        cojin.getTransforms().add(new Rotate(6, Rotate.Z_AXIS));

        agregar(g, cojin, 0, alto / 2, 0);

        return g;
    }
}

package pe.erp.sofas.ui.three.parts.concrete;

import javafx.scene.Group;
import javafx.scene.paint.PhongMaterial;
import pe.erp.sofas.ui.three.parts.BaseSofaPart;

/**
 * Puff u ottomán: módulo bajo sin respaldo, usado como posapies o asiento extra.
 * Típicamente cuadrado o rectangular, altura baja (40-50 cm).
 */

public class OttomanPart extends BaseSofaPart {

    private final PhongMaterial material;


    public OttomanPart(double ancho, double fondo, PhongMaterial material) {
        super("Puff", ancho, fondo, 40);  // Altura baja
        this.material = material == null ? materialPorDefecto() : material;
    }


    @Override
    public Group build() {
        Group g = new Group();

        // Cuerpo principal del puff: caja redondeada
        agregar(g, cajaRedondeada(ancho, alto, fondo, 6, material), 0, alto / 2, 0);

        return g;
    }
}

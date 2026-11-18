package pe.erp.sofas.ui.three;

import javafx.scene.image.Image;
import pe.erp.sofas.model.Producto;

import java.util.HashMap;
import java.util.Map;

/** Miniaturas renderizadas en 3D de los modelos sin foto (snapshot de la SubScene, cacheado). */

public final class Render3D {

    private static final Map<String, Image> CACHEPMMD = new HashMap<>();


    private Render3D() {
    }


    public static Image modelo(Producto p) {
        return CACHEPMMD.computeIfAbsent(p.getCodigo() + p.getColorHex() + p.getLargo(), k ->
                Viewer3D.render(new SofaBuilder().build(SofaBuilder.Opciones.de(p)), 640, 420, -28, 16));
    }
}

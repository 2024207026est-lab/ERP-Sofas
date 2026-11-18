package pe.erp.sofas.ui.three.parts.families;

import javafx.scene.paint.PhongMaterial;
import pe.erp.sofas.model.ConfigSofa;
import pe.erp.sofas.ui.three.parts.SofaPart;
import pe.erp.sofas.ui.three.parts.concrete.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Familia Modular: constructor libre, pieza a pieza.
 * Código: SF-013
 */

public class ModularFamily implements FamiliaFactory.FamiliaConstructor {

    @Override
    public List<SofaPart> construir(ConfigSofa config, PhongMaterial materialTela, PhongMaterial materialRespaldo) {
        List<SofaPart> partes = new ArrayList<>();
        PhongMaterial matResp = materialRespaldo == null ? materialTela : materialRespaldo;

        // TODO: Implementar módulos libres
        partes.add(new BaseSeatFramePart(200, 92, 13, materialTela));
        return partes;
    }
}

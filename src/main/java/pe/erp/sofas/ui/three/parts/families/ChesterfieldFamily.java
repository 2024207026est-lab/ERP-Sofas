package pe.erp.sofas.ui.three.parts.families;

import javafx.scene.paint.PhongMaterial;
import pe.erp.sofas.model.ConfigSofa;
import pe.erp.sofas.ui.three.parts.SofaPart;
import pe.erp.sofas.ui.three.parts.concrete.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Chesterfield: brazo enrollado, capitoné, patas bola.
 * Código: SF-006
 */

public class ChesterfieldFamily implements FamiliaFactory.FamiliaConstructor {

    @Override
    public List<SofaPart> construir(ConfigSofa config, PhongMaterial materialTela, PhongMaterial materialRespaldo) {
        List<SofaPart> partes = new ArrayList<>();
        PhongMaterial matResp = materialRespaldo == null ? materialTela : materialRespaldo;
        int cuerpos = config.cuerpos();
        double largo = cuerpos * 52;
        
        // TODO: Implementar Chesterfield con capitoné
        partes.add(new BaseSeatFramePart(largo, 92, 13, materialTela));
        return partes;
    }
}

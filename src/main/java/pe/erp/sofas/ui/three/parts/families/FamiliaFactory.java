package pe.erp.sofas.ui.three.parts.families;

import javafx.scene.paint.PhongMaterial;
import pe.erp.sofas.model.ConfigSofa;
import pe.erp.sofas.ui.three.parts.SofaPart;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Factory para construir familias de sofás.
 * Dado una familia, configuración y materiales, retorna la lista de partes que componen el sofá.
 *
 * Patrón: Strategy + Factory. Cada familia tiene su propio constructor (ej: ModernoFamily).
 */

public class FamiliaFactory {

    private final Map<SofaFamilia, FamiliaConstructor> constructoresPMMD = new HashMap<>();


    public FamiliaFactory() {
        // Registrar cada familia con su constructor
        constructoresPMMD.put(SofaFamilia.MODERNO, new ModernoFamily());
        constructoresPMMD.put(SofaFamilia.SECCIONAL_L_CHAISE, new SeccionalLChaiseFamily());
        constructoresPMMD.put(SofaFamilia.SECCIONAL_L_ESQUINERO, new SeccionalLEsquineroFamily());
        constructoresPMMD.put(SofaFamilia.SECCIONAL_U, new SeccionalUFamily());
        constructoresPMMD.put(SofaFamilia.MODULAR, new ModularFamily());
        constructoresPMMD.put(SofaFamilia.CHESTERFIELD, new ChesterfieldFamily());
        constructoresPMMD.put(SofaFamilia.ESCANDINAVO, new EscandinavoFamily());
        constructoresPMMD.put(SofaFamilia.SOFA_CAMA, new SofaCamaFamily());
        constructoresPMMD.put(SofaFamilia.RECLINABLE, new ReclinableFamily());
        constructoresPMMD.put(SofaFamilia.RETRO, new RetroFamily());
        constructoresPMMD.put(SofaFamilia.CLOUD, new CloudFamily());
        constructoresPMMD.put(SofaFamilia.TUXEDO, new TuxedoFamily());
        constructoresPMMD.put(SofaFamilia.SILLON, new SillonFamily());
    }

    /**
     * Construye la lista de partes para una familia.
     *
     * @param familia la familia (Moderno, L, etc)
     * @param config configuración del sofá (cuerpos, chaise, brazos, etc)
     * @param materialTela material de la tela principal
     * @param materialRespaldo material del respaldo (si es diferente)
     * @return List<SofaPart> ordenadas para ensamblar
     */

    public List<SofaPart> construir(SofaFamilia familia, ConfigSofa config,
                                   PhongMaterial materialTela, PhongMaterial materialRespaldo) {
        FamiliaConstructor constructor = constructoresPMMD.get(familia);
        if (constructor == null) {
            throw new IllegalArgumentException("Familia no registrada: " + familia);
        }
        return constructor.construir(config, materialTela, materialRespaldo);
    }

    /**
     * Interface que deben implementar todas las familias.
     */

    public interface FamiliaConstructor {
        /**
         * Construye la lista de partes para esta familia.
         */
        
        List<SofaPart> construir(ConfigSofa config, PhongMaterial materialTela, PhongMaterial materialRespaldo);
    }
}

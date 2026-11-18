package pe.erp.sofas.ui.three.parts.concrete;

import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.MeshView;
import pe.erp.sofas.ui.three.geom.PipingMesh;
import pe.erp.sofas.ui.three.parts.BaseSofaPart;

/**
 * Ribete decorativo: tubo delgado que delinea los bordes de un cojín.
 * Se posiciona en el perímetro (típicamente en la arista superior de un cojín de asiento).
 *
 * El ribete es principalmente decorativo pero añade realismo al sofá.
 * Color: típicamente 10-15% más oscuro que el cojín.
 */

public class RibetePart extends BaseSofaPart {

    private final PhongMaterial materialRibetePMMD;


    public RibetePart(double ancho, double profundidad, PhongMaterial materialBase) {
        super("Ribete", ancho, profundidad, 1);  // Alto mínimo, casi invisible
        // Ribete: color ligeramente más oscuro que el base
        if (materialBase != null) {
            Color colBase = (Color) materialBase.getDiffuseColor();
            Color colOscuro = colBase.deriveColor(0, 1, 0.85, 1);  // 15% más oscuro
            this.materialRibetePMMD = new PhongMaterial(colOscuro);
            this.materialRibetePMMD.setSpecularPower(materialBase.getSpecularPower());
        } else {
            this.materialRibetePMMD = new PhongMaterial(Color.web("#8B8B8B"));
            this.materialRibetePMMD.setSpecularPower(8);
        }
    }


    @Override
    public Group build() {
        Group g = new Group();

        // Crear tubo de ribete alrededor del perímetro
        MeshView ribete = new MeshView(PipingMesh.crearDefecto(ancho, fondo));
        ribete.setMaterial(materialRibetePMMD);
        ribete.setCullFace(javafx.scene.shape.CullFace.NONE);

        agregar(g, ribete, 0, 0, 0);

        return g;
    }
}

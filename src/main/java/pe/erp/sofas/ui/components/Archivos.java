package pe.erp.sofas.ui.components;

import javafx.scene.Node;
import javafx.stage.FileChooser;

import java.io.File;

/** Dialogo "Guardar como" con la carpeta de Documentos y el nombre sugerido. */

public final class Archivos {

    private static File ultimaCarpetaPMMD;


    private Archivos() {
    }


    public static File guardar(Node ancla, String nombre, String descripcion, String extension) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Guardar " + descripcion);
        fc.setInitialFileName(nombre + "." + extension);
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter(descripcion, "*." + extension));
        File dir = ultimaCarpetaPMMD != null ? ultimaCarpetaPMMD : new File(System.getProperty("user.home"), "Documents");
        if (dir.isDirectory()) fc.setInitialDirectory(dir);
        File f = fc.showSaveDialog(ancla == null || ancla.getScene() == null ? null : ancla.getScene().getWindow());
        if (f != null) ultimaCarpetaPMMD = f.getParentFile();
        return f;
    }
}

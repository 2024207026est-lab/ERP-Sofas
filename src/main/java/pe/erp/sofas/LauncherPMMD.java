package pe.erp.sofas;

import javafx.application.Application;

/** Punto de entrada. Clase separada para que el jar ejecutable arranque JavaFX sin module-path. */

public class LauncherPMMD {

    public static void main(String[] args) {
        Application.launch(MainAppPMMD.class, args);
    }
}

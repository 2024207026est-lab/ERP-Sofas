package pe.erp.sofas.ui.components;

import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material2.Material2OutlinedAL;
import org.kordamp.ikonli.material2.Material2OutlinedMZ;

/** Iconos Material Design 2 (variante outlined) por nombre: {@code Ico.of("DASHBOARD", 18)}. */

public final class Ico {

    private Ico() {
    }


    public static Ikon ikon(String nombre) {
        for (String n : new String[]{nombre, "MDOAL_" + nombre}) {
            try {
                return Material2OutlinedAL.valueOf(n);
            } catch (IllegalArgumentException ignored) {
                // se prueba la otra mitad del alfabeto
            }
            try {
                return Material2OutlinedMZ.valueOf(n);
            } catch (IllegalArgumentException ignored) {
                // siguiente variante
            }
        }
        return Material2OutlinedAL.INFO;
    }


    public static FontIcon of(String nombre) {
        return of(nombre, 18);
    }


    public static FontIcon of(String nombre, int size) {
        FontIcon icon = new FontIcon(ikon(nombre));
        icon.setIconSize(size);
        icon.getStyleClass().add("ico");
        return icon;
    }


    public static FontIcon of(String nombre, int size, String color) {
        FontIcon icon = of(nombre, size);
        icon.setStyle("-fx-icon-color: " + color + ";");
        return icon;
    }
}

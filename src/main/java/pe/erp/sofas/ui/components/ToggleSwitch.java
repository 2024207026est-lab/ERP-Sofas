package pe.erp.sofas.ui.components;

/** Interruptor on/off (basado en el ToggleSwitch de AtlantaFX, estilizado con los tokens del tema). */

public class ToggleSwitch extends atlantafx.base.controls.ToggleSwitch {

    public ToggleSwitch(boolean seleccionado) {
        setSelected(seleccionado);
        getStyleClass().add("is-switch");
    }


    public ToggleSwitch(String texto, boolean seleccionado) {
        super(texto);
        setSelected(seleccionado);
        getStyleClass().add("is-switch");
    }
}

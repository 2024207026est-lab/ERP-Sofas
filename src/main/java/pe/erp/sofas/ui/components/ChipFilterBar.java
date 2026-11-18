package pe.erp.sofas.ui.components;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.FlowPane;

import java.util.List;

/** Fila de chips de filtro con contador; siempre hay uno seleccionado. */

public class ChipFilterBar extends FlowPane {

    public record Chip(
      String clave, 
        String texto, 
          long conteo) {
    }


    private final ToggleGroup grupoPMMD = new ToggleGroup();

    private final ObjectProperty<String> seleccionPMMD = new SimpleObjectProperty<>();

    private boolean reconstruyendoPMMD;


    public ChipFilterBar() {
        super(8, 8);
        getStyleClass().add("chip-bar");
        setPrefWrapLength(1150);
        setMinWidth(0);
        setMinHeight(USE_PREF_SIZE);
        grupoPMMD.selectedToggleProperty().addListener((o, a, n) -> {
            if (reconstruyendoPMMD) return;
            if (n == null) {
                if (a != null) grupoPMMD.selectToggle(a);
                return;
            }
            seleccionPMMD.set((String) n.getUserData());
        });
    }

    /** Reemplaza los chips conservando la seleccion actual si sigue existiendo. */

    public void setChips(List<Chip> chips) {
        String actual = seleccionPMMD.get();
        reconstruyendoPMMD = true;
        getChildren().clear();
        grupoPMMD.getToggles().clear();
        ToggleButton aSeleccionar = null;
        for (Chip c : chips) {
            ToggleButton t = new ToggleButton(c.texto());
            t.setUserData(c.clave());
            t.setToggleGroup(grupoPMMD);
            t.getStyleClass().add("chip");
            t.setMnemonicParsing(false);
            if (c.conteo() >= 0) {
                Label n = UI.label(String.valueOf(c.conteo()), "chip-count");
                t.setGraphic(n);
                t.setContentDisplay(javafx.scene.control.ContentDisplay.RIGHT);
            }
            getChildren().add(t);
            if (aSeleccionar == null || c.clave().equals(actual)) aSeleccionar = t;
        }
        if (aSeleccionar != null) grupoPMMD.selectToggle(aSeleccionar);
        reconstruyendoPMMD = false;
        // Solo notifica si la clave seleccionada cambio (evita recursion cuando se llama desde un render).
        if (aSeleccionar != null) seleccionPMMD.set((String) aSeleccionar.getUserData());
    }


    public ObjectProperty<String> seleccionProperty() {
        return seleccionPMMD;
    }


    public String getSeleccion() {
        return seleccionPMMD.get();
    }


    public void seleccionar(String clave) {
        grupoPMMD.getToggles().stream().filter(t -> clave.equals(t.getUserData())).findFirst().ifPresent(grupoPMMD::selectToggle);
    }
}

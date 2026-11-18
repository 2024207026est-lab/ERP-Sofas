package pe.erp.sofas.ui.components;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;

/** Control segmentado (Hoy | Semana | Mes, Kanban | Tabla | Calendario...). */

public class SegmentedControl extends HBox {

    private final ToggleGroup grupoPMMD = new ToggleGroup();

    private final ObjectProperty<String> valorPMMD = new SimpleObjectProperty<>();


    public SegmentedControl(String... opciones) {
        getStyleClass().add("segmented");
        setMaxWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        for (String op : opciones) {
            agregar(op, null);
        }
        grupoPMMD.selectedToggleProperty().addListener((o, a, n) -> {
            if (n == null) {
                grupoPMMD.selectToggle(a);
                return;
            }
            valorPMMD.set((String) n.getUserData());
        });
        if (!grupoPMMD.getToggles().isEmpty()) grupoPMMD.selectToggle(grupoPMMD.getToggles().get(0));
        setAlignment(javafx.geometry.Pos.CENTER_LEFT);
    }

    /** Agrega un segmento con icono opcional. */

    public SegmentedControl agregar(String texto, String icono) {
        ToggleButton t = new ToggleButton(texto);
        t.setUserData(texto);
        t.setToggleGroup(grupoPMMD);
        t.getStyleClass().add("segment");
        t.setMnemonicParsing(false);
        t.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        if (icono != null) t.setGraphic(Ico.of(icono, 16));
        getChildren().add(t);
        return this;
    }


    public static SegmentedControl conIconos(String[] textos, String[] iconos) {
        SegmentedControl s = new SegmentedControl();
        for (int i = 0; i < textos.length; i++) s.agregar(textos[i], iconos[i]);
        s.grupoPMMD.selectToggle(s.grupoPMMD.getToggles().get(0));
        return s;
    }


    public ObjectProperty<String> valorProperty() {
        return valorPMMD;
    }


    public String getValor() {
        return valorPMMD.get();
    }


    public void setValor(String v) {
        grupoPMMD.getToggles().stream().filter(t -> v.equals(t.getUserData())).findFirst().ifPresent(grupoPMMD::selectToggle);
    }
}

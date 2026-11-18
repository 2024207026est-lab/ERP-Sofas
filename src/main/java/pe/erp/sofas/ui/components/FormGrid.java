package pe.erp.sofas.ui.components;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.*;

/** Formulario en rejilla: etiquetas arriba de cada control, secciones numeradas y mensaje de error. */

public class FormGrid extends VBox {

    private final GridPane gridPMMD = new GridPane();

    private final Label error = UI.label("", "form-error");

    private final int columnas;

    private int colPMMD;

    private int filaPMMD;


    public FormGrid(int columnas) {
        super(14);
        this.columnas = columnas;
        gridPMMD.setHgap(14);
        gridPMMD.setVgap(14);
        for (int i = 0; i < columnas; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(100.0 / columnas);
            cc.setHgrow(Priority.ALWAYS);
            gridPMMD.getColumnConstraints().add(cc);
        }
        error.setGraphic(Ico.of("ERROR", 16));
        error.setWrapText(true);
        error.setMaxWidth(Double.MAX_VALUE);
        error.setVisible(false);
        error.setManaged(false);
        getChildren().addAll(error, gridPMMD);
    }


    public FormGrid campo(String etiqueta, Node control) {
        return campo(etiqueta, control, 1);
    }


    public FormGrid campo(String etiqueta, Node control, int span) {
        return nodo(UI.field(etiqueta, control), span);
    }


    public FormGrid nodo(Node n, int span) {
        if (colPMMD + span > columnas) {
            colPMMD = 0;
            filaPMMD++;
        }
        gridPMMD.add(n, colPMMD, filaPMMD, span, 1);
        if (n instanceof Region r) r.setMaxWidth(Double.MAX_VALUE);
        colPMMD += span;
        if (colPMMD >= columnas) {
            colPMMD = 0;
            filaPMMD++;
        }
        return this;
    }

    /** Cabecera de seccion a todo el ancho: "1  Cliente". */

    public FormGrid seccion(String numero, String titulo) {
        if (colPMMD != 0) {
            colPMMD = 0;
            filaPMMD++;
        }
        Label num = UI.label(numero, "form-section-num");
        var h = UI.hbox(10, num, UI.label(titulo, "form-section-title"));
        if (filaPMMD > 0) h.setStyle("-fx-padding: 8 0 0 0;");
        gridPMMD.add(h, 0, filaPMMD++, columnas, 1);
        return this;
    }


    public void error(String mensaje) {
        error.setText(mensaje);
        error.setVisible(true);
        error.setManaged(true);
        UI.entrar(error);
    }


    public void limpiarError() {
        error.setVisible(false);
        error.setManaged(false);
    }
}

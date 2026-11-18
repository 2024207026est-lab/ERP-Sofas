package pe.erp.sofas.ui.components;

import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

/** Barra de progreso fina con color propio (etapas, stock, margen, % pagado). */

public class ProgressBarMini extends StackPane {

    private final Region rellenoPMMD = new Region();

    private double valor;


    public ProgressBarMini(double valor, String color, double alto) {
        getStyleClass().add("progress-track");
        rellenoPMMD.getStyleClass().add("progress-fill");
        setMinHeight(alto);
        setPrefHeight(alto);
        setMaxHeight(alto);
        rellenoPMMD.setMaxHeight(alto);
        setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        getChildren().add(rellenoPMMD);
        setStyle("-fx-background-radius: " + alto + ";");
        setColor(color);
        setValor(valor);
        widthProperty().addListener((o, a, n) -> ajustar());
    }


    public void setColor(String color) {
        rellenoPMMD.setStyle("-fx-background-color: " + color + "; -fx-background-radius: " + getPrefHeight() + ";");
    }

    /** Valor entre 0 y 1. */

    public void setValor(double v) {
        this.valor = Math.max(0, Math.min(1, v));
        ajustar();
    }


    private void ajustar() {
        double w = getWidth() * valor;
        rellenoPMMD.setMinWidth(w);
        rellenoPMMD.setPrefWidth(w);
        rellenoPMMD.setMaxWidth(w);
    }
}

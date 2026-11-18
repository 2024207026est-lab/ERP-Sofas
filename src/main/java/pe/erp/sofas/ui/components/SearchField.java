package pe.erp.sofas.ui.components;

import javafx.beans.property.StringProperty;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

/** Buscador con icono de lupa y boton para limpiar. */

public class SearchField extends HBox {

    private final TextField campo = new TextField();


    public SearchField(String prompt) {
        this(prompt, "SEARCH");
    }


    public SearchField(String prompt, String icono) {
        super(8);
        getStyleClass().add("search-field");
        campo.setPromptText(prompt);
        campo.getStyleClass().add("search-input");
        HBox.setHgrow(campo, Priority.ALWAYS);
        Button limpiar = UI.iconButton("CLOSE", "Limpiar búsqueda", "btn-ghost", "btn-xs");
        limpiar.visibleProperty().bind(campo.textProperty().isNotEmpty());
        limpiar.managedProperty().bind(limpiar.visibleProperty());
        limpiar.setOnAction(e -> {
            campo.clear();
            campo.requestFocus();
        });
        limpiar.setFocusTraversable(false);
        getChildren().addAll(Ico.of(icono, 18), campo, limpiar);
        setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        setOnMouseClicked(e -> campo.requestFocus());
    }


    public StringProperty textProperty() {
        return campo.textProperty();
    }


    public String getText() {
        return campo.getText() == null ? "" : campo.getText().trim();
    }


    public void setText(String t) {
        campo.setText(t);
    }


    public TextField campo() {
        return campo;
    }
}

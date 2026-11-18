package pe.erp.sofas.ui.views.personal;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import pe.erp.sofas.core.*;
import pe.erp.sofas.model.*;
import pe.erp.sofas.service.*;
import pe.erp.sofas.ui.components.*;

import java.util.*;

/** Personal del taller y la tienda, productividad del mes y pago a destajo por etapa. */

@Component
@Lazy
@Scope("prototype")
@RequiredArgsConstructor
public class PersonalController implements Vista {

    private final EmpleadoService empleadosPMMD;

    private final ParametroService parametrosPMMD;

    private final Overlays overlaysPMMD;

    private final Toast toastPMMD;


    @FXML private VBox raiz;


    private SegmentedControl tabsPMMD;

    private final VBox contenidoPMMD = new VBox(14);


    @FXML
    public void initialize() {
        tabsPMMD = new SegmentedControl("Equipo", "Productividad", "Pago a destajo");
        tabsPMMD.getStyleClass().add("tabs");
        tabsPMMD.valorProperty().addListener((o, a, b) -> render());
        Button nuevo = UI.primary("Nuevo trabajador", "PERSON_ADD");
        nuevo.setOnAction(e -> formulario(null));
        ScrollPane sp = UI.scroll(contenidoPMMD);
        VBox.setVgrow(sp, Priority.ALWAYS);
        raiz.getChildren().setAll(UI.hbox(10, tabsPMMD, UI.spacer(), nuevo), sp);
    }


    @Override
    public void onShow(Intent intent) {
        render();
    }


    private void render() {
        List<Empleado> todos = empleadosPMMD.listar();
        Node k = UI.fila(
                new KpiCard("ENGINEERING", "blue", "Trabajadores", String.valueOf(todos.size()), todos.stream().filter(Empleado::isTaller).count() + " en el taller"),
                new KpiCard("CARPENTER", "wood", "Carpinteros", String.valueOf(todos.stream().filter(e -> "Taller · Carpintería".equals(e.getArea())).count()), "Estructuras"),
                new KpiCard("GESTURE", "amber", "Costureras", String.valueOf(todos.stream().filter(e -> "Taller · Costura".equals(e.getArea())).count()), "Corte y costura"),
                new KpiCard("WEEKEND", "violet", "Tapiceros", String.valueOf(todos.stream().filter(e -> e.getRol() == Rol.TAPICERO).count()), "Tapizado y espumado"));
        switch (tabsPMMD.getValor()) {
            case "Productividad" -> contenidoPMMD.getChildren().setAll(k, productividad());
            case "Pago a destajo" -> contenidoPMMD.getChildren().setAll(k, destajo());
            default -> contenidoPMMD.getChildren().setAll(k, equipo(todos));
        }
    }


    private Node equipo(List<Empleado> todos) {
        TableView<Empleado> t = Cells.tabla("Sin trabajadores.");
        t.getColumns().addAll(List.of(
                Cells.nodo("Trabajador", e -> UI.hbox(10, AvatarInitials.conColor(e.getNombre(), 30, e.getColor() == null ? "#64748B" : e.getColor()),
                        new VBox(0, UI.label(e.getNombre(), "cell-title"), UI.label("DNI " + e.getDni(), "cell-sub"))), 240),
                Cells.nodo("Cargo", e -> new VBox(0, UI.label(e.getCargo(), "cell-title"),
                        UI.label(e.getEtapa() == null ? "—" : "Etapa: " + e.getEtapa().nombre(), "cell-sub")), 180),
                Cells.texto("Área", Empleado::getArea, 160, "cell-muted"),
                Cells.texto("Rol del sistema", e -> e.getRol() == null ? "—" : e.getRol().nombre(), 140),
                Cells.texto("Teléfono", Empleado::getTelefono, 110),
                Cells.texto("Ingreso", e -> AppClockPMMD.fmt(e.getIngreso()), 90),
                Cells.badge("Estado", Empleado::getEstado, 90)));
        t.getItems().setAll(todos);
        t.setFixedCellSize(52);
        t.setPrefHeight(52 * todos.size() + 44);
        t.setRowFactory(tv -> {
            TableRow<Empleado> r = new TableRow<>();
            r.setOnMouseClicked(e -> {
                if (!r.isEmpty() && e.getClickCount() == 2) formulario(r.getItem());
            });
            return r;
        });
        VBox card = new VBox(t);
        card.getStyleClass().addAll("card", "table-card");
        return new VBox(8, UI.label("Doble clic en una fila para editar.", "cell-sub"), card);
    }


    private Node productividad() {
        List<EmpleadoService.Productividad> p = empleadosPMMD.productividad();
        double max = p.stream().mapToInt(EmpleadoService.Productividad::sofasTerminados).max().orElse(1);
        VBox barras = new VBox(10);
        p.stream().sorted(Comparator.comparingInt(EmpleadoService.Productividad::sofasTerminados).reversed()).forEach(x ->
                barras.getChildren().add(Charts.barraHorizontal(x.empleado().getNombre(), x.sofasTerminados(), Math.max(1, max),
                        x.empleado().getColor() == null ? "#2563EB" : x.empleado().getColor(), x.sofasTerminados() + " sofás")));
        return UI.card(UI.cardHeader("Sofás terminados en octubre", "Órdenes en Listo de las que fue responsable (incluye las cerradas este mes)"), barras);
    }


    private Node destajo() {
        List<EmpleadoService.Productividad> p = empleadosPMMD.productividad();
        GridPane tarifas = new GridPane();
        tarifas.setHgap(12);
        tarifas.setVgap(6);
        tarifas.addRow(0, UI.label("ETAPA", "dato-label"), UI.label("TARIFA POR SOFÁ (S/)", "dato-label"));
        int i = 1;
        for (Etapa e : ParametroService.DESTAJO.keySet()) {
            TextField t = Forms.decimal(parametrosPMMD.tarifaDestajo(e));
            t.setPrefWidth(90);
            t.getStyleClass().add("cell-field");
            Runnable guardar = () -> {
                double v = Validacion.valor(t);
                if (Math.abs(v - parametrosPMMD.tarifaDestajo(e)) > 1e-9 && v >= 0) {
                    parametrosPMMD.fijar("destajo." + e.name(), v);
                    toastPMMD.exito("Tarifa de " + e.nombre() + ": S/ " + MoneyPMMD.numero(v) + " por sofá.");
                    render();
                }
            };
            t.setOnAction(x -> guardar.run());
            t.focusedProperty().addListener((o, a, b) -> {
                if (!b) guardar.run();
            });
            Region punto = new Region();
            punto.getStyleClass().add("legend-dot");
            punto.setStyle("-fx-background-color: " + e.color() + ";");
            tarifas.addRow(i++, UI.hbox(8, punto, UI.label(e.nombre(), "cell-title")), t);
        }
        TableView<EmpleadoService.Productividad> tabla = Cells.tabla("Sin trabajadores de taller.");
        tabla.getColumns().addAll(List.of(
                Cells.texto("Trabajador", x -> x.empleado().getNombre(), 200, "cell-strong"),
                Cells.texto("Etapa", x -> x.empleado().getEtapa() == null ? "—" : x.empleado().getEtapa().nombre(), 170),
                Cells.derecha("Sofás del mes", x -> String.valueOf(x.sofasEnEtapa()), 110),
                Cells.derecha("Tarifa", x -> MoneyPMMD.fmt(x.tarifa()), 90, "cell-muted"),
                Cells.derecha("Monto a destajo", x -> MoneyPMMD.fmt(x.destajo()), 120, "cell-strong")));
        tabla.getItems().setAll(p);
        tabla.setFixedCellSize(44);
        tabla.setPrefHeight(44 * p.size() + 44);
        double total = p.stream().mapToDouble(EmpleadoService.Productividad::destajo).sum();
        VBox card = new VBox(tabla);
        card.getStyleClass().addAll("card", "table-card");
        return UI.columnas(new double[]{1, 2.2}, UI.card(UI.cardHeader("Tarifas por etapa", "Configurables · se guardan al salir del campo"), tarifas),
                new VBox(10, UI.hbox(8, UI.label("Pago a destajo de octubre", "card-title"), UI.spacer(), UI.label("Total " + MoneyPMMD.fmt(total), "price-value")), card));
    }


    private void formulario(Empleado existente) {
        Empleado e = existente == null ? Empleado.builder().estado("Activo").ingreso(AppClockPMMD.hoy()).area("Taller · Tapizado").rol(Rol.TAPICERO).build() : existente;
        Validacion v = new Validacion();
        TextField nombre = UI.text(e.getNombre(), "Nombres y apellidos");
        TextField dni = Forms.digitos(e.getDni(), 8);
        TextField cargo = UI.text(e.getCargo(), "Tapicero");
        ComboBox<String> area = Forms.combo(Empleado.AREAS, e.getArea(), s -> s);
        ComboBox<Rol> rol = Forms.combo(List.of(Rol.values()), e.getRol(), Rol::nombre);
        List<Etapa> etapas = new ArrayList<>();
        etapas.add(null);
        etapas.addAll(List.of(Etapa.values()).subList(1, 10));
        ComboBox<Etapa> etapa = Forms.combo(etapas, e.getEtapa(), x -> x == null ? "No aplica" : x.nombre());
        TextField tel = UI.text(e.getTelefono(), "951 000 000");
        DatePicker ingreso = Forms.fecha(e.getIngreso());
        ComboBox<String> estado = Forms.combo(List.of("Activo", "Vacaciones", "Inactivo"), e.getEstado(), s -> s);
        FormGrid f = new FormGrid(2);
        f.nodo(v.campo("Nombre", nombre, () -> Validacion.requerido(nombre, "el nombre")), 2);
        f.nodo(v.campo("DNI", dni, () -> dni.getText().matches("\\d{8}") ? null : "El DNI debe tener 8 dígitos."), 1);
        f.nodo(v.campo("Cargo", cargo, () -> Validacion.requerido(cargo, "el cargo")), 1);
        f.nodo(v.campo("Área", area, null), 1);
        f.nodo(v.campo("Rol del sistema", rol, null), 1);
        f.nodo(v.campo("Etapa (destajo)", etapa, null), 1);
        f.nodo(v.campo("Teléfono", tel, null), 1);
        f.nodo(v.campo("Ingreso", ingreso, () -> ingreso.getValue() == null ? "Elige la fecha." : null), 1);
        f.nodo(v.campo("Estado", estado, null), 1);
        Button cancelar = UI.secondary("Cancelar", null);
        Button ok = UI.primary(existente == null ? "Registrar" : "Guardar cambios", "SAVE");
        v.enlazar(ok);
        var h = overlaysPMMD.modal(existente == null ? "Nuevo trabajador" : e.getNombre(), null, f, 580, cancelar, ok);
        cancelar.setOnAction(x -> h.cerrar());
        ok.setOnAction(x -> {
            e.setNombre(nombre.getText().trim());
            e.setDni(dni.getText());
            e.setCargo(cargo.getText());
            e.setArea(area.getValue());
            e.setRol(rol.getValue());
            e.setEtapa(etapa.getValue());
            e.setTelefono(tel.getText());
            e.setIngreso(ingreso.getValue());
            e.setEstado(estado.getValue());
            if (e.getColor() == null) e.setColor("#64748B");
            Empleado[] g = new Empleado[1];
            if (toastPMMD.intentar(() -> g[0] = empleadosPMMD.guardar(e))) {
                h.cerrar();
                toastPMMD.exito(g[0].getNombre() + " guardado.");
                render();
            }
        });
    }
}

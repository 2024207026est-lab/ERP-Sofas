package pe.erp.sofas.ui.views.produccion;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import pe.erp.sofas.core.*;
import pe.erp.sofas.model.Material;
import pe.erp.sofas.model.Proveedor;
import pe.erp.sofas.service.MrpServicePMMD;
import pe.erp.sofas.service.ProveedorService;
import pe.erp.sofas.ui.components.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Requerimiento de materiales: requerido, stock, transito y faltante neto, con generacion de OC por proveedor. */

@Component
@Lazy
@Scope("prototype")
@RequiredArgsConstructor
public class MrpController implements Vista {

    private final MrpServicePMMD mrpPMMD;

    private final ProveedorService proveedoresPMMD;

    private final CalculadoraMateriales calculadoraPMMD;

    private final Toast toastPMMD;

    private final ConfirmDialog confirmPMMD;

    private final Navigator navigatorPMMD;


    @FXML private VBox raiz;


    private final VBox kpisPMMD = new VBox();

    private DatePicker desdePMMD, hastaPMMD;

    private ComboBox<String> categoriaPMMD;

    private ComboBox<Proveedor> proveedorPMMD;

    private CheckBox soloFaltantesPMMD;

    private final Label notaPMMD = UI.label("", "cell-sub");

    private final HBarChart graficoPMMD = new HBarChart().colores("#14213D", "#16A34A").leyenda("Requerido", "Stock + tránsito").porFila(true);

    private final TableView<MrpServicePMMD.Fila> tablaPMMD = Cells.tabla("Sin materiales requeridos con estos filtros.");

    private final StackPane tablaHostPMMD = new StackPane();

    private MrpServicePMMD.Resultado ultimoPMMD;

    private int consultaPMMD;


    @FXML
    public void initialize() {
        desdePMMD = Forms.fecha(null);
        desdePMMD.setPromptText("Entrega desde");
        hastaPMMD = Forms.fecha(null);
        hastaPMMD.setPromptText("Entrega hasta");
        desdePMMD.setPrefWidth(150);
        hastaPMMD.setPrefWidth(150);
        List<String> cats = new ArrayList<>();
        cats.add(null);
        cats.addAll(Material.CATEGORIAS);
        categoriaPMMD = Forms.combo(cats, null, c -> c == null ? "Todas las categorías" : c);
        categoriaPMMD.setPrefWidth(190);
        List<Proveedor> ps = new ArrayList<>();
        ps.add(null);
        ps.addAll(proveedoresPMMD.listar());
        proveedorPMMD = Forms.combo(ps, null, p -> p == null ? "Todos los proveedores" : p.getRazonSocial());
        proveedorPMMD.setPrefWidth(220);
        soloFaltantesPMMD = new CheckBox("Solo faltantes");
        for (var p : List.of(desdePMMD.valueProperty(), hastaPMMD.valueProperty(), categoriaPMMD.valueProperty(), proveedorPMMD.valueProperty())) p.addListener((o, a, b) -> calcular());
        soloFaltantesPMMD.selectedProperty().addListener((o, a, b) -> calcular());
        Button generar = UI.primary("Generar órdenes de compra", "SHOPPING_CART");
        generar.setOnAction(e -> generar());
        HBox barra = UI.hbox(10, desdePMMD, hastaPMMD, categoriaPMMD, proveedorPMMD, soloFaltantesPMMD, UI.spacer(), generar);
        construirTabla();
        VBox tablaCard = new VBox(tablaHostPMMD);
        tablaCard.getStyleClass().addAll("card", "table-card");
        VBox.setVgrow(tablaHostPMMD, Priority.ALWAYS);
        VBox graf = UI.card(UI.cardHeader("Materiales críticos", "Los 10 de mayor faltante valorizado"), graficoPMMD);
        HBox cuerpo = new HBox(14, graf, tablaCard);
        graf.setPrefWidth(350);
        graf.setMinWidth(340);
        graf.setMaxWidth(350);
        HBox.setHgrow(tablaCard, Priority.ALWAYS);
        VBox.setVgrow(cuerpo, Priority.ALWAYS);
        raiz.getChildren().setAll(new ProduccionTabs(Modulo.MRP, navigatorPMMD, calculadoraPMMD::abrir), kpisPMMD, barra, notaPMMD, cuerpo);
    }


    @Override
    public void onShow(Intent intent) {
        calcular();
    }

    /** El calculo corre en un Task; la tabla muestra un indicador mientras tanto. */

    private void calcular() {
        int token = ++consultaPMMD;
        MrpServicePMMD.Filtro f = new MrpServicePMMD.Filtro(desdePMMD.getValue(), hastaPMMD.getValue(), categoriaPMMD.getValue(),
                proveedorPMMD.getValue() == null ? null : proveedorPMMD.getValue().getId(), soloFaltantesPMMD.isSelected());
        ProgressIndicator pi = new ProgressIndicator();
        pi.setMaxSize(40, 40);
        tablaHostPMMD.getChildren().setAll(tablaPMMD, cubierta(pi));
        Task<Object[]> t = new Task<>() {

            @Override
            protected Object[] call() {
                return new Object[]{mrpPMMD.calcular(f), mrpPMMD.comprasEnTransito()};
            }
        };
        t.setOnSucceeded(e -> {
            if (token != consultaPMMD) return;
            mostrar((MrpServicePMMD.Resultado) t.getValue()[0], (Long) t.getValue()[1]);
        });
        t.setOnFailed(e -> toastPMMD.error("No se pudo calcular el requerimiento: " + t.getException().getMessage()));
        Thread h = new Thread(t, "mrp");
        h.setDaemon(true);
        h.start();
    }


    private Node cubierta(Node n) {
        StackPane s = new StackPane(n);
        s.getStyleClass().add("overlay-busy");
        return s;
    }


    private void mostrar(MrpServicePMMD.Resultado r, long transito) {
        ultimoPMMD = r;
        kpisPMMD.getChildren().setAll(UI.fila(
                new KpiCard("FACT_CHECK", "blue", "Materiales requeridos", String.valueOf(r.filas().size()), "Por las órdenes y pedidos"),
                new KpiCard("REPORT_PROBLEM", "red", "Con faltante", String.valueOf(r.conFaltante()), "Faltante neto mayor a cero"),
                new KpiCard("ACCOUNT_BALANCE_WALLET", "amber", "Valor del faltante", MoneyPMMD.fmt0(r.valorFaltante()), "A costo unitario"),
                new KpiCard("LOCAL_SHIPPING", "teal", "Compras en tránsito", String.valueOf(transito), "OC solicitadas y aprobadas")));
        notaPMMD.setText(r.ops() + " OP activas + " + r.pedidos() + (r.pedidos() == 1 ? " pedido confirmado" : " pedidos confirmados")
                + " sin orden · faltante = máx(0, requerido − stock − tránsito)");
        List<HBarChart.Fila> top = r.filas().stream().sorted(Comparator.comparingDouble(MrpServicePMMD.Fila::valorFaltante).reversed()
                        .thenComparing(f -> -f.requerido() / Math.max(0.01, f.stock() + f.transito()))).limit(10)
                .map(f -> new HBarChart.Fila(f.material().getNombre(), f.requerido(), f.stock() + f.transito(),
                        f.faltante() > 1e-6 ? "falta " + MoneyPMMD.numero(f.faltante()) + " " + f.material().getUnidad() : "cubierto", f.faltante() > 1e-6))
                .toList();
        graficoPMMD.setFilas(top);
        tablaPMMD.getItems().setAll(r.filas());
        tablaHostPMMD.getChildren().setAll(tablaPMMD);
    }


    private void construirTabla() {
        TableColumn<MrpServicePMMD.Fila, String> falt = Cells.derecha("Faltante neto", f -> MoneyPMMD.numero(f.faltante()), 100);
        falt.setCellFactory(c -> new TableCell<>() {

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(null);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                    return;
                }
                MrpServicePMMD.Fila f = getTableRow().getItem();
                setGraphic(f.faltante() > 1e-6 ? new StatusBadge(MoneyPMMD.numero(f.faltante()) + " " + f.material().getUnidad(), "red")
                        : new StatusBadge("Cubierto", "green"));
                setAlignment(Pos.CENTER_RIGHT);
            }
        });
        falt.setComparator(Comparator.comparingDouble(s -> Double.parseDouble(s.replace(",", ""))));
        tablaPMMD.getColumns().addAll(List.of(
                Cells.texto("Código", f -> f.material().getCodigo(), 66, "cell-mono"),
                Cells.texto("Material", f -> f.material().getNombre(), 200, "cell-strong"),
                num("Requerido", MrpServicePMMD.Fila::requerido),
                num("Stock", MrpServicePMMD.Fila::stock),
                num("Reservado", MrpServicePMMD.Fila::reservado),
                num("Disponible", MrpServicePMMD.Fila::disponible),
                num("Tránsito", MrpServicePMMD.Fila::transito),
                falt,
                Cells.texto("Se necesita", f -> AppClockPMMD.fmt(f.necesitaEl()), 90),
                Cells.texto("Proveedor sugerido", f -> f.proveedor() == null ? "—" : f.proveedor().getRazonSocial(), 170, "cell-muted")));
        tablaPMMD.setRowFactory(tv -> {
            TableRow<MrpServicePMMD.Fila> r = new TableRow<>();
            Tooltip tip = new Tooltip();
            r.itemProperty().addListener((o, a, b) -> {
                if (b != null) {
                    tip.setText("Origen: " + String.join(", ", b.origenes()));
                    r.setTooltip(tip);
                }
            });
            r.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !r.isEmpty()) navigatorPMMD.goTo("inventario", Intent.abrir(r.getItem().material().getCodigo()));
            });
            return r;
        });
    }


    private TableColumn<MrpServicePMMD.Fila, String> num(String titulo, java.util.function.ToDoubleFunction<MrpServicePMMD.Fila> f) {
        TableColumn<MrpServicePMMD.Fila, String> c = Cells.derecha(titulo, x -> MoneyPMMD.numero(f.applyAsDouble(x)), 74);
        c.setComparator(Comparator.comparingDouble(s -> Double.parseDouble(s.replace(",", ""))));
        return c;
    }


    private void generar() {
        if (ultimoPMMD == null) return;
        List<MrpServicePMMD.Fila> conFaltante = ultimoPMMD.filas().stream().filter(f -> f.faltante() > 1e-6).toList();
        if (conFaltante.isEmpty()) {
            toastPMMD.info("No hay faltantes: el requerimiento está cubierto por el stock y las compras en tránsito.");
            return;
        }
        long proveedoresN = conFaltante.stream().map(MrpServicePMMD.Fila::proveedor).distinct().count();
        confirmPMMD.mostrar(ConfirmDialog.Tipo.INFO, "¿Generar " + proveedoresN + " órdenes de compra?",
                "Se creará una OC en Borrador por proveedor con " + conFaltante.size() + " materiales, redondeando a la unidad de compra "
                        + "(madera de 50 en 50 pt, metros de 10 en 10, unidades de 10 en 10, kg de 5 en 5).", "Generar", () -> {
                    List<String>[] c = new List[1];
                    if (toastPMMD.intentar(() -> c[0] = mrpPMMD.generarOrdenes(conFaltante, "MRP"))) {
                        toastPMMD.exito("Órdenes de compra creadas en Borrador: " + String.join(", ", c[0]) + ".");
                        navigatorPMMD.datosCambiaron();
                        calcular();
                    }
                });
    }
}

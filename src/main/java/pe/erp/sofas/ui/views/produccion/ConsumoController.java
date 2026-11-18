package pe.erp.sofas.ui.views.produccion;

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
import pe.erp.sofas.model.ConsumoReal;
import pe.erp.sofas.model.FichaTecnica;
import pe.erp.sofas.service.ConsumoService;
import pe.erp.sofas.ui.components.*;

import java.util.*;

/** Consumo real frente al estimado: KPI, rankings, ajuste de fichas con el promedio real y tabla editable. */

@Component
@Lazy
@Scope("prototype")
@RequiredArgsConstructor
public class ConsumoController implements Vista {

    private final ConsumoService consumoPMMD;

    private final CalculadoraMateriales calculadoraPMMD;

    private final Toast toastPMMD;

    private final ConfirmDialog confirmPMMD;

    private final Navigator navigatorPMMD;


    @FXML private VBox raiz;


    private final VBox kpisPMMD = new VBox();

    private ComboBox<String> cmbOpPMMD;

    private ComboBox<String> cmbProductoPMMD;

    private Button ajustarPMMD;

    private final HBarChart trabajadoresPMMD = new HBarChart().centrado(true);

    private final HBarChart productosPMMD = new HBarChart().centrado(true);

    private final TableView<ConsumoReal> tablaPMMD = Cells.tabla("Sin consumos registrados con estos filtros.");


    @FXML
    public void initialize() {
        cmbOpPMMD = Forms.combo(List.of(), null, s -> s == null ? "Todas las OP" : s);
        cmbOpPMMD.setPrefWidth(180);
        cmbProductoPMMD = Forms.combo(List.of(), null, s -> s == null ? "Todos los productos" : s);
        cmbProductoPMMD.setPrefWidth(240);
        cmbOpPMMD.valueProperty().addListener((o, a, b) -> render());
        cmbProductoPMMD.valueProperty().addListener((o, a, b) -> render());
        ajustarPMMD = UI.primary("Ajustar ficha con el promedio real", "TUNE");
        ajustarPMMD.setOnAction(e -> ajustarFicha());
        HBox barra = UI.hbox(10, cmbOpPMMD, cmbProductoPMMD, UI.label("Edita la columna «Real» para corregir un registro.", "cell-sub"), UI.spacer(), ajustarPMMD);
        construirTabla();
        VBox tablaCard = new VBox(tablaPMMD);
        tablaCard.getStyleClass().addAll("card", "table-card");
        VBox.setVgrow(tablaPMMD, Priority.ALWAYS);
        tablaCard.setMinHeight(300);
        VBox ranking1 = UI.card(UI.cardHeader("Eficiencia por trabajador", "Desvío del consumo real · verde si consume menos, rojo si supera +10 %"), trabajadoresPMMD);
        VBox ranking2 = UI.card(UI.cardHeader("Productos con mayor desvío", "Desvío valorizado por modelo"), productosPMMD);
        VBox contenido = new VBox(14, kpisPMMD, UI.columnas(new double[]{1, 1}, ranking1, ranking2), barra, tablaCard);
        ScrollPane sp = UI.scroll(contenido);
        VBox.setVgrow(sp, Priority.ALWAYS);
        raiz.getChildren().setAll(new ProduccionTabs(Modulo.CONSUMO, navigatorPMMD, calculadoraPMMD::abrir), sp);
    }


    @Override
    public void onShow(Intent intent) {
        List<ConsumoReal> todos = consumoPMMD.registros();
        List<String> ops = new ArrayList<>();
        ops.add(null);
        todos.stream().map(ConsumoReal::getOpCodigo).distinct().sorted(Comparator.reverseOrder()).forEach(ops::add);
        List<String> prods = new ArrayList<>();
        prods.add(null);
        todos.stream().map(ConsumoReal::getProducto).distinct().sorted().forEach(prods::add);
        String op = cmbOpPMMD.getValue(), pr = cmbProductoPMMD.getValue();
        cmbOpPMMD.getItems().setAll(ops);
        cmbProductoPMMD.getItems().setAll(prods);
        cmbOpPMMD.setValue(op);
        cmbProductoPMMD.setValue(pr);
        render();
    }


    private List<ConsumoReal> filtrados() {
        return consumoPMMD.filtrar(cmbOpPMMD.getValue(), cmbProductoPMMD.getValue());
    }


    private void render() {
        List<ConsumoReal> l = filtrados();
        ConsumoService.Kpis k = consumoPMMD.kpis(l);
        kpisPMMD.getChildren().setAll(UI.fila(
                new KpiCard("COMPARE_ARROWS", k.desvioPromedioPct() > 10 ? "red" : "amber", "Desvío promedio valorizado", String.format("%+.1f %%", k.desvioPromedioPct()),
                        "Real frente a la ficha"),
                new KpiCard("CONTENT_CUT", "red", "Costo de sobremerma", MoneyPMMD.fmt0(k.costoSobremerma()), "Consumo por encima del estimado"),
                new KpiCard("BUILD", "blue", "Órdenes registradas", String.valueOf(k.ordenes()), "Con consumo real cerrado"),
                new KpiCard("WARNING", "wood", "Material con mayor desvío", k.materialMayorDesvio(), String.format("%+.1f %% sobre la ficha", k.desvioMaterialPct()))));
        trabajadoresPMMD.setFilas(consumoPMMD.porTrabajador(consumoPMMD.registros()).stream()
                .map(r -> new HBarChart.Fila(r.nombre(), r.desvioPct(), 0, String.format("%+.1f %%", r.desvioPct()), r.desvioPct() > 10)).toList());
        productosPMMD.setFilas(consumoPMMD.porProducto(consumoPMMD.registros()).stream().limit(8)
                .map(r -> new HBarChart.Fila(r.nombre(), r.desvioPct(), 0, String.format("%+.1f %%", r.desvioPct()), r.desvioPct() > 10)).toList());
        tablaPMMD.getItems().setAll(l);
        Optional<String> ficha = consumoPMMD.fichaComun(l);
        ajustarPMMD.setDisable(ficha.isEmpty());
        UI.tooltip(ajustarPMMD, ficha.map(f -> "Crea una nueva versión en Borrador de " + f + " con las cantidades × (real / estimado).")
                .orElse("Filtra por un producto u OP para ajustar su ficha."));
    }


    private void construirTabla() {
        TableColumn<ConsumoReal, ConsumoReal> real = Cells.nodo("Real", c -> {
            TextField t = Forms.decimal(c.getRealConsumido());
            t.getStyleClass().add("cell-field");
            t.setPrefWidth(84);
            Runnable commit = () -> {
                double v = Validacion.valor(t);
                if (Math.abs(v - c.getRealConsumido()) > 1e-9) {
                    if (toastPMMD.intentar(() -> consumoPMMD.editarReal(c.getId(), v))) {
                        toastPMMD.exito("Consumo real de " + c.getMaterial().getNombre() + " en " + c.getOpCodigo() + " actualizado.");
                        render();
                    }
                }
            };
            t.setOnAction(e -> commit.run());
            t.focusedProperty().addListener((o, a, b) -> {
                if (!b) commit.run();
            });
            return t;
        }, 100);
        tablaPMMD.getColumns().addAll(List.of(
                Cells.texto("OP", ConsumoReal::getOpCodigo, 76, "cell-mono"),
                Cells.texto("Producto", ConsumoReal::getProducto, 170, "cell-strong"),
                Cells.texto("Responsable", ConsumoReal::getResponsable, 150),
                Cells.nodo("Material y fecha", c -> new VBox(0, UI.label(c.getMaterial().getNombre(), "cell-title"),
                        UI.label(c.getMaterial().getCodigo() + " · " + AppClockPMMD.fmt(c.getFecha()), "cell-sub")), 190),
                Cells.derecha("Estimado", c -> MoneyPMMD.numero(c.getEstimado()) + " " + c.getMaterial().getUnidad(), 100, "cell-muted"),
                real,
                Cells.derecha("Diferencia", c -> String.format("%+.2f", c.getDiferencia()), 80),
                Cells.nodo("Dif. %", c -> {
                    double p = c.getDiferenciaPct();
                    return new StatusBadge(String.format("%+.1f %%", p), p > 10 ? "red" : p < 0 ? "green" : p > 5 ? "amber" : "gray");
                }, 80)));
        tablaPMMD.setFixedCellSize(48);
        tablaPMMD.setPrefHeight(48 * 12 + 44);
    }


    private void ajustarFicha() {
        List<ConsumoReal> l = filtrados();
        Optional<String> ficha = consumoPMMD.fichaComun(l);
        if (ficha.isEmpty()) return;
        confirmPMMD.mostrar(ConfirmDialog.Tipo.INFO, "¿Ajustar " + ficha.get() + " con el promedio real?",
                "Se creará una nueva versión en Borrador con cada cantidad × (real / estimado) de " + l.stream().map(ConsumoReal::getOpCodigo).distinct().count()
                        + " órdenes. La versión aprobada no cambia hasta que apruebes la nueva.", "Crear versión", () -> {
                    FichaTecnica[] f = new FichaTecnica[1];
                    if (toastPMMD.intentar(() -> f[0] = consumoPMMD.ajustarFicha(ficha.get(), consumoPMMD.registros()))) {
                        toastPMMD.exito(f[0].getEtiqueta() + " creada en Borrador con el consumo real promedio.");
                        navigatorPMMD.goTo("fichas", Intent.abrir(f[0].getUid()));
                    }
                });
    }
}

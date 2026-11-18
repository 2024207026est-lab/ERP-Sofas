package pe.erp.sofas.ui.views.produccion;

import javafx.animation.PauseTransition;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import pe.erp.sofas.core.*;
import pe.erp.sofas.service.CorteService;
import pe.erp.sofas.service.ExportService;
import pe.erp.sofas.service.calculo.Corte;
import pe.erp.sofas.service.calculo.Pieza;
import pe.erp.sofas.service.calculo.TipoCorte;
import pe.erp.sofas.ui.AppWindow;
import pe.erp.sofas.ui.components.*;

import javax.imageio.ImageIO;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.*;

/** Hojas de corte: plano optimizado por material, KPI de aprovechamiento, lista de piezas, retazos y PDF. */

@Component
@Lazy
@Scope("prototype")
@RequiredArgsConstructor
public class CorteController implements Vista {

    private record FilaPieza(
      List<Integer> numeros, 
        Pieza pieza, 
          double ancho, 
            double alto, 
              String origen) {
        
        String medidas() {
            return alto <= 0 ? MoneyPMMD.numero(ancho) : MoneyPMMD.numero(ancho) + " × " + MoneyPMMD.numero(alto);
        }
    }


    private final CorteService cortePMMD;

    private final ExportService exportPMMD;

    private final CalculadoraMateriales calculadoraPMMD;

    private final Overlays overlaysPMMD;

    private final Toast toastPMMD;

    private final Navigator navigatorPMMD;

    private final AppWindow appWindowPMMD;

    private final pe.erp.sofas.core.ThemeManager temaPMMD;


    @FXML private VBox raiz;


    private ComboBox<CorteService.Fuente> fuentePMMD;

    private CheckBox conjuntoPMMD;

    private SegmentedControl tiposPMMD;

    private final ListView<CorteService.MaterialCorte> materialesPMMD = new ListView<>();

    private final VBox kpisPMMD = new VBox(8);

    private final StackPane planoHostPMMD = new StackPane();

    private final VBox listaPiezasPMMD = new VBox();

    private Corte.Estrategia estrategiaPMMD = Corte.Estrategia.MEJOR_LADO;

    private Corte.Resultado resultadoPMMD;

    private Corte.Resultado1D resultado1DPMMD;

    private List<CorteService.MaterialCorte> todosPMMD = List.of();

    private final Set<Integer> cortadasPMMD = new HashSet<>();

    private Canvas canvasPMMD;


    @FXML
    public void initialize() {
        fuentePMMD = Forms.combo(List.of(), null, CorteService.Fuente::titulo);
        fuentePMMD.valueProperty().addListener((o, a, b) -> cargarFuente());
        conjuntoPMMD = new CheckBox("Optimizar junto con las demás OP activas que usan el mismo material");
        conjuntoPMMD.setWrapText(true);
        conjuntoPMMD.selectedProperty().addListener((o, a, b) -> cargarFuente());
        tiposPMMD = new SegmentedControl("Espuma", "Tela", "Madera", "Tablero");
        tiposPMMD.valorProperty().addListener((o, a, b) -> filtrarMateriales());
        materialesPMMD.setPrefHeight(150);
        materialesPMMD.setCellFactory(lv -> new ListCell<>() {

            @Override
            protected void updateItem(CorteService.MaterialCorte m, boolean empty) {
                super.updateItem(m, empty);
                setText(null);
                setGraphic(empty || m == null ? null : UI.hbox(8, new VBox(0, UI.label(m.material().getNombre(), "cell-title"),
                        UI.label(m.material().getCodigo(), "cell-sub")), UI.spacer(), new StatusBadge(m.numeroPiezas() + " piezas", "gray")));
            }
        });
        materialesPMMD.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> optimizar(false));
        Button recalcular = UI.secondary("Recalcular", "REFRESH");
        recalcular.setOnAction(e -> optimizar(true));
        Button retazo = UI.secondary("Guardar retazo", "SAVE");
        retazo.setOnAction(e -> guardarRetazo());
        Button imprimir = UI.primary("Imprimir hoja de corte", "PRINT");
        imprimir.setOnAction(e -> imprimir());
        recalcular.setMaxWidth(Double.MAX_VALUE);
        retazo.setMaxWidth(Double.MAX_VALUE);
        imprimir.setMaxWidth(Double.MAX_VALUE);
        VBox izq = new VBox(12, UI.field("OP activa o ficha (1 unidad)", fuentePMMD), conjuntoPMMD, tiposPMMD, UI.label("Materiales del tipo", "form-label"), materialesPMMD,
                kpisPMMD, recalcular, retazo, imprimir);
        izq.getStyleClass().add("card");
        izq.setPadding(new Insets(16));
        ScrollPane spIzq = UI.scroll(izq);
        spIzq.setPrefWidth(340);
        spIzq.setMinWidth(320);
        planoHostPMMD.getStyleClass().add("corte-canvas-host");
        planoHostPMMD.setAlignment(Pos.TOP_LEFT);
        VBox der = new VBox(14, planoHostPMMD, listaPiezasPMMD);
        ScrollPane spDer = UI.scroll(der);
        HBox.setHgrow(spDer, Priority.ALWAYS);
        spDer.widthProperty().addListener((o, a, b) -> redibujar());
        HBox cuerpo = new HBox(14, spIzq, spDer);
        VBox.setVgrow(cuerpo, Priority.ALWAYS);
        raiz.getChildren().setAll(new ProduccionTabs(Modulo.CORTE, navigatorPMMD, calculadoraPMMD::abrir), cuerpo);
    }


    @Override
    public void onShow(Intent intent) {
        List<CorteService.Fuente> fs = cortePMMD.fuentes();
        fuentePMMD.getItems().setAll(fs);
        CorteService.Fuente elegida = fs.isEmpty() ? null : fs.get(0);
        if (intent.dato() != null) {
            String d = intent.dato().toString();
            elegida = fs.stream().filter(f -> (f.op() != null && f.op().getCodigo().equals(d)) || (f.ficha() != null && f.ficha().getUid().equals(d)))
                    .findFirst().orElse(elegida);
        } else if (fuentePMMD.getValue() != null) {
            CorteService.Fuente actual = fuentePMMD.getValue();
            elegida = fs.stream().filter(f -> f.codigo().equals(actual.codigo())).findFirst().orElse(elegida);
        }
        fuentePMMD.setValue(null);
        fuentePMMD.setValue(elegida);
    }


    private void cargarFuente() {
        CorteService.Fuente f = fuentePMMD.getValue();
        if (f == null) return;
        conjuntoPMMD.setDisable(f.op() == null);
        todosPMMD = cortePMMD.materiales(f, conjuntoPMMD.isSelected() && f.op() != null);
        cortadasPMMD.clear();
        Set<TipoCorte> presentes = new HashSet<>();
        todosPMMD.forEach(m -> presentes.add(m.tipo()));
        int idx = 0;
        String primero = null;
        for (Node n : tiposPMMD.getChildren()) {
            TipoCorte t = TipoCorte.values()[idx++];
            n.setDisable(!presentes.contains(t));
            if (primero == null && presentes.contains(t)) primero = t.nombre();
        }
        String actual = tiposPMMD.getValor();
        boolean actualValido = presentes.stream().anyMatch(t -> t.nombre().equals(actual));
        if (!actualValido && primero != null) tiposPMMD.setValor(primero);
        else filtrarMateriales();
    }


    private TipoCorte tipoActual() {
        for (TipoCorte t : TipoCorte.values()) if (t.nombre().equals(tiposPMMD.getValor())) return t;
        return TipoCorte.ESPUMA;
    }


    private void filtrarMateriales() {
        TipoCorte t = tipoActual();
        materialesPMMD.getItems().setAll(todosPMMD.stream().filter(m -> m.tipo() == t).toList());
        if (!materialesPMMD.getItems().isEmpty()) materialesPMMD.getSelectionModel().select(0);
        else {
            planoHostPMMD.getChildren().setAll(UI.empty("CONTENT_CUT", "Sin piezas de " + t.nombre().toLowerCase(), "Esta ficha no usa ese tipo de material.", null));
            listaPiezasPMMD.getChildren().clear();
        }
    }

    /** Optimiza en un Task; "Recalcular" alterna la estrategia y muestra el overlay. */

    private void optimizar(boolean alternar) {
        CorteService.MaterialCorte mc = materialesPMMD.getSelectionModel().getSelectedItem();
        if (mc == null) return;
        if (alternar) estrategiaPMMD = estrategiaPMMD.otra();
        cortadasPMMD.clear();
        StackPane overlay = new StackPane(new VBox(10, new ProgressIndicator(), UI.label("Optimizando cortes…", "card-title")));
        ((VBox) overlay.getChildren().get(0)).setAlignment(Pos.CENTER);
        overlay.getStyleClass().add("overlay-busy");
        overlay.setMinHeight(220);
        planoHostPMMD.getChildren().add(overlay);
        Corte.Estrategia est = estrategiaPMMD;
        Task<Object> t = new Task<>() {

            @Override
            protected Object call() {
                return mc.tipo() == TipoCorte.MADERA ? cortePMMD.optimizarMadera(mc) : cortePMMD.optimizar(mc, est);
            }
        };
        t.setOnSucceeded(e -> {
            PauseTransition p = new PauseTransition(Duration.millis(alternar ? 450 : 0));
            p.setOnFinished(x -> {
                if (t.getValue() instanceof Corte.Resultado1D r1) {
                    resultado1DPMMD = r1;
                    resultadoPMMD = null;
                } else {
                    resultadoPMMD = (Corte.Resultado) t.getValue();
                    resultado1DPMMD = null;
                }
                mostrarKpis(mc);
                redibujar();
                if (alternar) toastPMMD.info("Estrategia: " + est.nombre() + (resultadoPMMD != null ? " · orden " + resultadoPMMD.orden() : "") + ".");
            });
            p.play();
        });
        t.setOnFailed(e -> toastPMMD.error("No se pudo optimizar: " + t.getException().getMessage()));
        Thread h = new Thread(t, "corte");
        h.setDaemon(true);
        h.start();
    }


    private void mostrarKpis(CorteService.MaterialCorte mc) {
        double estimado = cortePMMD.estimadoFicha(fuentePMMD.getValue(), mc.material());
        String unidad = mc.material().getUnidad();
        String usado, sobrante;
        double aprov;
        if (resultado1DPMMD != null) {
            usado = resultado1DPMMD.tablas().size() + " tablas de 300 cm";
            sobrante = MoneyPMMD.numero(resultado1DPMMD.sobranteReutilizable()) + " cm";
            aprov = resultado1DPMMD.aprovechamiento();
        } else if (mc.tipo() == TipoCorte.TELA) {
            usado = MoneyPMMD.numero(resultadoPMMD.metros()) + " m de rollo";
            sobrante = MoneyPMMD.numero(resultadoPMMD.sobranteReutilizable()) + " m²";
            aprov = resultadoPMMD.aprovechamiento();
        } else {
            usado = resultadoPMMD.laminas() + (resultadoPMMD.laminas() == 1 ? " plancha" : " planchas");
            sobrante = MoneyPMMD.numero(resultadoPMMD.sobranteReutilizable()) + " m²";
            aprov = resultadoPMMD.aprovechamiento();
        }
        String color = aprov >= 75 ? "#16A34A" : aprov >= 60 ? "#F59E0B" : "#DC2626";
        Label pct = UI.label(String.format("%.0f %%", aprov), "mini-stat-value");
        pct.setStyle("-fx-text-fill: " + color + ";");
        ProgressBarMini barra = new ProgressBarMini(aprov / 100, color, 8);
        VBox ap = new VBox(4, UI.hbox(8, UI.label("Aprovechamiento", "mini-stat-label"), UI.spacer(), pct), barra);
        ap.getStyleClass().add("mini-stat");
        kpisPMMD.getChildren().setAll(UI.hbox(8, UI.miniStat("Cantidad usada", usado), UI.miniStat("Estima la ficha", MoneyPMMD.numero(estimado) + " " + unidad)),
                UI.miniStat("Sobrante reutilizable", sobrante), ap,
                UI.label(resultadoPMMD != null ? "Estrategia: " + resultadoPMMD.estrategia().nombre() + " · " + resultadoPMMD.orden() : "FFD con kerf de 5 mm", "cell-sub"));
    }


    private boolean oscuro() {
        return temaPMMD.isOscuro();
    }


    private void redibujar() {
        if (resultadoPMMD == null && resultado1DPMMD == null) return;
        double w = Math.max(520, planoHostPMMD.getWidth() > 0 ? planoHostPMMD.getWidth() - 24 : 760);
        canvasPMMD = resultado1DPMMD != null ? PlanoCorte.dibujar1D(resultado1DPMMD, w, cortadasPMMD, oscuro()) : PlanoCorte.dibujar2D(resultadoPMMD, w, cortadasPMMD, oscuro());
        planoHostPMMD.getChildren().setAll(canvasPMMD);
        renderPiezas();
    }


    private List<FilaPieza> filas() {
        List<Corte.Colocacion> cols = new ArrayList<>();
        if (resultadoPMMD != null) cols.addAll(resultadoPMMD.colocaciones());
        if (resultado1DPMMD != null) resultado1DPMMD.tablas().forEach(t -> cols.addAll(t.cortes()));
        Map<String, List<Corte.Colocacion>> g = new LinkedHashMap<>();
        cols.stream().sorted(Comparator.comparingInt(Corte.Colocacion::numero)).forEach(c -> g.computeIfAbsent(c.pieza().origen() + "|" + c.pieza().nombre() + "|"
                + c.pieza().ancho() + "|" + c.pieza().alto(), k -> new ArrayList<>()).add(c));
        List<FilaPieza> l = new ArrayList<>();
        g.values().forEach(v -> l.add(new FilaPieza(v.stream().map(Corte.Colocacion::numero).toList(), v.get(0).pieza(), v.get(0).pieza().ancho(),
                v.get(0).pieza().alto(), v.get(0).pieza().origen())));
        l.sort(Comparator.comparingInt(f -> f.numeros().get(0)));
        return l;
    }


    private void renderPiezas() {
        GridPane g = new GridPane();
        g.setHgap(12);
        g.setVgap(2);
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setHgrow(Priority.ALWAYS);
        g.getColumnConstraints().addAll(new ColumnConstraints(70), c1, new ColumnConstraints(120), new ColumnConstraints(50), new ColumnConstraints(90), new ColumnConstraints(70));
        g.addRow(0, UI.label("N.º", "dato-label"), UI.label("PIEZA", "dato-label"), UI.label("MEDIDAS (CM)", "dato-label"), UI.label("CANT.", "dato-label"),
                UI.label("ORIGEN", "dato-label"), UI.label("CORTADA", "dato-label"));
        int fila = 1;
        for (FilaPieza f : filas()) {
            Region punto = new Region();
            punto.getStyleClass().add("legend-dot");
            punto.setStyle("-fx-background-color: " + f.pieza().componente().color() + ";");
            CheckBox ck = new CheckBox();
            boolean todas = cortadasPMMD.containsAll(f.numeros());
            ck.setSelected(todas);
            List<Node> celdas = List.of(UI.label(PlanoCorte.rango(f.numeros()), "mono"), UI.hbox(8, punto, UI.label(f.pieza().nombre(), "cell-title")),
                    UI.label(f.medidas(), "cell-sub"), UI.label(String.valueOf(f.numeros().size()), "cell-title"), UI.label(f.origen(), "code"), ck);
            ck.selectedProperty().addListener((o, a, b) -> {
                if (b) cortadasPMMD.addAll(f.numeros());
                else cortadasPMMD.removeAll(f.numeros());
                redibujar();
            });
            int r = fila++;
            for (int i = 0; i < celdas.size(); i++) {
                g.add(celdas.get(i), i, r);
                if (todas) celdas.get(i).getStyleClass().add("pieza-cortada");
            }
        }
        listaPiezasPMMD.getChildren().setAll(UI.card(UI.cardHeader("Lista de piezas", "Marca cada pieza cuando esté cortada"), g));
    }


    private void guardarRetazo() {
        CorteService.MaterialCorte mc = materialesPMMD.getSelectionModel().getSelectedItem();
        if (mc == null) return;
        double cant;
        String detalle;
        if (resultado1DPMMD != null) {
            double cm = resultado1DPMMD.sobranteReutilizable();
            cant = Math.round(cm / 300 * 5 * 100) / 100.0;
            detalle = MoneyPMMD.numero(cm) + " cm de tabla";
        } else {
            double m2 = resultadoPMMD.sobranteReutilizable();
            cant = switch (mc.tipo()) {
                case ESPUMA -> m2 / 2.0;
                case TABLERO -> m2 / 2.9768;
                default -> m2 / 1.4;
            };
            cant = Math.round(cant * 100) / 100.0;
            detalle = MoneyPMMD.numero(m2) + " m² de " + mc.tipo().nombre().toLowerCase();
        }
        double c = cant;
        String d = detalle;
        if (toastPMMD.intentar(() -> cortePMMD.guardarRetazo(mc.material(), c, fuentePMMD.getValue().codigo(), d))) {
            toastPMMD.exito("Retazo guardado en el kardex de " + mc.material().getCodigo() + ": " + d + ".");
        }
    }


    private void imprimir() {
        CorteService.MaterialCorte mc = materialesPMMD.getSelectionModel().getSelectedItem();
        if (mc == null || canvasPMMD == null) return;
        File f = Archivos.guardar(raiz, "Hoja-de-corte-" + fuentePMMD.getValue().codigo() + "-" + mc.material().getCodigo(), "PDF", "pdf");
        if (f == null) return;
        boolean oscuro = oscuro();
        Canvas paraPdf = resultado1DPMMD != null ? PlanoCorte.dibujar1D(resultado1DPMMD, 1000, Set.of(), false) : PlanoCorte.dibujar2D(resultadoPMMD, 1000, Set.of(), false);
        SnapshotParameters sp = new SnapshotParameters();
        sp.setFill(Color.WHITE);
        byte[] png;
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(Snapshot.imagen(paraPdf.snapshot(sp, null)), "png", out);
            png = out.toByteArray();
        } catch (Exception e) {
            png = null;
        }
        byte[] imagen = png;
        List<ExportService.FilaCorte> piezas = filas().stream().map(x -> new ExportService.FilaCorte(PlanoCorte.rango(x.numeros()), x.pieza().nombre(),
                x.medidas(), x.numeros().size(), x.origen())).toList();
        String resumen = (resultado1DPMMD != null ? resultado1DPMMD.tablas().size() + " tablas" : resultadoPMMD.tipo() == TipoCorte.TELA
                ? MoneyPMMD.numero(resultadoPMMD.metros()) + " m de rollo" : resultadoPMMD.laminas() + " planchas")
                + String.format(" · aprovechamiento %.0f %%", resultado1DPMMD != null ? resultado1DPMMD.aprovechamiento() : resultadoPMMD.aprovechamiento());
        Tareas.ejecutar(overlaysPMMD, toastPMMD, "Generando hoja de corte…", () -> {
            exportPMMD.hojaCorte("Hoja de corte · " + mc.material().getNombre(), fuentePMMD.getValue().titulo() + " · " + mc.tipo().formato(), resumen, imagen, piezas, f);
            return f;
        }, a -> {
            toastPMMD.exito("Hoja de corte guardada: " + a.getName());
            appWindowPMMD.hostServices().showDocument(a.toURI().toString());
        });
        if (oscuro) redibujar();
    }
}

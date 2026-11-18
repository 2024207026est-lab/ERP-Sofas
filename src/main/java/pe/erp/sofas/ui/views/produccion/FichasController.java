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
import pe.erp.sofas.model.*;
import pe.erp.sofas.service.*;
import pe.erp.sofas.service.calculo.Explosion;
import pe.erp.sofas.service.calculo.Medidas;
import pe.erp.sofas.service.calculo.Opciones;
import pe.erp.sofas.ui.AppWindow;
import pe.erp.sofas.ui.components.*;

import java.io.File;
import java.util.*;
import java.util.function.Consumer;

/**
 * Fichas tecnicas en tres columnas: lista, editor de la lista de materiales y mano de obra, y resumen de costos.
 * Si la ficha no es Borrador, editar una celda crea automaticamente la siguiente version en Borrador.
 */

@Component
@Lazy
@Scope("prototype")
@RequiredArgsConstructor
public class FichasController implements Vista {

    private final FichaService fichaServicePMMD;

    private final ProductoService productoServicePMMD;

    private final InventarioService inventarioServicePMMD;

    private final ExportService exportServicePMMD;

    private final ParametroService parametrosPMMD;

    private final CalculadoraMateriales calculadoraPMMD;

    private final Overlays overlaysPMMD;

    private final Toast toastPMMD;

    private final ConfirmDialog confirmPMMD;

    private final Navigator navigatorPMMD;

    private final AppWindow appWindowPMMD;


    @FXML private VBox raiz;


    private SearchField buscadorPMMD;

    private final ChipFilterBar chipsPMMD = new ChipFilterBar();

    private final VBox listaPMMD = new VBox(8);

    private final VBox editorPMMD = new VBox(14);

    private final VBox resumenPMMD = new VBox(14);

    private Long seleccionPMMD;

    private TipoTela telaJuegoPMMD;


    @FXML
    public void initialize() {
        buscadorPMMD = new SearchField("Buscar ficha o modelo");
        buscadorPMMD.textProperty().addListener((o, a, b) -> renderLista());
        chipsPMMD.setChips(List.of(new ChipFilterBar.Chip("*", "Todas", -1), new ChipFilterBar.Chip("APROBADA", "Aprobada", -1),
                new ChipFilterBar.Chip("BORRADOR", "Borrador", -1), new ChipFilterBar.Chip("OBSOLETA", "Obsoleta", -1)));
        chipsPMMD.seleccionProperty().addListener((o, a, b) -> renderLista());
        Button nueva = UI.button("Nueva ficha", "ADD", "btn-sm", "btn-secondary");
        nueva.setOnAction(e -> nuevaFicha());
        ScrollPane spLista = UI.scroll(listaPMMD);
        VBox.setVgrow(spLista, Priority.ALWAYS);
        VBox izq = new VBox(10, UI.hbox(8, UI.label("Fichas", "card-title"), UI.spacer(), nueva), buscadorPMMD, chipsPMMD, spLista);
        izq.getStyleClass().add("card");
        izq.setPadding(new javafx.geometry.Insets(14));
        izq.setPrefWidth(236);
        izq.setMinWidth(236);
        izq.setMaxWidth(236);
        ScrollPane spEditor = UI.scroll(editorPMMD);
        spEditor.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        ScrollPane spResumen = UI.scroll(resumenPMMD);
        spResumen.setPrefWidth(268);
        spResumen.setMinWidth(268);
        spResumen.setMaxWidth(268);
        HBox cuerpo = new HBox(14, izq, spEditor, spResumen);
        HBox.setHgrow(spEditor, Priority.ALWAYS);
        VBox.setVgrow(cuerpo, Priority.ALWAYS);
        raiz.getChildren().setAll(new ProduccionTabs(Modulo.FICHAS, navigatorPMMD, calculadoraPMMD::abrir), cuerpo);
    }


    @Override
    public void onShow(Intent intent) {
        if (intent.es(Intent.ACCION_ABRIR) && intent.dato() != null) {
            fichaServicePMMD.porUid(intent.dato().toString()).ifPresent(f -> seleccionPMMD = f.getId());
        } else if (intent.es(Intent.ACCION_PRESELECCIONAR) && intent.dato() != null) {
            productoServicePMMD.porCodigo(intent.dato().toString()).ifPresent(p -> {
                FichaTecnica f = fichaServicePMMD.ultimaDe(p);
                if (f != null) seleccionPMMD = f.getId();
                else toastPMMD.info(p.getNombre() + " no tiene ficha: usa «Nueva ficha» para crearla.");
            });
        }
        if (seleccionPMMD == null) {
            FichaTecnica f = fichaServicePMMD.aprobada("FT-001");
            if (f != null) seleccionPMMD = f.getId();
        }
        renderLista();
        renderEditor();
    }


    private void seleccionar(Long id) {
        seleccionPMMD = id;
        renderLista();
        renderEditor();
    }

    // ================================================================ lista


    private void renderLista() {
        String q = BusquedaService.normalizar(buscadorPMMD.getText());
        String est = chipsPMMD.getSeleccion();
        listaPMMD.getChildren().clear();
        for (FichaTecnica f : fichaServicePMMD.todas()) {
            if (est != null && !est.equals("*") && !f.getEstado().name().equals(est)) continue;
            if (!q.isEmpty() && !BusquedaService.normalizar(f.getCodigo() + " " + f.getNombre()).contains(q)) continue;
            double costo = costoSeguro(f);
            VBox c = new VBox(4,
                    UI.hbox(6, UI.codigo(f.getEtiqueta()), UI.spacer(), new StatusBadge(f.getEstado().nombre())),
                    UI.label(f.getNombre(), "cell-title"),
                    UI.hbox(6, Boolean.TRUE.equals(f.getCompuesta()) ? new StatusBadge("Compuesta") : new Region(), UI.spacer(),
                            UI.label(costo > 0 ? MoneyPMMD.fmt(costo) : "—", "cell-sub")));
            c.getStyleClass().addAll("card", "ficha-card");
            if (f.getId().equals(seleccionPMMD)) c.getStyleClass().add("ficha-card-selected");
            c.setOnMouseClicked(e -> seleccionar(f.getId()));
            listaPMMD.getChildren().add(c);
        }
        if (listaPMMD.getChildren().isEmpty()) listaPMMD.getChildren().add(UI.label("Sin fichas con ese filtro.", "muted"));
    }


    private double costoSeguro(FichaTecnica f) {
        try {
            return fichaServicePMMD.costoEstandar(f);
        } catch (RuntimeException e) {
            return 0;
        }
    }

    // ================================================================ editor


    private void renderEditor() {
        editorPMMD.getChildren().clear();
        resumenPMMD.getChildren().clear();
        if (seleccionPMMD == null) {
            editorPMMD.getChildren().add(UI.empty("DESCRIPTION", "Elige una ficha", "Selecciona una ficha de la lista para ver su BOM.", null));
            return;
        }
        FichaTecnica f = fichaServicePMMD.ficha(seleccionPMMD);
        Explosion.Resultado base = fichaServicePMMD.explotar(f);
        Explosion.Resultado conOpcionales = fichaServicePMMD.explotar(f, 1, new Opciones(null, Map.of(), List.of(), true, 0, 0));

        StackPane icono = new StackPane(Ico.of(Boolean.TRUE.equals(f.getCompuesta()) ? "WIDGETS" : "WEEKEND", 22));
        icono.getStyleClass().addAll("kpi-icon", "kpi-wood");
        String aprobado = f.getAprobadoPor() == null ? "Sin aprobar" : "Aprobada por " + f.getAprobadoPor() + " el " + AppClockPMMD.fmt(f.getFechaAprobacion());
        VBox titulo = new VBox(3, UI.hbox(8, UI.label(f.getNombre(), "card-title"), new StatusBadge(f.getEstado().nombre())),
                UI.label(f.getCodigo() + " · versión " + f.getVersion() + " · " + f.getLargo() + " × " + f.getFondo() + " × " + f.getAlto() + " cm · "
                        + f.getCuerpos() + (f.getCuerpos() == 1 ? " cuerpo" : " cuerpos") + (Boolean.TRUE.equals(f.getChaise()) ? " · con chaise" : ""), "cell-sub"),
                UI.label(aprobado, "cell-sub"));
        Button simular = UI.button("Simular medida", "STRAIGHTEN", "btn-sm", "btn-secondary");
        simular.setDisable(Boolean.TRUE.equals(f.getCompuesta()));
        simular.setOnAction(e -> simular(f));
        Button duplicar = UI.button("Duplicar como nueva versión", "CONTENT_COPY", "btn-sm", "btn-secondary");
        duplicar.setOnAction(e -> {
            FichaTecnica[] n = new FichaTecnica[1];
            if (toastPMMD.intentar(() -> n[0] = fichaServicePMMD.duplicar(f.getId()))) {
                toastPMMD.exito(n[0].getEtiqueta() + " creada en Borrador a partir de " + f.getEtiqueta() + ".");
                seleccionar(n[0].getId());
            }
        });
        Button aprobar = UI.button("Aprobar", "VERIFIED", "btn-sm", "btn-success");
        aprobar.setDisable(f.getEstado() == EstadoFicha.APROBADA);
        aprobar.setOnAction(e -> confirmPMMD.mostrar(ConfirmDialog.Tipo.INFO, "¿Aprobar " + f.getEtiqueta() + "?",
                "Será la versión vigente para nuevas órdenes, cotizaciones y el costo estándar. La versión aprobada anterior pasará a Obsoleta; "
                        + "las órdenes en curso conservan su versión.", "Aprobar", () -> {
                    if (toastPMMD.intentar(() -> fichaServicePMMD.aprobar(f.getId()))) {
                        toastPMMD.exito(f.getEtiqueta() + " aprobada.");
                        seleccionar(f.getId());
                    }
                }));
        Button obsoleta = UI.button("Marcar obsoleta", "BLOCK", "btn-sm", "btn-ghost");
        obsoleta.setDisable(f.getEstado() == EstadoFicha.OBSOLETA);
        obsoleta.setOnAction(e -> confirmPMMD.mostrar("¿Marcar " + f.getEtiqueta() + " como obsoleta?",
                "Dejará de usarse para nuevas órdenes y cotizaciones" + (f.getEstado() == EstadoFicha.APROBADA ? "; el modelo quedará sin ficha aprobada." : "."),
                "Marcar obsoleta", () -> {
                    if (toastPMMD.intentar(() -> fichaServicePMMD.marcarObsoleta(f.getId()))) {
                        toastPMMD.exito(f.getEtiqueta() + " marcada como obsoleta.");
                        seleccionar(f.getId());
                    }
                }));
        Button pdf = UI.button("Exportar ficha (PDF)", "PICTURE_AS_PDF", "btn-sm", "btn-ghost");
        pdf.setOnAction(e -> exportar(f, base));
        HBox cab = UI.hbox(12, icono, titulo);
        FlowPane acciones = new FlowPane(8, 8, simular, duplicar, aprobar, obsoleta, pdf);
        VBox card = UI.card(cab, acciones);
        editorPMMD.getChildren().add(card);
        if (!f.isEditable()) {
            editorPMMD.getChildren().add(UI.banner("lock-banner", "LOCK", "Esta versión está " + f.getEstado().nombre().toLowerCase()
                    + ": al editar cualquier celda se crea automáticamente la versión " + (maxVersion(f) + 1) + " en Borrador con el cambio. "
                    + "Las órdenes en curso conservan su versión."));
        }
        if (f.getNotas() != null) editorPMMD.getChildren().add(UI.wrap(f.getNotas(), "muted"));

        if (Boolean.TRUE.equals(f.getCompuesta())) editorPMMD.getChildren().add(compuesta(f, base));
        else {
            editorPMMD.getChildren().add(bom(f, conOpcionales));
            editorPMMD.getChildren().add(manoObra(f, base));
        }
        renderResumen(f, base);
    }


    private int maxVersion(FichaTecnica f) {
        return fichaServicePMMD.todas().stream().filter(x -> x.getCodigo().equals(f.getCodigo())).mapToInt(FichaTecnica::getVersion).max().orElse(f.getVersion());
    }

    /** Aplica una edicion; si se creo una version nueva, la selecciona y lo avisa. */

    private void aplicar(java.util.function.Supplier<FichaService.Edicion> edicion, String descripcion) {
        FichaService.Edicion[] r = new FichaService.Edicion[1];
        if (toastPMMD.intentar(() -> r[0] = edicion.get())) {
            if (r[0].nuevaVersion()) {
                toastPMMD.advertencia("Se creó " + r[0].ficha().getEtiqueta() + " en Borrador con el cambio (" + descripcion + "). La versión anterior no se modificó.");
            }
            seleccionar(r[0].ficha().getId());
        }
    }


    private Node bom(FichaTecnica f, Explosion.Resultado r) {
        VBox v = new VBox(6);
        v.getStyleClass().add("card");
        v.setPadding(new javafx.geometry.Insets(16));
        v.getChildren().add(UI.cardHeader("Lista de materiales (BOM)", f.getLineas().size() + " líneas · agrupadas por componente"));
        HBox head = fila(UI.label("MATERIAL"), UI.label("BASE"), UI.label("MERMA"), UI.label("FÓRMULA"),
                UI.label("BRUTO"), UI.label("COSTO"), UI.label("OPC."), UI.label(""));
        head.getStyleClass().setAll("bom-head");
        v.getChildren().add(head);
        Map<Componente, List<Integer>> grupos = new LinkedHashMap<>();
        for (Componente c : Componente.values()) grupos.put(c, new ArrayList<>());
        for (int i = 0; i < f.getLineas().size(); i++) grupos.get(f.getLineas().get(i).getComponente()).add(i);
        for (var g : grupos.entrySet()) {
            if (g.getValue().isEmpty()) continue;
            double total = g.getValue().stream().mapToDouble(i -> r.lineas().get(i).costo()).sum();
            Region punto = new Region();
            punto.getStyleClass().add("legend-dot");
            punto.setStyle("-fx-background-color: " + g.getKey().color() + ";");
            HBox gh = UI.hbox(8, punto, UI.label(g.getKey().nombre(), "cell-title"), UI.label(g.getValue().size() + (g.getValue().size() == 1 ? " línea" : " líneas"), "cell-sub"),
                    UI.spacer(), UI.label(MoneyPMMD.fmt(total), "cell-title"));
            gh.getStyleClass().add("bom-group");
            v.getChildren().add(gh);
            for (int i : g.getValue()) v.getChildren().add(filaBom(f, i, r.lineas().get(i)));
        }
        // fila para agregar
        ComboBox<Material> mat = Forms.combo(inventarioServicePMMD.materiales(), null, m -> m.getCodigo() + " · " + m.getNombre());
        mat.setPromptText("Material");
        ComboBox<Componente> comp = Forms.combo(List.of(Componente.values()), Componente.TAPIZADO, Componente::nombre);
        TextField cant = Forms.decimal(1);
        cant.setPrefWidth(80);
        Button agregar = UI.button("Agregar línea", "ADD", "btn-sm", "btn-primary");
        agregar.disableProperty().bind(mat.valueProperty().isNull());
        agregar.setOnAction(e -> {
            Material m = mat.getValue();
            String tipo = m.getCategoria().equals("Madera") ? "Madera" : m.getCategoria().equals("Espuma") ? "Espuma" : m.isTela() ? "Tela"
                    : m.getCategoria().equals("Tableros") ? "Tableros" : "Insumos";
            double merma = m.isEntera() ? 0 : parametrosPMMD.merma(tipo);
            aplicar(() -> fichaServicePMMD.agregarLinea(f.getId(), m.getId(), comp.getValue(), Validacion.valor(cant), merma,
                    m.isEntera() ? Dependencia.F : Dependencia.L), "línea agregada");
        });
        HBox add = UI.hbox(8, mat, comp, cant, agregar);
        HBox.setHgrow(mat, Priority.ALWAYS);
        add.setStyle("-fx-padding: 10 0 0 0;");
        v.getChildren().add(add);
        return v;
    }


    private HBox fila(Node... n) {
        double[] w = {-1, 58, 50, 104, 66, 72, 26, 28};
        HBox h = new HBox(5);
        h.setAlignment(Pos.CENTER_LEFT);
        for (int i = 0; i < n.length; i++) {
            Node x = n[i];
            if (x instanceof Region r) {
                if (w[i] > 0) {
                    r.setPrefWidth(w[i]);
                    r.setMinWidth(w[i]);
                    r.setMaxWidth(w[i]);
                } else {
                    HBox.setHgrow(r, Priority.ALWAYS);
                    r.setMaxWidth(Double.MAX_VALUE);
                    r.setMinWidth(108);
                }
            }
            h.getChildren().add(x);
        }
        h.getStyleClass().add("bom-row");
        return h;
    }


    private Node filaBom(FichaTecnica f, int i, Explosion.Linea calc) {
        LineaBOM l = f.getLineas().get(i);
        Material m = l.getMaterial();
        VBox mat = new VBox(0, UI.label(m.getNombre(), "cell-title"), UI.label(m.getCodigo() + " · stock " + MoneyPMMD.numero(m.getStock()) + " " + m.getUnidad(), "cell-sub"));
        TextField cant = celda(l.getCantidad(), v -> aplicar(() -> fichaServicePMMD.editarLinea(f.getId(), i, x -> x.setCantidad(v)), "cantidad de " + m.getNombre()));
        TextField merma = celda(l.getMermaPct(), v -> aplicar(() -> fichaServicePMMD.editarLinea(f.getId(), i, x -> x.setMermaPct(v)), "merma de " + m.getNombre()));
        ComboBox<Dependencia> dep = Forms.combo(List.of(Dependencia.values()), l.getDependencia(), Dependencia::nombre);
        dep.getStyleClass().add("cell-field");
        dep.valueProperty().addListener((o, a, b) -> {
            if (b != null && b != l.getDependencia()) aplicar(() -> fichaServicePMMD.editarLinea(f.getId(), i, x -> x.setDependencia(b)), "fórmula de " + m.getNombre());
        });
        Tooltip.install(dep, new Tooltip(l.getDependencia().expresion()));
        CheckBox opc = new CheckBox();
        opc.setSelected(Boolean.TRUE.equals(l.getOpcional()));
        opc.selectedProperty().addListener((o, a, b) -> aplicar(() -> fichaServicePMMD.editarLinea(f.getId(), i, x -> x.setOpcional(b)), "opcional"));
        Button quitar = UI.iconButton("DELETE_OUTLINE", "Quitar línea", "btn-ghost", "btn-xs");
        quitar.setOnAction(e -> confirmPMMD.mostrar("¿Quitar " + m.getNombre() + " de la ficha?",
                f.isEditable() ? "La línea se elimina de " + f.getEtiqueta() + "." : "Se creará la siguiente versión en Borrador sin esta línea.",
                "Quitar", () -> aplicar(() -> fichaServicePMMD.quitarLinea(f.getId(), i), "línea quitada")));
        Label bruto = UI.label(MoneyPMMD.numero(calc.bruta()), "cell-sub");
        Label costo = UI.label(MoneyPMMD.fmt(calc.costo()), Boolean.TRUE.equals(l.getOpcional()) ? "cell-sub" : "cell-title");
        Tooltip.install(cant, new Tooltip("Cantidad base en " + m.getUnidad() + " para " + f.getLargo() + " × " + f.getFondo() + " × " + f.getAlto() + " cm"));
        bruto.setText(MoneyPMMD.numero(calc.bruta()) + " " + m.getUnidad());
        HBox h = fila(mat, cant, merma, dep, bruto, costo, opc, quitar);
        if (Boolean.TRUE.equals(l.getOpcional())) h.setOpacity(0.7);
        return h;
    }


    private TextField celda(double valor, Consumer<Double> alCambiar) {
        TextField t = Forms.decimal(valor);
        t.getStyleClass().add("cell-field");
        Runnable commit = () -> {
            double v = Validacion.valor(t);
            if (Math.abs(v - valor) > 1e-9 && v >= 0) alCambiar.accept(v);
        };
        t.setOnAction(e -> commit.run());
        t.focusedProperty().addListener((o, a, b) -> {
            if (!b) commit.run();
        });
        return t;
    }


    private Node manoObra(FichaTecnica f, Explosion.Resultado r) {
        VBox v = new VBox(6);
        v.getStyleClass().add("card");
        v.setPadding(new javafx.geometry.Insets(16));
        v.getChildren().add(UI.cardHeader("Mano de obra", MoneyPMMD.numero(f.getHorasMO()) + " horas · " + MoneyPMMD.fmt(Explosion.costoManoObra(f))));
        GridPane g = new GridPane();
        g.setHgap(12);
        g.setVgap(4);
        ColumnConstraints c0 = new ColumnConstraints();
        c0.setHgrow(Priority.ALWAYS);
        g.getColumnConstraints().addAll(c0, new ColumnConstraints(90), new ColumnConstraints(110), new ColumnConstraints(100));
        g.addRow(0, UI.label("ETAPA", "dato-label"), UI.label("HORAS", "dato-label"), UI.label("COSTO / HORA", "dato-label"), UI.label("COSTO", "dato-label"));
        for (int i = 0; i < f.getOperaciones().size(); i++) {
            OperacionMO o = f.getOperaciones().get(i);
            int idx = i;
            TextField h = celda(o.getHoras(), val -> aplicar(() -> fichaServicePMMD.editarHoras(f.getId(), idx, val), "horas de " + o.getEtapa().nombre()));
            g.addRow(i + 1, UI.label(o.getEtapa().nombre(), "cell-title"), h, UI.label("S/ " + MoneyPMMD.numero(o.getCostoHora()) + " por hora", "cell-sub"),
                    UI.label(MoneyPMMD.fmt(o.getCosto()), "cell-title"));
        }
        v.getChildren().add(g);
        return v;
    }


    private Node compuesta(FichaTecnica f, Explosion.Resultado r) {
        VBox v = new VBox(12);
        VBox hijas = new VBox(8);
        for (FichaTecnica h : fichaServicePMMD.hijas(f)) {
            Hyperlink link = new Hyperlink(h.getEtiqueta());
            link.setOnAction(e -> seleccionar(h.getId()));
            HBox c = UI.hbox(10, UI.label(h.getNombre(), "cell-title"), link, UI.spacer(), new StatusBadge(h.getEstado().nombre()),
                    UI.label(MoneyPMMD.fmt(fichaServicePMMD.costoEstandar(h)), "cell-title"));
            c.getStyleClass().add("card");
            c.setPadding(new javafx.geometry.Insets(10, 14, 10, 14));
            hijas.getChildren().add(c);
        }
        ComboBox<TipoTela> tela = Forms.combo(List.of(TipoTela.values()), telaJuegoPMMD, TipoTela::nombre);
        tela.setPromptText("Según cada ficha");
        tela.valueProperty().addListener((o, a, b) -> {
            telaJuegoPMMD = b;
            renderEditor();
        });
        Explosion.Resultado cons = r;
        if (telaJuegoPMMD != null) {
            Map<String, String> sust = new HashMap<>();
            for (TipoTela t : TipoTela.values()) sust.put(t.materialCodigo(), telaJuegoPMMD.materialCodigo());
            cons = fichaServicePMMD.explotar(f, 1, Opciones.sustituir(sust));
        }
        TableView<Explosion.PorMaterial> t = Cells.tabla("Sin materiales.");
        t.getColumns().addAll(List.of(
                Cells.texto("Código", pm -> pm.material().getCodigo(), 80, "cell-mono"),
                Cells.texto("Material consolidado", pm -> pm.material().getNombre(), 260, "cell-strong"),
                Cells.derecha("Cantidad", pm -> MoneyPMMD.numero(pm.cantidad()) + " " + pm.material().getUnidad(), 120),
                Cells.derecha("Costo", pm -> MoneyPMMD.fmt(pm.costo()), 110)));
        t.getItems().setAll(cons.porMaterial());
        t.setFixedCellSize(40);
        t.setPrefHeight(40 * cons.porMaterial().size() + 44);
        VBox tablaCard = new VBox(t);
        tablaCard.getStyleClass().addAll("card", "table-card");
        v.getChildren().addAll(UI.label("Fichas hijas (versiones aprobadas)", "card-title"), hijas,
                UI.hbox(10, UI.label("Misma tela para todo el juego", "form-label"), tela), tablaCard);
        return v;
    }

    // ================================================================ resumen


    private void renderResumen(FichaTecnica f, Explosion.Resultado r) {
        double precio = f.getProducto() == null ? 0 : f.getProducto().getPrecio().doubleValue();
        double costo = r.costoEstandar();
        double margen = precio == 0 ? 0 : (precio - costo) * 100 / precio;
        TextField ind = celda(f.getIndirectosPct(), v -> aplicar(() -> fichaServicePMMD.editarIndirectos(f.getId(), v), "indirectos"));
        ind.setPrefWidth(70);
        ind.setDisable(Boolean.TRUE.equals(f.getCompuesta()) && false);
        VBox costos = UI.card(UI.label("Resumen de costos", "card-title"),
                filaResumen("Costo de materiales", MoneyPMMD.fmt(r.costoMateriales())),
                filaResumen("Mano de obra (" + MoneyPMMD.numero(r.horasMO()) + " h)", MoneyPMMD.fmt(r.costoMO())),
                UI.hbox(8, minimo(UI.label("Indirectos", "muted")), ind, minimo(UI.label("%", "muted")), UI.spacer(), minimo(UI.label(MoneyPMMD.fmt(r.indirectos()), "cell-title"))),
                totalResumen("Costo estándar", MoneyPMMD.fmt(costo)),
                filaResumen("Precio de venta", MoneyPMMD.fmt(precio)),
                UI.hbox(8, UI.label("Margen", "muted"), UI.spacer(), new StatusBadge(String.format("%.1f %%", margen), margen >= parametrosPMMD.margenObjetivo() ? "green" : "red")));
        if (margen < parametrosPMMD.margenObjetivo() && precio > 0) {
            costos.getChildren().add(UI.banner("danger-banner", "WARNING", "Margen bajo el " + (int) parametrosPMMD.margenObjetivo()
                    + " %. Precio sugerido: " + MoneyPMMD.fmt0(costo / 0.65) + " (costo / 0.65)."));
        }
        var comp = fichaServicePMMD.composicion(r).stream().map(c -> new DonutChart.Segmento(c.nombre(), c.valor(), c.color())).toList();
        VBox dona = UI.card(UI.label("Composición del costo", "card-title"), new DonutChart(comp, 150, MoneyPMMD.corto(costo), "costo", true));
        List<OrdenProduccion> usos = fichaServicePMMD.usos(f);
        VBox uso = UI.card(UI.label("Uso de esta versión", "card-title"));
        if (usos.isEmpty()) uso.getChildren().add(UI.label("Ninguna orden activa usa " + f.getEtiqueta() + ".", "muted"));
        for (OrdenProduccion o : usos) {
            Hyperlink l = new Hyperlink(o.getCodigo() + " · " + o.getProductoNombre() + " ×" + o.getCantidad());
            l.setOnAction(e -> navigatorPMMD.goTo("produccion", Intent.abrir(o.getCodigo())));
            uso.getChildren().add(UI.hbox(6, l, UI.spacer(), UI.label(o.getEtapa().nombre(), "cell-sub")));
        }
        resumenPMMD.getChildren().setAll(costos, dona, uso);
    }


    private static Label minimo(Label l) {
        l.setMinWidth(Region.USE_PREF_SIZE);
        return l;
    }


    private HBox filaResumen(String a, String b) {
        HBox h = UI.hbox(8, UI.label(a, "muted"), UI.spacer(), UI.label(b, "cell-title"));
        h.getStyleClass().add("resumen-row");
        return h;
    }


    private HBox totalResumen(String a, String b) {
        HBox h = UI.hbox(8, UI.label(a), UI.spacer(), UI.label(b));
        h.getStyleClass().addAll("resumen-row", "resumen-total");
        return h;
    }

    // ================================================================ acciones


    private void simular(FichaTecnica f) {
        TextField largo = Forms.entero(f.getLargo()), fondo = Forms.entero(f.getFondo()), alto = Forms.entero(f.getAlto());
        Spinner<Integer> piezas = new Spinner<>(1, 50, 1);
        piezas.setEditable(true);
        VBox tabla = new VBox(4);
        Runnable calc = () -> {
            int L = Forms.entero(largo), F = Forms.entero(fondo), A = Forms.entero(alto);
            if (L < 30 || F < 30 || A < 30) return;
            FichaService.Simulacion s = fichaServicePMMD.simular(f, new Medidas(L, F, A, f.getCuerpos()), piezas.getValue());
            GridPane g = new GridPane();
            g.setHgap(12);
            g.setVgap(3);
            ColumnConstraints c0 = new ColumnConstraints();
            c0.setHgrow(Priority.ALWAYS);
            g.getColumnConstraints().addAll(c0, new ColumnConstraints(90), new ColumnConstraints(90), new ColumnConstraints(90));
            g.addRow(0, UI.label("MATERIAL", "dato-label"), UI.label("FICHA BASE", "dato-label"), UI.label("SIMULADO", "dato-label"), UI.label("DIFERENCIA", "dato-label"));
            int i = 1;
            for (var fila : s.filas()) {
                Label dif = UI.label(String.format("%+.2f", fila.diferencia()), "cell-title");
                if (fila.diferencia() > 1e-6) dif.setStyle("-fx-text-fill: -is-warn-ink;");
                else if (fila.diferencia() < -1e-6) dif.setStyle("-fx-text-fill: -is-success-ink;");
                g.addRow(i++, UI.label(fila.material().getNombre() + " · " + fila.componente().nombre(), "cell-sub"),
                        UI.label(MoneyPMMD.numero(fila.base()) + " " + fila.material().getUnidad(), "cell-sub"),
                        UI.label(MoneyPMMD.numero(fila.simulado()), "cell-title"), dif);
            }
            double cb = s.base().costoEstandar(), cs = s.simulado().costoEstandar();
            Label difC = UI.label(String.format("%+.1f %%", cb == 0 ? 0 : (cs - cb) * 100 / cb), "mini-stat-value");
            difC.setStyle(cs > cb ? "-fx-text-fill: -is-warn-ink;" : "-fx-text-fill: -is-success-ink;");
            VBox dv = new VBox(2, difC, UI.label("Diferencia", "mini-stat-label"));
            dv.getStyleClass().add("mini-stat");
            HBox.setHgrow(dv, Priority.ALWAYS);
            tabla.getChildren().setAll(UI.hbox(10, UI.miniStat("Costo base", MoneyPMMD.fmt(cb)), UI.miniStat("Costo simulado", MoneyPMMD.fmt(cs)), dv), g);
        };
        for (TextField t : List.of(largo, fondo, alto)) t.textProperty().addListener((o, a, b) -> calc.run());
        piezas.valueProperty().addListener((o, a, b) -> calc.run());
        calc.run();
        GridPane in = UI.grid(4);
        in.add(UI.field("Largo (cm)", largo), 0, 0);
        in.add(UI.field("Fondo (cm)", fondo), 1, 0);
        in.add(UI.field("Alto (cm)", alto), 2, 0);
        in.add(UI.field("Piezas", piezas), 3, 0);
        Button cerrar = UI.secondary("Cerrar", null);
        var h = overlaysPMMD.modal("Simular medida · " + f.getEtiqueta(), "Base " + f.getLargo() + " × " + f.getFondo() + " × " + f.getAlto() + " cm", new VBox(16, in, tabla), 680, cerrar);
        cerrar.setOnAction(e -> h.cerrar());
    }


    private void exportar(FichaTecnica f, Explosion.Resultado r) {
        File archivo = Archivos.guardar(editorPMMD, "Ficha-" + f.getCodigo() + "-v" + f.getVersion(), "PDF", "pdf");
        if (archivo == null) return;
        double precio = f.getProducto() == null ? 0 : f.getProducto().getPrecio().doubleValue();
        Tareas.ejecutar(overlaysPMMD, toastPMMD, "Exportando ficha…", () -> {
            exportServicePMMD.fichaTecnica(f, r, precio, archivo);
            return archivo;
        }, a -> {
            toastPMMD.exito("Ficha exportada: " + a.getName());
            appWindowPMMD.hostServices().showDocument(a.toURI().toString());
        });
    }


    private void nuevaFicha() {
        List<Producto> sin = productoServicePMMD.listar().stream().filter(p -> fichaServicePMMD.ultimaDe(p) == null).toList();
        if (sin.isEmpty()) {
            toastPMMD.info("Todos los modelos ya tienen ficha técnica. Usa «Duplicar como nueva versión» para cambiar una.");
            return;
        }
        ComboBox<Producto> p = Forms.combo(sin, sin.get(0), x -> x.getCodigo() + " · " + x.getNombre());
        List<FichaTecnica> aprobadas = fichaServicePMMD.todas().stream().filter(x -> x.getEstado() == EstadoFicha.APROBADA && !Boolean.TRUE.equals(x.getCompuesta())).toList();
        ComboBox<FichaTecnica> plantilla = Forms.combo(aprobadas, aprobadas.isEmpty() ? null : aprobadas.get(0), x -> x.getEtiqueta() + " · " + x.getNombre());
        Button cancelar = UI.secondary("Cancelar", null);
        Button ok = UI.primary("Crear en Borrador", "ADD");
        var h = overlaysPMMD.modal("Nueva ficha técnica", "Se crea en Borrador copiando las líneas de una ficha aprobada", new VBox(14,
                UI.field("Modelo sin ficha", p), UI.field("Copiar de", plantilla)), 480, cancelar, ok);
        cancelar.setOnAction(e -> h.cerrar());
        ok.setOnAction(e -> {
            FichaTecnica[] n = new FichaTecnica[1];
            if (toastPMMD.intentar(() -> n[0] = fichaServicePMMD.crearPara(p.getValue(), plantilla.getValue()))) {
                h.cerrar();
                toastPMMD.exito(n[0].getEtiqueta() + " creada en Borrador para " + p.getValue().getNombre() + ".");
                seleccionar(n[0].getId());
            }
        });
    }
}

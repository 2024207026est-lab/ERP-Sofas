package pe.erp.sofas.ui.views.inventario;

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
import pe.erp.sofas.service.calculo.UnidadCompra;
import pe.erp.sofas.ui.components.*;

import java.util.*;

/** Inventario: materiales (stock con reservado), kardex con saldo corrido y alertas de stock con generacion de OC. */

@Component
@Lazy
@Scope("prototype")
@RequiredArgsConstructor
public class InventarioController implements Vista {

    private final InventarioService inventarioPMMD;

    private final TelaService telasPMMD;

    private final ProveedorService proveedoresPMMD;

    private final MrpServicePMMD mrpPMMD;

    private final Overlays overlaysPMMD;

    private final Toast toastPMMD;

    private final Navigator navigatorPMMD;


    @FXML private VBox raiz;


    private final VBox kpisPMMD = new VBox();

    private SegmentedControl tabsPMMD;

    private SearchField buscadorPMMD;

    private ComboBox<String> categoriaPMMD;

    private ComboBox<String> estadoPMMD;

    private final StackPane contenidoPMMD = new StackPane();

    private final TableView<Material> tablaMatPMMD = Cells.tabla("Ningún material coincide con los filtros.");

    private ComboBox<Material> kardexMaterialPMMD;

    private final TableView<Movimiento> tablaKardexPMMD = Cells.tabla("Sin movimientos.");

    private final Map<Long, Double> saldosPMMD = new HashMap<>();


    @FXML
    public void initialize() {
        tabsPMMD = new SegmentedControl("Materiales", "Kardex", "Alertas");
        tabsPMMD.getStyleClass().add("tabs");
        tabsPMMD.valorProperty().addListener((o, a, b) -> render());
        buscadorPMMD = new SearchField("Buscar material, código o ubicación");
        buscadorPMMD.setPrefWidth(300);
        buscadorPMMD.textProperty().addListener((o, a, b) -> render());
        List<String> cats = new ArrayList<>();
        cats.add(null);
        cats.addAll(Material.CATEGORIAS);
        categoriaPMMD = Forms.combo(cats, null, c -> c == null ? "Todas las categorías" : c);
        categoriaPMMD.setPrefWidth(190);
        categoriaPMMD.valueProperty().addListener((o, a, b) -> render());
        estadoPMMD = Forms.combo(Arrays.asList(null, "Normal", "Stock bajo", "Crítico", "Agotado", "Bajo pedido"), null, s -> s == null ? "Todos los estados" : s);
        estadoPMMD.setPrefWidth(170);
        estadoPMMD.valueProperty().addListener((o, a, b) -> render());
        Button nuevo = UI.primary("Registrar material", "ADD");
        nuevo.setOnAction(e -> nuevoMaterial());
        construirTablas();
        VBox.setVgrow(contenidoPMMD, Priority.ALWAYS);
        raiz.getChildren().setAll(kpisPMMD, UI.hbox(10, tabsPMMD, UI.spacer(), buscadorPMMD, categoriaPMMD, estadoPMMD, nuevo), contenidoPMMD);
    }


    @Override
    public void onShow(Intent intent) {
        if (intent.es(Intent.ACCION_ABRIR) && intent.dato() != null) {
            tabsPMMD.setValor("Kardex");
            render();
            kardexMaterialPMMD.getItems().stream().filter(m -> m != null && m.getCodigo().equals(intent.dato().toString())).findFirst().ifPresent(kardexMaterialPMMD::setValue);
            return;
        }
        render();
    }


    private void render() {
        List<Material> mats = inventarioPMMD.materiales();
        long criticos = mats.stream().filter(Material::isCritico).count();
        kpisPMMD.getChildren().setAll(UI.fila(
                new KpiCard("ACCOUNT_BALANCE_WALLET", "wood", "Valor del inventario", MoneyPMMD.fmt0(inventarioPMMD.valorTotal()), "A costo unitario"),
                new KpiCard("ALL_INBOX", "blue", "Materiales", String.valueOf(mats.size()), Material.CATEGORIAS.size() + " categorías"),
                new KpiCard("WARNING", "red", "Crítico o agotado", String.valueOf(criticos), mats.stream().filter(m -> "Stock bajo".equals(m.getEstado())).count() + " más en stock bajo")
                        .alClic(() -> tabsPMMD.setValor("Alertas")),
                new KpiCard("HISTORY", "green", "Movimientos del mes", String.valueOf(inventarioPMMD.movimientosDelMes()), "Entradas, salidas, reservas y consumos")));
        String t = tabsPMMD.getValor();
        boolean esMat = "Materiales".equals(t);
        buscadorPMMD.setDisable(!esMat);
        categoriaPMMD.setDisable(!esMat);
        estadoPMMD.setDisable(!esMat);
        switch (t) {
            case "Kardex" -> contenidoPMMD.getChildren().setAll(kardex(mats));
            case "Alertas" -> contenidoPMMD.getChildren().setAll(alertas());
            default -> {
                String q = BusquedaService.normalizar(buscadorPMMD.getText());
                tablaMatPMMD.getItems().setAll(mats.stream()
                        .filter(m -> categoriaPMMD.getValue() == null || m.getCategoria().equals(categoriaPMMD.getValue()))
                        .filter(m -> estadoPMMD.getValue() == null || m.getEstado().equals(estadoPMMD.getValue()))
                        .filter(m -> q.isEmpty() || BusquedaService.normalizar(m.getCodigo() + " " + m.getNombre() + " " + m.getUbicacion()).contains(q)).toList());
                VBox card = new VBox(tablaMatPMMD);
                VBox.setVgrow(tablaMatPMMD, Priority.ALWAYS);
                card.getStyleClass().addAll("card", "table-card");
                contenidoPMMD.getChildren().setAll(card);
            }
        }
    }


    private void construirTablas() {
        TableColumn<Material, String> valor = Cells.derecha("Valor", m -> MoneyPMMD.fmt(m.getValor()), 100, "cell-strong");
        valor.setComparator(Comparator.comparing(MoneyPMMD::parse));
        tablaMatPMMD.getColumns().addAll(List.of(
                Cells.texto("Código", Material::getCodigo, 70, "cell-mono"),
                Cells.texto("Material", Material::getNombre, 200, "cell-strong"),
                Cells.texto("Categoría", Material::getCategoria, 100, "cell-muted"),
                Cells.nodo("Stock · mínimo", this::celdaStock, 220),
                Cells.derecha("C. unit.", m -> MoneyPMMD.fmt(m.getCosto()), 80, "cell-muted"),
                valor,
                Cells.texto("Ubicación", Material::getUbicacion, 140, "cell-muted"),
                Cells.badge("Estado", Material::getEstado, 100),
                Cells.acciones("", m -> new Node[]{
                        boton("MOVE_TO_INBOX", "Entrada", () -> movimiento(m, TipoMovimiento.ENTRADA)),
                        boton("UNARCHIVE", "Salida", () -> movimiento(m, TipoMovimiento.SALIDA)),
                        boton("TUNE", "Ajuste", () -> movimiento(m, TipoMovimiento.AJUSTE)),
                        boton("HISTORY", "Ver kardex", () -> {
                            tabsPMMD.setValor("Kardex");
                            kardexMaterialPMMD.setValue(kardexMaterialPMMD.getItems().stream().filter(x -> x != null && x.getId().equals(m.getId())).findFirst().orElse(null));
                        })}, 140)));
        tablaMatPMMD.setFixedCellSize(54);
        tablaMatPMMD.setRowFactory(tv -> {
            TableRow<Material> r = new TableRow<>();
            r.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !r.isEmpty()) {
                    tabsPMMD.setValor("Kardex");
                    kardexMaterialPMMD.setValue(kardexMaterialPMMD.getItems().stream().filter(x -> x != null && x.getId().equals(r.getItem().getId())).findFirst().orElse(null));
                }
            });
            return r;
        });
        kardexMaterialPMMD = Forms.combo(List.of(), null, m -> m == null ? "Todos los materiales" : m.getCodigo() + " · " + m.getNombre());
        kardexMaterialPMMD.setPrefWidth(340);
        kardexMaterialPMMD.valueProperty().addListener((o, a, b) -> llenarKardex());
        tablaKardexPMMD.getColumns().addAll(List.of(
                Cells.texto("Fecha", m -> AppClockPMMD.fmt(m.getFecha().toLocalDate()) + " " + m.getFecha().toLocalTime().toString().substring(0, 5), 120),
                Cells.texto("Material", m -> m.getMaterial().getCodigo() + " · " + m.getMaterial().getNombre(), 220, "cell-strong"),
                Cells.nodo("Tipo", m -> new StatusBadge(m.getTipo().nombre(), m.getTipo().variante()).conIcono(m.getTipo().icono()), 130),
                Cells.nodo("Cantidad", m -> {
                    double ef = m.getEfecto();
                    String signo = !m.getTipo().afectaSaldo() ? "" : ef >= 0 ? "+" : "−";
                    Label l = UI.label(signo + MoneyPMMD.numero(Math.abs(m.getTipo() == TipoMovimiento.AJUSTE ? ef : m.getCantidad())) + " " + m.getMaterial().getUnidad(), "cell-title");
                    if (!m.getTipo().afectaSaldo()) l.getStyleClass().add("muted");
                    else l.setStyle(ef >= 0 ? "-fx-text-fill: -is-success-ink;" : "-fx-text-fill: -is-danger-ink;");
                    return l;
                }, 120),
                Cells.texto("Referencia", Movimiento::getReferencia, 100, "cell-mono"),
                Cells.texto("Concepto", Movimiento::getConcepto, 240, "cell-muted"),
                Cells.derecha("Saldo", m -> MoneyPMMD.numero(saldosPMMD.getOrDefault(m.getId(), 0d)), 90, "cell-strong"),
                Cells.texto("Usuario", Movimiento::getUsuario, 90, "cell-muted")));
    }


    private Button boton(String icono, String tip, Runnable r) {
        Button b = UI.iconButton(icono, tip, "btn-ghost", "btn-xs");
        b.setOnAction(e -> r.run());
        return b;
    }


    private Node celdaStock(Material m) {
        String color = switch (m.getEstado()) {
            case "Normal" -> "#16A34A";
            case "Stock bajo", "Bajo pedido" -> "#F59E0B";
            default -> "#DC2626";
        };
        ProgressBarMini barra = new ProgressBarMini(m.getMinimo() <= 0 ? 1 : m.getDisponible() / (m.getMinimo() * 2), color, 6);
        barra.setPrefWidth(70);
        Label stock = UI.label(MoneyPMMD.numero(m.getStock()) + " " + m.getUnidad(), "cell-title");
        Label res = UI.label(m.getReservado() > 0 ? "res. " + MoneyPMMD.numero(m.getReservado()) : "sin reservas", "cell-sub");
        Tooltip.install(res, new Tooltip("Disponible: " + MoneyPMMD.numero(m.getDisponible()) + " · mínimo " + MoneyPMMD.numero(m.getMinimo())));
        VBox v = new VBox(2, UI.hbox(6, stock, res), UI.hbox(6, UI.grow(barra), UI.label("mín. " + MoneyPMMD.numero(m.getMinimo()), "cell-sub")));
        return v;
    }


    private Node kardex(List<Material> mats) {
        Material sel = kardexMaterialPMMD.getValue();
        List<Material> items = new ArrayList<>();
        items.add(null);
        items.addAll(mats);
        kardexMaterialPMMD.getItems().setAll(items);
        kardexMaterialPMMD.setValue(sel == null ? null : mats.stream().filter(m -> m.getId().equals(sel.getId())).findFirst().orElse(null));
        llenarKardex();
        VBox card = new VBox(tablaKardexPMMD);
        VBox.setVgrow(tablaKardexPMMD, Priority.ALWAYS);
        card.getStyleClass().addAll("card", "table-card");
        VBox.setVgrow(card, Priority.ALWAYS);
        HBox leyenda = new HBox(8);
        for (TipoMovimiento t : TipoMovimiento.values()) leyenda.getChildren().add(new StatusBadge(t.nombre(), t.variante()).conIcono(t.icono()));
        return new VBox(10, UI.hbox(10, kardexMaterialPMMD, UI.spacer(), UI.label("Reserva, Liberación y Retazo no alteran el saldo", "cell-sub")), leyenda, card);
    }

    /** Saldo corrido por material en orden cronologico; la tabla muestra lo mas reciente primero. */

    private void llenarKardex() {
        Material sel = kardexMaterialPMMD.getValue();
        List<Movimiento> movs = sel == null ? inventarioPMMD.movimientos() : inventarioPMMD.kardex(sel.getId());
        List<Movimiento> crono = new ArrayList<>(movs);
        crono.sort(Comparator.comparing(Movimiento::getFecha).thenComparing(Movimiento::getId));
        saldosPMMD.clear();
        Map<Long, Double> acum = new HashMap<>();
        if (sel == null) {
            // saldos por material desde el inicio de su kardex
            List<Movimiento> todos = new ArrayList<>(inventarioPMMD.movimientos());
            todos.sort(Comparator.comparing(Movimiento::getFecha).thenComparing(Movimiento::getId));
            for (Movimiento m : todos) {
                double s = acum.getOrDefault(m.getMaterial().getId(), 0d) + m.getEfecto();
                acum.put(m.getMaterial().getId(), s);
                saldosPMMD.put(m.getId(), s);
            }
        } else {
            double s = 0;
            for (Movimiento m : crono) {
                s += m.getEfecto();
                saldosPMMD.put(m.getId(), s);
            }
        }
        List<Movimiento> recientes = new ArrayList<>(crono);
        Collections.reverse(recientes);
        tablaKardexPMMD.getItems().setAll(recientes);
    }


    private Node alertas() {
        List<Material> bajo = inventarioPMMD.bajoMinimo();
        VBox lista = new VBox(8);
        if (bajo.isEmpty()) lista.getChildren().add(UI.empty("DONE_ALL", "Sin alertas", "Todos los materiales están sobre su mínimo.", null));
        for (Material m : bajo) {
            double sugerido = UnidadCompra.redondear(m.getUnidad(), Math.max(m.getMinimo() * 2 - m.getDisponible(), m.getMinimo()));
            Button oc = UI.button("Generar OC", "SHOPPING_CART", "btn-sm", "btn-wood");
            oc.setOnAction(e -> generarOC(List.of(m)));
            HBox h = UI.hbox(12, new StatusBadge(m.getEstado()), new VBox(1, UI.label(m.getCodigo() + " · " + m.getNombre(), "cell-title"),
                            UI.label("Disponible " + MoneyPMMD.numero(m.getDisponible()) + " " + m.getUnidad() + " (stock " + MoneyPMMD.numero(m.getStock()) + ", reservado "
                                    + MoneyPMMD.numero(m.getReservado()) + ") · mínimo " + MoneyPMMD.numero(m.getMinimo()) + " · proveedor "
                                    + (m.getProveedor() == null ? "—" : m.getProveedor().getRazonSocial()), "cell-sub")),
                    UI.spacer(), UI.label("Sugerido: " + MoneyPMMD.numero(sugerido) + " " + m.getUnidad(), "cell-sub"), oc);
            h.getStyleClass().add("alert-row");
            lista.getChildren().add(h);
        }
        Button todas = UI.primary("Generar OC de todas", "SHOPPING_CART");
        todas.setDisable(bajo.isEmpty());
        todas.setOnAction(e -> generarOC(bajo));
        List<Tela> telasBajas = telasPMMD.activas().stream().filter(t -> !"Disponible".equals(t.getEstado())).toList();
        VBox telasBox = new VBox(6, UI.label("Colores del muestrario por reponer", "card-title"));
        for (Tela t : telasBajas) {
            Hyperlink l = new Hyperlink("Ver en Telas y colores");
            l.setOnAction(e -> navigatorPMMD.goTo("telas", Intent.abrir(t.getCodigo())));
            telasBox.getChildren().add(UI.hbox(10, new ColorSwatch(t.getHex(), t.getTipo(), 22), UI.label(t.getNombre() + " · " + MoneyPMMD.numero(t.getStock()) + " m", "cell-title"),
                    new StatusBadge(t.getEstado()), UI.spacer(), l));
        }
        return new VBox(14, UI.card(UI.cardHeader("Materiales bajo el mínimo", bajo.size() + " materiales · el disponible descuenta lo reservado", todas), lista), UI.card(telasBox));
    }


    private void generarOC(List<Material> mats) {
        List<MrpServicePMMD.Fila> filas = new ArrayList<>();
        for (Material m : mats) {
            double falta = Math.max(m.getMinimo() * 2 - m.getDisponible(), m.getMinimo());
            if (m.getProveedor() != null) filas.add(new MrpServicePMMD.Fila(m, falta, 0, falta, AppClockPMMD.hoy(), m.getProveedor(), Set.of("Alerta de stock")));
        }
        List<String>[] c = new List[1];
        if (toastPMMD.intentar(() -> c[0] = mrpPMMD.generarOrdenes(filas, "Alerta de stock"))) {
            toastPMMD.exito("Órdenes de compra en borrador: " + String.join(", ", c[0]) + ".");
            navigatorPMMD.datosCambiaron();
        }
    }


    private void movimiento(Material m, TipoMovimiento tipo) {
        Validacion v = new Validacion();
        TextField cantidad = Forms.decimal(tipo == TipoMovimiento.AJUSTE ? 0 : 1);
        if (tipo == TipoMovimiento.AJUSTE) cantidad.setTextFormatter(new TextFormatter<String>(c -> c.getControlNewText().matches("-?[0-9.,]*") ? c : null));
        TextField ref = UI.text("", tipo == TipoMovimiento.ENTRADA ? "Guía o factura" : "Vale o documento");
        ComboBox<String> motivo = Forms.combo(switch (tipo) {
            case ENTRADA -> List.of("Compra sin OC", "Devolución del taller", "Donación / muestra", "Otro");
            case SALIDA -> List.of("Uso en taller (sin OP)", "Merma / daño", "Muestra para cliente", "Otro");
            default -> List.of("Inventario físico", "Error de registro", "Otro");
        }, null, s -> s);
        FormGrid f = new FormGrid(2);
        ComboBox<Tela> color = null;
        if (m.isTela()) {
            color = Forms.combo(telasPMMD.porTipo(TipoTela.porMaterial(m.getCodigo())).stream().filter(t -> !t.getDescontinuada()).toList(), null,
                    t -> t.getColor() + " · " + MoneyPMMD.numero(t.getStock()) + " m");
            ComboBox<Tela> cc = color;
            f.nodo(v.campo("Color de la tela", cc, () -> Validacion.requerido(cc, "el color")), 2);
        }
        f.nodo(v.campo(tipo == TipoMovimiento.AJUSTE ? "Cantidad (+/−, " + m.getUnidad() + ")" : "Cantidad (" + m.getUnidad() + ")", cantidad, () -> {
            double x = Validacion.valor(cantidad);
            if (tipo == TipoMovimiento.AJUSTE) return x == 0 ? "Ingresa una cantidad distinta de cero." : m.getStock() + x < 0 ? "El stock quedaría negativo." : null;
            if (x <= 0) return "La cantidad debe ser mayor a cero.";
            return tipo == TipoMovimiento.SALIDA && x > m.getDisponible() + 1e-9 ? "Solo hay " + MoneyPMMD.numero(m.getDisponible()) + " disponibles (el resto está reservado)." : null;
        }), 1);
        f.nodo(v.campo("Referencia", ref, null), 1);
        f.nodo(v.campo("Motivo", motivo, () -> Validacion.requerido(motivo, "el motivo")), 2);
        VBox cuerpo = new VBox(12, UI.hbox(10, UI.miniStat("Stock", MoneyPMMD.numero(m.getStock()) + " " + m.getUnidad()), UI.miniStat("Reservado", MoneyPMMD.numero(m.getReservado())),
                UI.miniStat("Disponible", MoneyPMMD.numero(m.getDisponible()))), f);
        Button cancelar = UI.secondary("Cancelar", null);
        Button ok = UI.primary("Registrar " + tipo.nombre().toLowerCase(), tipo.icono());
        v.enlazar(ok);
        var h = overlaysPMMD.modal(tipo.nombre() + " · " + m.getNombre(), m.getCodigo() + " · " + m.getUbicacion(), cuerpo, 520, cancelar, ok);
        cancelar.setOnAction(e -> h.cerrar());
        ComboBox<Tela> colorFinal = color;
        ok.setOnAction(e -> {
            if (toastPMMD.intentar(() -> inventarioPMMD.movimientoManual(m.getId(), tipo, Validacion.valor(cantidad),
                    colorFinal == null || colorFinal.getValue() == null ? null : colorFinal.getValue().getId(), ref.getText(), motivo.getValue()))) {
                h.cerrar();
                toastPMMD.exito(tipo.nombre() + " de " + cantidad.getText() + " " + m.getUnidad() + " de " + m.getNombre() + " registrada en el kardex.");
                navigatorPMMD.datosCambiaron();
                render();
            }
        });
    }


    private void nuevoMaterial() {
        Validacion v = new Validacion();
        TextField nombre = UI.text("", "Nombre del material");
        ComboBox<String> cat = Forms.combo(Material.CATEGORIAS, "Insumos", s -> s);
        ComboBox<String> unidad = Forms.combo(InventarioService.UNIDADES, "unidad", s -> s);
        TextField costo = Forms.decimal(0), stock = Forms.decimal(0), minimo = Forms.decimal(0);
        TextField ubic = UI.text("", "Almacén A · Rack 1");
        ComboBox<Proveedor> prov = Forms.combo(proveedoresPMMD.activos(), null, Proveedor::getRazonSocial);
        CheckBox bajoPedido = new CheckBox("Se compra bajo pedido (sin stock)");
        FormGrid f = new FormGrid(3);
        f.nodo(v.campo("Nombre", nombre, () -> Validacion.requerido(nombre, "el nombre")), 3);
        f.nodo(v.campo("Categoría", cat, null), 1);
        f.nodo(v.campo("Unidad", unidad, null), 1);
        f.nodo(v.campo("Costo unitario (S/)", costo, () -> Validacion.numero(costo, 0, "El costo")), 1);
        f.nodo(v.campo("Stock inicial", stock, () -> Validacion.numero(stock, 0, "El stock")), 1);
        f.nodo(v.campo("Mínimo", minimo, () -> Validacion.numero(minimo, 0, "El mínimo")), 1);
        f.nodo(v.campo("Ubicación", ubic, null), 1);
        f.nodo(v.campo("Proveedor", prov, null), 2);
        f.nodo(bajoPedido, 1);
        Button cancelar = UI.secondary("Cancelar", null);
        Button ok = UI.primary("Registrar material", "SAVE");
        v.enlazar(ok);
        var h = overlaysPMMD.modal("Registrar material", "El stock inicial queda registrado como Entrada en el kardex", f, 640, cancelar, ok);
        cancelar.setOnAction(e -> h.cerrar());
        ok.setOnAction(e -> {
            Material[] g = new Material[1];
            if (toastPMMD.intentar(() -> g[0] = inventarioPMMD.guardarMaterial(Material.builder().nombre(nombre.getText().trim()).categoria(cat.getValue())
                    .unidad(unidad.getValue()).costo(Validacion.valor(costo)).stock(Validacion.valor(stock)).minimo(Validacion.valor(minimo))
                    .ubicacion(ubic.getText()).proveedor(prov.getValue()).bajoPedido(bajoPedido.isSelected()).reservado(0d).build()))) {
                h.cerrar();
                toastPMMD.exito(g[0].getCodigo() + " · " + g[0].getNombre() + " registrado.");
                render();
            }
        });
    }
}

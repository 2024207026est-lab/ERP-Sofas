package pe.erp.sofas.ui.views.telas;

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
import pe.erp.sofas.service.calculo.Opciones;
import pe.erp.sofas.ui.components.*;

import java.util.*;

/** Muestrario de telas: una tarjeta por tipo con sus colores, entrada de rollo y calculadora de tela. */

@Component
@Lazy
@Scope("prototype")
@RequiredArgsConstructor
public class TelasController implements Vista {

    private static final List<String> DISPONIBILIDADPMMD = List.of("Todas", "Disponible", "Stock bajo", "Agotado", "Descontinuada");


    private final TelaService telaServicePMMD;

    private final ProductoService productoServicePMMD;

    private final FichaService fichaServicePMMD;

    private final CorteService corteServicePMMD;

    private final InventarioService inventarioServicePMMD;

    private final Overlays overlaysPMMD;

    private final Toast toastPMMD;

    private final ConfirmDialog confirmPMMD;

    private final Navigator navigatorPMMD;


    @FXML private VBox raiz;


    private final VBox kpisPMMD = new VBox();

    private SearchField buscadorPMMD;

    private final ChipFilterBar chipsPMMD = new ChipFilterBar();

    private ComboBox<String> cmbDispPMMD;

    private CheckBox chkMascotasPMMD;

    private final VBox tarjetasPMMD = new VBox(16);

    private String resaltarPMMD;


    @FXML
    public void initialize() {
        buscadorPMMD = new SearchField("Buscar color o código (TL-07, Mostaza…)");
        buscadorPMMD.setPrefWidth(300);
        buscadorPMMD.textProperty().addListener((o, a, b) -> render());
        cmbDispPMMD = Forms.combo(DISPONIBILIDADPMMD, "Todas", s -> "Disponibilidad: " + s);
        cmbDispPMMD.setPrefWidth(230);
        cmbDispPMMD.valueProperty().addListener((o, a, b) -> render());
        chkMascotasPMMD = new CheckBox("Apta para mascotas");
        chkMascotasPMMD.selectedProperty().addListener((o, a, b) -> render());
        chipsPMMD.seleccionProperty().addListener((o, a, b) -> render());
        Button calc = UI.secondary("Calculadora de tela", "CALCULATE");
        calc.setOnAction(e -> calculadora(null, null));
        Button nueva = UI.primary("Registrar tela", "ADD");
        nueva.setOnAction(e -> registrar());
        HBox barra = UI.hbox(10, buscadorPMMD, cmbDispPMMD, chkMascotasPMMD, UI.spacer(), calc, nueva);
        ScrollPane sp = UI.scroll(tarjetasPMMD);
        VBox.setVgrow(sp, Priority.ALWAYS);
        raiz.getChildren().setAll(kpisPMMD, barra, chipsPMMD, sp);
    }


    @Override
    public void onShow(Intent intent) {
        List<Tela> todas = telaServicePMMD.telas();
        List<ChipFilterBar.Chip> cs = new ArrayList<>();
        cs.add(new ChipFilterBar.Chip("*", "Todos los tipos", todas.size()));
        for (TipoTela t : TipoTela.values()) cs.add(new ChipFilterBar.Chip(t.name(), t.nombre(), todas.stream().filter(x -> x.getTipo() == t).count()));
        chipsPMMD.setChips(cs);
        resaltarPMMD = intent.es(Intent.ACCION_ABRIR) && intent.dato() != null ? intent.dato().toString() : null;
        if (resaltarPMMD != null) {
            buscadorPMMD.setText("");
            chipsPMMD.seleccionar("*");
            cmbDispPMMD.setValue("Todas");
        }
        render();
        if (resaltarPMMD != null) telaServicePMMD.porCodigo(resaltarPMMD).ifPresent(this::usos);
    }


    private void render() {
        if (cmbDispPMMD == null) return;
        kpisPMMD.getChildren().setAll(UI.fila(
                new KpiCard("PALETTE", "blue", "Colores activos", String.valueOf(telaServicePMMD.activas().size()), TipoTela.values().length + " tipos de tela"),
                new KpiCard("STRAIGHTEN", "teal", "Metros en stock", MoneyPMMD.numero(telaServicePMMD.metrosEnStock()) + " m", "Suma del muestrario"),
                new KpiCard("WARNING", "amber", "Stock bajo o agotado", String.valueOf(telaServicePMMD.conAlerta()), "Colores por reponer"),
                new KpiCard("ACCOUNT_BALANCE_WALLET", "wood", "Valor del muestrario", MoneyPMMD.fmt0(telaServicePMMD.valorMuestrario()), "A costo por metro")));
        String q = BusquedaService.normalizar(buscadorPMMD.getText());
        String tipoSel = chipsPMMD.getSeleccion();
        String disp = cmbDispPMMD.getValue();
        tarjetasPMMD.getChildren().clear();
        for (TipoTela tipo : TipoTela.values()) {
            if (tipoSel != null && !tipoSel.equals("*") && !tipoSel.equals(tipo.name())) continue;
            if (chkMascotasPMMD.isSelected() && !tipo.mascotas()) continue;
            List<Tela> colores = telaServicePMMD.porTipo(tipo).stream()
                    .filter(t -> disp.equals("Todas") || t.getEstado().equals(disp))
                    .filter(t -> q.isEmpty() || BusquedaService.normalizar(t.getColor() + " " + t.getCodigo() + " " + t.getCodigoProveedor()).contains(q))
                    .toList();
            if (colores.isEmpty()) continue;
            tarjetasPMMD.getChildren().add(tarjetaTipo(tipo, colores));
        }
        if (tarjetasPMMD.getChildren().isEmpty()) {
            tarjetasPMMD.getChildren().add(UI.empty("PALETTE", "Ningún color coincide", "Cambia el tipo, la disponibilidad o la búsqueda.", null));
        }
    }


    private Node tarjetaTipo(TipoTela tipo, List<Tela> colores) {
        List<Tela> todos = telaServicePMMD.porTipo(tipo);
        double metros = todos.stream().filter(t -> !t.getDescontinuada()).mapToDouble(Tela::getStock).sum();
        HBox caracteristicas = new HBox(6);
        if (tipo.antimanchas()) caracteristicas.getChildren().add(chip("Antimanchas", "CLEANING_SERVICES"));
        if (tipo.mascotas()) caracteristicas.getChildren().add(chip("Mascotas", "PETS"));
        if (tipo.impermeable()) caracteristicas.getChildren().add(chip("Impermeable", "OPACITY"));
        if (!tipo.permiteGiro()) caracteristicas.getChildren().add(chip("Sentido del pelo", "SWAP_VERT"));
        ColorSwatch muestra = new ColorSwatch(colores.get(0).getHex(), tipo, 40);
        VBox titulo = new VBox(2, UI.label(tipo.nombre(), "tela-tipo"),
                UI.label("S/ " + (int) tipo.costoMetro() + "/m · recargo al cliente " + (int) tipo.recargoPct() + " % · Martindale "
                        + MoneyPMMD.entero(tipo.martindale()) + " ciclos", "cell-sub"));
        VBox totales = new VBox(2, UI.label(todos.stream().filter(t -> !t.getDescontinuada()).count() + " colores · " + MoneyPMMD.numero(metros) + " m", "cell-title"),
                UI.label("Material " + tipo.materialCodigo(), "code"));
        totales.setAlignment(Pos.CENTER_RIGHT);
        HBox cab = new HBox(14, muestra, titulo, caracteristicas, UI.spacer(), totales);
        cab.getStyleClass().add("tela-header");
        FlowPane grid = new FlowPane(12, 12);
        for (Tela t : colores) grid.getChildren().add(muestra(t));
        return UI.card(cab, grid);
    }


    private Label chip(String texto, String icono) {
        Label l = UI.label(texto, "feature-chip");
        l.setGraphic(Ico.of(icono, 12));
        return l;
    }


    private Node muestra(Tela t) {
        ColorSwatch sw = new ColorSwatch(t.getHex(), t.getTipo(), 52);
        double ratio = t.getMinimo() <= 0 ? 1 : t.getStock() / (t.getMinimo() * 2.5);
        String color = switch (t.getEstado()) {
            case "Disponible" -> "#16A34A";
            case "Stock bajo" -> "#F59E0B";
            case "Descontinuada" -> "#94A3B8";
            default -> "#DC2626";
        };
        ProgressBarMini barra = new ProgressBarMini(ratio, color, 6);
        barra.setPrefWidth(120);
        Tooltip.install(barra, new Tooltip(MoneyPMMD.numero(t.getStock()) + " m · mínimo " + MoneyPMMD.numero(t.getMinimo()) + " m"));
        Button rollo = UI.button("Rollo", "ADD", "btn-sm", "btn-secondary");
        rollo.setDisable(t.getDescontinuada());
        rollo.setOnAction(e -> entradaRollo(t));
        rollo.setStyle("-fx-padding: 0 10 0 8;");
        Button sofas = UI.button("Sofás", null, "btn-sm", "btn-ghost");
        sofas.setStyle("-fx-padding: 0 8;");
        sofas.setOnAction(e -> usos(t));
        Button estado = UI.iconButton(t.getDescontinuada() ? "RESTORE" : "BLOCK", t.getDescontinuada() ? "Reactivar color" : "Descontinuar color", "btn-ghost", "btn-xs");
        estado.setOnAction(e -> alternar(t));
        VBox tile = new VBox(7, sw, UI.label(t.getColor(), "swatch-name"), UI.codigo(t.getCodigo() + " · " + t.getCodigoProveedor()),
                new StatusBadge(t.getEstado()), UI.hbox(6, UI.label(MoneyPMMD.numero(t.getStock()) + " m", "cell-title"), UI.spacer(),
                UI.label("mín. " + MoneyPMMD.numero(t.getMinimo()), "cell-sub")), barra, UI.hbox(4, rollo, sofas, UI.spacer(), estado));
        tile.getStyleClass().add("swatch-tile");
        if (t.getDescontinuada()) tile.getStyleClass().add("swatch-tile-off");
        if (t.getCodigo().equals(resaltarPMMD)) tile.setStyle("-fx-border-color: -is-primary; -fx-border-width: 2;");
        tile.setPrefWidth(196);
        tile.setMaxWidth(196);
        tile.setMinWidth(196);
        return tile;
    }


    private void alternar(Tela t) {
        boolean desc = !t.getDescontinuada();
        confirmPMMD.mostrar(desc ? ConfirmDialog.Tipo.PELIGRO : ConfirmDialog.Tipo.INFO,
                (desc ? "¿Descontinuar " : "¿Reactivar ") + t.getNombre() + "?",
                desc ? "El color deja de ofrecerse y sus " + MoneyPMMD.numero(t.getStock()) + " m dejan de sumar al stock del material " + t.getTipo().materialCodigo() + "."
                        : "El color vuelve al muestrario y sus metros suman otra vez al inventario.",
                desc ? "Descontinuar" : "Reactivar", () -> {
                    if (toastPMMD.intentar(() -> telaServicePMMD.alternarDescontinuada(t.getId()))) {
                        toastPMMD.exito(t.getNombre() + (desc ? " descontinuada." : " reactivada."));
                        render();
                        navigatorPMMD.datosCambiaron();
                    }
                });
    }


    private void entradaRollo(Tela t) {
        Validacion v = new Validacion();
        TextField metros = Forms.decimal(30);
        TextField ref = UI.text("", "Guía o factura (opcional)");
        Label resultado = UI.label("", "muted");
        Runnable prev = () -> resultado.setText("Quedará en " + MoneyPMMD.numero(t.getStock() + Validacion.valor(metros)) + " m (hoy " + MoneyPMMD.numero(t.getStock()) + " m).");
        metros.textProperty().addListener((o, a, b) -> prev.run());
        prev.run();
        VBox cuerpo = new VBox(14, UI.hbox(12, new ColorSwatch(t.getHex(), t.getTipo(), 44), new VBox(2, UI.label(t.getNombre(), "card-title"), UI.codigo(t.getCodigo()))),
                v.campo("Metros recibidos", metros, () -> Validacion.numero(metros, 0.1, "Los metros")), v.campo("Referencia", ref, null), resultado,
                UI.wrap("Suma al color y al material " + t.getTipo().materialCodigo() + " del inventario, y registra una Entrada en el kardex.", "cell-sub"));
        Button cancelar = UI.secondary("Cancelar", null);
        Button ok = UI.primary("Registrar entrada", "MOVE_TO_INBOX");
        v.enlazar(ok);
        var h = overlaysPMMD.modal("Entrada de rollo", t.getTipo().nombre(), cuerpo, 440, cancelar, ok);
        cancelar.setOnAction(e -> h.cerrar());
        ok.setOnAction(e -> {
            if (toastPMMD.intentar(() -> telaServicePMMD.entradaRollo(t.getId(), Validacion.valor(metros), ref.getText()))) {
                h.cerrar();
                toastPMMD.exito("+" + metros.getText() + " m de " + t.getNombre() + " · kardex " + t.getTipo().materialCodigo() + " actualizado.");
                render();
                navigatorPMMD.datosCambiaron();
            }
        });
    }


    private void usos(Tela t) {
        TelaService.Usos u = telaServicePMMD.usos(t);
        VBox cuerpo = new VBox(14);
        cuerpo.getChildren().add(UI.hbox(12, new ColorSwatch(t.getHex(), t.getTipo(), 48),
                new VBox(2, UI.label(t.getNombre(), "card-title"), UI.label(MoneyPMMD.numero(t.getStock()) + " m · " + t.getEstado(), "cell-sub"))));
        cuerpo.getChildren().add(seccion("Modelos del catálogo", u.productos().stream().map(p -> fila("WEEKEND", p.getNombre(), p.getCodigo() + " · " + MoneyPMMD.fmt(p.getPrecio()),
                () -> navigatorPMMD.goTo("modelos", Intent.abrir(p.getCodigo())))).toList()));
        cuerpo.getChildren().add(seccion("Pedidos", u.pedidos().stream().map(p -> fila("ASSIGNMENT", p.getCodigo() + " · " + p.getProductoNombre(),
                p.getCliente().getNombre() + " · " + p.getEstado().nombre(), () -> navigatorPMMD.goTo("pedidos", Intent.abrir(p.getCodigo())))).toList()));
        cuerpo.getChildren().add(seccion("Órdenes de producción", u.ordenes().stream().map(o -> fila("BUILD", o.getCodigo() + " · " + o.getProductoNombre() + " ×" + o.getCantidad(),
                o.getClienteNombre() + " · " + o.getEtapa().nombre(), () -> navigatorPMMD.goTo("produccion", Intent.abrir(o.getCodigo())))).toList()));
        overlaysPMMD.panel("Sofás con esta tela", u.total() + " usos en catálogo, pedidos y producción", cuerpo, null, 440);
    }


    private Node seccion(String titulo, List<Node> filas) {
        VBox v = new VBox(4, UI.label(titulo + " (" + filas.size() + ")", "card-title"));
        if (filas.isEmpty()) v.getChildren().add(UI.label("Sin registros.", "muted"));
        v.getChildren().addAll(filas);
        return v;
    }


    private Node fila(String icono, String titulo, String sub, Runnable r) {
        StackPane ic = new StackPane(Ico.of(icono, 16));
        ic.getStyleClass().add("popup-item-icon");
        HBox h = new HBox(10, ic, new VBox(1, UI.label(titulo, "cell-title"), UI.label(sub, "cell-sub")), UI.spacer(), Ico.of("CHEVRON_RIGHT", 16));
        h.getStyleClass().add("alert-row");
        h.setOnMouseClicked(e -> r.run());
        return h;
    }


    private void registrar() {
        Validacion v = new Validacion();
        ComboBox<TipoTela> tipo = Forms.combo(List.of(TipoTela.values()), TipoTela.CHENILLE, TipoTela::nombre);
        TextField color = UI.text("", "Nombre del color (p. ej. Vino)");
        ColorPicker picker = new ColorPicker(javafx.scene.paint.Color.web("#7A3B3B"));
        picker.setMaxWidth(Double.MAX_VALUE);
        TextField codigo = UI.text("", "Código del proveedor (CH-210 VIN)");
        TextField stock = Forms.decimal(0);
        TextField minimo = Forms.decimal(8);
        ColorSwatch vista = new ColorSwatch("#7A3B3B", TipoTela.CHENILLE, 64);
        Runnable act = () -> vista.pintar(hex(picker.getValue()), tipo.getValue());
        picker.valueProperty().addListener((o, a, b) -> act.run());
        tipo.valueProperty().addListener((o, a, b) -> act.run());
        FormGrid f = new FormGrid(2);
        f.nodo(UI.hbox(14, vista, UI.wrap("La muestra aplica la textura del tipo de tela elegido.", "muted")), 2);
        f.nodo(v.campo("Tipo de tela", tipo, () -> Validacion.requerido(tipo, "el tipo")), 1);
        f.nodo(v.campo("Nombre del color", color, () -> {
            String e = Validacion.requerido(color, "el nombre del color");
            if (e != null) return e;
            return telaServicePMMD.porTipo(tipo.getValue()).stream().anyMatch(t -> t.getColor().equalsIgnoreCase(color.getText().trim()))
                    ? "Ya existe ese color en " + tipo.getValue().nombre() + "." : null;
        }), 1);
        f.nodo(v.campo("Color", picker, null), 1);
        f.nodo(v.campo("Código del proveedor", codigo, null), 1);
        f.nodo(v.campo("Stock inicial (m)", stock, () -> Validacion.numero(stock, 0, "El stock")), 1);
        f.nodo(v.campo("Mínimo (m)", minimo, () -> Validacion.numero(minimo, 0, "El mínimo")), 1);
        v.escuchar(tipo.valueProperty());
        Button cancelar = UI.secondary("Cancelar", null);
        Button ok = UI.primary("Registrar tela", "SAVE");
        v.enlazar(ok);
        var h = overlaysPMMD.modal("Registrar tela", "Nuevo color del muestrario", f, 560, cancelar, ok);
        cancelar.setOnAction(e -> h.cerrar());
        ok.setOnAction(e -> {
            if (toastPMMD.intentar(() -> {
                Tela t = telaServicePMMD.registrar(tipo.getValue(), color.getText(), hex(picker.getValue()), codigo.getText(),
                        Validacion.valor(stock), Validacion.valor(minimo));
                toastPMMD.exito(t.getCodigo() + " · " + t.getNombre() + " registrada.");
            })) {
                h.cerrar();
                onShow(Intent.NONE);
            }
        });
    }


    private static String hex(javafx.scene.paint.Color c) {
        return String.format("#%02X%02X%02X", (int) (c.getRed() * 255), (int) (c.getGreen() * 255), (int) (c.getBlue() * 255));
    }

    /** Calculadora de tela: metros segun la ficha y segun el plano de corte para un modelo, cantidad y color. */

    private void calculadora(Producto inicial, Tela telaInicial) {
        List<Producto> conFicha = productoServicePMMD.listar().stream().filter(p -> fichaServicePMMD.aprobadaDe(p) != null).toList();
        ComboBox<Producto> modelo = Forms.combo(conFicha, inicial == null ? conFicha.get(0) : inicial, p -> p.getCodigo() + " · " + p.getNombre());
        Spinner<Integer> cantidad = new Spinner<>(1, 50, 1);
        cantidad.setEditable(true);
        cantidad.setMaxWidth(Double.MAX_VALUE);
        ComboBox<Tela> tela = Forms.combo(telaServicePMMD.activas(), telaInicial == null ? telaServicePMMD.activas().get(0) : telaInicial, Tela::getNombre);
        VBox res = new VBox(10);
        Button plano = UI.secondary("Ver plano de corte", "CONTENT_CUT");
        Runnable calcular = () -> {
            Producto p = modelo.getValue();
            Tela t = tela.getValue();
            if (p == null || t == null) return;
            FichaTecnica f = fichaServicePMMD.aprobadaDe(p);
            int n = cantidad.getValue();
            Map<String, String> sust = new HashMap<>();
            for (TipoTela tt : TipoTela.values()) sust.put(tt.materialCodigo(), t.getTipo().materialCodigo());
            Explosion.Resultado r = fichaServicePMMD.explotar(f, n, Opciones.sustituir(sust));
            double metrosFicha = r.porMaterial().stream().filter(x -> x.material().isTela()).mapToDouble(Explosion.PorMaterial::cantidad).sum();
            double metrosPlano = corteServicePMMD.metrosPlano(f, n, t.getTipo()) * 1.10;
            double necesario = Math.max(metrosFicha, metrosPlano);
            double falta = Math.max(0, necesario - t.getStock());
            double costo = necesario * t.getTipo().costoMetro();
            double recargo = p.getPrecio().doubleValue() * n * t.getTipo().recargoPct() / 100;
            Label lf = UI.label(MoneyPMMD.numero(falta) + " m", "mini-stat-value");
            if (falta > 0) lf.setStyle("-fx-text-fill: -is-danger;");
            VBox faltante = new VBox(2, lf, UI.label("Faltante", "mini-stat-label"));
            faltante.getStyleClass().add("mini-stat");
            HBox.setHgrow(faltante, Priority.ALWAYS);
            res.getChildren().setAll(
                    UI.hbox(10, UI.miniStat("Según la ficha " + f.getEtiqueta() + " (con merma)", MoneyPMMD.numero(metrosFicha) + " m"),
                            UI.miniStat("Según el plano de corte + 10 %", MoneyPMMD.numero(metrosPlano) + " m")),
                    UI.hbox(10, UI.miniStat("Stock de " + t.getColor(), MoneyPMMD.numero(t.getStock()) + " m"), faltante),
                    UI.hbox(10, UI.miniStat("Costo de tela", MoneyPMMD.fmt(costo)), UI.miniStat("Recargo al cliente (" + (int) t.getTipo().recargoPct() + " %)", MoneyPMMD.fmt(recargo))),
                    falta > 0 ? UI.banner("danger-banner", "WARNING", "No alcanza: faltan " + MoneyPMMD.numero(falta) + " m de " + t.getNombre() + ". Registra un rollo o genera la compra.")
                            : UI.banner("ok-banner", "CHECK_CIRCLE", "Alcanza el stock del color."));
            plano.setOnAction(e -> navigatorPMMD.goTo("corte", Intent.preseleccionar(f.getUid())));
        };
        modelo.valueProperty().addListener((o, a, b) -> calcular.run());
        cantidad.valueProperty().addListener((o, a, b) -> calcular.run());
        tela.valueProperty().addListener((o, a, b) -> calcular.run());
        calcular.run();
        FormGrid f = new FormGrid(3);
        f.campo("Modelo", modelo, 3);
        f.campo("Cantidad", cantidad, 1);
        f.campo("Tela y color", tela, 2);
        VBox cuerpo = new VBox(16, f, res);
        Button cerrar = UI.secondary("Cerrar", null);
        var h = overlaysPMMD.modal("Calculadora de tela", "Metros por modelo, cantidad y color", cuerpo, 620, plano, UI.spacer(), cerrar);
        cerrar.setOnAction(e -> h.cerrar());
    }
}

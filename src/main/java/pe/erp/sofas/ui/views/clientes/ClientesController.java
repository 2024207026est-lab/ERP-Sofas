package pe.erp.sofas.ui.views.clientes;

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
import pe.erp.sofas.service.BusquedaService;
import pe.erp.sofas.service.ClienteService;
import pe.erp.sofas.ui.components.*;

import java.math.BigDecimal;
import java.util.*;

/** Cartera de clientes con total comprado y saldo, ficha lateral (pedidos, ventas, pagos) y CRUD validado. */

@Component
@Lazy
@Scope("prototype")
@RequiredArgsConstructor
public class ClientesController implements Vista {

    private final ClienteService clientesPMMD;

    private final Overlays overlaysPMMD;

    private final Toast toastPMMD;

    private final ConfirmDialog confirmPMMD;

    private final Navigator navigatorPMMD;


    @FXML private VBox raiz;


    private SearchField buscadorPMMD;

    private SegmentedControl tipoPMMD;

    private final TableView<Cliente> tablaPMMD = Cells.tabla("Ningún cliente coincide.");

    private final Map<Long, ClienteService.Resumen> resumenesPMMD = new HashMap<>();

    private final VBox kpisPMMD = new VBox();

    private Overlays.Handle panelPMMD;


    @FXML
    public void initialize() {
        buscadorPMMD = new SearchField("Buscar por nombre, DNI/RUC o teléfono");
        buscadorPMMD.setPrefWidth(320);
        buscadorPMMD.textProperty().addListener((o, a, b) -> render());
        tipoPMMD = new SegmentedControl("Todos", "Persona", "Empresa");
        tipoPMMD.valorProperty().addListener((o, a, b) -> render());
        Button nuevo = UI.primary("Nuevo cliente", "PERSON_ADD");
        nuevo.setOnAction(e -> formulario(null));
        TableColumn<Cliente, String> total = Cells.derecha("Total comprado", c -> MoneyPMMD.fmt(resumenesPMMD.get(c.getId()).totalComprado()), 120, "cell-strong");
        total.setComparator(Comparator.comparing(MoneyPMMD::parse));
        TableColumn<Cliente, String> saldo = Cells.derecha("Saldo pendiente", c -> MoneyPMMD.fmt(resumenesPMMD.get(c.getId()).saldo()), 120);
        saldo.setComparator(Comparator.comparing(MoneyPMMD::parse));
        tablaPMMD.getColumns().addAll(List.of(
                Cells.texto("Código", Cliente::getCodigo, 76, "cell-mono"),
                Cells.nodo("Cliente", c -> UI.hbox(10, new AvatarInitials(c.getNombre(), 30, c.getTipo() == Cliente.Tipo.EMPRESA ? "navy" : "wood"),
                        UI.label(c.getNombre(), "cell-title")), 260),
                Cells.badge("Tipo", c -> c.getTipo() == Cliente.Tipo.EMPRESA ? "Empresa" : "Persona", 90),
                Cells.texto("Documento", c -> c.getTipoDocumento() + " " + c.getDocumento(), 140, "cell-mono"),
                Cells.texto("Teléfono", Cliente::getTelefono, 110),
                Cells.texto("Ciudad", Cliente::getCiudad, 90, "cell-muted"),
                total, saldo));
        tablaPMMD.setRowFactory(tv -> {
            TableRow<Cliente> r = new TableRow<>();
            r.setOnMouseClicked(e -> {
                if (!r.isEmpty()) ficha(r.getItem());
            });
            return r;
        });
        VBox card = new VBox(tablaPMMD);
        VBox.setVgrow(tablaPMMD, Priority.ALWAYS);
        card.getStyleClass().addAll("card", "table-card");
        VBox.setVgrow(card, Priority.ALWAYS);
        raiz.getChildren().setAll(kpisPMMD, UI.hbox(10, buscadorPMMD, tipoPMMD, UI.spacer(), nuevo), card);
    }


    @Override
    public void onShow(Intent intent) {
        render();
        if (intent.es(Intent.ACCION_NUEVO)) formulario(null);
        else if (intent.es(Intent.ACCION_ABRIR) && intent.dato() != null) {
            clientesPMMD.listar().stream().filter(c -> c.getCodigo().equals(intent.dato().toString())).findFirst().ifPresent(this::ficha);
        }
    }


    private void render() {
        List<Cliente> todos = clientesPMMD.listar();
        resumenesPMMD.clear();
        todos.forEach(c -> resumenesPMMD.put(c.getId(), clientesPMMD.resumen(c)));
        BigDecimal saldo = resumenesPMMD.values().stream().map(ClienteService.Resumen::saldo).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal total = resumenesPMMD.values().stream().map(ClienteService.Resumen::totalComprado).reduce(BigDecimal.ZERO, BigDecimal::add);
        kpisPMMD.getChildren().setAll(UI.fila(
                new KpiCard("PEOPLE", "blue", "Clientes", String.valueOf(todos.size()), todos.stream().filter(c -> c.getTipo() == Cliente.Tipo.EMPRESA).count() + " empresas"),
                new KpiCard("ACCOUNT_BALANCE_WALLET", "green", "Total comprado", MoneyPMMD.fmt0(total), "Ventas, pedidos y retapizados"),
                new KpiCard("PENDING_ACTIONS", "amber", "Saldo por cobrar", MoneyPMMD.fmt0(saldo), "Pedidos abiertos"),
                new KpiCard("STAR", "wood", "Mejor cliente", todos.stream().max(Comparator.comparing(c -> resumenesPMMD.get(c.getId()).totalComprado()))
                        .map(Cliente::getNombre).orElse("—"), "Por monto comprado")));
        String q = BusquedaService.normalizar(buscadorPMMD.getText());
        String t = tipoPMMD.getValor();
        tablaPMMD.getItems().setAll(todos.stream()
                .filter(c -> t == null || t.equals("Todos") || (t.equals("Empresa") == (c.getTipo() == Cliente.Tipo.EMPRESA)))
                .filter(c -> q.isEmpty() || BusquedaService.normalizar(c.getNombre() + " " + c.getDocumento() + " " + c.getTelefono() + " " + c.getCodigo()).contains(q))
                .toList());
    }


    private void ficha(Cliente c) {
        if (panelPMMD != null) panelPMMD.cerrar();
        ClienteService.Resumen r = clientesPMMD.resumen(c);
        GridPane datos = UI.grid(2);
        datos.add(UI.dato(c.getTipoDocumento(), c.getDocumento()), 0, 0);
        datos.add(UI.dato("Teléfono", c.getTelefono()), 1, 0);
        datos.add(UI.dato("Correo", c.getEmail()), 0, 1);
        datos.add(UI.dato("Dirección", c.getDireccion() + ", " + c.getCiudad()), 1, 1);
        datos.add(UI.dato("Cliente desde", AppClockPMMD.fmt(c.getFechaRegistro())), 0, 2);
        VBox cuerpo = new VBox(16, UI.hbox(12, new AvatarInitials(c.getNombre(), 52, c.getTipo() == Cliente.Tipo.EMPRESA ? "navy" : "gradient"),
                new VBox(2, UI.label(c.getNombre(), "card-title"), UI.label(c.getCodigo() + " · " + c.getTipo().nombre(), "cell-sub"))), datos,
                UI.hbox(8, UI.miniStat("Compras en tienda", String.valueOf(r.compras())), UI.miniStat("Pedidos", String.valueOf(r.pedidos())),
                        UI.miniStat("Saldo", MoneyPMMD.fmt0(r.saldo()))),
                UI.hbox(8, UI.label("Total comprado", "muted"), UI.spacer(), UI.label(MoneyPMMD.fmt(r.totalComprado()), "price-value")));
        SegmentedControl tabs = new SegmentedControl("Pedidos", "Ventas", "Pagos");
        tabs.getStyleClass().add("tabs");
        VBox lista = new VBox(4);
        Runnable mostrar = () -> {
            lista.getChildren().clear();
            switch (tabs.getValor()) {
                case "Ventas" -> clientesPMMD.ventas(c).forEach(v -> lista.getChildren().add(fila("RECEIPT_LONG", v.getNumeroCompleto() + " · " + MoneyPMMD.fmt(v.getTotal()),
                        AppClockPMMD.fmt(v.getFecha().toLocalDate()) + " · " + v.getItems().stream().map(i -> i.getProducto().getNombre()).reduce((a, b) -> a + ", " + b).orElse(""),
                        new StatusBadge(v.getComprobante().nombre()), () -> navigatorPMMD.goTo("ventas"))));
                case "Pagos" -> clientesPMMD.pagos(c).forEach(p -> lista.getChildren().add(fila("PAYMENTS", MoneyPMMD.fmt(p.getMonto()) + " · " + p.getConcepto(),
                        AppClockPMMD.fmt(p.getFecha()) + " · " + p.getMetodo() + " · " + p.getReferencia(), new Region(), () -> {
                        })));
                default -> clientesPMMD.pedidos(c).forEach(p -> lista.getChildren().add(fila("ASSIGNMENT", p.getCodigo() + " · " + p.getProductoNombre(),
                        "Entrega " + AppClockPMMD.fmt(p.getFechaEntrega()) + " · " + MoneyPMMD.fmt(p.getPrecio()) + " · saldo " + MoneyPMMD.fmt(p.getSaldo()),
                        new StatusBadge(p.getEstado().nombre()), () -> navigatorPMMD.goTo("pedidos", Intent.abrir(p.getCodigo())))));
            }
            if (lista.getChildren().isEmpty()) lista.getChildren().add(UI.label("Sin registros.", "muted"));
        };
        tabs.valorProperty().addListener((o, a, b) -> mostrar.run());
        mostrar.run();
        cuerpo.getChildren().addAll(tabs, lista);
        Button pedido = UI.primary("Nuevo pedido", "ASSIGNMENT");
        pedido.setOnAction(e -> navigatorPMMD.goTo("pedidos", new Intent(Intent.ACCION_NUEVO, c)));
        Button venta = UI.button("Nueva venta", "POINT_OF_SALE", "btn-wood");
        venta.setOnAction(e -> navigatorPMMD.goTo("ventas", new Intent(Intent.ACCION_NUEVO, c)));
        Button editar = UI.secondary("Editar", "EDIT");
        editar.setOnAction(e -> {
            panelPMMD.cerrar();
            formulario(c);
        });
        Button eliminar = UI.iconButton("DELETE_OUTLINE", "Eliminar cliente", "btn-danger-soft");
        eliminar.setOnAction(e -> confirmPMMD.mostrar("¿Eliminar a " + c.getNombre() + "?", "Solo se pueden eliminar clientes sin ventas ni pedidos.", "Eliminar", () -> {
            if (toastPMMD.intentar(() -> clientesPMMD.eliminar(c.getId()))) {
                panelPMMD.cerrar();
                toastPMMD.exito(c.getNombre() + " eliminado.");
                render();
            }
        }));
        panelPMMD = overlaysPMMD.panel("Ficha del cliente", c.getNombre(), cuerpo, UI.hbox(8, eliminar, UI.spacer(), editar, venta, pedido), 520);
    }


    private Node fila(String icono, String titulo, String sub, Node derecha, Runnable r) {
        StackPane ic = new StackPane(Ico.of(icono, 16));
        ic.getStyleClass().add("popup-item-icon");
        VBox t = new VBox(1, UI.label(titulo, "cell-title"), UI.wrap(sub, "cell-sub"));
        HBox.setHgrow(t, Priority.ALWAYS);
        HBox h = new HBox(10, ic, t, derecha);
        h.getStyleClass().add("alert-row");
        h.setOnMouseClicked(e -> r.run());
        return h;
    }


    private void formulario(Cliente existente) {
        Cliente c = existente == null ? Cliente.builder().tipo(Cliente.Tipo.PERSONA).ciudad("Juliaca").build() : existente;
        Validacion v = new Validacion();
        ComboBox<Cliente.Tipo> tipoC = Forms.combo(List.of(Cliente.Tipo.values()), c.getTipo(), Cliente.Tipo::nombre);
        TextField nombre = UI.text(c.getNombre(), "Nombre completo o razón social");
        TextField doc = Forms.digitos(c.getDocumento(), 11);
        TextField tel = UI.text(c.getTelefono(), "951 234 567");
        TextField email = UI.text(c.getEmail(), "correo@ejemplo.com");
        TextField dir = UI.text(c.getDireccion(), "Jr. …");
        TextField ciudad = UI.text(c.getCiudad(), "Juliaca");
        Label docLabel = UI.label("", "form-label");
        FormGrid f = new FormGrid(2);
        f.nodo(v.campo("Tipo", tipoC, null), 1);
        f.nodo(v.campo("DNI / RUC", doc, () -> {
            String d = doc.getText().trim();
            if (tipoC.getValue() == Cliente.Tipo.PERSONA && !d.matches("\\d{8}")) return "El DNI debe tener 8 dígitos.";
            if (tipoC.getValue() == Cliente.Tipo.EMPRESA && !d.matches("\\d{11}")) return "El RUC debe tener 11 dígitos.";
            return null;
        }), 1);
        f.nodo(v.campo("Nombre o razón social", nombre, () -> Validacion.requerido(nombre, "el nombre")), 2);
        f.nodo(v.campo("Teléfono", tel, () -> tel.getText() == null || tel.getText().isBlank() || tel.getText().replace(" ", "").matches("\\+?\\d{6,12}") ? null : "Revisa el teléfono."), 1);
        f.nodo(v.campo("Correo", email, () -> email.getText() == null || email.getText().isBlank() || email.getText().matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+") ? null : "Correo no válido."), 1);
        f.nodo(v.campo("Dirección", dir, null), 1);
        f.nodo(v.campo("Ciudad", ciudad, null), 1);
        v.escuchar(tipoC.valueProperty());
        Button cancelar = UI.secondary("Cancelar", null);
        Button ok = UI.primary(existente == null ? "Registrar cliente" : "Guardar cambios", "SAVE");
        v.enlazar(ok);
        var h = overlaysPMMD.modal(existente == null ? "Nuevo cliente" : "Editar " + c.getCodigo(), "Persona con DNI de 8 dígitos o empresa con RUC de 11", f, 580, cancelar, ok);
        cancelar.setOnAction(e -> h.cerrar());
        ok.setOnAction(e -> {
            c.setTipo(tipoC.getValue());
            c.setNombre(nombre.getText().trim());
            c.setDocumento(doc.getText().trim());
            c.setTelefono(tel.getText());
            c.setEmail(email.getText());
            c.setDireccion(dir.getText());
            c.setCiudad(ciudad.getText());
            Cliente[] g = new Cliente[1];
            if (toastPMMD.intentar(() -> g[0] = clientesPMMD.guardar(c))) {
                h.cerrar();
                toastPMMD.exito(g[0].getCodigo() + " · " + g[0].getNombre() + (existente == null ? " registrado." : " actualizado."));
                render();
            }
        });
    }
}

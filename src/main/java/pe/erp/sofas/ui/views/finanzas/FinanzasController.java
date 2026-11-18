package pe.erp.sofas.ui.views.finanzas;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import pe.erp.sofas.core.*;
import pe.erp.sofas.model.Gasto;
import pe.erp.sofas.model.OrdenCompra;
import pe.erp.sofas.model.Pedido;
import pe.erp.sofas.service.FinanzasService;
import pe.erp.sofas.ui.components.*;

import java.util.List;

/** Finanzas: ingresos, egresos, utilidad, cuentas por cobrar y por pagar y rentabilidad por modelo y por tela. */

@Component
@Lazy
@Scope("prototype")
@RequiredArgsConstructor
public class FinanzasController implements Vista {

    private final FinanzasService finanzasPMMD;

    private final Navigator navigatorPMMD;


    @FXML private VBox raiz;


    private final VBox contenidoPMMD = new VBox(16);


    @FXML
    public void initialize() {
        ScrollPane sp = UI.scroll(contenidoPMMD);
        VBox.setVgrow(sp, Priority.ALWAYS);
        raiz.getChildren().setAll(sp);
    }


    @Override
    public void onShow(Intent intent) {
        FinanzasService.Kpis k = finanzasPMMD.kpis();
        Node kpis = UI.fila(
                new KpiCard("TRENDING_UP", "green", "Ingresos del mes", MoneyPMMD.fmt0(k.ingresos()), "Ventas + cobros de pedidos").bordeSuperior("#16A34A"),
                new KpiCard("TRENDING_DOWN", "red", "Egresos del mes", MoneyPMMD.fmt0(k.egresos()), "Compras recibidas + gastos").bordeSuperior("#DC2626"),
                new KpiCard("ACCOUNT_BALANCE_WALLET", "blue", "Utilidad", MoneyPMMD.fmt0(k.utilidad()), String.format("Margen %.0f %%", k.ingresos() == 0 ? 0 : k.utilidad() * 100 / k.ingresos())).bordeSuperior("#2563EB"),
                new KpiCard("PENDING_ACTIONS", "amber", "Cuentas por cobrar", MoneyPMMD.fmt0(k.porCobrar()), finanzasPMMD.porCobrar().size() + " pedidos con saldo").bordeSuperior("#F59E0B")
                        .alClic(() -> navigatorPMMD.goTo("pedidos")),
                new KpiCard("RECEIPT_LONG", "wood", "Cuentas por pagar", MoneyPMMD.fmt0(k.porPagar()), finanzasPMMD.porPagar().size() + " órdenes de compra").bordeSuperior("#A47148")
                        .alClic(() -> navigatorPMMD.goTo("compras")));
        var meses = finanzasPMMD.ultimos12();
        var chart = Charts.barras(List.of(
                new Charts.Serie("Ingresos", meses.stream().map(FinanzasService.Mes::etiqueta).toList(), meses.stream().map(FinanzasService.Mes::ingresos).toList()),
                new Charts.Serie("Egresos", meses.stream().map(FinanzasService.Mes::etiqueta).toList(), meses.stream().map(FinanzasService.Mes::egresos).toList())),
                MoneyPMMD::corto, null);
        chart.setPrefHeight(280);
        VBox grafico = UI.card(UI.cardHeader("Ingresos frente a egresos", "Últimos 12 meses (octubre: en curso)"), chart);

        TableView<Pedido> cxc = Cells.tabla("Sin saldos por cobrar.");
        cxc.getColumns().addAll(List.of(
                Cells.texto("Pedido", Pedido::getCodigo, 80, "cell-mono"),
                Cells.texto("Cliente", p -> p.getCliente().getNombre(), 190, "cell-strong"),
                Cells.texto("Entrega", p -> AppClockPMMD.fmt(p.getFechaEntrega()), 90),
                Cells.derecha("Saldo", p -> MoneyPMMD.fmt(p.getSaldo()), 100, "cell-strong")));
        cxc.getItems().setAll(finanzasPMMD.porCobrar());
        alto(cxc, 6);
        cxc.setRowFactory(tv -> {
            TableRow<Pedido> r = new TableRow<>();
            r.setOnMouseClicked(e -> {
                if (!r.isEmpty()) navigatorPMMD.goTo("pedidos", Intent.abrir(r.getItem().getCodigo()));
            });
            return r;
        });
        TableView<OrdenCompra> cxp = Cells.tabla("Sin cuentas por pagar.");
        cxp.getColumns().addAll(List.of(
                Cells.texto("OC", OrdenCompra::getCodigo, 80, "cell-mono"),
                Cells.texto("Proveedor", o -> o.getProveedor().getRazonSocial(), 190, "cell-strong"),
                Cells.texto("Condición", o -> o.getProveedor().getCondicionPago(), 110, "cell-muted"),
                Cells.derecha("Total", o -> MoneyPMMD.fmt(o.getTotal()), 100, "cell-strong")));
        cxp.getItems().setAll(finanzasPMMD.porPagar());
        alto(cxp, 6);
        Node cuentas = UI.columnas(new double[]{1, 1}, tarjeta("Cuentas por cobrar", "Saldos de pedidos abiertos", cxc),
                tarjeta("Cuentas por pagar", "OC aprobadas o recibidas sin pago", cxp));

        TableView<FinanzasService.Rentabilidad> modelo = tablaRent("Modelo");
        modelo.getItems().setAll(finanzasPMMD.porModelo());
        alto(modelo, 8);
        TableView<FinanzasService.Rentabilidad> tela = tablaRent("Tela");
        tela.getItems().setAll(finanzasPMMD.porTela());
        alto(tela, 6);
        Node rent = UI.columnas(new double[]{1.2, 1}, tarjeta("Rentabilidad por modelo", "Ventas y pedidos frente al costo estándar", modelo),
                tarjeta("Rentabilidad por tela", "Pedidos y metros consumidos por tipo", tela));

        TableView<Gasto> gastos = Cells.tabla("Sin gastos.");
        gastos.getColumns().addAll(List.of(
                Cells.texto("Fecha", g -> AppClockPMMD.fmt(g.getFecha()), 90),
                Cells.texto("Categoría", Gasto::getCategoria, 120),
                Cells.texto("Concepto", Gasto::getConcepto, 300, "cell-muted"),
                Cells.derecha("Monto", g -> MoneyPMMD.fmt(g.getMonto()), 100, "cell-strong")));
        gastos.getItems().setAll(finanzasPMMD.gastosMes());
        alto(gastos, 5);
        contenidoPMMD.getChildren().setAll(kpis, grafico, cuentas, rent, tarjeta("Gastos del mes", "Egresos operativos de octubre", gastos));
    }


    private TableView<FinanzasService.Rentabilidad> tablaRent(String titulo) {
        TableView<FinanzasService.Rentabilidad> t = Cells.tabla("Sin datos.");
        t.getColumns().addAll(List.of(
                Cells.nodo(titulo, r -> new VBox(0, UI.label(r.nombre(), "cell-title"), UI.label(r.detalle(), "cell-sub")), 200),
                Cells.derecha("Unid.", r -> String.valueOf(r.unidades()), 60),
                Cells.derecha("Ingresos", r -> MoneyPMMD.fmt0(r.ingresos()), 100),
                Cells.derecha("Costo", r -> MoneyPMMD.fmt0(r.costo()), 100, "cell-muted"),
                Cells.derecha("Margen", r -> MoneyPMMD.fmt0(r.margen()), 100, "cell-strong"),
                Cells.nodo("%", r -> new StatusBadge(String.format("%.0f %%", r.margenPct()), r.margenPct() >= 35 ? "green" : r.margenPct() >= 20 ? "amber" : "red"), 70)));
        return t;
    }


    private static void alto(TableView<?> t, int filas) {
        t.setFixedCellSize(46);
        t.setPrefHeight(46 * Math.max(3, Math.min(filas, Math.max(t.getItems().size(), 3))) + 44);
    }


    private Node tarjeta(String titulo, String sub, TableView<?> t) {
        VBox v = new VBox(10, UI.cardHeader(titulo, sub), t);
        v.getStyleClass().add("card");
        v.setStyle("-fx-padding: 16 0 0 0;");
        ((HBox) v.getChildren().get(0)).setStyle("-fx-padding: 0 18;");
        return v;
    }
}

package pe.erp.sofas.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import pe.erp.sofas.core.NegocioExceptionPMMD;
import pe.erp.sofas.model.*;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;


@IntegracionTest
class VentasYPedidosTest {

    @Autowired VentaServicePMMD ventas;

    @Autowired ProductoService productos;

    @Autowired ClienteService clientes;

    @Autowired PedidoServicePMMD pedidos;

    @Autowired ConfiguradorService configurador;

    @Autowired ProduccionServicePMMD produccion;


    @Test
    void cobrarDescuentaStockYNumeraLaBoleta() {
        Producto p = productos.porCodigo("SF-001").orElseThrow();
        int stock = p.getStock();
        int numero = ventas.siguienteNumero(Venta.Comprobante.BOLETA);
        Venta v = ventas.cobrar(new VentaServicePMMD.Cobro(null, Venta.Comprobante.BOLETA,
                List.of(new VentaServicePMMD.Linea(p.getId(), 1, null)), BigDecimal.ZERO, "Efectivo", new BigDecimal("2500"), null, null, null));
        assertThat(v.getSerie()).isEqualTo("B001");
        assertThat(v.getNumero()).isEqualTo(numero);
        assertThat(v.getVuelto()).isEqualByComparingTo("10.00");
        assertThat(productos.porCodigo("SF-001").orElseThrow().getStock()).isEqualTo(stock - 1);
    }


    @Test
    void facturaExigeRucYMixtoDebeSumarElTotal() {
        Producto p = productos.porCodigo("SF-010").orElseThrow();
        Cliente persona = clientes.listar().stream().filter(c -> !c.tieneRuc()).findFirst().orElseThrow();
        assertThatThrownBy(() -> ventas.cobrar(new VentaServicePMMD.Cobro(persona.getId(), Venta.Comprobante.FACTURA,
                List.of(new VentaServicePMMD.Linea(p.getId(), 1, null)), null, "Tarjeta", null, null, null, null)))
                .isInstanceOf(NegocioExceptionPMMD.class).hasMessageContaining("RUC");
        assertThatThrownBy(() -> ventas.cobrar(new VentaServicePMMD.Cobro(null, Venta.Comprobante.BOLETA,
                List.of(new VentaServicePMMD.Linea(p.getId(), 1, null)), null, "Mixto", null, "Yape", new BigDecimal("100"), new BigDecimal("100"))))
                .isInstanceOf(NegocioExceptionPMMD.class);
        Venta ok = ventas.cobrar(new VentaServicePMMD.Cobro(null, Venta.Comprobante.BOLETA,
                List.of(new VentaServicePMMD.Linea(p.getId(), 1, null)), null, "Mixto", null, "Yape", new BigDecimal("160"), new BigDecimal("100")));
        assertThat(ok.getMetodoPago()).startsWith("Mixto");
        Producto sinStock = productos.porCodigo("SF-007").orElseThrow();
        assertThatThrownBy(() -> ventas.cobrar(new VentaServicePMMD.Cobro(null, Venta.Comprobante.BOLETA,
                List.of(new VentaServicePMMD.Linea(sinStock.getId(), 1, null)), null, "Tarjeta", null, null, null, null)))
                .isInstanceOf(NegocioExceptionPMMD.class).hasMessageContaining("Stock insuficiente");
    }


    @Test
    void pedidoNecesitaElCincuentaPorCientoParaConfirmarYGenerarOP() {
        Pedido p = pedidos.listar().stream().filter(x -> x.getCodigo().equals("PE-0249")).findFirst().orElseThrow();
        assertThatThrownBy(() -> pedidos.avanzarA(p.getId(), EstadoPedido.CONFIRMADO)).isInstanceOf(NegocioExceptionPMMD.class);
        assertThatThrownBy(() -> pedidos.generarOP(p.getId(), null)).isInstanceOf(NegocioExceptionPMMD.class);
        Pedido conAdelanto = pedidos.registrarAdelanto(p.getId(), p.getPrecio().multiply(new BigDecimal("0.5")), "Yape");
        assertThat(conAdelanto.getEstado()).isEqualTo(EstadoPedido.CONFIRMADO);
        OrdenProduccion op = pedidos.generarOP(p.getId(), null);
        assertThat(op.getFichaUid()).isEqualTo("FT-009v1");
        assertThat(pedidos.pedido(p.getId()).getEstado()).isEqualTo(EstadoPedido.EN_PRODUCCION);
        assertThat(pedidos.pedido(p.getId()).getOpCodigo()).isEqualTo(op.getCodigo());
    }


    @Test
    void crearPedidoDesdeElConfiguradorConSuOP() {
        Cliente c = clientes.listar().get(0);
        ConfigSofa cfg = configurador.defecto(ConfigSofa.Modelo.CHESTERFIELD);
        ConfiguradorService.Cotizacion cot = configurador.cotizar(cfg);
        Pedido p = configurador.crearPedido(cfg, c.getId(), null, BigDecimal.valueOf(cot.precio() / 2), "Efectivo", null, true, null);
        assertThat(p.getTipo()).isEqualTo(Pedido.Tipo.PERSONALIZADO);
        assertThat(p.getPrecio()).isEqualByComparingTo(BigDecimal.valueOf(cot.precio()));
        assertThat(p.getOpCodigo()).isNotNull();
        OrdenProduccion op = produccion.porCodigo(p.getOpCodigo()).orElseThrow();
        assertThat(op.getFichaUid()).isEqualTo("FT-001v3");
        assertThat(op.getExtras()).anyMatch(x -> x.getMaterialCodigo().equals("MP-016") && x.getCantidad() == 48);
        assertThat(op.getSustituciones()).containsEntry("MP-003", "MP-004");
        assertThat(ConfigSofa.leer(p.getConfiguracion())).isEqualTo(cfg);
    }
}

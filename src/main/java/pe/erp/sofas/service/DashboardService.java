package pe.erp.sofas.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.erp.sofas.core.AppClockPMMD;
import pe.erp.sofas.model.*;
import pe.erp.sofas.repository.ResumenMensualRepository;

import java.time.LocalDate;
import java.util.*;

/** Indicadores y series del Dashboard (todo calculado de la base compartida). */

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    public record Kpis(
      double ventasMes, 
        double variacionPct, 
          int sofasVendidos, 
            long sofasFabricacion, 
              long ordenesFabricacion,
                       
                       long pedidosPendientesPMMD, 
                         double metrosTelaPMMD, 
                           long colores) {
    }


    public record Entrega(
      LocalDate fecha, 
        String titulo, 
          String cliente, 
            String codigo, 
              String modulo, 
                String estado) {
    }


    public record Segmento(
      String nombre, 
        double valor, 
          String color) {
    }


    private final VentaServicePMMD ventaServicePMMD;

    private final ProduccionServicePMMD produccionServicePMMD;

    private final PedidoServicePMMD pedidoServicePMMD;

    private final TelaService telaServicePMMD;

    private final FinanzasService finanzasServicePMMD;

    private final ResumenMensualRepository resumenRepositoryPMMD;


    public Kpis kpis() {
        LocalDate anterior = AppClockPMMD.hoy().minusMonths(1);
        double previo = resumenRepositoryPMMD.findAll().stream()
                .filter(r -> r.getAnio() == anterior.getYear() && r.getMes() == anterior.getMonthValue())
                .mapToDouble(r -> r.getVentas().doubleValue()).findFirst().orElse(0);
        double ventas = ventaServicePMMD.totalMes().doubleValue();
        var k = produccionServicePMMD.kpis();
        return new Kpis(ventas, previo == 0 ? 0 : (ventas - previo) * 100 / previo, ventaServicePMMD.sofasMes(),
                k.sofasEnProceso(), k.ordenesEnProceso(), pedidoServicePMMD.pendientesAMedida(), telaServicePMMD.metrosEnStock(),
                telaServicePMMD.activas().size());
    }


    public List<FinanzasService.Mes> ventas12() {
        return finanzasServicePMMD.ultimos12();
    }

    /** Ventas del mes por grupo de modelo. */

    public List<Segmento> ventasPorModelo() {
        Map<String, Double> m = new LinkedHashMap<>();
        for (String g : List.of("Sofá moderno", "Seccional", "Juego de sala", "Sofá cama", "Chesterfield", "Otros")) m.put(g, 0d);
        for (Venta v : ventaServicePMMD.delMes(AppClockPMMD.hoy())) {
            double factor = v.getItems().stream().mapToDouble(i -> i.getSubtotal().doubleValue()).sum();
            factor = factor == 0 ? 0 : v.getTotal().doubleValue() / factor;
            for (ItemVenta i : v.getItems()) m.merge(grupo(i.getProducto().getForma()), i.getSubtotal().doubleValue() * factor, Double::sum);
        }
        String[] colores = {"#2563EB", "#A47148", "#14B8A6", "#8B5CF6", "#C69A6D", "#94A3B8"};
        List<Segmento> l = new ArrayList<>();
        int i = 0;
        for (var e : m.entrySet()) l.add(new Segmento(e.getKey(), e.getValue(), colores[i++]));
        return l;
    }


    public static String grupo(Forma f) {
        return switch (f) {
            case MODERNO -> "Sofá moderno";
            case SECCIONAL_L, SECCIONAL_U -> "Seccional";
            case JUEGO -> "Juego de sala";
            case SOFA_CAMA -> "Sofá cama";
            case CHESTERFIELD -> "Chesterfield";
            default -> "Otros";
        };
    }


    public List<Entrega> proximasEntregas(int max) {
        List<Entrega> l = new ArrayList<>();
        for (Pedido p : pedidoServicePMMD.listar()) {
            if (p.getEstado().cerrado() || p.getFechaEntrega() == null || p.getFechaEntrega().isBefore(AppClockPMMD.hoy().minusDays(3))) continue;
            l.add(new Entrega(p.getFechaEntrega(), p.getCantidad() + " × " + p.getProductoNombre(), p.getCliente().getNombre(),
                    p.getCodigo(), "pedidos", p.isRetrasado() ? "Retrasado" : p.getEstado().nombre()));
        }
        l.sort(Comparator.comparing(Entrega::fecha));
        return l.subList(0, Math.min(max, l.size()));
    }


    public List<Pedido> ultimosPedidos(int max) {
        List<Pedido> l = pedidoServicePMMD.listar();
        return l.subList(0, Math.min(max, l.size()));
    }
}

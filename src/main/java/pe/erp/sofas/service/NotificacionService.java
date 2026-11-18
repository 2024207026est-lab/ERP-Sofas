package pe.erp.sofas.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.erp.sofas.core.AppClockPMMD;
import pe.erp.sofas.model.*;
import pe.erp.sofas.repository.NotificacionEstadoRepository;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Alertas calculadas sobre los datos (stock critico, ordenes retrasadas, entregas de manana, material agotado
 * y ordenes bloqueadas). Lo leido y lo descartado se guarda por clave.
 */

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificacionService {
    
    /** tipo: danger · warn · info · success. */

    public record Notificacion(
      String clave, 
        String tipo, 
          String icono, 
            String titulo, 
              String detalle, 
                String hace,
                               
                               String modulo, 
                                 Object dato) {
    }


    private final InventarioService inventarioServicePMMD;

    private final TelaService telaServicePMMD;

    private final ProduccionServicePMMD produccionServicePMMD;

    private final PedidoServicePMMD pedidoServicePMMD;

    private final NotificacionEstadoRepository estadoRepositoryPMMD;


    public List<Notificacion> todas() {
        List<Notificacion> l = new ArrayList<>();
        List<String> criticos = new ArrayList<>();
        inventarioServicePMMD.materiales().stream().filter(m -> "Crítico".equals(m.getEstado()))
                .forEach(m -> criticos.add(m.getNombre().toLowerCase(Locale.ROOT)));
        telaServicePMMD.activas().stream().filter(t -> "Stock bajo".equals(t.getEstado()))
                .forEach(t -> criticos.add("tela " + t.getColor()));
        if (!criticos.isEmpty()) {
            l.add(new Notificacion("stock:" + String.join(",", criticos), "danger", "ALL_INBOX",
                    "Stock crítico: " + unir(criticos), "Por debajo del mínimo; revisa el requerimiento de materiales.",
                    "Hace 20 min", "mrp", null));
        }
        for (OrdenProduccion op : produccionServicePMMD.activas()) {
            if (!op.isRetrasada()) continue;
            String cliente = op.getCliente() == null ? "stock" : primerosNombresPMMD(op.getCliente().getNombre());
            l.add(new Notificacion("retraso:" + op.getCodigo() + ":" + op.getFechaEntrega(), "danger", "ALARM",
                    "Orden retrasada: " + modeloCortoPMMD(op.getProductoNombre()) + " de " + cliente + ", vencida el " + AppClockPMMD.corta(op.getFechaEntrega()),
                    op.getCodigo() + " · sigue en " + op.getEtapa().nombre(),
                    "Hace 1 h", "produccion", op.getCodigo()));
        }
        var manana = AppClockPMMD.hoy().plusDays(1);
        for (Pedido p : pedidoServicePMMD.listar()) {
            if (p.getEstado().cerrado() || !manana.equals(p.getFechaEntrega())) continue;
            l.add(new Notificacion("entrega:" + p.getCodigo(), "info", "LOCAL_SHIPPING",
                    "Entrega mañana: " + p.getCantidad() + (p.getCantidad() == 1 ? " sofá" : " sofás") + " para " + nombreCortoPMMD(p.getCliente().getNombre()),
                    p.getCodigo() + " · " + p.getProductoNombre() + " · " + p.getEstado().nombre(), "Hace 2 h", "pedidos", p.getCodigo()));
        }
        List<Material> agotados = inventarioServicePMMD.materiales().stream()
                .filter(m -> "Agotado".equals(m.getEstado()) && !m.getBajoPedido()).toList();
        if (!agotados.isEmpty()) {
            l.add(new Notificacion("agotado:" + agotados.stream().map(Material::getCodigo).collect(Collectors.joining(",")), "warn", "REMOVE_SHOPPING_CART",
                    "Material agotado: " + unir(agotados.stream().map(m -> m.getNombre().toLowerCase(Locale.ROOT)).toList()),
                    "Sin disponible para nuevas órdenes. Genera la orden de compra.", "Hace 3 h", "inventario",
                    agotados.get(0).getCodigo()));
        }
        List<OrdenProduccion> bloqueadas = bloqueadas();
        if (!bloqueadas.isEmpty()) {
            l.add(new Notificacion("bloqueadas:" + bloqueadas.stream().map(OrdenProduccion::getCodigo).collect(Collectors.joining(",")), "warn", "BLOCK",
                    bloqueadas.size() + (bloqueadas.size() == 1 ? " orden bloqueada" : " órdenes bloqueadas") + " por falta de material",
                    bloqueadas.stream().map(OrdenProduccion::getCodigo).collect(Collectors.joining(", ")), "Hoy", "produccion", null));
        }
        Set<String> descartadas = new HashSet<>();
        estadoRepositoryPMMD.findAll().forEach(e -> {
            if (Boolean.TRUE.equals(e.getDescartada())) descartadas.add(e.getClave());
        });
        l.removeIf(n -> descartadas.contains(n.clave()));
        return l;
    }

    /** OP en produccion con materiales parciales o faltantes. */

    public List<OrdenProduccion> bloqueadas() {
        return produccionServicePMMD.activas().stream().filter(o -> o.getEtapa().productiva() && !o.isSinFicha())
                .filter(o -> produccionServicePMMD.estadoMateriales(o).faltantes() > 0).toList();
    }


    public boolean leida(Notificacion n) {
        return estadoRepositoryPMMD.findById(n.clave()).map(e -> Boolean.TRUE.equals(e.getLeida())).orElse(false);
    }


    public long noLeidas() {
        return todas().stream().filter(n -> !leida(n)).count();
    }


    @Transactional
    public void marcarLeida(Notificacion n) {
        var e = estadoRepositoryPMMD.findById(n.clave()).orElse(new NotificacionEstado(n.clave(), false, false));
        e.setLeida(true);
        estadoRepositoryPMMD.save(e);
    }


    @Transactional
    public void marcarTodasLeidas() {
        todas().forEach(this::marcarLeida);
    }


    @Transactional
    public void descartar(Notificacion n) {
        var e = estadoRepositoryPMMD.findById(n.clave()).orElse(new NotificacionEstado(n.clave(), true, false));
        e.setDescartada(true);
        estadoRepositoryPMMD.save(e);
    }


    private static String unir(List<String> l) {
        if (l.size() == 1) return l.get(0);
        return String.join(", ", l.subList(0, l.size() - 1)) + " y " + l.get(l.size() - 1);
    }

    /** "Reclinable 3 cuerpos" → "reclinable". */

    private static String modeloCortoPMMD(String n) {
        String s = n.split(" · ")[0].replaceAll("\\s+\\d+ cuerpos?$", "");
        return s.toLowerCase(Locale.ROOT);
    }

    /** "Carmen Flores Pari" → "Carmen Flores". */

    private static String primerosNombresPMMD(String n) {
        String[] p = n.split(" ");
        return p.length >= 2 ? p[0] + " " + p[1] : n;
    }

    /** "Hotel Royal Inn Juliaca S.A.C." → "el Hotel Royal Inn". */

    private static String nombreCortoPMMD(String n) {
        if (n.startsWith("Hotel")) return "el " + n.replace(" Juliaca", "").replace(" S.A.C.", "");
        return primerosNombresPMMD(n);
    }
}

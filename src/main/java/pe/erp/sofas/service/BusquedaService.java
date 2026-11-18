package pe.erp.sofas.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.erp.sofas.model.*;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Buscador global (Ctrl+K): sofas, telas, clientes, pedidos, ordenes y materiales. */

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BusquedaService {

    public record Resultado(
      String grupo, 
        String icono, 
          String titulo, 
            String subtitulo, 
              String modulo, 
                Object dato) {
    }


    private static final int MAX_POR_GRUPOPMMD = 4;


    private final ProductoService productoServicePMMD;

    private final TelaService telaServicePMMD;

    private final ClienteService clienteServicePMMD;

    private final PedidoServicePMMD pedidoServicePMMD;

    private final ProduccionServicePMMD produccionServicePMMD;

    private final InventarioService inventarioServicePMMD;


    public List<Resultado> buscar(String texto) {
        String q = normalizar(texto);
        List<Resultado> r = new ArrayList<>();
        if (q.length() < 2) return r;
        int n = 0;
        for (Producto p : productoServicePMMD.listar()) {
            if (n < MAX_POR_GRUPOPMMD && coincide(q, p.getCodigo(), p.getNombre(), p.getCategoria(), p.getColor())) {
                r.add(new Resultado("Sofás", "WEEKEND", p.getNombre(), p.getCodigo() + " · " + p.getMedidas(), "modelos", p.getCodigo()));
                n++;
            }
        }
        n = 0;
        for (Tela t : telaServicePMMD.telas()) {
            if (n < MAX_POR_GRUPOPMMD && coincide(q, t.getCodigo(), t.getColor(), t.getTipo().nombre(), t.getCodigoProveedor())) {
                r.add(new Resultado("Telas", "PALETTE", t.getNombre(), t.getCodigo() + " · " + InventarioService.fmt(t.getStock()) + " m · " + t.getEstado(), "telas", t.getCodigo()));
                n++;
            }
        }
        n = 0;
        for (Cliente c : clienteServicePMMD.listar()) {
            if (n < MAX_POR_GRUPOPMMD && coincide(q, c.getCodigo(), c.getNombre(), c.getDocumento(), c.getTelefono())) {
                r.add(new Resultado("Clientes", "PERSON", c.getNombre(), c.getCodigo() + " · " + c.getTipoDocumento() + " " + c.getDocumento(), "clientes", c.getCodigo()));
                n++;
            }
        }
        n = 0;
        for (Pedido p : pedidoServicePMMD.listar()) {
            if (n < MAX_POR_GRUPOPMMD && coincide(q, p.getCodigo(), p.getCliente().getNombre(), p.getProductoNombre())) {
                r.add(new Resultado("Pedidos", "ASSIGNMENT", p.getCodigo() + " · " + p.getProductoNombre(), p.getCliente().getNombre() + " · " + p.getEstado().nombre(), "pedidos", p.getCodigo()));
                n++;
            }
        }
        n = 0;
        for (OrdenProduccion o : produccionServicePMMD.ordenes()) {
            if (n < MAX_POR_GRUPOPMMD && coincide(q, o.getCodigo(), o.getProductoNombre(), o.getClienteNombre(), o.getTelaNombre())) {
                r.add(new Resultado("Órdenes", "BUILD", o.getCodigo() + " · " + o.getProductoNombre(), o.getClienteNombre() + " · " + o.getEtapa().nombre(), "produccion", o.getCodigo()));
                n++;
            }
        }
        n = 0;
        for (Material m : inventarioServicePMMD.materiales()) {
            if (n < MAX_POR_GRUPOPMMD && coincide(q, m.getCodigo(), m.getNombre(), m.getCategoria())) {
                r.add(new Resultado("Materiales", "ALL_INBOX", m.getNombre(), m.getCodigo() + " · " + InventarioService.fmt(m.getStock()) + " " + m.getUnidad() + " · " + m.getEstado(), "inventario", m.getCodigo()));
                n++;
            }
        }
        return r;
    }


    private static boolean coincide(String q, String... campos) {
        for (String c : campos) if (c != null && normalizar(c).contains(q)) return true;
        return false;
    }


    public static String normalizar(String s) {
        if (s == null) return "";
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT).trim();
    }
}

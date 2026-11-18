package pe.erp.sofas.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.erp.sofas.core.NegocioExceptionPMMD;
import pe.erp.sofas.model.*;
import pe.erp.sofas.repository.*;

import java.util.*;

/**
 * Muestrario de telas. Regla: el stock del material de inventario de cada tipo de tela es la suma de los
 * metros de sus colores no descontinuados.
 */

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TelaService {

    private final TelaRepository telaRepositoryPMMD;

    private final MaterialRepository materialRepositoryPMMD;

    private final MovimientoRepository movimientoRepositoryPMMD;

    private final ProductoRepository productoRepositoryPMMD;

    private final PedidoRepository pedidoRepositoryPMMD;

    private final OrdenProduccionRepository opRepositoryPMMD;

    private final InventarioService inventarioServicePMMD;

    /** Usos de una tela en el catalogo, pedidos y produccion. */

    public record Usos(
      List<Producto> productos, 
        List<Pedido> pedidos, 
          List<OrdenProduccion> ordenes) {

        public int total() {
            return productos.size() + pedidos.size() + ordenes.size();
        }
    }


    public List<Tela> telas() {
        return telaRepositoryPMMD.findAllByOrderByCodigoAsc();
    }


    public List<Tela> activas() {
        return telas().stream().filter(t -> !t.getDescontinuada()).toList();
    }


    public List<Tela> porTipo(TipoTela tipo) {
        return telas().stream().filter(t -> t.getTipo() == tipo).toList();
    }


    public Tela tela(Long id) {
        return telaRepositoryPMMD.findById(id).orElseThrow(() -> new NegocioExceptionPMMD("La tela ya no existe."));
    }


    public Optional<Tela> porCodigo(String codigo) {
        return telaRepositoryPMMD.findByCodigo(codigo);
    }


    public double metrosEnStock() {
        return activas().stream().mapToDouble(Tela::getStock).sum();
    }


    public double valorMuestrario() {
        return activas().stream().mapToDouble(t -> t.getStock() * t.getTipo().costoMetro()).sum();
    }


    public long conAlerta() {
        return activas().stream().filter(t -> !"Disponible".equals(t.getEstado())).count();
    }


    public String siguienteCodigo() {
        return Codigos.siguiente(telaRepositoryPMMD.findAll(), Tela::getCodigo, "TL-", 2);
    }

    /** Ajusta el stock del material de inventario a la suma de los colores activos. */

    @Transactional
    public void sincronizar(TipoTela tipo) {
        materialRepositoryPMMD.findByCodigo(tipo.materialCodigo()).ifPresent(m -> {
            double suma = telaRepositoryPMMD.findByTipo(tipo).stream().filter(t -> !t.getDescontinuada()).mapToDouble(Tela::getStock).sum();
            m.setStock(suma);
            materialRepositoryPMMD.save(m);
        });
    }

    /** Entrada de rollo: suma al color y al material, y registra la Entrada en el kardex. */

    @Transactional
    public Tela entradaRollo(Long telaId, double metros, String referencia) {
        if (metros <= 0) throw new NegocioExceptionPMMD("Ingresa los metros recibidos.");
        Tela t = tela(telaId);
        if (t.getDescontinuada()) throw new NegocioExceptionPMMD("El color está descontinuado. Reactívalo para recibir rollos.");
        t.setStock(t.getStock() + metros);
        telaRepositoryPMMD.save(t);
        Material m = inventarioServicePMMD.material(t.getTipo().materialCodigo());
        inventarioServicePMMD.registrar(m, TipoMovimiento.ENTRADA, metros, referencia == null || referencia.isBlank() ? "ROLLO" : referencia,
                "Rollo " + t.getColor() + " " + t.getCodigo());
        return t;
    }


    @Transactional
    public Tela registrar(TipoTela tipo, String color, String hex, String codigoProveedor, double stock, double minimo) {
        if (color == null || color.isBlank()) throw new NegocioExceptionPMMD("Escribe el nombre del color.");
        if (stock < 0 || minimo < 0) throw new NegocioExceptionPMMD("Los metros no pueden ser negativos.");
        boolean existe = porTipo(tipo).stream().anyMatch(t -> t.getColor().equalsIgnoreCase(color.trim()));
        if (existe) throw new NegocioExceptionPMMD("Ya existe el color " + color.trim() + " en " + tipo.nombre() + ".");
        Tela t = telaRepositoryPMMD.save(Tela.builder().codigo(siguienteCodigo()).tipo(tipo).color(color.trim())
                .hex(hex).codigoProveedor(codigoProveedor).stock(0d).minimo(minimo).build());
        if (stock > 0) entradaRollo(t.getId(), stock, "INI-" + t.getCodigo());
        return t;
    }


    @Transactional
    public Tela alternarDescontinuada(Long telaId) {
        Tela t = tela(telaId);
        Material m = inventarioServicePMMD.material(t.getTipo().materialCodigo());
        boolean descontinuar = !t.getDescontinuada();
        if (descontinuar && t.getStock() > 0 && m.getStock() - t.getStock() < m.getReservado() - 1e-9) {
            throw new NegocioExceptionPMMD("No se puede descontinuar: sus metros están reservados para producción.");
        }
        t.setDescontinuada(descontinuar);
        telaRepositoryPMMD.save(t);
        if (t.getStock() > 0) {
            // El material deja de contar (o vuelve a contar) los metros del color.
            movimientoRepositoryPMMD.save(Movimiento.builder().material(m)
                    .fecha(java.time.LocalDateTime.of(pe.erp.sofas.core.AppClockPMMD.hoy(), pe.erp.sofas.core.AppClockPMMD.HORA))
                    .tipo(TipoMovimiento.AJUSTE).cantidad(descontinuar ? -t.getStock() : t.getStock())
                    .referencia(t.getCodigo()).concepto((descontinuar ? "Descontinuado " : "Reactivado ") + t.getColor())
                    .saldo(m.getStock() + (descontinuar ? -t.getStock() : t.getStock())).usuario("sistema").build());
        }
        sincronizar(t.getTipo());
        return t;
    }

    /** Descuenta metros de un color (consumo de una OP o venta de retazo) y mantiene la suma del material. */

    @Transactional
    public void descontar(Tela t, double metros) {
        Tela fresca = tela(t.getId());
        fresca.setStock(Math.max(0, fresca.getStock() - metros));
        telaRepositoryPMMD.save(fresca);
    }


    public Usos usos(Tela t) {
        String nombre = t.getColor().toLowerCase(Locale.ROOT);
        List<Producto> productos = productoRepositoryPMMD.findAllByOrderByCodigoAsc().stream()
                .filter(p -> p.getColor() != null && p.getColor().toLowerCase(Locale.ROOT).equals(nombre)
                        && p.getMaterial() != null && t.getTipo().nombre().toLowerCase(Locale.ROOT).startsWith(p.getMaterial().toLowerCase(Locale.ROOT).split(" ")[0]))
                .toList();
        List<Pedido> pedidos = pedidoRepositoryPMMD.findAllByOrderByCodigoDesc().stream()
                .filter(p -> p.getTela() != null && p.getTela().getId().equals(t.getId())).toList();
        List<OrdenProduccion> ops = opRepositoryPMMD.findAllByOrderByCodigoAsc().stream()
                .filter(o -> o.getTela() != null && o.getTela().getId().equals(t.getId()) && !o.getAnulada()).toList();
        return new Usos(productos, pedidos, ops);
    }

    /** Disponibilidad textual para el configurador. */

    public String notaDisponibilidad(Tela t, double metrosNecesarios) {
        if (t.getDescontinuada()) return "Color descontinuado";
        if (t.getStock() <= 0) return "Agotado · reposición en 7 días";
        if (t.getStock() < metrosNecesarios) return "Solo " + InventarioService.fmt(t.getStock()) + " m · faltan " + InventarioService.fmt(metrosNecesarios - t.getStock()) + " m";
        return InventarioService.fmt(t.getStock()) + " m disponibles";
    }
}

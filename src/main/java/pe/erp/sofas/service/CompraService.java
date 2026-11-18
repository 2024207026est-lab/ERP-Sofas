package pe.erp.sofas.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.erp.sofas.core.AppClockPMMD;
import pe.erp.sofas.core.NegocioExceptionPMMD;
import pe.erp.sofas.model.*;
import pe.erp.sofas.repository.*;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

/** Ordenes de compra: Borrador → Solicitada → Aprobada → Recibida (o Cancelada). */

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompraService {

    public record LineaCompra(
      Long materialId, 
        Long telaId, 
          double cantidad, 
            BigDecimal precioUnitario) {
    }


    private final OrdenCompraRepository repositoryPMMD;

    private final ProveedorRepository proveedorRepositoryPMMD;

    private final MaterialRepository materialRepositoryPMMD;

    private final TelaRepository telaRepositoryPMMD;

    private final InventarioService inventarioServicePMMD;

    private final TelaService telaServicePMMD;


    public List<OrdenCompra> listar() {
        return repositoryPMMD.findAllByOrderByCodigoDesc();
    }


    public OrdenCompra orden(Long id) {
        return repositoryPMMD.findById(id).orElseThrow(() -> new NegocioExceptionPMMD("La orden de compra ya no existe."));
    }


    public String siguienteCodigo() {
        return Codigos.siguiente(repositoryPMMD.findAll(), OrdenCompra::getCodigo, "OC-", 4);
    }

    /** Ordenes aprobadas o recibidas aun no pagadas. */

    public List<OrdenCompra> porPagar() {
        return listar().stream()
                .filter(o -> (o.getEstado() == EstadoCompra.APROBADA || o.getEstado() == EstadoCompra.RECIBIDA) && o.getFechaPago() == null)
                .toList();
    }


    @Transactional
    public OrdenCompra crear(Long proveedorId, List<LineaCompra> lineas, boolean enviarSolicitud, String observaciones,
                             String origen, boolean nueva) {
        if (proveedorId == null) throw new NegocioExceptionPMMD("Selecciona un proveedor.");
        Proveedor proveedor = proveedorRepositoryPMMD.findById(proveedorId).orElseThrow();
        if (!proveedor.getActivo()) throw new NegocioExceptionPMMD("El proveedor está inactivo.");
        List<LineaCompra> validas = lineas.stream().filter(l -> l.materialId() != null && l.cantidad() > 0).toList();
        if (validas.isEmpty()) throw new NegocioExceptionPMMD("Agrega al menos un material con cantidad.");
        OrdenCompra orden = OrdenCompra.builder()
                .codigo(siguienteCodigo()).proveedor(proveedor).fecha(AppClockPMMD.hoy()).fechaEntrega(AppClockPMMD.hoy().plusDays(5))
                .estado(enviarSolicitud ? EstadoCompra.SOLICITADA : EstadoCompra.BORRADOR)
                .origen(origen == null ? "Manual" : origen).nueva(nueva)
                .observaciones(observaciones).build();
        for (LineaCompra l : validas) {
            if (l.precioUnitario() == null || l.precioUnitario().signum() < 0) throw new NegocioExceptionPMMD("Revisa los precios unitarios.");
            Material m = materialRepositoryPMMD.findById(l.materialId()).orElseThrow();
            Tela tela = l.telaId() == null ? null : telaRepositoryPMMD.findById(l.telaId()).orElse(null);
            if (tela == null && m.isTela()) tela = colorParaReponerPMMD(m);
            orden.agregar(ItemCompra.builder().material(m).tela(tela).cantidad(l.cantidad()).precioUnitario(l.precioUnitario()).build());
        }
        return repositoryPMMD.save(orden);
    }

    /** Color de la tela con menor cobertura (stock / minimo) para reponer. */

    private Tela colorParaReponerPMMD(Material m) {
        TipoTela tipo = TipoTela.porMaterial(m.getCodigo());
        return telaServicePMMD.porTipo(tipo).stream().filter(t -> !t.getDescontinuada())
                .min(Comparator.comparingDouble(t -> t.getStock() / Math.max(1, t.getMinimo()))).orElse(null);
    }


    @Transactional
    public OrdenCompra solicitar(Long id) {
        return transicion(id, EstadoCompra.BORRADOR, EstadoCompra.SOLICITADA);
    }


    @Transactional
    public OrdenCompra aprobar(Long id) {
        return transicion(id, EstadoCompra.SOLICITADA, EstadoCompra.APROBADA);
    }


    @Transactional
    public void marcarVista(Long id) {
        OrdenCompra o = orden(id);
        if (Boolean.TRUE.equals(o.getNueva())) {
            o.setNueva(false);
            repositoryPMMD.save(o);
        }
    }

    /** Recepcion: suma al stock (en las telas, al color del muestrario) y registra "Entrada" en el kardex. */

    @Transactional
    public OrdenCompra recibir(Long id) {
        OrdenCompra orden = orden(id);
        if (orden.getEstado() != EstadoCompra.APROBADA) throw new NegocioExceptionPMMD("Solo se pueden recibir órdenes aprobadas.");
        for (ItemCompra item : orden.getItems()) {
            Material m = materialRepositoryPMMD.findById(item.getMaterial().getId()).orElseThrow();
            if (m.isTela() && item.getTela() != null) {
                telaServicePMMD.entradaRollo(item.getTela().getId(), item.getCantidad(), orden.getCodigo());
            } else {
                inventarioServicePMMD.registrar(m, TipoMovimiento.ENTRADA, item.getCantidad(), orden.getCodigo(),
                        "Recepción " + orden.getProveedor().getRazonSocial());
            }
        }
        orden.setEstado(EstadoCompra.RECIBIDA);
        orden.setFechaRecepcion(AppClockPMMD.hoy());
        orden.setNueva(false);
        Proveedor p = orden.getProveedor();
        p.setTotalComprado(p.getTotalComprado().add(orden.getTotal()));
        proveedorRepositoryPMMD.save(p);
        return repositoryPMMD.save(orden);
    }


    @Transactional
    public OrdenCompra cancelar(Long id) {
        OrdenCompra orden = orden(id);
        if (orden.getEstado() == EstadoCompra.RECIBIDA) throw new NegocioExceptionPMMD("No se puede cancelar una orden ya recibida.");
        orden.setEstado(EstadoCompra.CANCELADA);
        orden.setNueva(false);
        return repositoryPMMD.save(orden);
    }


    @Transactional
    public OrdenCompra registrarPago(Long id) {
        OrdenCompra orden = orden(id);
        orden.setFechaPago(AppClockPMMD.hoy());
        return repositoryPMMD.save(orden);
    }


    private OrdenCompra transicion(Long id, EstadoCompra desde, EstadoCompra hacia) {
        OrdenCompra orden = orden(id);
        if (orden.getEstado() != desde) throw new NegocioExceptionPMMD("La orden está en estado " + orden.getEstado() + ".");
        orden.setEstado(hacia);
        orden.setNueva(false);
        return repositoryPMMD.save(orden);
    }
}

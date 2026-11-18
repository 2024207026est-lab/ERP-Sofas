package pe.erp.sofas.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.erp.sofas.core.NegocioExceptionPMMD;
import pe.erp.sofas.model.*;
import pe.erp.sofas.repository.*;

import java.util.List;
import java.util.Optional;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductoService {
    
    /** Costo estandar de la ficha aprobada del modelo (null si no tiene). */

    public record CostoEstandar(
      FichaTecnica ficha, 
        double costo, 
          double margenPct) {
    }


    private final ProductoRepository repositoryPMMD;

    private final FichaService fichaServicePMMD;

    private final OrdenProduccionRepository opRepositoryPMMD;

    private final PedidoRepository pedidoRepositoryPMMD;


    public List<Producto> listar() {
        return repositoryPMMD.findAllByOrderByCodigoAsc().stream().filter(Producto::getActivo).toList();
    }


    public Producto producto(Long id) {
        return repositoryPMMD.findById(id).orElseThrow(() -> new NegocioExceptionPMMD("El modelo ya no existe."));
    }


    public Optional<Producto> porCodigo(String codigo) {
        return repositoryPMMD.findByCodigo(codigo);
    }


    public String siguienteCodigo() {
        return Codigos.siguiente(repositoryPMMD.findAll(), Producto::getCodigo, "SF-", 3);
    }


    public CostoEstandar costoEstandar(Producto p) {
        FichaTecnica f = fichaServicePMMD.aprobadaDe(p);
        if (f == null) return null;
        double costo = fichaServicePMMD.costoEstandar(f);
        double precio = p.getPrecio().doubleValue();
        return new CostoEstandar(f, costo, precio == 0 ? 0 : (precio - costo) * 100 / precio);
    }


    @Transactional
    public Producto guardar(Producto p) {
        validar(p);
        if (p.getId() == null) p.setCodigo(siguienteCodigo());
        return repositoryPMMD.save(p);
    }


    public static void validar(Producto p) {
        if (p.getNombre() == null || p.getNombre().isBlank()) throw new NegocioExceptionPMMD("El nombre del modelo es obligatorio.");
        if (p.getPrecio() == null || p.getPrecio().signum() <= 0) throw new NegocioExceptionPMMD("Ingresa el precio de venta.");
        if (p.getCosto() == null || p.getCosto().signum() < 0) throw new NegocioExceptionPMMD("Ingresa el costo.");
        if (p.getPrecio().compareTo(p.getCosto()) <= 0) throw new NegocioExceptionPMMD("El precio debe ser mayor que el costo.");
        if (p.getStock() == null || p.getStock() < 0) throw new NegocioExceptionPMMD("El stock no puede ser negativo.");
    }


    @Transactional
    public void eliminar(Long id) {
        Producto p = producto(id);
        boolean enUso = opRepositoryPMMD.findAll().stream().anyMatch(o -> o.getProducto() != null && o.getProducto().getId().equals(id) && o.isActiva())
                || pedidoRepositoryPMMD.findAll().stream().anyMatch(x -> x.getProducto() != null && x.getProducto().getId().equals(id) && !x.getEstado().cerrado());
        if (enUso) throw new NegocioExceptionPMMD("El modelo tiene pedidos u órdenes activas; no se puede eliminar.");
        // Baja logica: se conserva para el historial de ventas.
        p.setActivo(false);
        repositoryPMMD.save(p);
    }


    @Transactional
    public Producto fijarImagen(Long id, int indice, String ruta) {
        Producto p = producto(id);
        p.setImagen(indice, ruta);
        return repositoryPMMD.save(p);
    }
}

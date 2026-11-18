package pe.erp.sofas.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.erp.sofas.core.NegocioExceptionPMMD;
import pe.erp.sofas.model.Material;
import pe.erp.sofas.model.OrdenCompra;
import pe.erp.sofas.model.Proveedor;
import pe.erp.sofas.repository.MaterialRepository;
import pe.erp.sofas.repository.OrdenCompraRepository;
import pe.erp.sofas.repository.ProveedorRepository;

import java.util.List;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProveedorService {

    private final ProveedorRepository repositoryPMMD;

    private final MaterialRepository materialRepositoryPMMD;

    private final OrdenCompraRepository compraRepositoryPMMD;


    public List<Proveedor> listar() {
        return repositoryPMMD.findAllByOrderByCodigoAsc();
    }


    public List<Proveedor> activos() {
        return listar().stream().filter(Proveedor::getActivo).toList();
    }


    public Proveedor proveedor(Long id) {
        return repositoryPMMD.findById(id).orElseThrow(() -> new NegocioExceptionPMMD("El proveedor ya no existe."));
    }


    public List<Material> materiales(Proveedor p) {
        return materialRepositoryPMMD.findAllByOrderByCodigoAsc().stream()
                .filter(m -> m.getProveedor() != null && m.getProveedor().getId().equals(p.getId())).toList();
    }


    public List<OrdenCompra> compras(Proveedor p) {
        return compraRepositoryPMMD.findAllByOrderByCodigoDesc().stream().filter(o -> o.getProveedor().getId().equals(p.getId())).toList();
    }


    @Transactional
    public Proveedor guardar(Proveedor p) {
        if (p.getRazonSocial() == null || p.getRazonSocial().isBlank()) throw new NegocioExceptionPMMD("La razón social es obligatoria.");
        if (p.getRuc() == null || !p.getRuc().matches("\\d{11}")) throw new NegocioExceptionPMMD("El RUC debe tener 11 dígitos.");
        boolean duplicado = listar().stream().anyMatch(x -> x.getRuc() != null && x.getRuc().equals(p.getRuc()) && !x.getId().equals(p.getId()));
        if (duplicado) throw new NegocioExceptionPMMD("Ya existe un proveedor con ese RUC.");
        if (p.getId() == null) p.setCodigo(Codigos.siguiente(repositoryPMMD.findAll(), Proveedor::getCodigo, "PR-", 2));
        return repositoryPMMD.save(p);
    }
}

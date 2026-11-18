package pe.erp.sofas.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pe.erp.sofas.model.EstadoFicha;
import pe.erp.sofas.model.FichaTecnica;
import pe.erp.sofas.model.Material;
import pe.erp.sofas.repository.FichaTecnicaRepository;
import pe.erp.sofas.repository.MaterialRepository;
import pe.erp.sofas.service.calculo.Catalogo;

/** Implementacion del catalogo de la explosion sobre los repositorios. */

@Service
@RequiredArgsConstructor
public class CatalogoService implements Catalogo {

    private final FichaTecnicaRepository fichaRepositoryPMMD;

    private final MaterialRepository materialRepositoryPMMD;


    @Override
    public FichaTecnica fichaAprobada(String codigo) {
        return fichaRepositoryPMMD.findByCodigoOrderByVersionDesc(codigo).stream()
                .filter(f -> f.getEstado() == EstadoFicha.APROBADA).findFirst().orElse(null);
    }


    @Override
    public Material material(String codigo) {
        return materialRepositoryPMMD.findByCodigo(codigo).orElse(null);
    }
}

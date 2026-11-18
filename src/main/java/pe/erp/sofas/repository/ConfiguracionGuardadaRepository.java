package pe.erp.sofas.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.erp.sofas.model.ConfiguracionGuardada;
import pe.erp.sofas.model.Usuario;

import java.util.List;
import java.util.Optional;


@Repository
public interface ConfiguracionGuardadaRepository extends JpaRepository<ConfiguracionGuardada, Long> {
    
    List<ConfiguracionGuardada> findByUsuarioOrderByFechaCreacionDesc(Usuario usuario);

    
    List<ConfiguracionGuardada> findByUsuarioAndEsFavoritoTrueOrderByFechaCreacionDesc(Usuario usuario);

    
    Optional<ConfiguracionGuardada> findByIdAndUsuario(Long id, Usuario usuario);

    
    List<ConfiguracionGuardada> findByUsuarioAndEtiquetaOrderByFechaCreacionDesc(Usuario usuario, String etiqueta);

    
    long countByUsuario(Usuario usuario);
}

package pe.erp.sofas.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.erp.sofas.model.*;

import java.util.List;
import java.util.Optional;


@Repository
public interface PermisoRepository extends JpaRepository<Permiso, Long> {
    
    List<Permiso> findByRol(Rol rol);

    
    Optional<Permiso> findByRolAndModulo(Rol rol, String modulo);
}

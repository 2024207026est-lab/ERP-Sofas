package pe.erp.sofas.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.erp.sofas.model.*;

import java.util.List;
import java.util.Optional;


@Repository
public interface FichaTecnicaRepository extends JpaRepository<FichaTecnica, Long> {
    
    Optional<FichaTecnica> findByUid(String uid);

    
    List<FichaTecnica> findByCodigoOrderByVersionDesc(String codigo);

    
    List<FichaTecnica> findAllByOrderByCodigoAscVersionDesc();
}

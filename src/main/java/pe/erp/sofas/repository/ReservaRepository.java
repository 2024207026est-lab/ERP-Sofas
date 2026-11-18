package pe.erp.sofas.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.erp.sofas.model.*;

import java.util.List;
import java.util.Optional;


@Repository
public interface ReservaRepository extends JpaRepository<Reserva, Long> {
    
    List<Reserva> findByOpId(Long opId);

    
    Optional<Reserva> findByOpIdAndMaterialId(Long opId, Long materialId);

    
    List<Reserva> findByMaterialId(Long materialId);
}

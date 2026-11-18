package pe.erp.sofas.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.erp.sofas.model.*;

import java.util.List;
import java.util.Optional;


@Repository
public interface EmpleadoRepository extends JpaRepository<Empleado, Long> {
    
    Optional<Empleado> findByCodigo(String codigo);

    
    List<Empleado> findAllByOrderByCodigoAsc();
}

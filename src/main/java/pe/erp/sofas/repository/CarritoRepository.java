package pe.erp.sofas.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.erp.sofas.model.Carrito;
import pe.erp.sofas.model.Usuario;

import java.util.Optional;


@Repository
public interface CarritoRepository extends JpaRepository<Carrito, Long> {
    
    Optional<Carrito> findByUsuario(Usuario usuario);

    
    void deleteByUsuario(Usuario usuario);
}

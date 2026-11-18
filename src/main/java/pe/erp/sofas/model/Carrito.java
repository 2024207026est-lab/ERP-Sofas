package pe.erp.sofas.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@Entity
@Table(name = "carrito")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@EqualsAndHashCode(of = "id")
public class Carrito {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;


    @Builder.Default
    @OneToMany(mappedBy = "carrito", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("fechaAnadido ASC")
    private List<CarritoItem> items = new ArrayList<>();


    @Builder.Default
    @Column(nullable = false, updatable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now();


    @Builder.Default
    private LocalDateTime fechaModificacion = LocalDateTime.now();


    @PreUpdate
    void preUpdate() {
        this.fechaModificacion = LocalDateTime.now();
    }


    public void agregarItem(ConfiguracionGuardada config, Integer cantidad) {
        CarritoItem item = CarritoItem.builder()
            .carrito(this)
            .configuracion(config)
            .cantidad(cantidad)
            .build();
        items.add(item);
    }


    public void removerItem(Long itemId) {
        items.removeIf(item -> item.getId().equals(itemId));
    }


    public void actualizarCantidad(Long itemId, Integer nuevaCantidad) {
        items.stream()
            .filter(item -> item.getId().equals(itemId))
            .findFirst()
            .ifPresent(item -> item.setCantidad(nuevaCantidad));
    }


    public void vaciar() {
        items.clear();
    }


    public Integer obtenerCantidadTotal() {
        return items.stream()
            .mapToInt(CarritoItem::getCantidad)
            .sum();
    }


    public Integer obtenerCantidadItems() {
        return items.size();
    }
}

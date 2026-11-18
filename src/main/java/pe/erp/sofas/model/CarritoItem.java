package pe.erp.sofas.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;


@Entity
@Table(name = "carrito_item")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@EqualsAndHashCode(of = "id")
public class CarritoItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "carrito_id")
    private Carrito carrito;


    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "configuracion_id")
    private ConfiguracionGuardada configuracion;


    @Column(nullable = false)
    private Integer cantidad;


    @Builder.Default
    @Column(nullable = false, updatable = false)
    private LocalDateTime fechaAnadido = LocalDateTime.now();


    private String notas;


    public CarritoItem copiaConCantidad(Integer nuevaCantidad) {
        return toBuilder()
            .id(null)
            .cantidad(nuevaCantidad)
            .build();
    }
}

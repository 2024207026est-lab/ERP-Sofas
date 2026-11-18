package pe.erp.sofas.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** Movimiento del kardex. La cantidad es positiva salvo en los ajustes, que llevan signo. */

@Entity
@Table(name = "movimiento", indexes = @Index(columnList = "material_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class Movimiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "material_id")
    private Material material;


    @Column(nullable = false)
    private LocalDateTime fecha;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoMovimiento tipo;


    @Column(nullable = false)
    private Double cantidad;


    private String referencia;


    private String concepto;

    /** Saldo fisico del material despues del movimiento. */

    private Double saldo;


    private String usuario;

    /** Cantidad con signo segun su efecto en el saldo (0 si no lo afecta). */

    public double getEfecto() {
        return switch (tipo) {
            case ENTRADA -> cantidad;
            case SALIDA, CONSUMO_OP -> -cantidad;
            case AJUSTE -> cantidad;
            default -> 0;
        };
    }
}

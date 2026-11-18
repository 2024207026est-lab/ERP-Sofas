package pe.erp.sofas.model;

import jakarta.persistence.*;
import lombok.*;

/** Cantidad de un material apartada para una orden de produccion. */

@Entity
@Table(name = "reserva", uniqueConstraints = @UniqueConstraint(columnNames = {"op_id", "material_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class Reserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "op_id")
    private OrdenProduccion op;


    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "material_id")
    private Material material;


    @Column(nullable = false)
    private Double cantidad;
}

package pe.erp.sofas.model;

import jakarta.persistence.*;
import lombok.*;

/** Horas de mano de obra de una etapa en la ficha tecnica. */

@Entity
@Table(name = "operacion_mo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@EqualsAndHashCode(of = "id")
public class OperacionMO {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ficha_id")
    @ToString.Exclude
    private FichaTecnica ficha;


    private Integer orden;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EtapaMO etapa;


    @Column(nullable = false)
    private Double horas;


    @Column(nullable = false)
    private Double costoHora;


    public double getCosto() {
        return horas * costoHora;
    }


    public OperacionMO copia() {
        return toBuilder().id(null).ficha(null).build();
    }
}

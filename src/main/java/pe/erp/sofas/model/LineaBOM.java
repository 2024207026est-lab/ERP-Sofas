package pe.erp.sofas.model;

import jakarta.persistence.*;
import lombok.*;

/** Linea de la lista de materiales: cantidad base para las medidas de la ficha, merma y escalado. */

@Entity
@Table(name = "linea_bom")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@EqualsAndHashCode(of = "id")
public class LineaBOM {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ficha_id")
    @ToString.Exclude
    private FichaTecnica ficha;


    private Integer orden;


    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "material_id")
    private Material material;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Componente componente;


    @Column(nullable = false)
    private Double cantidad;


    @Builder.Default
    @Column(nullable = false)
    private Double mermaPct = 0d;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 4)
    @Builder.Default
    private Dependencia dependencia = Dependencia.F;


    @Builder.Default
    @Column(nullable = false)
    private Boolean opcional = false;

    /** Copia sin id ni ficha (para duplicar versiones). */

    public LineaBOM copia() {
        return toBuilder().id(null).ficha(null).build();
    }
}

package pe.erp.sofas.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

/** Material adicional a la ficha (extras del configurador: chaise, cojines, mecanismos...). Por unidad. */

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LineaExtra {

    @Column(name = "material_codigo", length = 20)
    private String materialCodigo;


    @Column(length = 20)
    private String componente;


    private Double cantidad;
}

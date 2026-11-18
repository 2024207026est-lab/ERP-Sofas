package pe.erp.sofas.model;

import jakarta.persistence.*;
import lombok.*;

/** Parametro de configuracion clave/valor (mermas por defecto, costos por hora, tarifas de destajo...). */

@Entity
@Table(name = "parametro")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Parametro {

    @Id
    @Column(length = 60)
    private String clave;


    @Column(nullable = false)
    private String valor;
}

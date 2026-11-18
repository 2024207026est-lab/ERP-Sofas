package pe.erp.sofas.model;

import jakarta.persistence.*;
import lombok.*;

/** Estado de una alerta calculada (leida o descartada), identificada por su clave estable. */

@Entity
@Table(name = "notificacion_estado")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NotificacionEstado {

    @Id
    @Column(length = 80)
    private String clave;


    private Boolean leida;


    private Boolean descartada;
}

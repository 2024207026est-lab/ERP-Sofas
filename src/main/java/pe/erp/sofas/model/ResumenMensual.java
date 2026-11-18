package pe.erp.sofas.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/** Cierre historico de un mes (ventas y egresos) para los graficos de 12 meses. */

@Entity
@Table(name = "resumen_mensual", uniqueConstraints = @UniqueConstraint(columnNames = {"anio", "mes"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class ResumenMensual {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    private Integer anio;

    private Integer mes;


    @Column(precision = 12, scale = 2)
    private BigDecimal ventas;


    @Column(precision = 12, scale = 2)
    private BigDecimal egresos;


    private Integer sofas;
}

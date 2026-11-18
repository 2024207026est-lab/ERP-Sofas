package pe.erp.sofas.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Egreso operativo (planilla, alquiler, servicios, transporte...). */

@Entity
@Table(name = "gasto")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class Gasto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    private LocalDate fecha;


    private String categoria;


    private String concepto;


    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;
}

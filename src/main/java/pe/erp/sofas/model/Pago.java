package pe.erp.sofas.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Pago de un cliente: adelanto o saldo de un pedido, o pago de un retapizado. */

@Entity
@Table(name = "pago")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    /** Pedido o retapizado al que se aplica. */

    @Column(length = 12)
    private String referencia;


    private LocalDate fecha;


    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;


    private String metodo;


    private String concepto;
}

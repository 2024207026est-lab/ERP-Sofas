package pe.erp.sofas.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/** Consumo real frente al estimado por la ficha al cerrar una OP (base de Consumo y mermas). */

@Entity
@Table(name = "consumo_real")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class ConsumoReal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Column(nullable = false, length = 12)
    private String opCodigo;


    private String producto;

    /** Codigo de la ficha (FT-001) usada, para ajustar la ficha con el promedio real. */

    @Column(length = 16)
    private String fichaUid;


    private String responsable;


    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "material_id")
    private Material material;


    @Column(nullable = false)
    private Double estimado;


    @Column(nullable = false)
    private Double realConsumido;


    private LocalDate fecha;


    public double getDiferencia() {
        return realConsumido - estimado;
    }


    public double getDiferenciaPct() {
        return estimado == 0 ? 0 : (realConsumido - estimado) * 100 / estimado;
    }


    public double getDiferenciaValor() {
        return getDiferencia() * material.getCosto();
    }
}

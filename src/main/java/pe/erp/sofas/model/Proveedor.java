package pe.erp.sofas.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;


@Entity
@Table(name = "proveedor")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@EqualsAndHashCode(of = "id")
public class Proveedor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Column(nullable = false, unique = true, length = 10)
    private String codigo;


    @Column(nullable = false)
    private String razonSocial;


    @Column(length = 11)
    private String ruc;


    private String contacto;

    private String telefono;

    private String email;

    private String direccion;

    /** Materiales que abastece (texto libre: "madera, triplay, patas de madera"). */

    private String rubro;


    private String condicionPago;


    @Builder.Default
    @Column(nullable = false)
    private Boolean activo = true;


    @Builder.Default
    @Column(precision = 12, scale = 2)
    private BigDecimal totalComprado = BigDecimal.ZERO;


    @Override
    public String toString() {
        return razonSocial;
    }
}

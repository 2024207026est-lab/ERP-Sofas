package pe.erp.sofas.model;

import jakarta.persistence.*;
import lombok.*;

/** Color del muestrario. Los metros suman al material de inventario de su tipo de tela. */

@Entity
@Table(name = "tela")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@EqualsAndHashCode(of = "id")
public class Tela {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Column(nullable = false, unique = true, length = 10)
    private String codigo;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoTela tipo;

    /** Nombre del color: "Gris perla". */

    @Column(nullable = false)
    private String color;


    @Column(nullable = false, length = 9)
    private String hex;

    /** Codigo del proveedor para el color (CH-210 GP). */

    private String codigoProveedor;


    @Builder.Default
    @Column(nullable = false)
    private Double stock = 0d;


    @Builder.Default
    @Column(nullable = false)
    private Double minimo = 8d;


    @Builder.Default
    @Column(nullable = false)
    private Boolean descontinuada = false;

    /** Disponible · Stock bajo · Agotado · Descontinuada. */

    public String getEstado() {
        if (Boolean.TRUE.equals(descontinuada)) return "Descontinuada";
        if (stock <= 0) return "Agotado";
        if (stock < minimo) return "Stock bajo";
        return "Disponible";
    }


    public String getNombre() {
        return tipo.nombre() + " " + color;
    }


    @Override
    public String toString() {
        return getNombre();
    }
}

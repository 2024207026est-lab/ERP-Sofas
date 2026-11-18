package pe.erp.sofas.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;


@Entity
@Table(name = "empleado")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@EqualsAndHashCode(of = "id")
public class Empleado {

    public static final java.util.List<String> AREAS = java.util.List.of(
            "Taller · Carpintería", "Taller · Costura", "Taller · Tapizado", "Taller · Espumado", "Tienda", "Almacén", "Administración");


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Column(nullable = false, unique = true, length = 10)
    private String codigo;


    @Column(nullable = false)
    private String nombre;


    @Column(length = 8)
    private String dni;


    private String cargo;


    private String area;


    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Rol rol;


    private String telefono;


    private LocalDate ingreso;

    /** Activo · Vacaciones · Inactivo. */

    @Builder.Default
    private String estado = "Activo";

    /** Etapa del taller en la que trabaja (para el pago a destajo). */

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Etapa etapa;


    private String color;


    public boolean isTaller() {
        return area != null && area.startsWith("Taller");
    }


    @Override
    public String toString() {
        return nombre;
    }
}

package pe.erp.sofas.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;


@Entity
@Table(name = "usuario")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@EqualsAndHashCode(of = "id")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Column(nullable = false, unique = true, length = 40)
    private String username;


    @Column(nullable = false)
    private String nombre;


    private String email;

    /** Cargo mostrado en la barra superior (p. ej. "Gerente General"). */

    private String cargo;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Rol rol;

    /** Hash BCrypt de la contrasena. */

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;


    @Builder.Default
    @Column(nullable = false)
    private Boolean activo = true;


    private LocalDate ultimoAcceso;


    public String getPrimerNombre() {
        return nombre.trim().split("\\s+")[0];
    }
}

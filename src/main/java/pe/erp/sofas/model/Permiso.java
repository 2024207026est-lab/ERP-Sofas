package pe.erp.sofas.model;

import jakarta.persistence.*;
import lombok.*;

/** Celda de la matriz de permisos: que puede hacer un rol en un modulo. */

@Entity
@Table(name = "permiso", uniqueConstraints = @UniqueConstraint(columnNames = {"rol", "modulo"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class Permiso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Rol rol;


    @Column(nullable = false, length = 30)
    private String modulo;


    @Builder.Default
    private Boolean ver = false;


    @Builder.Default
    private Boolean editar = false;
}

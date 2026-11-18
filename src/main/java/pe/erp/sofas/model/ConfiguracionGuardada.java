package pe.erp.sofas.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;


@Entity
@Table(name = "configuracion_guardada")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@EqualsAndHashCode(of = "id")
public class ConfiguracionGuardada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;


    @Column(nullable = false, length = 100)
    private String nombre;


    @Column(columnDefinition = "TEXT")
    private String descripcion;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ConfigSofa.Modelo modelo;


    private Integer cuerpos;


    @Enumerated(EnumType.STRING)
    private ConfigSofa.Chaise chaise;


    private Integer profundidad;


    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "tela_id")
    private Tela tela;


    @Enumerated(EnumType.STRING)
    private ConfigSofa.Brazo brazo;


    @Enumerated(EnumType.STRING)
    private ConfigSofa.Patas patas;


    @Enumerated(EnumType.STRING)
    private ConfigSofa.AcabadoMadera acabado;


    @Enumerated(EnumType.STRING)
    private ConfigSofa.Espuma espuma;


    private Integer cojines;


    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "tela_cojines_id")
    private Tela telaCojines;


    @Enumerated(EnumType.STRING)
    private ConfigSofa.Mecanismo mecanismo;


    @Builder.Default
    @Column(nullable = false)
    private Boolean esFavorito = false;


    @Builder.Default
    @Column(nullable = false, updatable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now();


    @Builder.Default
    private LocalDateTime fechaModificacion = LocalDateTime.now();


    @Column(length = 50)
    private String etiqueta;


    @PreUpdate
    void preUpdate() {
        this.fechaModificacion = LocalDateTime.now();
    }


    public ConfigSofa toConfigSofa() {
        return new ConfigSofa(
            modelo,
            cuerpos != null ? cuerpos : 1,
            chaise != null ? chaise : ConfigSofa.Chaise.NINGUNA,
            profundidad != null ? profundidad : 92,
            tela != null ? tela.getId() : null,
            brazo != null ? brazo : ConfigSofa.Brazo.RECTO,
            patas != null ? patas : ConfigSofa.Patas.MADERA,
            acabado != null ? acabado : ConfigSofa.AcabadoMadera.NATURAL,
            espuma != null ? espuma : ConfigSofa.Espuma.D23,
            cojines != null ? cojines : 1,
            telaCojines != null ? telaCojines.getId() : null,
            mecanismo != null ? mecanismo : ConfigSofa.Mecanismo.FIJO
        );
    }


    public static ConfiguracionGuardada fromConfigSofa(ConfigSofa config, Usuario usuario, String nombre, String descripcion) {
        return ConfiguracionGuardada.builder()
            .usuario(usuario)
            .nombre(nombre)
            .descripcion(descripcion)
            .modelo(config.modelo())
            .cuerpos(config.cuerpos())
            .chaise(config.chaise())
            .profundidad(config.profundidad())
            .brazo(config.brazo())
            .patas(config.patas())
            .acabado(config.acabado())
            .espuma(config.espuma())
            .cojines(config.cojines())
            .mecanismo(config.mecanismo())
            .build();
    }
}

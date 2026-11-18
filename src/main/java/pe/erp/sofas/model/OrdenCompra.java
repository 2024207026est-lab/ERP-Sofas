package pe.erp.sofas.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;


@Entity
@Table(name = "orden_compra")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class OrdenCompra {

    public static final BigDecimal IGV = new BigDecimal("0.18");


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Column(nullable = false, unique = true, length = 12)
    private String codigo;


    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "proveedor_id")
    private Proveedor proveedor;


    private LocalDate fecha;


    private LocalDate fechaEntrega;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private EstadoCompra estado;

    /** MRP · Calculadora · OP-1024 · Manual · Alerta de stock. */

    @Builder.Default
    private String origen = "Manual";

    /** Recien generada (badge "Nueva") hasta que se abre. */

    @Builder.Default
    private Boolean nueva = false;


    private LocalDate fechaRecepcion;

    private LocalDate fechaPago;


    @Column(length = 500)
    private String observaciones;


    @Builder.Default
    @OneToMany(mappedBy = "orden", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @Fetch(FetchMode.SUBSELECT)
    private List<ItemCompra> items = new ArrayList<>();


    public void agregar(ItemCompra item) {
        item.setOrden(this);
        items.add(item);
    }

    /** Los precios de compra se registran sin IGV. */

    public BigDecimal getSubtotal() {
        return items.stream().map(ItemCompra::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }


    public BigDecimal getIgv() {
        return getSubtotal().multiply(IGV).setScale(2, RoundingMode.HALF_UP);
    }


    public BigDecimal getTotal() {
        return getSubtotal().add(getIgv());
    }
}

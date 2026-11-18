package pe.erp.sofas.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.erp.sofas.core.AppClockPMMD;
import pe.erp.sofas.core.MoneyPMMD;
import pe.erp.sofas.core.NegocioExceptionPMMD;
import pe.erp.sofas.model.*;
import pe.erp.sofas.repository.ClienteRepository;
import pe.erp.sofas.repository.ProductoRepository;
import pe.erp.sofas.repository.VentaRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Punto de venta: descuenta stock, numera B001 / F001 y valida factura, efectivo y pago mixto. */

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VentaServicePMMD {

    public record Linea(
      Long productoId, 
        int cantidad, 
          BigDecimal precioUnitario) {
    }

    /**
     * Datos del cobro. En pago mixto {@code metodo2} y los montos deben sumar el total; en efectivo
     * {@code recibido} debe cubrir el total.
     */

    public record Cobro(
      Long clienteId, 
        Venta.Comprobante comprobante, 
          List<Linea> lineas, 
            BigDecimal descuento,
                        
                        String metodo, 
                          BigDecimal recibido, 
                            String metodo2, 
                              BigDecimal monto1, 
                                BigDecimal monto2) {
    }


    private final VentaRepository repositoryPMMD;

    private final ProductoRepository productoRepositoryPMMD;

    private final ClienteRepository clienteRepositoryPMMD;

    private final UsuarioActual usuarioPMMD;


    public List<Venta> historial() {
        return repositoryPMMD.findAllByOrderByFechaDesc();
    }


    public List<Venta> delMes(LocalDate referencia) {
        LocalDate inicio = referencia.withDayOfMonth(1);
        LocalDate fin = inicio.plusMonths(1);
        return historial().stream().filter(v -> !v.getFecha().toLocalDate().isBefore(inicio) && v.getFecha().toLocalDate().isBefore(fin)).toList();
    }


    public BigDecimal totalMes() {
        return delMes(AppClockPMMD.hoy()).stream().map(Venta::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }


    public int sofasMes() {
        return delMes(AppClockPMMD.hoy()).stream().mapToInt(Venta::getUnidades).sum();
    }


    public int siguienteNumero(Venta.Comprobante c) {
        return repositoryPMMD.findBySerie(c.serie()).stream().mapToInt(Venta::getNumero).max().orElse(0) + 1;
    }


    @Transactional
    public Venta cobrar(Cobro c) {
        if (c.lineas() == null || c.lineas().isEmpty()) throw new NegocioExceptionPMMD("El carrito está vacío.");
        Cliente cliente = c.clienteId() == null ? null : clienteRepositoryPMMD.findById(c.clienteId()).orElseThrow();
        if (c.comprobante() == Venta.Comprobante.FACTURA && (cliente == null || !cliente.tieneRuc())) {
            throw new NegocioExceptionPMMD("La factura exige un cliente con RUC.");
        }
        Venta venta = Venta.builder().comprobante(c.comprobante()).serie(c.comprobante().serie())
                .numero(siguienteNumero(c.comprobante())).fecha(LocalDateTime.of(AppClockPMMD.hoy(), AppClockPMMD.HORA))
                .cliente(cliente).vendedor(usuarioPMMD.nombre()).build();
        BigDecimal bruto = BigDecimal.ZERO;
        for (Linea l : c.lineas()) {
            Producto p = productoRepositoryPMMD.findById(l.productoId()).orElseThrow();
            if (l.cantidad() <= 0) throw new NegocioExceptionPMMD("Cantidad inválida para " + p.getNombre() + ".");
            if (p.getStock() < l.cantidad()) {
                throw new NegocioExceptionPMMD("Stock insuficiente de " + p.getNombre() + ": quedan " + p.getStock() + ".");
            }
            p.setStock(p.getStock() - l.cantidad());
            productoRepositoryPMMD.save(p);
            BigDecimal precio = l.precioUnitario() == null ? p.getPrecio() : l.precioUnitario();
            venta.agregar(ItemVenta.builder().producto(p).cantidad(l.cantidad()).precioUnitario(precio).build());
            bruto = bruto.add(precio.multiply(BigDecimal.valueOf(l.cantidad())));
        }
        BigDecimal descuento = c.descuento() == null ? BigDecimal.ZERO : c.descuento().setScale(2, RoundingMode.HALF_UP);
        if (descuento.signum() < 0 || descuento.compareTo(bruto) >= 0) throw new NegocioExceptionPMMD("El descuento no es válido.");
        BigDecimal total = bruto.subtract(descuento);
        venta.setDescuento(descuento);
        venta.setTotal(total);
        String metodo = c.metodo() == null ? "Efectivo" : c.metodo();
        if ("Mixto".equals(metodo)) {
            BigDecimal a = c.monto1() == null ? BigDecimal.ZERO : c.monto1();
            BigDecimal b = c.monto2() == null ? BigDecimal.ZERO : c.monto2();
            if (a.add(b).compareTo(total) != 0) {
                throw new NegocioExceptionPMMD("En pago mixto los montos deben sumar " + MoneyPMMD.fmt(total) + ".");
            }
            metodo = "Mixto (Efectivo " + MoneyPMMD.fmt(a) + " + " + (c.metodo2() == null ? "Yape" : c.metodo2()) + " " + MoneyPMMD.fmt(b) + ")";
        }
        if ("Efectivo".equals(metodo)) {
            BigDecimal recibido = c.recibido() == null ? total : c.recibido();
            if (recibido.compareTo(total) < 0) throw new NegocioExceptionPMMD("El monto recibido no cubre el total.");
            venta.setRecibido(recibido);
            venta.setVuelto(recibido.subtract(total));
        }
        venta.setMetodoPago(metodo);
        return repositoryPMMD.save(venta);
    }
}

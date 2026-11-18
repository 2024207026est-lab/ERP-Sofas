package pe.erp.sofas.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pe.erp.sofas.model.*;
import pe.erp.sofas.repository.CarritoRepository;
import pe.erp.sofas.repository.PedidoRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


@Service
@RequiredArgsConstructor
public class CreadorPedidoServicePMMD {

    private final CarritoRepository carritoRepoPMMD;

    private final PedidoRepository pedidoRepoPMMD;

    private final CalculoPrecioServicePMMD precioServicePMMD;

    private final GestorCarritoService gestorCarritoPMMD;


    public List<Pedido> crearPedidosDesdeCarrito(Usuario usuario, Cliente cliente, boolean vaciarCarrito) {
        Carrito carrito = gestorCarritoPMMD.obtenerOCrearCarrito(usuario);

        if (carrito.getItems().isEmpty()) {
            throw new IllegalArgumentException("El carrito está vacío");
        }

        List<Pedido> pedidosCreados = new ArrayList<>();

        for (CarritoItem item : carrito.getItems()) {
            Pedido pedido = crearPedidoDesdeItemPMMD(item, cliente);
            Pedido pedidoGuardado = pedidoRepoPMMD.save(pedido);
            pedidosCreados.add(pedidoGuardado);
        }

        if (vaciarCarrito) {
            gestorCarritoPMMD.vaciarCarrito(usuario);
        }

        return pedidosCreados;
    }


    private Pedido crearPedidoDesdeItemPMMD(CarritoItem item, Cliente cliente) {
        ConfiguracionGuardada config = item.getConfiguracion();
        ConfigSofa sofaConfig = config.toConfigSofa();

        // Calcular precio
        BigDecimal precioUnitario = precioServicePMMD.calcularPrecio(sofaConfig);
        BigDecimal precioTotal = precioUnitario.multiply(BigDecimal.valueOf(item.getCantidad()));

        // Generar código único
        String codigo = generarCodigoPedidoPMMD();

        // Generar especificación legible
        String especificacion = generarEspecificacionPMMD(sofaConfig);

        // Generar configuración serializada
        String configSerialized = generarConfigSerializedPMMD(sofaConfig);

        Pedido pedido = Pedido.builder()
            .codigo(codigo)
            .cliente(cliente)
            .tipo(Pedido.Tipo.PERSONALIZADO)
            .cantidad(item.getCantidad())
            .fecha(LocalDate.now())
            .precio(precioTotal)
            .estado(EstadoPedido.CONFIRMADO)
            .largo(sofaConfig.largo())
            .fondo(sofaConfig.profundidad())
            .alto(sofaConfig.alto())
            .material(sofaConfig.espuma().nombre())
            .especificacion(especificacion)
            .configuracion(configSerialized)
            .observaciones(item.getNotas())
            .build();

        return pedido;
    }


    private String generarCodigoPedidoPMMD() {
        // Formato: PED-YYYYMM-XXXXX (ej: PED-202610-A1B2C)
        LocalDate hoy = LocalDate.now();
        String fechaParte = String.format("%04d%02d", hoy.getYear(), hoy.getMonthValue());
        String aleatorioParte = UUID.randomUUID().toString().substring(0, 5).toUpperCase();
        return "PED-" + fechaParte + "-" + aleatorioParte;
    }


    private String generarEspecificacionPMMD(ConfigSofa config) {
        return String.format(
            "%s | %d cuerpos | %s | %s | %s | %s",
            config.modelo().nombre(),
            config.cuerpos(),
            config.espuma().nombre(),
            config.brazo().nombre(),
            config.patas().nombre(),
            config.mecanismo().nombre()
        );
    }


    private String generarConfigSerializedPMMD(ConfigSofa config) {
        return String.format(
            "modelo=%s;cuerpos=%d;chaise=%s;profundidad=%d;brazo=%s;patas=%s;" +
            "acabado=%s;espuma=%s;mecanismo=%s",
            config.modelo().name(),
            config.cuerpos(),
            config.chaise().name(),
            config.profundidad(),
            config.brazo().name(),
            config.patas().name(),
            config.acabado().name(),
            config.espuma().name(),
            config.mecanismo().name()
        );
    }


    public Pedido actualizarPedido(Long pedidoId, Cliente cliente, LocalDate fechaEntrega, String observaciones) {
        Pedido pedido = pedidoRepoPMMD.findById(pedidoId)
            .orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado"));

        if (cliente != null) {
            pedido.setCliente(cliente);
        }
        if (fechaEntrega != null) {
            pedido.setFechaEntrega(fechaEntrega);
        }
        if (observaciones != null) {
            pedido.setObservaciones(observaciones);
        }

        return pedidoRepoPMMD.save(pedido);
    }


    public Pedido registrarAdelanto(Long pedidoId, BigDecimal monto) {
        Pedido pedido = pedidoRepoPMMD.findById(pedidoId)
            .orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado"));

        BigDecimal nuevoAdelanto = pedido.getAdelanto().add(monto);

        if (nuevoAdelanto.compareTo(pedido.getPrecio()) > 0) {
            throw new IllegalArgumentException("El adelanto no puede exceder el precio total");
        }

        pedido.setAdelanto(nuevoAdelanto);
        return pedidoRepoPMMD.save(pedido);
    }


    public Pedido cambiarEstado(Long pedidoId, EstadoPedido nuevoEstado) {
        Pedido pedido = pedidoRepoPMMD.findById(pedidoId)
            .orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado"));

        pedido.setEstado(nuevoEstado);
        return pedidoRepoPMMD.save(pedido);
    }


    public record ResumenPedidos(
        
        List<Pedido> pedidos,
        
        Integer totalPedidos,
        
        BigDecimal montoTotal,
        
        Integer cantidadTotal
    ) {

        public static ResumenPedidos desde(List<Pedido> pedidos) {
            Integer totalPedidos = pedidos.size();
            BigDecimal montoTotal = pedidos.stream()
                .map(Pedido::getPrecio)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            Integer cantidadTotal = pedidos.stream()
                .mapToInt(Pedido::getCantidad)
                .sum();

            return new ResumenPedidos(pedidos, totalPedidos, montoTotal, cantidadTotal);
        }
    }
}

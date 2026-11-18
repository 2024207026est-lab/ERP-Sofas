package pe.erp.sofas.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pe.erp.sofas.model.*;
import pe.erp.sofas.repository.CarritoRepository;
import pe.erp.sofas.repository.ConfiguracionGuardadaRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;


@Service
@RequiredArgsConstructor
public class GestorCarritoService {

    private final CarritoRepository carritoRepoPMMD;

    private final ConfiguracionGuardadaRepository configRepoPMMD;

    private final CalculoPrecioServicePMMD precioServicePMMD;


    public Carrito obtenerOCrearCarrito(Usuario usuario) {
        Optional<Carrito> existente = carritoRepoPMMD.findByUsuario(usuario);
        if (existente.isPresent()) {
            return existente.get();
        }

        Carrito nuevo = Carrito.builder()
            .usuario(usuario)
            .build();
        return carritoRepoPMMD.save(nuevo);
    }


    public Carrito agregarAlCarrito(Usuario usuario, Long configuracionId, Integer cantidad) {
        Carrito carrito = obtenerOCrearCarrito(usuario);

        ConfiguracionGuardada config = configRepoPMMD.findByIdAndUsuario(configuracionId, usuario)
            .orElseThrow(() -> new IllegalArgumentException("Configuración no encontrada"));

        // Verificar si ya existe en carrito
        Optional<CarritoItem> existente = carrito.getItems().stream()
            .filter(item -> item.getConfiguracion().getId().equals(configuracionId))
            .findFirst();

        if (existente.isPresent()) {
            existente.get().setCantidad(existente.get().getCantidad() + cantidad);
        } else {
            carrito.agregarItem(config, cantidad);
        }

        return carritoRepoPMMD.save(carrito);
    }


    public Carrito removerDelCarrito(Usuario usuario, Long itemId) {
        Carrito carrito = obtenerOCrearCarrito(usuario);
        carrito.removerItem(itemId);
        return carritoRepoPMMD.save(carrito);
    }


    public Carrito actualizarCantidad(Usuario usuario, Long itemId, Integer nuevaCantidad) {
        Carrito carrito = obtenerOCrearCarrito(usuario);

        if (nuevaCantidad <= 0) {
            carrito.removerItem(itemId);
        } else {
            carrito.actualizarCantidad(itemId, nuevaCantidad);
        }

        return carritoRepoPMMD.save(carrito);
    }


    public Carrito vaciarCarrito(Usuario usuario) {
        Carrito carrito = obtenerOCrearCarrito(usuario);
        carrito.vaciar();
        return carritoRepoPMMD.save(carrito);
    }


    public List<CarritoItem> listarItems(Usuario usuario) {
        Carrito carrito = obtenerOCrearCarrito(usuario);
        return carrito.getItems();
    }


    public BigDecimal calcularTotal(Usuario usuario) {
        Carrito carrito = obtenerOCrearCarrito(usuario);
        BigDecimal total = BigDecimal.ZERO;

        for (CarritoItem item : carrito.getItems()) {
            ConfigSofa config = item.getConfiguracion().toConfigSofa();
            BigDecimal precioUnitario = precioServicePMMD.calcularPrecio(config);
            total = total.add(precioUnitario.multiply(BigDecimal.valueOf(item.getCantidad())));
        }

        return total.setScale(2, java.math.RoundingMode.HALF_UP);
    }


    public DetalleCarrito obtenerDetalleCarrito(Usuario usuario) {
        Carrito carrito = obtenerOCrearCarrito(usuario);
        Integer cantidadItems = carrito.obtenerCantidadItems();
        Integer cantidadTotal = carrito.obtenerCantidadTotal();
        BigDecimal precioTotal = calcularTotal(usuario);

        return new DetalleCarrito(
            cantidadItems,
            cantidadTotal,
            precioTotal,
            carrito.getItems()
        );
    }


    public void eliminarCarrito(Usuario usuario) {
        carritoRepoPMMD.deleteByUsuario(usuario);
    }


    public record DetalleCarrito(
        
        Integer cantidadItems,
        
        Integer cantidadTotal,
        
        BigDecimal precioTotal,
        
        List<CarritoItem> items
    ) {}
}

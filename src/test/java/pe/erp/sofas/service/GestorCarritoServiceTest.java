package pe.erp.sofas.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import pe.erp.sofas.model.*;
import pe.erp.sofas.repository.CarritoRepository;
import pe.erp.sofas.repository.ConfiguracionGuardadaRepository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


class GestorCarritoServiceTest {

    private GestorCarritoService servicePMMD;


    @Mock
    private CarritoRepository carritoRepoPMMD;


    @Mock
    private ConfiguracionGuardadaRepository configRepoPMMD;


    @Mock
    private CalculoPrecioServicePMMD precioServicePMMD;


    private Usuario usuarioTestPMMD;

    private ConfiguracionGuardada configTestPMMD;


    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        servicePMMD = new GestorCarritoService(carritoRepoPMMD, configRepoPMMD, precioServicePMMD);

        usuarioTestPMMD = new Usuario();
        usuarioTestPMMD.setId(1L);
        usuarioTestPMMD.setNombre("Test User");

        configTestPMMD = ConfiguracionGuardada.builder()
            .id(1L)
            .usuario(usuarioTestPMMD)
            .nombre("Mi Moderno")
            .modelo(ConfigSofa.Modelo.MODERNO)
            .cuerpos(3)
            .build();
    }


    @Test
    void testObtenerOCrearCarritoNuevo() {
        when(carritoRepoPMMD.findByUsuario(usuarioTestPMMD)).thenReturn(Optional.empty());
        when(carritoRepoPMMD.save(any(Carrito.class))).thenAnswer(inv -> {
            Carrito c = inv.getArgument(0);
            c.setId(1L);
            return c;
        });

        Carrito carrito = servicePMMD.obtenerOCrearCarrito(usuarioTestPMMD);

        assertNotNull(carrito);
        assertEquals(usuarioTestPMMD, carrito.getUsuario());
    }


    @Test
    void testAgregarAlCarrito() {
        Carrito carrito = Carrito.builder().usuario(usuarioTestPMMD).build();

        when(carritoRepoPMMD.findByUsuario(usuarioTestPMMD)).thenReturn(Optional.of(carrito));
        when(configRepoPMMD.findByIdAndUsuario(1L, usuarioTestPMMD)).thenReturn(Optional.of(configTestPMMD));
        when(carritoRepoPMMD.save(any(Carrito.class))).thenReturn(carrito);

        servicePMMD.agregarAlCarrito(usuarioTestPMMD, 1L, 2);

        assertEquals(1, carrito.getItems().size());
        assertEquals(2, carrito.getItems().get(0).getCantidad());
    }


    @Test
    void testRemoverDelCarrito() {
        CarritoItem item = CarritoItem.builder().id(1L).configuracion(configTestPMMD).cantidad(2).build();
        List<CarritoItem> items = new ArrayList<>();
        items.add(item);
        Carrito carrito = Carrito.builder().usuario(usuarioTestPMMD).items(items).build();

        when(carritoRepoPMMD.findByUsuario(usuarioTestPMMD)).thenReturn(Optional.of(carrito));
        when(carritoRepoPMMD.save(any(Carrito.class))).thenReturn(carrito);

        servicePMMD.removerDelCarrito(usuarioTestPMMD, 1L);

        assertTrue(carrito.getItems().isEmpty());
    }


    @Test
    void testActualizarCantidad() {
        CarritoItem item = CarritoItem.builder().id(1L).configuracion(configTestPMMD).cantidad(2).build();
        List<CarritoItem> items = new ArrayList<>();
        items.add(item);
        Carrito carrito = Carrito.builder().usuario(usuarioTestPMMD).items(items).build();

        when(carritoRepoPMMD.findByUsuario(usuarioTestPMMD)).thenReturn(Optional.of(carrito));
        when(carritoRepoPMMD.save(any(Carrito.class))).thenReturn(carrito);

        servicePMMD.actualizarCantidad(usuarioTestPMMD, 1L, 5);

        assertEquals(5, carrito.getItems().get(0).getCantidad());
    }


    @Test
    void testCalcularTotal() {
        CarritoItem item = CarritoItem.builder().id(1L).configuracion(configTestPMMD).cantidad(2).build();
        List<CarritoItem> items = new ArrayList<>();
        items.add(item);
        Carrito carrito = Carrito.builder().usuario(usuarioTestPMMD).items(items).build();

        when(carritoRepoPMMD.findByUsuario(usuarioTestPMMD)).thenReturn(Optional.of(carrito));
        when(precioServicePMMD.calcularPrecio(any(ConfigSofa.class))).thenReturn(BigDecimal.valueOf(1500));

        BigDecimal total = servicePMMD.calcularTotal(usuarioTestPMMD);

        assertEquals(BigDecimal.valueOf(3000).setScale(2, java.math.RoundingMode.HALF_UP), total);
    }


    @Test
    void testObtenerDetalleCarrito() {
        CarritoItem item = CarritoItem.builder().id(1L).configuracion(configTestPMMD).cantidad(2).build();
        List<CarritoItem> items = new ArrayList<>();
        items.add(item);
        Carrito carrito = Carrito.builder().usuario(usuarioTestPMMD).items(items).build();

        when(carritoRepoPMMD.findByUsuario(usuarioTestPMMD)).thenReturn(Optional.of(carrito));
        when(precioServicePMMD.calcularPrecio(any(ConfigSofa.class))).thenReturn(BigDecimal.valueOf(1500));

        GestorCarritoService.DetalleCarrito detalle = servicePMMD.obtenerDetalleCarrito(usuarioTestPMMD);

        assertEquals(1, detalle.cantidadItems());
        assertEquals(2, detalle.cantidadTotal());
    }
}

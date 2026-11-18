package pe.erp.sofas.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import pe.erp.sofas.model.*;
import pe.erp.sofas.repository.CarritoRepository;
import pe.erp.sofas.repository.PedidoRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


class CreadorPedidoServiceTest {

    private CreadorPedidoServicePMMD servicePMMD;


    @Mock
    private CarritoRepository carritoRepoPMMD;


    @Mock
    private PedidoRepository pedidoRepoPMMD;


    @Mock
    private CalculoPrecioServicePMMD precioServicePMMD;


    @Mock
    private GestorCarritoService gestorCarritoPMMD;


    private Usuario usuarioTestPMMD;

    private Cliente clienteTestPMMD;

    private ConfiguracionGuardada configTestPMMD;


    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        servicePMMD = new CreadorPedidoServicePMMD(carritoRepoPMMD, pedidoRepoPMMD, precioServicePMMD, gestorCarritoPMMD);

        usuarioTestPMMD = new Usuario();
        usuarioTestPMMD.setId(1L);

        clienteTestPMMD = new Cliente();
        clienteTestPMMD.setId(1L);
        clienteTestPMMD.setNombre("Cliente Test");

        configTestPMMD = ConfiguracionGuardada.builder()
            .id(1L)
            .usuario(usuarioTestPMMD)
            .nombre("Mi Moderno")
            .modelo(ConfigSofa.Modelo.MODERNO)
            .cuerpos(3)
            .build();
    }


    @Test
    void testCrearPedidoDesdeCarritoVacio() {
        Carrito carritoVacio = Carrito.builder().usuario(usuarioTestPMMD).items(new ArrayList<>()).build();
        when(gestorCarritoPMMD.obtenerOCrearCarrito(usuarioTestPMMD)).thenReturn(carritoVacio);

        assertThrows(IllegalArgumentException.class, () ->
            servicePMMD.crearPedidosDesdeCarrito(usuarioTestPMMD, clienteTestPMMD, false)
        );
    }


    @Test
    void testCrearPedidoDesdeCarritoConItems() {
        CarritoItem item = CarritoItem.builder()
            .id(1L)
            .configuracion(configTestPMMD)
            .cantidad(2)
            .build();

        List<CarritoItem> items = new ArrayList<>();
        items.add(item);

        Carrito carrito = Carrito.builder()
            .usuario(usuarioTestPMMD)
            .items(items)
            .build();

        when(gestorCarritoPMMD.obtenerOCrearCarrito(usuarioTestPMMD)).thenReturn(carrito);
        when(precioServicePMMD.calcularPrecio(any(ConfigSofa.class))).thenReturn(BigDecimal.valueOf(1500));
        when(pedidoRepoPMMD.save(any(Pedido.class))).thenAnswer(inv -> {
            Pedido p = inv.getArgument(0);
            p.setId(1L);
            return p;
        });

        List<Pedido> pedidos = servicePMMD.crearPedidosDesdeCarrito(usuarioTestPMMD, clienteTestPMMD, false);

        assertEquals(1, pedidos.size());
        Pedido pedido = pedidos.get(0);
        assertEquals(clienteTestPMMD, pedido.getCliente());
        assertEquals(2, pedido.getCantidad());
        assertEquals(BigDecimal.valueOf(3000), pedido.getPrecio());
    }


    @Test
    void testCrearPedidoVaciaCarrito() {
        CarritoItem item = CarritoItem.builder()
            .id(1L)
            .configuracion(configTestPMMD)
            .cantidad(1)
            .build();

        List<CarritoItem> items = new ArrayList<>();
        items.add(item);

        Carrito carrito = Carrito.builder()
            .usuario(usuarioTestPMMD)
            .items(items)
            .build();

        when(gestorCarritoPMMD.obtenerOCrearCarrito(usuarioTestPMMD)).thenReturn(carrito);
        when(precioServicePMMD.calcularPrecio(any(ConfigSofa.class))).thenReturn(BigDecimal.valueOf(1500));
        when(pedidoRepoPMMD.save(any(Pedido.class))).thenAnswer(inv -> {
            Pedido p = inv.getArgument(0);
            p.setId(1L);
            return p;
        });

        servicePMMD.crearPedidosDesdeCarrito(usuarioTestPMMD, clienteTestPMMD, true);

        verify(gestorCarritoPMMD).vaciarCarrito(usuarioTestPMMD);
    }


    @Test
    void testRegistrarAdelanto() {
        Pedido pedido = Pedido.builder()
            .id(1L)
            .precio(BigDecimal.valueOf(3000))
            .adelanto(BigDecimal.valueOf(500))
            .build();

        when(pedidoRepoPMMD.findById(1L)).thenReturn(Optional.of(pedido));
        when(pedidoRepoPMMD.save(any(Pedido.class))).thenReturn(pedido);

        servicePMMD.registrarAdelanto(1L, BigDecimal.valueOf(1000));

        assertEquals(BigDecimal.valueOf(1500), pedido.getAdelanto());
    }


    @Test
    void testRegistrarAdelantoExcedente() {
        Pedido pedido = Pedido.builder()
            .id(1L)
            .precio(BigDecimal.valueOf(1000))
            .adelanto(BigDecimal.valueOf(500))
            .build();

        when(pedidoRepoPMMD.findById(1L)).thenReturn(Optional.of(pedido));

        assertThrows(IllegalArgumentException.class, () ->
            servicePMMD.registrarAdelanto(1L, BigDecimal.valueOf(700))
        );
    }


    @Test
    void testCambiarEstado() {
        Pedido pedido = Pedido.builder()
            .id(1L)
            .estado(EstadoPedido.CONFIRMADO)
            .build();

        when(pedidoRepoPMMD.findById(1L)).thenReturn(Optional.of(pedido));
        when(pedidoRepoPMMD.save(any(Pedido.class))).thenReturn(pedido);

        servicePMMD.cambiarEstado(1L, EstadoPedido.EN_PRODUCCION);

        assertEquals(EstadoPedido.EN_PRODUCCION, pedido.getEstado());
    }


    @Test
    void testResumenPedidos() {
        Pedido p1 = Pedido.builder()
            .id(1L)
            .precio(BigDecimal.valueOf(1500))
            .cantidad(1)
            .build();

        Pedido p2 = Pedido.builder()
            .id(2L)
            .precio(BigDecimal.valueOf(3000))
            .cantidad(2)
            .build();

        List<Pedido> pedidos = new ArrayList<>();
        pedidos.add(p1);
        pedidos.add(p2);

        CreadorPedidoServicePMMD.ResumenPedidos resumen = CreadorPedidoServicePMMD.ResumenPedidos.desde(pedidos);

        assertEquals(2, resumen.totalPedidos());
        assertEquals(BigDecimal.valueOf(4500), resumen.montoTotal());
        assertEquals(3, resumen.cantidadTotal());
    }
}

package pe.erp.sofas.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import pe.erp.sofas.model.ConfigSofa;
import pe.erp.sofas.model.ConfiguracionGuardada;
import pe.erp.sofas.model.Usuario;
import pe.erp.sofas.repository.ConfiguracionGuardadaRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


class PersistenciaConfiguracionServiceTest {

    private PersistenciaConfiguracionService servicePMMD;


    @Mock
    private ConfiguracionGuardadaRepository repositoryPMMD;


    private Usuario usuarioTestPMMD;

    private ConfigSofa configTestPMMD;


    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        servicePMMD = new PersistenciaConfiguracionService(repositoryPMMD);

        usuarioTestPMMD = new Usuario();
        usuarioTestPMMD.setId(1L);
        usuarioTestPMMD.setNombre("Test User");

        configTestPMMD = new ConfigSofa(
            ConfigSofa.Modelo.MODERNO,
            3,
            ConfigSofa.Chaise.NINGUNA,
            92,
            null,
            ConfigSofa.Brazo.RECTO,
            ConfigSofa.Patas.MADERA,
            ConfigSofa.AcabadoMadera.NATURAL,
            ConfigSofa.Espuma.D23,
            1,
            null,
            ConfigSofa.Mecanismo.FIJO
        );
    }


    @Test
    void testGuardarConfiguracion() {
        ConfiguracionGuardada guardada = ConfiguracionGuardada.builder()
            .id(1L)
            .usuario(usuarioTestPMMD)
            .nombre("Mi Moderno")
            .modelo(ConfigSofa.Modelo.MODERNO)
            .cuerpos(3)
            .build();

        when(repositoryPMMD.save(any(ConfiguracionGuardada.class))).thenReturn(guardada);

        ConfiguracionGuardada resultado = servicePMMD.guardar(usuarioTestPMMD, configTestPMMD, "Mi Moderno", "Descripción");

        assertNotNull(resultado);
        assertEquals("Mi Moderno", resultado.getNombre());
        assertEquals(usuarioTestPMMD, resultado.getUsuario());
        verify(repositoryPMMD).save(any(ConfiguracionGuardada.class));
    }


    @Test
    void testCargarConfiguracion() {
        ConfiguracionGuardada guardada = ConfiguracionGuardada.builder()
            .id(1L)
            .usuario(usuarioTestPMMD)
            .nombre("Mi Moderno")
            .modelo(ConfigSofa.Modelo.MODERNO)
            .cuerpos(3)
            .build();

        when(repositoryPMMD.findByIdAndUsuario(1L, usuarioTestPMMD)).thenReturn(Optional.of(guardada));

        ConfigSofa resultado = servicePMMD.cargar(1L, usuarioTestPMMD);

        assertNotNull(resultado);
        assertEquals(ConfigSofa.Modelo.MODERNO, resultado.modelo());
        verify(repositoryPMMD).findByIdAndUsuario(1L, usuarioTestPMMD);
    }


    @Test
    void testListarPorUsuario() {
        ConfiguracionGuardada guardada1 = ConfiguracionGuardada.builder()
            .id(1L)
            .usuario(usuarioTestPMMD)
            .nombre("Config 1")
            .build();

        ConfiguracionGuardada guardada2 = ConfiguracionGuardada.builder()
            .id(2L)
            .usuario(usuarioTestPMMD)
            .nombre("Config 2")
            .build();

        when(repositoryPMMD.findByUsuarioOrderByFechaCreacionDesc(usuarioTestPMMD))
            .thenReturn(List.of(guardada2, guardada1));

        List<ConfiguracionGuardada> resultado = servicePMMD.listarPorUsuario(usuarioTestPMMD);

        assertEquals(2, resultado.size());
        verify(repositoryPMMD).findByUsuarioOrderByFechaCreacionDesc(usuarioTestPMMD);
    }


    @Test
    void testMarcarFavorito() {
        ConfiguracionGuardada guardada = ConfiguracionGuardada.builder()
            .id(1L)
            .usuario(usuarioTestPMMD)
            .nombre("Mi Moderno")
            .esFavorito(false)
            .build();

        when(repositoryPMMD.findByIdAndUsuario(1L, usuarioTestPMMD)).thenReturn(Optional.of(guardada));
        when(repositoryPMMD.save(guardada)).thenReturn(guardada);

        servicePMMD.marcarFavorito(1L, usuarioTestPMMD, true);

        assertTrue(guardada.getEsFavorito());
        verify(repositoryPMMD).save(guardada);
    }


    @Test
    void testEliminar() {
        ConfiguracionGuardada guardada = ConfiguracionGuardada.builder()
            .id(1L)
            .usuario(usuarioTestPMMD)
            .nombre("Mi Moderno")
            .build();

        when(repositoryPMMD.findByIdAndUsuario(1L, usuarioTestPMMD)).thenReturn(Optional.of(guardada));

        servicePMMD.eliminar(1L, usuarioTestPMMD);

        verify(repositoryPMMD).delete(guardada);
    }


    @Test
    void testContarPorUsuario() {
        when(repositoryPMMD.countByUsuario(usuarioTestPMMD)).thenReturn(5L);

        long resultado = servicePMMD.contarPorUsuario(usuarioTestPMMD);

        assertEquals(5L, resultado);
        verify(repositoryPMMD).countByUsuario(usuarioTestPMMD);
    }
}

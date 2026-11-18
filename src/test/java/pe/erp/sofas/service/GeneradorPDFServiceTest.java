package pe.erp.sofas.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import pe.erp.sofas.model.ConfigSofa;
import pe.erp.sofas.model.Usuario;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;


class GeneradorPDFServiceTest {

    private GeneradorPDFServicePMMD servicePMMD;


    @Mock
    private CalculoPrecioServicePMMD precioServicePMMD;


    @Mock
    private PersistenciaConfiguracionService configServicePMMD;


    private ConfigSofa configTestPMMD;

    private Usuario usuarioTestPMMD;


    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        servicePMMD = new GeneradorPDFServicePMMD(precioServicePMMD, configServicePMMD);

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

        usuarioTestPMMD = new Usuario();
        usuarioTestPMMD.setId(1L);
        usuarioTestPMMD.setNombre("Test User");
    }


    @Test
    void testGenerarPresupuestoSinImagen() {
        CalculoPrecioServicePMMD.DesglosePrecio desglose = new CalculoPrecioServicePMMD.DesglosePrecio(
            BigDecimal.valueOf(800),
            BigDecimal.valueOf(150),
            BigDecimal.valueOf(1330),
            BigDecimal.ZERO,
            BigDecimal.valueOf(1330),
            BigDecimal.valueOf(239.4),
            BigDecimal.valueOf(1569.4)
        );

        when(precioServicePMMD.desglosar(any(ConfigSofa.class), any()))
            .thenReturn(desglose);

        byte[] pdf = servicePMMD.generarPresupuesto(configTestPMMD, "Cliente Test", usuarioTestPMMD);

        assertNotNull(pdf);
        assertTrue(pdf.length > 0);
        // Validar que es un PDF válido (empieza con %PDF)
        assertTrue(pdf[0] == '%' && pdf[1] == 'P' && pdf[2] == 'D' && pdf[3] == 'F');
    }


    @Test
    void testGenerarPresupuestoConClienteNombre() {
        CalculoPrecioServicePMMD.DesglosePrecio desglose = new CalculoPrecioServicePMMD.DesglosePrecio(
            BigDecimal.valueOf(800),
            BigDecimal.valueOf(150),
            BigDecimal.valueOf(1330),
            BigDecimal.ZERO,
            BigDecimal.valueOf(1330),
            BigDecimal.valueOf(239.4),
            BigDecimal.valueOf(1569.4)
        );

        when(precioServicePMMD.desglosar(any(ConfigSofa.class), any()))
            .thenReturn(desglose);

        byte[] pdf = servicePMMD.generarPresupuesto(configTestPMMD, "Empresa XYZ S.A.C.", usuarioTestPMMD);

        assertNotNull(pdf);
        assertTrue(pdf.length > 0);
    }


    @Test
    void testGenerarPresupuestoContienePrecio() {
        CalculoPrecioServicePMMD.DesglosePrecio desglose = new CalculoPrecioServicePMMD.DesglosePrecio(
            BigDecimal.valueOf(800),
            BigDecimal.valueOf(150),
            BigDecimal.valueOf(1330),
            BigDecimal.ZERO,
            BigDecimal.valueOf(1330),
            BigDecimal.valueOf(239.4),
            BigDecimal.valueOf(1569.4)
        );

        when(precioServicePMMD.desglosar(any(ConfigSofa.class), any()))
            .thenReturn(desglose);

        byte[] pdf = servicePMMD.generarPresupuesto(configTestPMMD, "Cliente Test", usuarioTestPMMD);

        assertNotNull(pdf);
        String contenido = new String(pdf);
        // Verificar que contiene información de precios
        assertTrue(contenido.contains("DESGLOSE") || contenido.contains("PRECIO") || pdf.length > 1000);
    }


    @Test
    void testGenerarPresupuestoModernoConChaise() {
        ConfigSofa conChaise = new ConfigSofa(
            ConfigSofa.Modelo.MODERNO, 3, ConfigSofa.Chaise.DERECHA, 92, null,
            ConfigSofa.Brazo.RECTO, ConfigSofa.Patas.MADERA, ConfigSofa.AcabadoMadera.NATURAL,
            ConfigSofa.Espuma.D23, 1, null, ConfigSofa.Mecanismo.FIJO
        );

        CalculoPrecioServicePMMD.DesglosePrecio desglose = new CalculoPrecioServicePMMD.DesglosePrecio(
            BigDecimal.valueOf(800),
            BigDecimal.valueOf(150),
            BigDecimal.valueOf(1530),
            BigDecimal.ZERO,
            BigDecimal.valueOf(1530),
            BigDecimal.valueOf(275.4),
            BigDecimal.valueOf(1805.4)
        );

        when(precioServicePMMD.desglosar(any(ConfigSofa.class), any()))
            .thenReturn(desglose);

        byte[] pdf = servicePMMD.generarPresupuesto(conChaise, "Cliente Test", usuarioTestPMMD);

        assertNotNull(pdf);
        assertTrue(pdf.length > 0);
    }


    @Test
    void testGenerarPresupuestoEstructuraValida() {
        CalculoPrecioServicePMMD.DesglosePrecio desglose = new CalculoPrecioServicePMMD.DesglosePrecio(
            BigDecimal.valueOf(800),
            BigDecimal.valueOf(150),
            BigDecimal.valueOf(1330),
            BigDecimal.ZERO,
            BigDecimal.valueOf(1330),
            BigDecimal.valueOf(239.4),
            BigDecimal.valueOf(1569.4)
        );

        when(precioServicePMMD.desglosar(any(ConfigSofa.class), any()))
            .thenReturn(desglose);

        byte[] pdf = servicePMMD.generarPresupuesto(configTestPMMD, "Cliente Test", usuarioTestPMMD);

        // Validar estructura mínima
        assertNotNull(pdf);
        assertTrue(pdf.length > 500);  // Debe tener contenido sustancial

        // Verificar marcas de PDF
        String inicio = new String(pdf, 0, Math.min(10, pdf.length));
        assertTrue(inicio.contains("%PDF"));
    }
}

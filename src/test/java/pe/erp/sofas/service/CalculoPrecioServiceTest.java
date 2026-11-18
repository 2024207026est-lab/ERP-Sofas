package pe.erp.sofas.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import pe.erp.sofas.model.ConfigSofa;
import pe.erp.sofas.repository.FichaTecnicaRepository;
import pe.erp.sofas.repository.TelaRepository;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;


class CalculoPrecioServiceTest {

    private CalculoPrecioServicePMMD servicePMMD;


    @Mock
    private FichaTecnicaRepository fichaTecnicaRepoPMMD;


    @Mock
    private TelaRepository telaRepoPMMD;


    private ConfigSofa configTestPMMD;


    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        servicePMMD = new CalculoPrecioServicePMMD(fichaTecnicaRepoPMMD, telaRepoPMMD);

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
    void testCalcularPrecioSinFicha() {
        when(fichaTecnicaRepoPMMD.findByCodigoOrderByVersionDesc(anyString())).thenReturn(List.of());

        BigDecimal precio = servicePMMD.calcularPrecio(configTestPMMD);

        assertNotNull(precio);
        assertTrue(precio.compareTo(BigDecimal.ZERO) > 0);
    }


    @Test
    void testCalcularPrecioConDescuento() {
        when(fichaTecnicaRepoPMMD.findByCodigoOrderByVersionDesc(anyString())).thenReturn(List.of());

        BigDecimal precioSinDescuento = servicePMMD.calcularPrecio(configTestPMMD, BigDecimal.ZERO);
        BigDecimal precioConDescuento10 = servicePMMD.calcularPrecio(configTestPMMD, BigDecimal.TEN);

        assertTrue(precioSinDescuento.compareTo(precioConDescuento10) > 0);
    }


    @Test
    void testCalcularPrecioConEspumaD30() {
        when(fichaTecnicaRepoPMMD.findByCodigoOrderByVersionDesc(anyString())).thenReturn(List.of());

        ConfigSofa conD30 = new ConfigSofa(
            ConfigSofa.Modelo.MODERNO, 3, ConfigSofa.Chaise.NINGUNA, 92, null,
            ConfigSofa.Brazo.RECTO, ConfigSofa.Patas.MADERA, ConfigSofa.AcabadoMadera.NATURAL,
            ConfigSofa.Espuma.D30, 1, null, ConfigSofa.Mecanismo.FIJO
        );

        BigDecimal precioD30 = servicePMMD.calcularPrecio(conD30);

        assertTrue(precioD30.compareTo(BigDecimal.ZERO) > 0);
    }


    @Test
    void testCalcularPrecioConReclinableManual() {
        when(fichaTecnicaRepoPMMD.findByCodigoOrderByVersionDesc(anyString())).thenReturn(List.of());

        ConfigSofa conReclinable = new ConfigSofa(
            ConfigSofa.Modelo.MODERNO, 3, ConfigSofa.Chaise.NINGUNA, 92, null,
            ConfigSofa.Brazo.RECTO, ConfigSofa.Patas.MADERA, ConfigSofa.AcabadoMadera.NATURAL,
            ConfigSofa.Espuma.D23, 1, null, ConfigSofa.Mecanismo.RECLINABLE_MANUAL
        );

        BigDecimal precioReclinable = servicePMMD.calcularPrecio(conReclinable);

        assertTrue(precioReclinable.compareTo(BigDecimal.ZERO) > 0);
    }


    @Test
    void testDesglosePositivo() {
        when(fichaTecnicaRepoPMMD.findByCodigoOrderByVersionDesc(anyString())).thenReturn(List.of());

        CalculoPrecioServicePMMD.DesglosePrecio desglose = servicePMMD.desglosar(configTestPMMD, BigDecimal.TEN);

        assertNotNull(desglose);
        assertTrue(desglose.costoBOM().compareTo(BigDecimal.ZERO) > 0);
        assertTrue(desglose.precioFinal().compareTo(BigDecimal.ZERO) > 0);
    }


    @Test
    void testPrecioEsPositivo() {
        when(fichaTecnicaRepoPMMD.findByCodigoOrderByVersionDesc(anyString())).thenReturn(List.of());

        BigDecimal precio = servicePMMD.calcularPrecio(configTestPMMD);

        assertTrue(precio.compareTo(BigDecimal.ZERO) > 0);
    }
}

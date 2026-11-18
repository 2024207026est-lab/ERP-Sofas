package pe.erp.sofas.ui.three.parts.families;

import javafx.scene.paint.PhongMaterial;
import pe.erp.sofas.model.ConfigSofa;
import pe.erp.sofas.ui.three.parts.SofaPart;
import pe.erp.sofas.ui.three.parts.concrete.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Familia Seccional L con Esquinero: lado A + módulo esquina + lado B.
 * El esquinero es cuadrado con respaldos en dos lados (NUEVO en Fase 2).
 * Código: SF-012
 */

public class SeccionalLEsquineroFamily implements FamiliaFactory.FamiliaConstructor {

    private static final double ANCHO_CUERPOPMMD = 52;

    private static final double TAMANIO_ESQUINEROPMMD = 95;

    private static final double FONDO_ESTANDARPMMD = 92;

    private static final double ALTO_ESTANDARPMMD = 85;


    @Override
    public List<SofaPart> construir(ConfigSofa config, PhongMaterial materialTela, PhongMaterial materialRespaldo) {
        List<SofaPart> partes = new ArrayList<>();

        int cuerposA = 3;  // Lado A: 3 cuerpos típico
        int cuerposB = 2;  // Lado B: 2 cuerpos típico
        double largoA = cuerposA * ANCHO_CUERPOPMMD;
        double largoB = cuerposB * ANCHO_CUERPOPMMD;
        double largoTotal = largoA + TAMANIO_ESQUINEROPMMD + largoB;
        double fondo = config.profundidad();
        double alto = ALTO_ESTANDARPMMD;

        PhongMaterial matResp = materialRespaldo == null ? materialTela : materialRespaldo;

        // Base de asiento
        partes.add(new BaseSeatFramePart(largoTotal, fondo, 13, materialTela));

        // Lado A: cojines principales
        for (int i = 0; i < cuerposA; i++) {
            partes.add(new SeatCushionPart(ANCHO_CUERPOPMMD - 1, fondo, materialTela));
        }

        // Esquinero en el centro
        partes.add(new CornerPart(TAMANIO_ESQUINEROPMMD, materialTela, matResp));

        // Lado B: cojines secundarios
        for (int i = 0; i < cuerposB; i++) {
            partes.add(new SeatCushionPart(ANCHO_CUERPOPMMD - 1, fondo, materialTela));
        }

        // Respaldo lado A
        partes.add(new BackrestPart(largoA - 34, alto - 18 - 3, matResp));

        // Brazo lado A (extremo)
        partes.add(new ArmPart(ArmPart.TipoBrazo.RECTO, alto * 0.7, fondo, materialTela));

        // Brazo lado B (extremo)
        partes.add(new ArmPart(ArmPart.TipoBrazo.RECTO, alto * 0.7, fondo, materialTela));

        // Patas
        partes.add(new LegsPart(largoTotal, fondo, LegsPart.TipoPata.MADERA_CONICA, materialTela));

        return partes;
    }
}

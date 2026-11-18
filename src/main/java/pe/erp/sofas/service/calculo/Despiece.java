package pe.erp.sofas.service.calculo;

import pe.erp.sofas.model.Componente;

import java.util.*;

/**
 * Despiece de un sofa para la hoja de corte (L largo, F fondo, A alto, n cuerpos). Con chaise, el cuerpo
 * principal tiene F − 65 de fondo y la chaise lleva su propio cojin. Las medidas incluyen 1.5 cm de costura
 * por lado en la tela.
 */

public final class Despiece {
    
    /** Codigos de material genericos (los de la ficha); la OP puede sustituirlos. */

    public static final String D23 = "MP-019";

    public static final String D18_4 = "MP-018";

    public static final String D18_2 = "MP-017";

    public static final String MADERA = "MP-001";

    public static final String TRIPLAY = "MP-002";

    public static final String TELA = "MP-003";


    private static final double COSTURAPMMD = 3;

    private static final double ANCHO_CHAISEPMMD = 90;

    private static final double ROLLOPMMD = 140;

    private static final double TABLAPMMD = 300;


    public record Sofa(
      double largo, 
        double fondo, 
          double alto, 
            int cuerpos, 
              boolean chaise, 
                boolean puff,
                       
                       boolean girarTela, 
                         String origen) {
    }


    private Despiece() {
    }

    /** Piezas por codigo de material generico. */

    public static Map<String, List<Pieza>> piezas(Sofa s) {
        Map<String, List<Pieza>> m = new LinkedHashMap<>();
        if (s.puff()) {
            puff(s, m);
        } else {
            sofa(s, m);
        }
        return m;
    }


    private static void sofa(Sofa s, Map<String, List<Pieza>> m) {
        int n = Math.max(1, s.cuerpos());
        double L = s.largo(), F = s.fondo(), A = s.alto();
        double Fm = s.chaise() ? F - 65 : F;           // fondo del cuerpo principal
        double Lm = s.chaise() ? L - ANCHO_CHAISEPMMD : L; // largo del cuerpo principal
        double brazos = s.chaise() ? 16 : 32;          // con chaise queda un solo brazo
        double anchoCojin = (Lm - brazos) / n;
        double fondoCojin = Fm - 22;
        double altoRespaldo = A - 40;
        String o = s.origen();
        boolean g = s.girarTela();

        // ---- Espuma D23 5" (asientos)
        List<Pieza> d23 = lista(m, D23);
        d23.add(new Pieza("Cojín de asiento", Componente.ASIENTO, r(anchoCojin), r(fondoCojin), n, true, o));
        if (s.chaise()) d23.add(new Pieza("Cojín de chaise", Componente.ASIENTO, ANCHO_CHAISEPMMD - 16, r(F - 22), 1, true, o));

        // ---- Espuma D18 4" (respaldos)
        lista(m, D18_4).add(new Pieza("Respaldo", Componente.RESPALDO, r(anchoCojin), r(altoRespaldo), n, true, o));

        // ---- Espuma D18 2" (brazos interiores, tapas y frentes)
        List<Pieza> d18 = lista(m, D18_2);
        int nBrazos = s.chaise() ? 1 : 2;
        d18.add(new Pieza("Brazo interior", Componente.BRAZOS, r(Fm - 10), r(A - 35), nBrazos, true, o));
        d18.add(new Pieza("Tapa de brazo", Componente.BRAZOS, r(Fm - 10), 16, nBrazos, true, o));
        d18.add(new Pieza("Frente de asiento", Componente.ASIENTO, Math.min(r(Lm - brazos), 200), 18, 1, true, o));
        if (s.chaise()) d18.add(new Pieza("Frente de chaise", Componente.ASIENTO, ANCHO_CHAISEPMMD - 16, 18, 1, true, o));

        // ---- Tela (rollo de 140 cm)
        List<Pieza> tela = lista(m, TELA);
        double espesorAsiento = 12.7 + COSTURAPMMD, espesorRespaldo = 10.2 + COSTURAPMMD;
        tela.add(new Pieza("Cara de cojín de asiento", Componente.ASIENTO, r(anchoCojin + COSTURAPMMD), r(fondoCojin + COSTURAPMMD), 2 * n, g, o));
        banda(tela, "Banda de cojín de asiento", Componente.ASIENTO, 2 * (anchoCojin + fondoCojin) + COSTURAPMMD, espesorAsiento, n, g, o);
        if (s.chaise()) {
            tela.add(new Pieza("Cara de cojín de chaise", Componente.ASIENTO, ANCHO_CHAISEPMMD - 16 + COSTURAPMMD, r(F - 22 + COSTURAPMMD), 2, g, o));
            banda(tela, "Banda de cojín de chaise", Componente.ASIENTO, 2 * (ANCHO_CHAISEPMMD - 16 + F - 22) + COSTURAPMMD, espesorAsiento, 1, g, o);
        }
        tela.add(new Pieza("Cara de respaldo", Componente.RESPALDO, r(anchoCojin + COSTURAPMMD), r(altoRespaldo + COSTURAPMMD), 2 * n, g, o));
        banda(tela, "Banda de respaldo", Componente.RESPALDO, 2 * (anchoCojin + altoRespaldo) + COSTURAPMMD, espesorRespaldo, n, g, o);
        tela.add(new Pieza("Brazo interior", Componente.BRAZOS, r(Fm - 10 + COSTURAPMMD), r(A - 35 + 16 + COSTURAPMMD), nBrazos, g, o));
        tela.add(new Pieza("Brazo exterior", Componente.BRAZOS, r(Fm + COSTURAPMMD), r(A - 15 + COSTURAPMMD), nBrazos, g, o));
        tela.add(new Pieza("Frente de brazo", Componente.BRAZOS, 16 + COSTURAPMMD, r(A - 15 + COSTURAPMMD), nBrazos, g, o));
        banda(tela, "Frente de asiento", Componente.TAPIZADO, Lm - brazos + COSTURAPMMD, 25 + COSTURAPMMD, 1, g, o);
        banda(tela, "Respaldo exterior", Componente.TAPIZADO, L + COSTURAPMMD, A - 15 + COSTURAPMMD, 1, g, o);
        banda(tela, "Faldón", Componente.TAPIZADO, L + 2 * F + COSTURAPMMD, 18 + COSTURAPMMD, 1, g, o);

        // ---- Madera (tablas de 300 cm)
        List<Pieza> madera = lista(m, MADERA);
        liston(madera, "Larguero de asiento", Componente.ESTRUCTURA, L - 4, 2, o);
        liston(madera, "Travesaño", Componente.ESTRUCTURA, F - 10, n + 1 + (s.chaise() ? 2 : 0), o);
        madera.add(new Pieza("Parante de respaldo", Componente.ESTRUCTURA, r(A - 15), 0, n + 1, false, o));
        madera.add(new Pieza("Larguero de brazo", Componente.BRAZOS, r(Fm - 8), 0, 2 * nBrazos, false, o));
        liston(madera, "Refuerzo", Componente.ESTRUCTURA, L / 2 - 10, 2, o);
        madera.add(new Pieza("Taco", Componente.ENSAMBLAJE, 12, 0, 8 + (s.chaise() ? 2 : 0), false, o));

        // ---- Triplay 4 mm (planchas de 244 × 122)
        List<Pieza> tri = lista(m, TRIPLAY);
        tablero(tri, "Fondo de asiento", Componente.ASIENTO, Lm - brazos, Fm - 22, o);
        if (s.chaise()) tablero(tri, "Fondo de chaise", Componente.ASIENTO, ANCHO_CHAISEPMMD - 16, F - 22, o);
        tablero(tri, "Tapa trasera", Componente.RESPALDO, L - 4, A - 40, o);
        tri.add(new Pieza("Lateral", Componente.BRAZOS, r(Fm - 5), r(A - 20), nBrazos, true, o));
    }


    private static void puff(Sofa s, Map<String, List<Pieza>> m) {
        double L = s.largo(), A = s.alto();
        String o = s.origen();
        boolean g = s.girarTela();
        lista(m, D23).add(new Pieza("Tapa de puff", Componente.ASIENTO, L - 4, L - 4, 1, true, o));
        lista(m, D18_2).add(new Pieza("Lateral de puff", Componente.TAPIZADO, L - 4, A - 12, 4, true, o));
        List<Pieza> tela = lista(m, TELA);
        tela.add(new Pieza("Tapa de puff", Componente.ASIENTO, L + COSTURAPMMD, L + COSTURAPMMD, 1, g, o));
        tela.add(new Pieza("Lateral de puff", Componente.TAPIZADO, L + COSTURAPMMD, A + COSTURAPMMD, 4, g, o));
        List<Pieza> mad = lista(m, MADERA);
        mad.add(new Pieza("Marco de puff", Componente.ESTRUCTURA, L - 4, 0, 4, false, o));
        mad.add(new Pieza("Parante de puff", Componente.ESTRUCTURA, A - 14, 0, 4, false, o));
        List<Pieza> tri = lista(m, TRIPLAY);
        tri.add(new Pieza("Base y tapa de puff", Componente.ESTRUCTURA, L - 4, L - 4, 2, true, o));
    }

    /** Bandas: si son mas largas que el rollo y no se pueden girar, se dividen en dos con empalme. */

    private static void banda(List<Pieza> l, String nombre, Componente c, double largo, double ancho, int n,
                              boolean girable, String o) {
        if (largo > ROLLOPMMD && !girable) {
            int partes = (int) Math.ceil(largo / (ROLLOPMMD - COSTURAPMMD));
            l.add(new Pieza(nombre + " (empalme)", c, r(largo / partes + COSTURAPMMD), r(ancho), partes * n, false, o));
        } else {
            l.add(new Pieza(nombre, c, r(largo), r(ancho), n, girable, o));
        }
    }


    private static void liston(List<Pieza> l, String nombre, Componente c, double largo, int n, String o) {
        if (largo > TABLAPMMD) {
            l.add(new Pieza(nombre + " (empalme)", c, r(largo / 2 + 10), 0, 2 * n, false, o));
        } else {
            l.add(new Pieza(nombre, c, r(largo), 0, n, false, o));
        }
    }


    private static void tablero(List<Pieza> l, String nombre, Componente c, double w, double h, String o) {
        double ww = w, hh = h;
        int partes = 1;
        while (Math.max(ww, hh) > 240 || Math.min(ww, hh) > 120) {
            partes++;
            ww = w / partes;
            hh = h;
        }
        l.add(new Pieza(nombre + (partes > 1 ? " (1/" + partes + ")" : ""), c, r(ww), r(hh), partes, true, o));
    }


    private static List<Pieza> lista(Map<String, List<Pieza>> m, String codigo) {
        return m.computeIfAbsent(codigo, k -> new ArrayList<>());
    }


    private static double r(double v) {
        return Math.round(v * 10) / 10.0;
    }
}

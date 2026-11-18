package pe.erp.sofas.service.calculo;

import java.time.LocalDate;
import java.util.Set;

/**
 * Regla de garantia: pata floja, estructura quebrada y mecanismo trabado tienen 12 meses (estructura);
 * el resto 6 meses (tapiz y espuma).
 */

public final class Cobertura {

    private static final Set<String> ESTRUCTURAPMMD = Set.of("Pata floja", "Estructura quebrada", "Mecanismo trabado");


    public record Resultado(
      boolean cubierto, 
        int meses, 
          LocalDate vence, 
            String tipo) {

        public String texto() {
            return cubierto ? "En garantía hasta " + pe.erp.sofas.core.AppClockPMMD.fmt(vence) : "Fuera de garantía";
        }
    }


    private Cobertura() {
    }


    public static int meses(String falla) {
        return ESTRUCTURAPMMD.contains(falla) ? 12 : 6;
    }


    public static Resultado evaluar(String falla, LocalDate fechaCompra, LocalDate fechaReclamo) {
        int meses = meses(falla);
        if (fechaCompra == null) return new Resultado(false, meses, null, tipo(meses));
        LocalDate vence = fechaCompra.plusMonths(meses);
        LocalDate ref = fechaReclamo == null ? pe.erp.sofas.core.AppClockPMMD.hoy() : fechaReclamo;
        return new Resultado(!ref.isAfter(vence), meses, vence, tipo(meses));
    }


    private static String tipo(int meses) {
        return meses == 12 ? "Estructura · 12 meses" : "Tapiz y espuma · 6 meses";
    }
}

package pe.erp.sofas.service.calculo;

/**
 * Redondeo de compra segun la unidad (5.8): madera de 50 en 50 pt, plancha 1, metro de 10 en 10,
 * unidad de 10 en 10, caja 1, galon 1, kg de 5 en 5.
 */

public final class UnidadCompra {

    private UnidadCompra() {
    }


    public static double paso(String unidad) {
        if (unidad == null) return 1;
        return switch (unidad) {
            case "pt" -> 50;
            case "m" -> 10;
            case "unidad" -> 10;
            case "kg" -> 5;
            default -> 1; // plancha, caja, galón, juego, par
        };
    }


    public static double redondear(String unidad, double cantidad) {
        if (cantidad <= 0) return 0;
        double paso = paso(unidad);
        return Math.ceil(cantidad / paso - 1e-9) * paso;
    }
}

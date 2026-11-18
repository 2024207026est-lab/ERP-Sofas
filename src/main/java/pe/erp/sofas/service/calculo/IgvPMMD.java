package pe.erp.sofas.service.calculo;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Los precios de venta incluyen IGV (18 %): subtotal = total / 1.18. */

public final class IgvPMMD {

    public static final BigDecimal TASA = new BigDecimal("0.18");

    private static final BigDecimal FACTORPMMD = BigDecimal.ONE.add(TASA);


    private IgvPMMD() {
    }


    public static BigDecimal subtotal(BigDecimal total) {
        return total.divide(FACTORPMMD, 2, RoundingMode.HALF_UP);
    }


    public static BigDecimal igv(BigDecimal total) {
        return total.setScale(2, RoundingMode.HALF_UP).subtract(subtotal(total));
    }
}

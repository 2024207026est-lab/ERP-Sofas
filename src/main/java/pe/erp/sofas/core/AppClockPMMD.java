package pe.erp.sofas.core;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Reloj de la demo (AppClock). Toda la aplicacion usa esta fecha como "hoy" para que los datos de ejemplo
 * (retrasos, vencimientos, calendario) se vean igual que en el prototipo aprobado.
 */

public final class AppClockPMMD {
    
    /** Fecha actual de la demo: jueves, 15 de octubre de 2026. */

    public static final LocalDate HOY = LocalDate.of(2026, 10, 15);

    public static final LocalTime HORA = LocalTime.of(10, 30);


    public static final Locale PERU = Locale.forLanguageTag("es-PE");

    public static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public static final DateTimeFormatter FECHA_CORTA = DateTimeFormatter.ofPattern("dd/MM");

    private static final DateTimeFormatter LARGAPMMD = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM 'de' yyyy", PERU);

    private static final DateTimeFormatter DIA_MESPMMD = DateTimeFormatter.ofPattern("d MMM", PERU);


    private AppClockPMMD() {
    }


    public static LocalDate hoy() {
        return HOY;
    }


    public static String horaActual() {
        return HORA.format(DateTimeFormatter.ofPattern("HH:mm"));
    }

    /** "Jueves, 15 de octubre de 2026". */

    public static String fechaLarga() {
        return capitalizar(HOY.format(LARGAPMMD));
    }


    public static String fmt(LocalDate fecha) {
        return fecha == null ? "—" : fecha.format(FECHA);
    }


    public static String corta(LocalDate fecha) {
        return fecha == null ? "—" : fecha.format(FECHA_CORTA);
    }

    /** "15 oct". */

    public static String diaMes(LocalDate fecha) {
        return fecha == null ? "—" : fecha.format(DIA_MESPMMD).replace(".", "");
    }

    /** Nombre corto del mes: "Oct". */

    public static String mesCorto(int mes) {
        String m = capitalizar(java.time.Month.of(mes).getDisplayName(java.time.format.TextStyle.SHORT, PERU)).replace(".", "");
        return m.equals("Set") ? "Sep" : m;
    }


    public static String mesLargo(int mes) {
        return capitalizar(java.time.Month.of(mes).getDisplayName(java.time.format.TextStyle.FULL, PERU));
    }


    public static String capitalizar(String s) {
        return s == null || s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}

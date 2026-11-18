package pe.erp.sofas.core;

/** Regla de negocio incumplida; el mensaje se muestra tal cual al usuario. */

public class NegocioExceptionPMMD extends RuntimeException {

    public NegocioExceptionPMMD(String mensaje) {
        super(mensaje);
    }
}

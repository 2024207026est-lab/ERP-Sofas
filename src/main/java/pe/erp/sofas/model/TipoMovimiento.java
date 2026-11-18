package pe.erp.sofas.model;

/** Tipos de movimiento del kardex. Reserva, Liberacion y Retazo no alteran el saldo fisico. */

public enum TipoMovimiento {
    
    ENTRADA("Entrada", "MOVE_TO_INBOX", "green", 1),
    
    SALIDA("Salida", "UNARCHIVE", "amber", -1),
    
    AJUSTE("Ajuste", "TUNE", "blue", 0),
    
    RESERVA("Reserva", "LOCK", "blue", 0),
    
    LIBERACION("Liberación", "LOCK_OPEN", "gray", 0),
    
    CONSUMO_OP("Consumo OP", "BUILD", "red", -1),
    
    RETAZO("Retazo", "CONTENT_CUT", "wood", 0);


    private final String nombre;

    private final String icono;

    private final String variante;
    /** +1 suma, -1 resta, 0 no cambia el saldo (el ajuste usa el signo de la cantidad). */

    private final int signo;

    
    TipoMovimiento(String nombre, String icono, String variante, int signo) {
        this.nombre = nombre;
        this.icono = icono;
        this.variante = variante;
        this.signo = signo;
    }


    public String nombre() {
        return nombre;
    }


    public String icono() {
        return icono;
    }


    public String variante() {
        return variante;
    }


    public int signo() {
        return signo;
    }

    /** true si el movimiento cambia el stock fisico. */

    public boolean afectaSaldo() {
        return this == ENTRADA || this == SALIDA || this == AJUSTE || this == CONSUMO_OP;
    }


    @Override
    public String toString() {
        return nombre;
    }
}

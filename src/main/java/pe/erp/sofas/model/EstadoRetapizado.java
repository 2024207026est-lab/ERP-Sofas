package pe.erp.sofas.model;

/** Por cotizar → Cotizado → Aprobado → Recojo programado → En taller → Listo → Entregado. */

public enum EstadoRetapizado {
    
    POR_COTIZAR("Por cotizar", "Cotizar"),
    
    COTIZADO("Cotizado", "Aprobar"),
    
    APROBADO("Aprobado", "Programar recojo"),
    
    RECOJO_PROGRAMADO("Recojo programado", "Ingresar a taller"),
    
    EN_TALLER("En taller", "Marcar listo"),
    
    LISTO("Listo", "Entregar"),
    
    ENTREGADO("Entregado", null);


    private final String nombre;

    private final String accion;

    
    EstadoRetapizado(String nombre, String accion) {
        this.nombre = nombre;
        this.accion = accion;
    }


    public String nombre() {
        return nombre;
    }

    /** Texto del boton que lleva al siguiente estado. */

    public String accion() {
        return accion;
    }


    public EstadoRetapizado siguiente() {
        return this == ENTREGADO ? ENTREGADO : values()[ordinal() + 1];
    }


    @Override
    public String toString() {
        return nombre;
    }
}

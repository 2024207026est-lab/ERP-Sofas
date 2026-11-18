package pe.erp.sofas.model;

/** Borrador → Solicitada → Aprobada → Recibida (o Cancelada). */

public enum EstadoCompra {
    
    BORRADOR("Borrador"),
    
    SOLICITADA("Solicitada"),
    
    APROBADA("Aprobada"),
    
    RECIBIDA("Recibida"),
    
    CANCELADA("Cancelada");


    private final String nombre;

    
    EstadoCompra(String nombre) {
        this.nombre = nombre;
    }


    public String nombre() {
        return nombre;
    }

    /** Compra en transito para el MRP. */

    public boolean enTransito() {
        return this == SOLICITADA || this == APROBADA;
    }


    @Override
    public String toString() {
        return nombre;
    }
}

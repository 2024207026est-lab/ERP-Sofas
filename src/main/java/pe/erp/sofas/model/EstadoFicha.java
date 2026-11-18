package pe.erp.sofas.model;


public enum EstadoFicha {
    
    BORRADOR("Borrador"),
    
    APROBADA("Aprobada"),
    
    OBSOLETA("Obsoleta");


    private final String nombre;

    
    EstadoFicha(String nombre) {
        this.nombre = nombre;
    }


    public String nombre() {
        return nombre;
    }


    @Override
    public String toString() {
        return nombre;
    }
}

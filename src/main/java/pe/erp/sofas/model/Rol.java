package pe.erp.sofas.model;

/** Roles del sistema; la matriz de permisos por modulo esta en {@link Permiso}. */

public enum Rol {
    
    GERENTE("Gerente"),
    
    ADMINISTRADOR("Administrador"),
    
    VENDEDOR("Vendedor"),
    
    SUPERVISOR("Supervisor de taller"),
    
    CARPINTERO("Carpintero"),
    
    COSTURERA("Costurera"),
    
    TAPICERO("Tapicero"),
    
    ALMACENERO("Almacenero");


    private final String nombre;

    
    Rol(String nombre) {
        this.nombre = nombre;
    }


    public String nombre() {
        return nombre;
    }

    /** Puede autorizar que una OP avance sin ficha aprobada. */

    public boolean autoriza() {
        return this == GERENTE || this == SUPERVISOR || this == ADMINISTRADOR;
    }


    @Override
    public String toString() {
        return nombre;
    }
}

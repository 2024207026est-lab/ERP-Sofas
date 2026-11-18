package pe.erp.sofas.model;

/** Forma del modelo de sofa: decide como se dibuja en 3D y que despiece usa la hoja de corte. */

public enum Forma {
    
    MODERNO("Sofá Moderno"),
    
    SECCIONAL_L("Seccional L"),
    
    SILLON("Sillón"),
    
    JUEGO("Juego de sala"),
    
    CHESTERFIELD("Chesterfield"),
    
    SOFA_CAMA("Sofá cama"),
    
    RECLINABLE("Reclinable"),
    
    ESCANDINAVO("Escandinavo"),
    
    PUFF("Puff"),
    
    SECCIONAL_U("Seccional U");


    private final String nombre;

    
    Forma(String nombre) {
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

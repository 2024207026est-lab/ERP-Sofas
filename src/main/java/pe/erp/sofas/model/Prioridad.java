package pe.erp.sofas.model;


public enum Prioridad {
    
    ALTA("Alta", "#DC2626"),
    
    MEDIA("Media", "#F59E0B"),
    
    BAJA("Baja", "#94A3B8");


    private final String nombre;

    private final String color;

    
    Prioridad(String nombre, String color) {
        this.nombre = nombre;
        this.color = color;
    }


    public String nombre() {
        return nombre;
    }


    public String color() {
        return color;
    }


    @Override
    public String toString() {
        return nombre;
    }
}

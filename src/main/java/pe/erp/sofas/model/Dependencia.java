package pe.erp.sofas.model;

/**
 * Formula de escalado de una linea de la ficha segun las medidas pedidas (L largo, F fondo, A alto,
 * n cuerpos) frente a las medidas base de la ficha (L0, F0, A0, n0). La expresion se evalua con exp4j.
 */

public enum Dependencia {
    
    F("Fija", "1"),
    
    L("× largo", "L / L0"),
    
    LF("× largo·fondo", "(L * F) / (L0 * F0)"),
    
    LA("× largo·alto", "(L * A) / (L0 * A0)"),
    
    T("× superficie tapizada", "(L * (F + A)) / (L0 * (F0 + A0))"),
    
    N("× cuerpos", "n / n0"),
    
    P("Patas 4/6 según largo", "patas(L) / patas(L0)");


    private final String nombre;

    private final String expresion;

    
    Dependencia(String nombre, String expresion) {
        this.nombre = nombre;
        this.expresion = expresion;
    }


    public String nombre() {
        return nombre;
    }


    public String expresion() {
        return expresion;
    }


    @Override
    public String toString() {
        return nombre;
    }
}

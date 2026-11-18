package pe.erp.sofas.ui.three.parts;

/**
 * Layouts soportados para ensamblar módulos de sofá.
 * Define cómo se encadenan las partes espacialmente.
 */

public enum PartLayout {
    /**
     * Sofá lineal: una fila de módulos alineados.
     * Ejemplo: 3 cuerpos lineales.
     */
    
    LINEAL("Sofá lineal"),

    /**
     * Seccional en L con chaise: lado principal + chaise extendida.
     * Ejemplo: 3 cuerpos + chaise derecha.
     */
    
    L_CHAISE_IZQ("L con chaise izquierda"),
    
    L_CHAISE_DER("L con chaise derecha"),

    /**
     * Seccional en L con esquinero: lado A + módulo esquina + lado B.
     * El esquinero es cuadrado con respaldo en dos lados.
     * Ejemplo: 3 cuerpos + esquinero 95×95 + 2 cuerpos.
     */
    
    L_ESQUINERO_IZQ("L con esquinero izquierdo"),
    
    L_ESQUINERO_DER("L con esquinero derecho"),

    /**
     * Seccional en U: dos esquineros y módulos entre ellos.
     * Ejemplo: 2 + E + 3 + E + 2.
     */
    
    U_ESQUINEROS("Forma U"),

    /**
     * Constructor modular libre: cadena arbitraria de módulos.
     * Permite composiciones personalizadas pieza a pieza.
     */
    
    MODULAR("Constructor modular");


    private final String nombre;

    
    PartLayout(String nombre) {
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

package pe.erp.sofas.service.calculo;

import pe.erp.sofas.model.Componente;

/**
 * Pieza a cortar: ancho (a lo ancho de la plancha o del rollo) × alto (a lo largo), en cm.
 * En madera solo cuenta el largo ({@code ancho}).
 */

public record Pieza(
  String nombre, 
    Componente componente, 
      double ancho, 
        double alto, 
          int cantidad,
                    
                    boolean girable, 
                      String origen) {

    public Pieza conCantidad(int n) {
        return new Pieza(nombre, componente, ancho, alto, n, girable, origen);
    }


    public Pieza conOrigen(String o) {
        return new Pieza(nombre, componente, ancho, alto, cantidad, girable, o);
    }


    public Pieza conGiro(boolean g) {
        return new Pieza(nombre, componente, ancho, alto, cantidad, g, origen);
    }


    public double area() {
        return ancho * alto;
    }
}

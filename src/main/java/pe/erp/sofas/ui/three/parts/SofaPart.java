package pe.erp.sofas.ui.three.parts;

import javafx.geometry.Bounds;
import javafx.scene.Group;
import java.util.Map;

/**
 * Interfaz base para partes modulares de un sofá. Cada parte es responsable de:
 * - Generar su geometría (build)
 * - Reportar sus dimensiones físicas (ancho, fondo, alto)
 * - Proporcionar anclajes para encadenamiento de módulos (izq, der, frente)
 */

public interface SofaPart {
    
    /**
     * Construye la geometría de esta parte y la retorna como Group listo para posicionar.
     * El Group debe estar centrado en el origen, con Y hacia arriba.
     *
     * @return Group con la geometría de la parte (mallas, materiales, etc)
     */
    
    Group build();

    /**
     * Retorna las dimensiones físicas de esta parte en centímetros.
     * @return Dimensiones (ancho, fondo, alto)
     */
    
    PartDimensions getDimensions();

    /**
     * Retorna los puntos de anclaje para encadenar módulos.
     * Claves comunes: "left", "right", "front", "back"
     * @return Mapa de etiqueta → punto 3D
     */
    
    Map<String, Punto3D> getAnchors();

    /**
     * Información sobre esta parte para debugging.
     */
    
    String getName();

    /**
     * Dimensiones físicas de una parte.
     */

    record PartDimensions(
        
        double ancho,    // eje X
        
        double fondo,    // eje Z
        
        double alto      // eje Y
    ) {}

    /**
     * Punto en 3D para anclajes.
     */

    record Punto3D(
     double x, 
       double y, 
         double z) {}
}

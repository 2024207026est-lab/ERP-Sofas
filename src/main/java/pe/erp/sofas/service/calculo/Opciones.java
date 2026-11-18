package pe.erp.sofas.service.calculo;

import pe.erp.sofas.model.LineaExtra;

import java.util.List;
import java.util.Map;

/**
 * Opciones de una explosion: medidas a pedido (null = las de la ficha), sustituciones de material
 * (codigo original → elegido), materiales extra por unidad y factor adicional de mano de obra.
 */

public record Opciones(
  Medidas medidas, 
    Map<String, String> sustituciones, 
       List<LineaExtra> extras,
                       
                       boolean incluirOpcionales, 
                         double recargoManoObraPct, 
                           double manoObraFija) {

    public static final Opciones NINGUNA = new Opciones(null, Map.of(), List.of(), false, 0, 0);


    public static Opciones medidas(Medidas m) {
        return new Opciones(m, Map.of(), List.of(), false, 0, 0);
    }


    public static Opciones sustituir(Map<String, String> sust) {
        return new Opciones(null, sust, List.of(), false, 0, 0);
    }


    public Opciones conMedidas(Medidas m) {
        return new Opciones(m, sustituciones, extras, incluirOpcionales, recargoManoObraPct, manoObraFija);
    }


    public Map<String, String> sust() {
        return sustituciones == null ? Map.of() : sustituciones;
    }


    public List<LineaExtra> ext() {
        return extras == null ? List.of() : extras;
    }
}

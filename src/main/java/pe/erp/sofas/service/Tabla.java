package pe.erp.sofas.service;

import java.util.ArrayList;
import java.util.List;

/** Tabla generica para reportes y exportaciones (PDF y Excel). {@code derecha[i]} alinea la columna a la derecha. */

public record Tabla(
  String titulo, 
    String subtitulo, 
      List<String> columnas, 
        boolean[] derecha, 
          List<List<String>> filas,
                    
                    List<String> totales) {

    public static Tabla de(String titulo, String subtitulo, List<String> columnas, boolean[] derecha) {
        return new Tabla(titulo, subtitulo, columnas, derecha, new ArrayList<>(), null);
    }


    public Tabla fila(Object... valores) {
        List<String> f = new ArrayList<>();
        for (Object v : valores) f.add(v == null ? "—" : v.toString());
        filas.add(f);
        return this;
    }


    public Tabla conTotales(String... valores) {
        return new Tabla(titulo, subtitulo, columnas, derecha, filas, List.of(valores));
    }
}

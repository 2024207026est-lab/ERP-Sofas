package pe.erp.sofas.service.calculo;

import pe.erp.sofas.model.FichaTecnica;

/** Largo, fondo y alto en cm y numero de cuerpos. */

public record Medidas(
  double largo, 
    double fondo, 
      double alto, 
        int cuerpos) {

    public static Medidas de(FichaTecnica f) {
        return new Medidas(nz(f.getLargo()), nz(f.getFondo()), nz(f.getAlto()), f.getCuerpos() == null ? 1 : f.getCuerpos());
    }


    private static double nz(Integer v) {
        return v == null ? 0 : v;
    }


    public boolean igual(Medidas o) {
        return o != null && Math.abs(largo - o.largo) < 0.01 && Math.abs(fondo - o.fondo) < 0.01
                && Math.abs(alto - o.alto) < 0.01 && cuerpos == o.cuerpos;
    }


    public String texto() {
        return Math.round(largo) + " × " + Math.round(fondo) + " × " + Math.round(alto) + " cm";
    }
}

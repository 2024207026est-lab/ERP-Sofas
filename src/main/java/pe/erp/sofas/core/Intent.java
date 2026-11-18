package pe.erp.sofas.core;

/**
 * "Intencion" de navegacion: ademas de abrir un modulo, le pide que haga algo al mostrarse
 * (p. ej. abrir el formulario de nuevo registro o el detalle de un codigo).
 */

public record Intent(
  String accion, 
    Object dato) {

    public static final String ACCION_NUEVO = "NEW";

    public static final String ACCION_ABRIR = "OPEN";

    public static final String ACCION_PRESELECCIONAR = "PRESELECT";


    public static final Intent NONE = new Intent(null, null);

    public static final Intent NEW = new Intent(ACCION_NUEVO, null);


    public static Intent abrir(Object dato) {
        return new Intent(ACCION_ABRIR, dato);
    }


    public static Intent preseleccionar(Object dato) {
        return new Intent(ACCION_PRESELECCIONAR, dato);
    }


    public boolean es(String accion) {
        return accion.equals(this.accion);
    }
}

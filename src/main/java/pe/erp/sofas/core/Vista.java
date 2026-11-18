package pe.erp.sofas.core;

/** Controlador de modulo: se refresca cada vez que el usuario entra al modulo. */

public interface Vista {
    
    /** Se invoca al mostrar el modulo; {@code intent} puede pedir abrir un formulario o un registro. */
    
    void onShow(Intent intent);

    /** Vistas con recursos que deben pausarse al salir (p. ej. el AnimationTimer del 3D). */

    interface ConCiclo extends Vista {
        
        void onHide();
    }
}

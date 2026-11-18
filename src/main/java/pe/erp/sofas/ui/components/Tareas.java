package pe.erp.sofas.ui.components;

import javafx.concurrent.Task;
import pe.erp.sofas.core.NegocioExceptionPMMD;
import pe.erp.sofas.core.Overlays;
import pe.erp.sofas.core.Toast;

import java.util.function.Consumer;
import java.util.function.Supplier;

/** Ejecuta trabajos pesados (PDF, Excel, MRP, optimizacion de corte) en un Task con indicador de progreso. */

public final class Tareas {

    private Tareas() {
    }


    public static <T> void ejecutar(Overlays overlays, Toast toast, String mensaje, Supplier<T> trabajo, Consumer<T> alTerminar) {
        Overlays.Handle espera = overlays.cargando(mensaje);
        Task<T> t = new Task<>() {

            @Override
            protected T call() {
                return trabajo.get();
            }
        };
        t.setOnSucceeded(e -> {
            espera.cerrar();
            alTerminar.accept(t.getValue());
        });
        t.setOnFailed(e -> {
            espera.cerrar();
            Throwable ex = t.getException();
            while (ex != null && !(ex instanceof NegocioExceptionPMMD) && ex.getCause() != null) ex = ex.getCause();
            toast.error(ex instanceof NegocioExceptionPMMD ? ex.getMessage() : "No se pudo completar: " + (ex == null ? "error" : ex.getMessage()));
            if (!(ex instanceof NegocioExceptionPMMD) && t.getException() != null) {
                org.slf4j.LoggerFactory.getLogger(Tareas.class).error("Tarea fallida", t.getException());
            }
        });
        Thread hilo = new Thread(t, "innova-tarea");
        hilo.setDaemon(true);
        hilo.start();
    }
}

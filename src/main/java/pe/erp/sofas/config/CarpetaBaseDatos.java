package pe.erp.sofas.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;

import java.io.File;

/** Crea la carpeta de la base SQLite ({@code app.db.dir}) y la de imagenes antes de abrir la conexion. */

public class CarpetaBaseDatos implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment env, SpringApplication app) {
        String dir = env.getProperty("app.db.dir");
        if (dir != null && !dir.isBlank()) {
            new File(dir).mkdirs();
            new File(dir, "images").mkdirs();
        }
    }
}

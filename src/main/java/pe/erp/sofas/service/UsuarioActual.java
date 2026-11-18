package pe.erp.sofas.service;

import org.springframework.stereotype.Component;
import pe.erp.sofas.model.Rol;

/** Usuario que firma los movimientos del kardex y las aprobaciones (lo fija el login). */

@Component
public class UsuarioActual {

    private String username = "sistema";

    private String nombre = "Sistema";

    private Rol rol = Rol.GERENTE;


    public void fijar(String username, String nombre, Rol rol) {
        this.username = username;
        this.nombre = nombre;
        this.rol = rol;
    }


    public String username() {
        return username;
    }


    public String nombre() {
        return nombre;
    }


    public Rol rol() {
        return rol;
    }
}

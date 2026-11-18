package pe.erp.sofas.core;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import pe.erp.sofas.model.Rol;
import pe.erp.sofas.model.Usuario;
import pe.erp.sofas.service.UsuarioActual;

/** Usuario con sesion iniciada (tambien firma los movimientos del kardex). */

@Component
@Lazy
@RequiredArgsConstructor
public class Session {

    private final UsuarioActual usuarioActualPMMD;

    private Usuario usuario;


    public Usuario usuario() {
        return usuario;
    }


    public void iniciar(Usuario usuario) {
        this.usuario = usuario;
        usuarioActualPMMD.fijar(usuario.getUsername(), usuario.getNombre(), usuario.getRol());
    }


    public void cerrar() {
        this.usuario = null;
    }


    public String nombre() {
        return usuario == null ? "Carlos Mendoza" : usuario.getNombre();
    }


    public String cargo() {
        return usuario == null ? "Gerente General" : usuario.getCargo();
    }


    public Rol rol() {
        return usuario == null ? Rol.GERENTE : usuario.getRol();
    }


    public String username() {
        return usuario == null ? "cmendoza" : usuario.getUsername();
    }
}

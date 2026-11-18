package pe.erp.sofas.service;

import lombok.RequiredArgsConstructor;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.erp.sofas.core.AppClockPMMD;
import pe.erp.sofas.core.NegocioExceptionPMMD;
import pe.erp.sofas.model.Usuario;
import pe.erp.sofas.repository.UsuarioRepository;

/**
 * Inicio de sesion con contrasenas BCrypt (jBCrypt). En modo prototipo ({@code app.auth.modo-prototipo=true})
 * cualquier credencial no vacia ingresa como el usuario demo.
 */

@Service
@RequiredArgsConstructor
public class AuthServicePMMD {

    public static final String USUARIO_DEMO = "cmendoza";


    private final UsuarioRepository repositoryPMMD;

    private final UsuarioActual usuarioActualPMMD;


    @Value("${app.auth.modo-prototipo:false}")
    private boolean modoPrototipo;


    @Transactional
    public Usuario login(String username, String password) {
        String user = username == null ? "" : username.trim();
        if (user.isEmpty() || password == null || password.isEmpty()) throw new NegocioExceptionPMMD("Ingresa tu usuario y contraseña.");
        var encontrado = repositoryPMMD.findByUsernameIgnoreCase(user);
        Usuario u;
        if (modoPrototipo) {
            u = encontrado.filter(Usuario::getActivo).orElseGet(() -> repositoryPMMD.findByUsernameIgnoreCase(USUARIO_DEMO).orElseThrow());
        } else {
            u = encontrado.orElseThrow(() -> new NegocioExceptionPMMD("Usuario o contraseña incorrectos."));
            if (!u.getActivo()) throw new NegocioExceptionPMMD("El usuario está desactivado.");
            if (!verificar(password, u.getPasswordHash())) throw new NegocioExceptionPMMD("Usuario o contraseña incorrectos.");
        }
        u.setUltimoAcceso(AppClockPMMD.hoy());
        usuarioActualPMMD.fijar(u.getUsername(), u.getNombre(), u.getRol());
        return repositoryPMMD.save(u);
    }

    /** Valida la clave de un Supervisor (o Gerente/Administrador) para autorizar una excepcion. */

    public Usuario autorizar(String username, String password) {
        Usuario u = repositoryPMMD.findByUsernameIgnoreCase(username == null ? "" : username.trim())
                .orElseThrow(() -> new NegocioExceptionPMMD("Usuario no encontrado."));
        if (!u.getRol().autoriza()) throw new NegocioExceptionPMMD(u.getNombre() + " no puede autorizar: se requiere un Supervisor.");
        if (!verificar(password == null ? "" : password, u.getPasswordHash())) throw new NegocioExceptionPMMD("Contraseña de autorización incorrecta.");
        return u;
    }


    public java.util.List<Usuario> autorizadores() {
        return repositoryPMMD.findAll().stream().filter(u -> u.getActivo() && u.getRol().autoriza()).toList();
    }


    public static String hash(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt(10));
    }


    public static boolean verificar(String password, String hash) {
        try {
            return hash != null && BCrypt.checkpw(password, hash);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}

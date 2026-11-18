package pe.erp.sofas.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.erp.sofas.core.NegocioExceptionPMMD;
import pe.erp.sofas.model.*;
import pe.erp.sofas.repository.EmpresaRepository;
import pe.erp.sofas.repository.PermisoRepository;
import pe.erp.sofas.repository.UsuarioRepository;

import java.util.List;

/** Empresa, usuarios, roles y matriz de permisos. */

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConfiguracionService {

    private final EmpresaRepository empresaRepositoryPMMD;

    private final UsuarioRepository usuarioRepositoryPMMD;

    private final PermisoRepository permisoRepositoryPMMD;


    public Empresa empresa() {
        return empresaRepositoryPMMD.findById(1L).orElseGet(() -> Empresa.builder().id(1L).razonSocial("SISTEMA ERP SOFAS S.A.C.").build());
    }


    @Transactional
    public Empresa guardarEmpresa(Empresa e) {
        if (e.getRazonSocial() == null || e.getRazonSocial().isBlank()) throw new NegocioExceptionPMMD("La razón social es obligatoria.");
        if (e.getRuc() == null || !e.getRuc().matches("\\d{11}")) throw new NegocioExceptionPMMD("El RUC debe tener 11 dígitos.");
        e.setId(1L);
        return empresaRepositoryPMMD.save(e);
    }


    public List<Usuario> usuarios() {
        return usuarioRepositoryPMMD.findAll();
    }


    @Transactional
    public Usuario guardarUsuario(Usuario u, String password) {
        if (u.getUsername() == null || !u.getUsername().matches("[a-zA-Z0-9._]{3,30}")) throw new NegocioExceptionPMMD("Usuario de 3 a 30 caracteres, sin espacios.");
        if (u.getNombre() == null || u.getNombre().isBlank()) throw new NegocioExceptionPMMD("El nombre es obligatorio.");
        boolean duplicado = usuarios().stream().anyMatch(x -> x.getUsername().equalsIgnoreCase(u.getUsername()) && !x.getId().equals(u.getId()));
        if (duplicado) throw new NegocioExceptionPMMD("El usuario " + u.getUsername() + " ya existe.");
        if (u.getId() == null && (password == null || password.length() < 6)) throw new NegocioExceptionPMMD("La contraseña debe tener al menos 6 caracteres.");
        if (password != null && !password.isEmpty()) u.setPasswordHash(AuthServicePMMD.hash(password));
        return usuarioRepositoryPMMD.save(u);
    }


    public List<Permiso> permisos(Rol rol) {
        return permisoRepositoryPMMD.findByRol(rol);
    }


    @Transactional
    public Permiso fijarPermiso(Rol rol, String modulo, boolean ver, boolean editar) {
        Permiso p = permisoRepositoryPMMD.findByRolAndModulo(rol, modulo).orElseGet(() -> Permiso.builder().rol(rol).modulo(modulo).build());
        p.setVer(ver || editar);
        p.setEditar(editar);
        return permisoRepositoryPMMD.save(p);
    }


    public boolean puedeVer(Rol rol, String modulo) {
        if (rol == Rol.GERENTE) return true;
        return permisoRepositoryPMMD.findByRolAndModulo(rol, modulo).map(Permiso::getVer).orElse(false);
    }
}

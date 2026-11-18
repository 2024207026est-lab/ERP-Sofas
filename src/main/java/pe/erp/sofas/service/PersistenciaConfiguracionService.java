package pe.erp.sofas.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pe.erp.sofas.model.ConfigSofa;
import pe.erp.sofas.model.ConfiguracionGuardada;
import pe.erp.sofas.model.Usuario;
import pe.erp.sofas.repository.ConfiguracionGuardadaRepository;

import java.util.List;
import java.util.Optional;


@Service
@RequiredArgsConstructor
public class PersistenciaConfiguracionService {

    private final ConfiguracionGuardadaRepository repoPMMD;


    public ConfiguracionGuardada guardar(Usuario usuario, ConfigSofa config, String nombre, String descripcion) {
        ConfiguracionGuardada guardada = ConfiguracionGuardada.fromConfigSofa(config, usuario, nombre, descripcion);
        return repoPMMD.save(guardada);
    }


    public ConfigSofa cargar(Long id, Usuario usuario) {
        ConfiguracionGuardada guardada = repoPMMD.findByIdAndUsuario(id, usuario)
            .orElseThrow(() -> new IllegalArgumentException("Configuración no encontrada"));
        return guardada.toConfigSofa();
    }


    public Optional<ConfiguracionGuardada> obtener(Long id, Usuario usuario) {
        return repoPMMD.findByIdAndUsuario(id, usuario);
    }


    public List<ConfiguracionGuardada> listarPorUsuario(Usuario usuario) {
        return repoPMMD.findByUsuarioOrderByFechaCreacionDesc(usuario);
    }


    public List<ConfiguracionGuardada> listarFavoritos(Usuario usuario) {
        return repoPMMD.findByUsuarioAndEsFavoritoTrueOrderByFechaCreacionDesc(usuario);
    }


    public List<ConfiguracionGuardada> listarPorEtiqueta(Usuario usuario, String etiqueta) {
        return repoPMMD.findByUsuarioAndEtiquetaOrderByFechaCreacionDesc(usuario, etiqueta);
    }


    public ConfiguracionGuardada marcarFavorito(Long id, Usuario usuario, boolean esFavorito) {
        ConfiguracionGuardada guardada = repoPMMD.findByIdAndUsuario(id, usuario)
            .orElseThrow(() -> new IllegalArgumentException("Configuración no encontrada"));
        guardada.setEsFavorito(esFavorito);
        return repoPMMD.save(guardada);
    }


    public ConfiguracionGuardada actualizar(Long id, Usuario usuario, String nombre, String descripcion, String etiqueta) {
        ConfiguracionGuardada guardada = repoPMMD.findByIdAndUsuario(id, usuario)
            .orElseThrow(() -> new IllegalArgumentException("Configuración no encontrada"));
        guardada.setNombre(nombre);
        guardada.setDescripcion(descripcion);
        guardada.setEtiqueta(etiqueta);
        return repoPMMD.save(guardada);
    }


    public void eliminar(Long id, Usuario usuario) {
        ConfiguracionGuardada guardada = repoPMMD.findByIdAndUsuario(id, usuario)
            .orElseThrow(() -> new IllegalArgumentException("Configuración no encontrada"));
        repoPMMD.delete(guardada);
    }


    public long contarPorUsuario(Usuario usuario) {
        return repoPMMD.countByUsuario(usuario);
    }
}

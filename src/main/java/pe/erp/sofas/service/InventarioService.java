package pe.erp.sofas.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.erp.sofas.core.AppClockPMMD;
import pe.erp.sofas.core.NegocioExceptionPMMD;
import pe.erp.sofas.model.*;
import pe.erp.sofas.repository.MaterialRepository;
import pe.erp.sofas.repository.MovimientoRepository;
import pe.erp.sofas.repository.TelaRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/** Materiales del almacen y kardex. Reserva, Liberacion y Retazo no alteran el saldo fisico. */

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InventarioService {

    public static final List<String> UNIDADES = List.of("pt", "plancha", "m", "unidad", "caja", "galón", "kg", "juego", "par");


    private final MaterialRepository materialRepositoryPMMD;

    private final MovimientoRepository movimientoRepositoryPMMD;

    private final TelaRepository telaRepositoryPMMD;

    private final UsuarioActual usuarioPMMD;


    public List<Material> materiales() {
        return materialRepositoryPMMD.findAllByOrderByCodigoAsc();
    }


    public Material material(Long id) {
        return materialRepositoryPMMD.findById(id).orElseThrow(() -> new NegocioExceptionPMMD("El material ya no existe."));
    }


    public Material material(String codigo) {
        return materialRepositoryPMMD.findByCodigo(codigo).orElseThrow(() -> new NegocioExceptionPMMD("No existe el material " + codigo + "."));
    }


    public List<Movimiento> movimientos() {
        return movimientoRepositoryPMMD.findAllByOrderByFechaDescIdDesc();
    }


    public List<Movimiento> kardex(Long materialId) {
        return movimientoRepositoryPMMD.findByMaterialIdOrderByFechaAscIdAsc(materialId);
    }

    /** Materiales bajo el minimo (por disponible), del mas urgente al menos urgente. */

    public List<Material> bajoMinimo() {
        return materiales().stream().filter(Material::isBajoMinimo)
                .sorted(Comparator.comparingDouble(m -> m.getDisponible() / Math.max(0.01, m.getMinimo())))
                .toList();
    }


    public List<Material> criticos() {
        return materiales().stream().filter(Material::isCritico).toList();
    }


    public double valorTotal() {
        return materiales().stream().mapToDouble(Material::getValor).sum();
    }


    public long movimientosDelMes() {
        LocalDate inicio = AppClockPMMD.hoy().withDayOfMonth(1);
        return movimientoRepositoryPMMD.findAll().stream().filter(m -> !m.getFecha().toLocalDate().isBefore(inicio)).count();
    }


    public String siguienteCodigo() {
        return Codigos.siguiente(materialRepositoryPMMD.findAll(), Material::getCodigo, "MP-", 3);
    }


    @Transactional
    public Material guardarMaterial(Material material) {
        if (material.getNombre() == null || material.getNombre().isBlank()) throw new NegocioExceptionPMMD("El nombre del material es obligatorio.");
        if (material.getCosto() == null || material.getCosto() < 0) throw new NegocioExceptionPMMD("Ingresa un costo unitario válido.");
        if (material.getStock() < 0 || material.getMinimo() < 0) throw new NegocioExceptionPMMD("Las cantidades no pueden ser negativas.");
        boolean nuevo = material.getId() == null;
        if (nuevo) material.setCodigo(siguienteCodigo());
        double inicial = material.getStock();
        if (nuevo) material.setStock(0d);
        Material g = materialRepositoryPMMD.save(material);
        if (nuevo && inicial > 0) registrar(g, TipoMovimiento.ENTRADA, inicial, "INI-" + g.getCodigo(), "Inventario inicial");
        return g;
    }

    /**
     * Entrada, salida o ajuste manual. En las telas los metros se aplican a un color del muestrario
     * (el stock del material es siempre la suma de sus colores).
     */

    @Transactional
    public Movimiento movimientoManual(Long materialId, TipoMovimiento tipo, double cantidad, Long telaId,
                                       String referencia, String motivo) {
        if (tipo != TipoMovimiento.ENTRADA && tipo != TipoMovimiento.SALIDA && tipo != TipoMovimiento.AJUSTE) {
            throw new NegocioExceptionPMMD("Movimiento no permitido.");
        }
        if (tipo == TipoMovimiento.AJUSTE ? cantidad == 0 : cantidad <= 0) throw new NegocioExceptionPMMD("La cantidad debe ser mayor a cero.");
        Material m = material(materialId);
        if (m.isTela()) {
            if (telaId == null) throw new NegocioExceptionPMMD("Elige el color de la tela.");
            Tela t = telaRepositoryPMMD.findById(telaId).orElseThrow();
            double efecto = tipo == TipoMovimiento.SALIDA ? -cantidad : cantidad;
            if (t.getStock() + efecto < -1e-9) throw new NegocioExceptionPMMD("El color " + t.getColor() + " solo tiene " + fmt(t.getStock()) + " m.");
            t.setStock(t.getStock() + efecto);
            telaRepositoryPMMD.save(t);
            motivo = (motivo == null || motivo.isBlank() ? tipo.nombre() : motivo) + " · " + t.getColor() + " " + t.getCodigo();
        }
        return registrar(m, tipo, cantidad, referencia, motivo);
    }

    /**
     * Registra un movimiento y actualiza el saldo fisico. Una salida o consumo nunca deja el stock en negativo
     * ni por debajo de lo reservado por otras ordenes (salvo el propio consumo de la OP).
     */

    @Transactional
    public Movimiento registrar(Material material, TipoMovimiento tipo, double cantidad, String referencia, String concepto) {
        double saldo = material.getStock();
        double efecto = switch (tipo) {
            case ENTRADA -> cantidad;
            case SALIDA, CONSUMO_OP -> -cantidad;
            case AJUSTE -> cantidad;
            default -> 0;
        };
        if (saldo + efecto < -1e-9) {
            throw new NegocioExceptionPMMD("Stock insuficiente: hay " + fmt(saldo) + " " + material.getUnidad() + " de " + material.getNombre() + ".");
        }
        if (tipo == TipoMovimiento.SALIDA && saldo + efecto < material.getReservado() - 1e-9) {
            throw new NegocioExceptionPMMD("No se puede sacar material reservado para producción: disponible "
                    + fmt(material.getDisponible()) + " " + material.getUnidad() + ".");
        }
        material.setStock(Math.max(0, saldo + efecto));
        materialRepositoryPMMD.save(material);
        return movimientoRepositoryPMMD.save(Movimiento.builder()
                .material(material).fecha(LocalDateTime.of(AppClockPMMD.hoy(), AppClockPMMD.HORA)).tipo(tipo)
                .cantidad(cantidad).saldo(material.getStock())
                .referencia(referencia == null || referencia.isBlank() ? "—" : referencia.trim())
                .concepto(concepto == null || concepto.isBlank() ? tipo.nombre() : concepto.trim())
                .usuario(usuarioPMMD.username()).build());
    }


    public static String fmt(double n) {
        return n == Math.floor(n) ? String.valueOf((long) n) : String.format(java.util.Locale.US, "%.2f", n);
    }
}

package pe.erp.sofas.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.erp.sofas.core.AppClockPMMD;
import pe.erp.sofas.core.NegocioExceptionPMMD;
import pe.erp.sofas.model.*;
import pe.erp.sofas.repository.ConsumoRealRepository;
import pe.erp.sofas.repository.EmpleadoRepository;
import pe.erp.sofas.repository.OrdenProduccionRepository;

import java.time.LocalDate;
import java.util.*;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmpleadoService {
    
    /** Productividad del mes: sofas terminados por la persona y monto a destajo de su etapa. */

    public record Productividad(
      Empleado empleado, 
        int sofasTerminados, 
          int sofasEnEtapa, 
            double tarifa, 
              double destajo) {
    }


    private final EmpleadoRepository repositoryPMMD;

    private final OrdenProduccionRepository opRepositoryPMMD;

    private final ConsumoRealRepository consumoRepositoryPMMD;

    private final ParametroService parametrosPMMD;


    public List<Empleado> listar() {
        return repositoryPMMD.findAllByOrderByCodigoAsc();
    }


    public List<Empleado> taller() {
        return listar().stream().filter(e -> e.isTaller() && !"Inactivo".equals(e.getEstado())).toList();
    }


    public Empleado empleado(Long id) {
        return repositoryPMMD.findById(id).orElseThrow(() -> new NegocioExceptionPMMD("El empleado ya no existe."));
    }


    @Transactional
    public Empleado guardar(Empleado e) {
        if (e.getNombre() == null || e.getNombre().isBlank()) throw new NegocioExceptionPMMD("El nombre es obligatorio.");
        if (e.getDni() != null && !e.getDni().isBlank() && !e.getDni().matches("\\d{8}")) throw new NegocioExceptionPMMD("El DNI debe tener 8 dígitos.");
        if (e.getId() == null) e.setCodigo(Codigos.siguiente(repositoryPMMD.findAll(), Empleado::getCodigo, "E-", 2));
        return repositoryPMMD.save(e);
    }

    /**
     * Sofas terminados del mes por persona (OP en Listo de las que fue responsable) y, para el destajo,
     * sofas que pasaron por su etapa este mes (OP en curso que ya superaron su etapa + terminadas).
     */

    public List<Productividad> productividad() {
        LocalDate inicio = AppClockPMMD.hoy().withDayOfMonth(1);
        List<OrdenProduccion> ops = opRepositoryPMMD.findAll().stream().filter(o -> !o.getAnulada()).toList();
        List<Productividad> r = new ArrayList<>();
        for (Empleado e : taller()) {
            int terminados = ops.stream().filter(o -> o.getEtapa() == Etapa.LISTO && o.getFechaFin() != null
                            && !o.getFechaFin().isBefore(inicio) && o.getResponsable() != null && o.getResponsable().getId().equals(e.getId()))
                    .mapToInt(OrdenProduccion::getCantidad).sum();
            int enEtapa = 0;
            if (e.getEtapa() != null) {
                enEtapa = ops.stream().filter(o -> o.getEtapa().ordinal() > e.getEtapa().ordinal()
                                && (o.getEtapa() != Etapa.LISTO || (o.getFechaFin() != null && !o.getFechaFin().isBefore(inicio))))
                        .mapToInt(OrdenProduccion::getCantidad).sum();
                // Historico del mes (OP ya cerradas por consumo) para dar una base realista.
                enEtapa += historicoMesPMMD(e);
            }
            double tarifa = e.getEtapa() == null ? 0 : parametrosPMMD.tarifaDestajo(e.getEtapa());
            r.add(new Productividad(e, terminados + historicoTerminadosPMMD(e), enEtapa, tarifa, tarifa * enEtapa));
        }
        return r;
    }

    /** Ordenes cerradas este mes (registradas en consumo real): pasaron por todas las etapas. */

    private int historicoMesPMMD(Empleado e) {
        LocalDate inicio = AppClockPMMD.hoy().withDayOfMonth(1);
        return (int) consumoRepositoryPMMD.findAll().stream().filter(c -> c.getFecha() != null && !c.getFecha().isBefore(inicio))
                .map(ConsumoReal::getOpCodigo).distinct().count();
    }


    private int historicoTerminadosPMMD(Empleado e) {
        LocalDate inicio = AppClockPMMD.hoy().withDayOfMonth(1);
        return (int) consumoRepositoryPMMD.findAll().stream().filter(c -> c.getFecha() != null && !c.getFecha().isBefore(inicio)
                        && e.getNombre().equals(c.getResponsable())).map(ConsumoReal::getOpCodigo).distinct().count();
    }
}

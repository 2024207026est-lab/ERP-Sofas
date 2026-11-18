package pe.erp.sofas.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.erp.sofas.core.AppClockPMMD;
import pe.erp.sofas.core.NegocioExceptionPMMD;
import pe.erp.sofas.model.*;
import pe.erp.sofas.repository.*;
import pe.erp.sofas.service.calculo.Explosion;
import pe.erp.sofas.service.calculo.Formula;
import pe.erp.sofas.service.calculo.Medidas;
import pe.erp.sofas.service.calculo.Opciones;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

/**
 * Retapizado: Por cotizar → Cotizado → Aprobado → Recojo programado → En taller → Listo → Entregado.
 * Cotizacion: ficha de 1, 2 o 3 cuerpos escalada a las medidas; tela + guata, notex e insumos; espuma ×0 / 0.5 / 1
 * segun su estado; refuerzo de madera ×0 / 0.1 / 0.3 segun la estructura; mano de obra
 * ((tapizado + costura) × 1.2 + acabado) a S/ 9 por hora; precio = costo × 1.06 × 1.45 a la decena.
 */

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RetapizadoService {

    public static final double COSTO_HORA = 9;


    public record LineaCot(
      String concepto, 
        Material material, 
          double cantidad, 
            double costo) {
    }


    public record Cotizacion(
      List<LineaCot> lineas, 
        double metrosTelaPMMD, 
          double horas, 
            double costoMO, 
              double costo,
                             
                             double precio, 
                               String aviso, 
                                 FichaTecnica ficha) {

        public double costoMateriales() {
            return lineas.stream().mapToDouble(LineaCot::costo).sum();
        }
    }


    public record Kpis(
      long activos, 
        long enTaller, 
          long recojos, 
            BigDecimal ingresosMes) {
    }


    private final RetapizadoRepository repositoryPMMD;

    private final ClienteRepository clienteRepositoryPMMD;

    private final TelaRepository telaRepositoryPMMD;

    private final EmpleadoRepository empleadoRepositoryPMMD;

    private final PagoRepository pagoRepositoryPMMD;

    private final FichaService fichaServicePMMD;

    private final ProduccionServicePMMD produccionServicePMMD;


    public List<Retapizado> listar() {
        return repositoryPMMD.findAllByOrderByCodigoDesc();
    }


    public Retapizado retapizado(Long id) {
        return repositoryPMMD.findById(id).orElseThrow(() -> new NegocioExceptionPMMD("El retapizado ya no existe."));
    }


    public Kpis kpis() {
        List<Retapizado> l = listar();
        LocalDate inicio = AppClockPMMD.hoy().withDayOfMonth(1);
        BigDecimal ingresos = l.stream().filter(r -> r.getEstado() == EstadoRetapizado.ENTREGADO && r.getFechaEntrega() != null
                && !r.getFechaEntrega().isBefore(inicio) && r.getTotal() != null).map(Retapizado::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new Kpis(l.stream().filter(r -> r.getEstado() != EstadoRetapizado.ENTREGADO).count(),
                l.stream().filter(r -> r.getEstado() == EstadoRetapizado.EN_TALLER).count(),
                l.stream().filter(r -> r.getEstado() == EstadoRetapizado.RECOJO_PROGRAMADO).count(), ingresos);
    }


    public static String fichaPorCuerpos(int cuerpos) {
        return cuerpos <= 1 ? "FT-003" : cuerpos == 2 ? "FT-002" : "FT-001";
    }


    public Cotizacion cotizar(int cuerpos, double largo, double fondo, double alto, String estructura, String espuma, Tela tela) {
        FichaTecnica f = fichaServicePMMD.aprobada(fichaPorCuerpos(cuerpos));
        if (f == null) throw new NegocioExceptionPMMD("No hay ficha aprobada para cotizar.");
        Map<String, String> sust = new HashMap<>();
        if (tela != null) {
            for (LineaBOM l : f.getLineas()) if (l.getMaterial().isTela()) sust.put(l.getMaterial().getCodigo(), tela.getTipo().materialCodigo());
        }
        Medidas m = new Medidas(largo <= 0 ? f.getLargo() : largo, fondo <= 0 ? f.getFondo() : fondo, alto <= 0 ? f.getAlto() : alto, cuerpos);
        Explosion.Resultado r = fichaServicePMMD.explotar(f, 1, new Opciones(m, sust, List.of(), false, 0, 0));
        double factorEspuma = switch (espuma == null ? "Buena" : espuma) {
            case "Hundida" -> 0.5;
            case "Cambio total" -> 1;
            default -> 0;
        };
        double factorMadera = switch (estructura == null ? "Buena" : estructura) {
            case "Regular" -> 0.1;
            case "Requiere refuerzo" -> 0.3;
            default -> 0;
        };
        List<LineaCot> lineas = new ArrayList<>();
        double metros = 0;
        Map<String, double[]> acc = new LinkedHashMap<>();
        Map<String, Material> mats = new HashMap<>();
        Map<String, String> conceptos = new HashMap<>();
        for (Explosion.Linea l : r.lineas()) {
            Material mat = l.material();
            String cat = mat.getCategoria();
            double factor;
            String concepto;
            if (mat.isTela()) {
                factor = 1;
                concepto = "Tela nueva";
            } else if (cat.equals("Espuma")) {
                factor = factorEspuma;
                concepto = "Espuma (" + (espuma == null ? "Buena" : espuma.toLowerCase(Locale.ROOT)) + ")";
            } else if (cat.equals("Madera") || cat.equals("Tableros")) {
                factor = factorMadera;
                concepto = "Refuerzo de estructura";
            } else if (l.componente() == Componente.TAPIZADO || l.componente() == Componente.COJINES || cat.equals("Insumos")) {
                factor = 1;
                concepto = "Guata, notex e insumos";
            } else {
                continue; // patas, mecanismos, cincha, embalaje: se reutilizan
            }
            if (factor == 0) continue;
            double cant = l.bruta() * factor;
            if (mat.isEntera()) cant = Math.ceil(cant - 1e-9);
            if (mat.isTela()) metros += cant;
            double[] a = acc.computeIfAbsent(mat.getCodigo(), k -> new double[2]);
            a[0] += cant;
            a[1] += cant * mat.getCosto();
            mats.put(mat.getCodigo(), mat);
            conceptos.put(mat.getCodigo(), concepto);
        }
        acc.forEach((codigo, a) -> lineas.add(new LineaCot(conceptos.get(codigo), mats.get(codigo), a[0], a[1])));
        double horasTapizado = horas(f, EtapaMO.TAPIZADO) + horas(f, EtapaMO.COSTURA);
        double horas = (horasTapizado * 1.2 + horas(f, EtapaMO.ACABADO)) * Formula.factorManoObra(m, Medidas.de(f));
        double mo = horas * COSTO_HORA;
        double costo = lineas.stream().mapToDouble(LineaCot::costo).sum() + mo;
        double precio = Formula.redondearDecena(costo * 1.06 * 1.45);
        String aviso = null;
        if (tela != null && tela.getStock() < metros) {
            aviso = "La tela " + tela.getNombre() + " no alcanza: hay " + InventarioService.fmt(tela.getStock()) + " m y se necesitan "
                    + InventarioService.fmt(Formula.r2(metros)) + " m.";
        }
        return new Cotizacion(lineas, metros, horas, mo, costo, precio, aviso, f);
    }


    private static double horas(FichaTecnica f, EtapaMO e) {
        return f.getOperaciones().stream().filter(o -> o.getEtapa() == e).mapToDouble(OperacionMO::getHoras).sum();
    }


    public Cotizacion cotizar(Retapizado r) {
        return cotizar(r.getCuerpos(), nz(r.getLargo()), nz(r.getFondo()), nz(r.getAlto()), r.getEstructura(), r.getEspuma(), r.getTela());
    }


    @Transactional
    public Retapizado crear(Long clienteId, int cuerpos, String descripcion, Integer largo, Integer fondo, Integer alto,
                            String estructura, String espuma, LocalDate recojo, Long telaId, String foto) {
        if (clienteId == null) throw new NegocioExceptionPMMD("Elige el cliente.");
        if (descripcion == null || descripcion.isBlank()) throw new NegocioExceptionPMMD("Describe el sofá del cliente.");
        if (cuerpos < 1 || cuerpos > 4) throw new NegocioExceptionPMMD("Los cuerpos deben estar entre 1 y 4.");
        Tela tela = telaId == null ? null : telaRepositoryPMMD.findById(telaId).orElseThrow();
        Retapizado r = Retapizado.builder()
                .codigo(Codigos.siguiente(repositoryPMMD.findAll(), Retapizado::getCodigo, "RT-", 3))
                .cliente(clienteRepositoryPMMD.findById(clienteId).orElseThrow()).descripcion(descripcion.trim())
                .cuerpos(cuerpos).largo(largo).fondo(fondo).alto(alto).estructura(estructura).espuma(espuma)
                .tela(tela).fechaRegistro(AppClockPMMD.hoy()).fechaRecojo(recojo).foto(foto)
                .estado(tela == null ? EstadoRetapizado.POR_COTIZAR : EstadoRetapizado.COTIZADO).build();
        if (tela != null) {
            Cotizacion c = cotizar(r);
            r.setMetros(Formula.r2(c.metrosTelaPMMD()));
            r.setTotal(BigDecimal.valueOf(c.precio()).setScale(2, RoundingMode.HALF_UP));
        }
        return repositoryPMMD.save(r);
    }

    /** Ejecuta la siguiente accion del flujo. Al pasar a En taller crea la OP "Retapizado · …" en Corte de tela. */

    @Transactional
    public String avanzar(Long id) {
        Retapizado r = retapizado(id);
        EstadoRetapizado sig = r.getEstado().siguiente();
        switch (sig) {
            case COTIZADO -> {
                if (r.getTela() == null) throw new NegocioExceptionPMMD("Elige la tela nueva para cotizar.");
                Cotizacion c = cotizar(r);
                r.setMetros(Formula.r2(c.metrosTelaPMMD()));
                r.setTotal(BigDecimal.valueOf(c.precio()).setScale(2, RoundingMode.HALF_UP));
            }
            case RECOJO_PROGRAMADO -> {
                if (r.getFechaRecojo() == null || r.getFechaRecojo().isBefore(AppClockPMMD.hoy())) r.setFechaRecojo(AppClockPMMD.hoy().plusDays(2));
            }
            case EN_TALLER -> {
                Empleado costurera = empleadoRepositoryPMMD.findAll().stream()
                        .filter(e -> e.getEtapa() == Etapa.CORTE_TELA || e.getEtapa() == Etapa.COSTURA).findFirst().orElse(null);
                OrdenProduccion op = produccionServicePMMD.crear(new ProduccionServicePMMD.NuevaOP(null,
                        "Retapizado · " + r.getDescripcion(), 1, r.getCliente().getId(), costurera == null ? null : costurera.getId(),
                        Prioridad.MEDIA, AppClockPMMD.hoy(), AppClockPMMD.hoy().plusDays(6), r.getTela() == null ? null : r.getTela().getId(),
                        null, Map.of(), List.of(), null, r.getCodigo(), "Retapizado " + r.getMedidas(), Etapa.CORTE_TELA,
                        r.getTotal() == null ? null : r.getTotal().divide(new BigDecimal("1.537"), 2, RoundingMode.HALF_UP), null));
                produccionServicePMMD.autorizar(op.getId(), "Retapizado " + r.getCodigo() + " · no requiere ficha");
                r.setOpCodigo(op.getCodigo());
                r.setFechaEntrega(AppClockPMMD.hoy().plusDays(6));
            }
            case ENTREGADO -> {
                r.setFechaEntrega(AppClockPMMD.hoy());
                if (r.getTotal() != null) {
                    pagoRepositoryPMMD.save(Pago.builder().cliente(r.getCliente()).referencia(r.getCodigo()).fecha(AppClockPMMD.hoy())
                            .monto(r.getTotal()).metodo("Efectivo").concepto("Retapizado").build());
                }
            }
            default -> {
            }
        }
        if (sig == r.getEstado()) throw new NegocioExceptionPMMD("El retapizado ya fue entregado.");
        r.setEstado(sig);
        repositoryPMMD.save(r);
        return r.getCodigo() + " → " + sig.nombre() + (sig == EstadoRetapizado.EN_TALLER ? " · " + r.getOpCodigo() + " creada en Corte de tela" : "");
    }


    @Transactional
    public Retapizado fijarFoto(Long id, String ruta) {
        Retapizado r = retapizado(id);
        r.setFoto(ruta);
        return repositoryPMMD.save(r);
    }


    private static double nz(Integer v) {
        return v == null ? 0 : v;
    }
}

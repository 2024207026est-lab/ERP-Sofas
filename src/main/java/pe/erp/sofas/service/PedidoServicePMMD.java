package pe.erp.sofas.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.erp.sofas.core.AppClockPMMD;
import pe.erp.sofas.core.MoneyPMMD;
import pe.erp.sofas.core.NegocioExceptionPMMD;
import pe.erp.sofas.model.*;
import pe.erp.sofas.repository.*;
import pe.erp.sofas.service.calculo.Formula;
import pe.erp.sofas.service.calculo.Medidas;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Pedidos a medida: Registrado → Cotizado → Confirmado (adelanto ≥ 50 %) → En produccion (con OP)
 * → Listo para entrega → Entregado; o Cancelado.
 */

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PedidoServicePMMD {

    public static final BigDecimal MIN_ADELANTO = new BigDecimal("0.50");


    public record NuevoPedido(
      Long clienteId, 
        Long productoId, 
          Pedido.Tipo tipo, 
            int cantidad, 
              Integer largo,
                              
                              Integer fondo, 
                                Integer alto, 
                                  Long telaId, 
                                    LocalDate fechaEntrega, 
                                      BigDecimal precio,
                              
                              BigDecimal adelanto, 
                                String metodo, 
                                  String observaciones, 
                                    String configuracion,
                              
                              String especificacion) {
    }


    private final PedidoRepository repositoryPMMD;

    private final ClienteRepository clienteRepositoryPMMD;

    private final ProductoRepository productoRepositoryPMMD;

    private final TelaRepository telaRepositoryPMMD;

    private final PagoRepository pagoRepositoryPMMD;

    private final FichaService fichaServicePMMD;

    private final ProduccionServicePMMD produccionServicePMMD;

    private final ObjectProvider<ConfiguradorService> configuradorPMMD;


    public List<Pedido> listar() {
        return repositoryPMMD.findAllByOrderByCodigoDesc();
    }


    public Pedido pedido(Long id) {
        return repositoryPMMD.findById(id).orElseThrow(() -> new NegocioExceptionPMMD("El pedido ya no existe."));
    }


    public Optional<Pedido> porCodigo(String codigo) {
        return repositoryPMMD.findByCodigo(codigo);
    }


    public String siguienteCodigo() {
        return Codigos.siguiente(repositoryPMMD.findAll(), Pedido::getCodigo, "PE-", 4);
    }

    /** Pedidos a medida (personalizados) aun no entregados ni cancelados. */

    public long pendientesAMedida() {
        return listar().stream().filter(p -> p.getTipo() == Pedido.Tipo.PERSONALIZADO && !p.getEstado().cerrado()).count();
    }


    public long abiertos() {
        return listar().stream().filter(p -> !p.getEstado().cerrado()).count();
    }


    public List<Pago> pagos(Pedido p) {
        return pagoRepositoryPMMD.findAllByOrderByFechaDesc().stream().filter(x -> p.getCodigo().equals(x.getReferencia())).toList();
    }

    /** Precio sugerido: precio del modelo escalado por el largo pedido y con el recargo de la tela. */

    public BigDecimal precioSugerido(Producto p, Integer largo, Tela tela, int cantidad) {
        if (p == null) return BigDecimal.ZERO;
        double base = p.getPrecio().doubleValue();
        if (largo != null && largo > 0 && p.getLargo() != null && p.getLargo() > 0 && !largo.equals(p.getLargo())) {
            base = base * Math.pow(largo / (double) p.getLargo(), 0.85);
        }
        if (tela != null) base = base * (1 + tela.getTipo().recargoPct() / 100.0);
        return BigDecimal.valueOf(Formula.redondearDecena(base) * Math.max(1, cantidad)).setScale(2, RoundingMode.HALF_UP);
    }


    @Transactional
    public Pedido crear(NuevoPedido d) {
        if (d.clienteId() == null) throw new NegocioExceptionPMMD("Elige el cliente.");
        if (d.productoId() == null) throw new NegocioExceptionPMMD("Elige el modelo de sofá.");
        if (d.cantidad() <= 0) throw new NegocioExceptionPMMD("La cantidad debe ser mayor a cero.");
        if (d.precio() == null || d.precio().signum() <= 0) throw new NegocioExceptionPMMD("Ingresa el precio del pedido.");
        if (d.fechaEntrega() != null && d.fechaEntrega().isBefore(AppClockPMMD.hoy())) throw new NegocioExceptionPMMD("La entrega no puede ser en el pasado.");
        BigDecimal adelanto = d.adelanto() == null ? BigDecimal.ZERO : d.adelanto();
        if (adelanto.signum() < 0 || adelanto.compareTo(d.precio()) > 0) throw new NegocioExceptionPMMD("El adelanto no puede superar el precio.");
        Cliente c = clienteRepositoryPMMD.findById(d.clienteId()).orElseThrow();
        Pedido p = Pedido.builder().codigo(siguienteCodigo()).cliente(c)
                .producto(productoRepositoryPMMD.findById(d.productoId()).orElseThrow())
                .tipo(d.tipo() == null ? Pedido.Tipo.ESTANDAR : d.tipo()).cantidad(d.cantidad())
                .fecha(AppClockPMMD.hoy()).fechaEntrega(d.fechaEntrega()).precio(d.precio().setScale(2, RoundingMode.HALF_UP))
                .adelanto(BigDecimal.ZERO).estado(EstadoPedido.REGISTRADO)
                .largo(d.largo()).fondo(d.fondo()).alto(d.alto())
                .tela(d.telaId() == null ? null : telaRepositoryPMMD.findById(d.telaId()).orElse(null))
                .observaciones(d.observaciones()).configuracion(d.configuracion()).especificacion(d.especificacion())
                .build();
        if (p.getTela() != null) p.setMaterial(p.getTela().getTipo().nombre());
        p = repositoryPMMD.save(p);
        if (adelanto.signum() > 0) {
            p = registrarAdelanto(p.getId(), adelanto, d.metodo() == null ? "Efectivo" : d.metodo());
        }
        return p;
    }

    /** Registra un pago a cuenta. Si el pedido estaba cotizado y llega al 50 %, queda confirmado. */

    @Transactional
    public Pedido registrarAdelanto(Long id, BigDecimal monto, String metodo) {
        Pedido p = pedido(id);
        if (p.getEstado() == EstadoPedido.CANCELADO) throw new NegocioExceptionPMMD("El pedido está cancelado.");
        if (monto == null || monto.signum() <= 0) throw new NegocioExceptionPMMD("Ingresa un monto válido.");
        if (monto.compareTo(p.getSaldo()) > 0) throw new NegocioExceptionPMMD("El monto supera el saldo de " + MoneyPMMD.fmt(p.getSaldo()) + ".");
        p.setAdelanto(p.getAdelanto().add(monto));
        pagoRepositoryPMMD.save(Pago.builder().cliente(p.getCliente()).referencia(p.getCodigo()).fecha(AppClockPMMD.hoy())
                .monto(monto).metodo(metodo).concepto(p.getAdelanto().compareTo(p.getPrecio()) >= 0 ? "Cancelación" : "Adelanto").build());
        if ((p.getEstado() == EstadoPedido.COTIZADO || p.getEstado() == EstadoPedido.REGISTRADO) && cubreMinimo(p)) {
            p.setEstado(EstadoPedido.CONFIRMADO);
        }
        return repositoryPMMD.save(p);
    }


    public static boolean cubreMinimo(Pedido p) {
        return p.getAdelanto().compareTo(p.getPrecio().multiply(MIN_ADELANTO)) >= 0;
    }

    /** Avanza al siguiente estado del flujo validando cada paso. */

    @Transactional
    public Pedido avanzar(Long id) {
        Pedido p = pedido(id);
        return avanzarA(id, p.getEstado().siguiente());
    }


    @Transactional
    public Pedido avanzarA(Long id, EstadoPedido destino) {
        Pedido p = pedido(id);
        if (p.getEstado().cerrado()) throw new NegocioExceptionPMMD("El pedido ya está " + p.getEstado().nombre().toLowerCase() + ".");
        switch (destino) {
            case CONFIRMADO -> {
                if (!cubreMinimo(p)) {
                    throw new NegocioExceptionPMMD("Para confirmar se necesita un adelanto mínimo del 50 % ("
                            + MoneyPMMD.fmt(p.getPrecio().multiply(MIN_ADELANTO)) + ").");
                }
            }
            case EN_PRODUCCION -> {
                if (p.getOpCodigo() == null) throw new NegocioExceptionPMMD("Genera la orden de producción para pasar a producción.");
            }
            case LISTO -> {
                if (p.getOpCodigo() != null) {
                    var op = produccionServicePMMD.porCodigo(p.getOpCodigo()).orElse(null);
                    if (op != null && op.getEtapa() != Etapa.LISTO) {
                        throw new NegocioExceptionPMMD("La orden " + op.getCodigo() + " aún está en " + op.getEtapa().nombre() + ".");
                    }
                }
            }
            case ENTREGADO -> {
                if (p.getSaldo().signum() > 0) throw new NegocioExceptionPMMD("Registra el saldo de " + MoneyPMMD.fmt(p.getSaldo()) + " antes de entregar.");
            }
            default -> {
            }
        }
        p.setEstado(destino);
        return repositoryPMMD.save(p);
    }


    @Transactional
    public Pedido vincularOP(Long id, String opCodigo) {
        Pedido p = pedido(id);
        p.setOpCodigo(opCodigo);
        if (p.getEstado() == EstadoPedido.CONFIRMADO) p.setEstado(EstadoPedido.EN_PRODUCCION);
        return repositoryPMMD.save(p);
    }

    /** Genera la OP con la ficha aprobada del modelo y pasa el pedido a "En produccion". */

    @Transactional
    public OrdenProduccion generarOP(Long id, Long responsableId) {
        Pedido p = pedido(id);
        if (p.getOpCodigo() != null) throw new NegocioExceptionPMMD("El pedido ya tiene la orden " + p.getOpCodigo() + ".");
        if (p.getEstado().cerrado()) throw new NegocioExceptionPMMD("El pedido está cerrado.");
        if (!cubreMinimo(p)) throw new NegocioExceptionPMMD("Registra un adelanto mínimo del 50 % antes de producir.");
        if (p.getEstado() == EstadoPedido.REGISTRADO || p.getEstado() == EstadoPedido.COTIZADO) p.setEstado(EstadoPedido.CONFIRMADO);
        repositoryPMMD.save(p);
        OrdenProduccion op;
        ConfigSofa cfg = ConfigSofa.leer(p.getConfiguracion());
        if (cfg != null) {
            ConfiguradorService conf = configuradorPMMD.getObject();
            op = produccionServicePMMD.crear(conf.nuevaOP(cfg, conf.cotizar(cfg), p.getCliente().getId(), responsableId,
                    p.getFechaEntrega(), p.getCodigo()));
        } else {
            FichaTecnica f = fichaServicePMMD.aprobadaDe(p.getProducto());
            Medidas medidas = null;
            if (f != null && p.getLargo() != null && p.getLargo() > 0 && !Boolean.TRUE.equals(f.getCompuesta())
                    && !p.getLargo().equals(f.getLargo())) {
                medidas = new Medidas(p.getLargo(), p.getFondo() == null ? f.getFondo() : p.getFondo(),
                        p.getAlto() == null ? f.getAlto() : p.getAlto(), f.getCuerpos());
            }
            op = produccionServicePMMD.crear(new ProduccionServicePMMD.NuevaOP(p.getProducto().getId(), null, p.getCantidad(),
                    p.getCliente().getId(), responsableId, Prioridad.MEDIA, AppClockPMMD.hoy(), p.getFechaEntrega(),
                    p.getTela() == null ? null : p.getTela().getId(), medidas, Map.of(), List.of(), p.getCodigo(), null,
                    p.getEspecificacion(), Etapa.PENDIENTE, null, null));
        }
        vincularOP(id, op.getCodigo());
        return op;
    }


    @Transactional
    public Pedido cancelar(Long id) {
        Pedido p = pedido(id);
        if (p.getEstado() == EstadoPedido.ENTREGADO) throw new NegocioExceptionPMMD("No se puede cancelar un pedido entregado.");
        if (p.getOpCodigo() != null) {
            produccionServicePMMD.porCodigo(p.getOpCodigo()).filter(OrdenProduccion::isActiva)
                    .ifPresent(op -> produccionServicePMMD.anular(op.getId()));
            p = pedido(id);
        }
        p.setEstado(EstadoPedido.CANCELADO);
        return repositoryPMMD.save(p);
    }

    /** Mensaje de WhatsApp (simulado). */

    public String mensajeWhatsapp(Pedido p) {
        return "Hola " + p.getCliente().getNombre().split(" ")[0] + ", te saluda SISTEMA ERP SOFAS Sofás. "
                + "Tu pedido " + p.getCodigo() + " (" + p.getProductoNombre() + (p.getTela() == null ? "" : " en " + p.getTela().getNombre())
                + ") está en estado: " + p.getEstado().nombre() + ". Precio " + MoneyPMMD.fmt(p.getPrecio()) + ", a cuenta "
                + MoneyPMMD.fmt(p.getAdelanto()) + ", saldo " + MoneyPMMD.fmt(p.getSaldo())
                + (p.getFechaEntrega() == null ? "" : ". Entrega estimada: " + AppClockPMMD.fmt(p.getFechaEntrega())) + ". ¡Gracias!";
    }
}

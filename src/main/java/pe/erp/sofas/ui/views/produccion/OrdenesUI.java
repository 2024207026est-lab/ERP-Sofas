package pe.erp.sofas.ui.views.produccion;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import pe.erp.sofas.core.*;
import pe.erp.sofas.model.*;
import pe.erp.sofas.service.*;
import pe.erp.sofas.service.calculo.Explosion;
import pe.erp.sofas.ui.AppWindow;
import pe.erp.sofas.ui.components.*;

import java.io.File;
import java.util.*;

/**
 * Acciones de las ordenes de produccion compartidas por el tablero y otros modulos: detalle con stepper y
 * materiales, mover de etapa con sus reglas (autorizacion, faltantes, consumo) y nueva orden.
 */

@Component
@Lazy
@RequiredArgsConstructor
public class OrdenesUI {

    private final ProduccionServicePMMD produccionPMMD;

    private final ProductoService productosPMMD;

    private final FichaService fichasPMMD;

    private final ClienteService clientesPMMD;

    private final EmpleadoService empleadosPMMD;

    private final TelaService telasPMMD;

    private final MrpServicePMMD mrpPMMD;

    private final AuthServicePMMD authPMMD;

    private final ExportService exportPMMD;

    private final Session sessionPMMD;

    private final Overlays overlaysPMMD;

    private final Toast toastPMMD;

    private final ConfirmDialog confirmPMMD;

    private final Navigator navigatorPMMD;

    private final AppWindow appWindowPMMD;


    private Overlays.Handle panelPMMD;

    // ================================================================ mover

    /** Mueve la OP a la etapa aplicando las reglas; {@code despues} refresca la vista. */

    public void mover(OrdenProduccion op, Etapa destino, Runnable despues) {
        if (op.getEtapa() == destino) return;
        ProduccionServicePMMD.Evaluacion ev;
        try {
            ev = produccionPMMD.evaluar(op, destino);
        } catch (NegocioExceptionPMMD e) {
            toastPMMD.error(e.getMessage());
            return;
        }
        Runnable fin = () -> {
            navigatorPMMD.datosCambiaron();
            despues.run();
        };
        switch (ev.requisito()) {
            case NINGUNO -> ejecutar(() -> produccionPMMD.mover(op.getId(), destino, null), fin);
            case LIBERAR -> confirmPMMD.mostrar(ConfirmDialog.Tipo.AVISO, "¿Volver " + op.getCodigo() + " a Pendiente?",
                    "Se liberarán las reservas de material de la orden para que las usen otras órdenes.", "Volver y liberar",
                    () -> ejecutar(() -> produccionPMMD.mover(op.getId(), destino, null), fin));
            case AUTORIZACION -> autorizar(op, destino, fin);
            case CONFIRMAR_FALTANTES -> {
                StringBuilder sb = new StringBuilder("Al reservar faltarán:\n");
                ev.faltantes().stream().limit(6).forEach(f -> sb.append("• ").append(f.material().getNombre()).append(": ")
                        .append(MoneyPMMD.numero(f.falta())).append(" ").append(f.material().getUnidad()).append("\n"));
                if (ev.faltantes().size() > 6) sb.append("… y ").append(ev.faltantes().size() - 6).append(" más.\n");
                sb.append("Se reservará lo disponible y la orden quedará con reserva parcial.");
                confirmPMMD.mostrar(ConfirmDialog.Tipo.AVISO, "Faltan " + ev.faltantes().size() + " materiales para " + op.getCodigo(), sb.toString(),
                        "Avanzar de todos modos", () -> ejecutar(() -> produccionPMMD.mover(op.getId(), destino, null), fin));
            }
            case CONFIRMAR_CONSUMO -> confirmarConsumo(op, fin);
        }
    }


    private void ejecutar(java.util.function.Supplier<String> accion, Runnable fin) {
        String[] msg = new String[1];
        if (toastPMMD.intentar(() -> msg[0] = accion.get())) {
            if (msg[0] != null) toastPMMD.exito(msg[0]);
            fin.run();
        }
    }


    private void autorizar(OrdenProduccion op, Etapa destino, Runnable fin) {
        Validacion v = new Validacion();
        List<Usuario> sup = authPMMD.autorizadores();
        Usuario propio = sup.stream().filter(u -> u.getUsername().equals(sessionPMMD.username())).findFirst().orElse(null);
        ComboBox<Usuario> quien = Forms.combo(sup, propio != null ? propio : sup.stream().filter(u -> u.getRol() == Rol.SUPERVISOR).findFirst().orElse(sup.get(0)),
                u -> u.getNombre() + " · " + u.getRol().nombre());
        PasswordField clave = new PasswordField();
        clave.setPromptText("Contraseña del supervisor");
        TextArea motivo = Forms.area("", "Motivo (p. ej. ficha en revisión, pedido urgente)", 2);
        VBox cuerpo = new VBox(12, UI.banner("lock-banner", "WARNING", op.getCodigo() + " no tiene ficha técnica aprobada: avanzará sin reservar material."),
                v.campo("Autoriza", quien, () -> Validacion.requerido(quien, "quién autoriza")),
                v.campo("Contraseña", clave, () -> Validacion.requerido(clave, "la contraseña")),
                v.campo("Motivo", motivo, () -> Validacion.requerido(motivo, "el motivo")));
        Button cancelar = UI.secondary("Cancelar", null);
        Button ok = UI.primary("Autorizar y avanzar", "VERIFIED_USER");
        v.enlazar(ok);
        var h = overlaysPMMD.modal("Autorización de Supervisor", op.getCodigo() + " → " + destino.nombre(), cuerpo, 480, cancelar, ok);
        cancelar.setOnAction(e -> h.cerrar());
        ok.setOnAction(e -> {
            if (toastPMMD.intentar(() -> {
                Usuario u = authPMMD.autorizar(quien.getValue().getUsername(), clave.getText());
                String texto = "Autorizado por " + u.getNombre() + " (" + u.getRol().nombre() + ") el " + AppClockPMMD.fmt(AppClockPMMD.hoy()) + ": " + motivo.getText().trim();
                toastPMMD.exito(produccionPMMD.mover(op.getId(), destino, texto));
            })) {
                h.cerrar();
                fin.run();
            }
        });
    }


    private void confirmarConsumo(OrdenProduccion op, Runnable fin) {
        ProduccionServicePMMD.EstadoMateriales em = produccionPMMD.estadoMateriales(op);
        if (em.explosion() == null) {
            confirmPMMD.mostrar(ConfirmDialog.Tipo.INFO, "¿Pasar " + op.getCodigo() + " a Listo?", "La orden no tiene ficha: no hay consumo de materiales que registrar.",
                    "Marcar como lista", () -> ejecutar(() -> produccionPMMD.completar(op.getId(), Map.of()), fin));
            return;
        }
        GridPane g = new GridPane();
        g.setHgap(10);
        g.setVgap(6);
        g.addRow(0, UI.label("MATERIAL", "dato-label"), UI.label("ESTIMADO", "dato-label"), UI.label("RESERVADO", "dato-label"), UI.label("REAL", "dato-label"));
        Map<Long, TextField> reales = new LinkedHashMap<>();
        int fila = 1;
        for (var l : em.lineas()) {
            TextField real = Forms.decimal(round(l.reservado()));
            real.setPrefWidth(90);
            real.getStyleClass().add("cell-field");
            reales.put(l.material().getId(), real);
            g.addRow(fila++, new VBox(0, UI.label(l.material().getNombre(), "cell-title"), UI.label(l.material().getCodigo() + " · " + l.material().getUnidad(), "cell-sub")),
                    UI.label(MoneyPMMD.numero(l.necesario()), "cell-sub"), UI.label(MoneyPMMD.numero(l.reservado()), "cell-sub"), real);
        }
        VBox cuerpo = new VBox(12, UI.wrap("Se descuenta del stock el consumo real (por defecto, lo reservado), se registra «Consumo OP» en el kardex "
                + "y se guarda el consumo real frente al estimado para Consumo y mermas.", "muted"), g);
        Button cancelar = UI.secondary("Cancelar", null);
        Button ok = UI.success("Confirmar consumo y pasar a Listo", "DONE_ALL");
        var h = overlaysPMMD.modal("Confirmar consumo de " + op.getCodigo(), op.getProductoNombre() + " ×" + op.getCantidad(), cuerpo, 620, cancelar, ok);
        cancelar.setOnAction(e -> h.cerrar());
        ok.setOnAction(e -> {
            Map<Long, Double> m = new HashMap<>();
            reales.forEach((id, tf) -> m.put(id, Validacion.valor(tf)));
            h.cerrar();
            ejecutar(() -> produccionPMMD.completar(op.getId(), m), fin);
        });
    }


    private static double round(double v) {
        return Math.round(v * 100) / 100.0;
    }

    // ================================================================ detalle


    public void detalle(OrdenProduccion o, Runnable alCambiar) {
        if (panelPMMD != null) panelPMMD.cerrar();
        OrdenProduccion op = produccionPMMD.op(o.getId());
        Runnable refrescar = () -> {
            alCambiar.run();
            detalle(op, alCambiar);
        };
        VBox cuerpo = new VBox(16);
        Stepper stepper = new Stepper(op.getEtapa(), e -> mover(op, e, refrescar));
        Button ant = UI.secondary("Etapa anterior", "CHEVRON_LEFT");
        ant.setDisable(op.getEtapa() == Etapa.PENDIENTE || op.getEtapa() == Etapa.LISTO || op.getAnulada());
        ant.setOnAction(e -> mover(op, op.getEtapa().anterior(), refrescar));
        Button sig = UI.primary("Siguiente etapa", "CHEVRON_RIGHT");
        sig.setContentDisplay(ContentDisplay.RIGHT);
        sig.setDisable(op.getEtapa() == Etapa.LISTO || op.getAnulada());
        sig.setOnAction(e -> mover(op, op.getEtapa().siguiente(), refrescar));
        HBox nav = UI.hbox(8, new StatusBadge(op.getEstado()), UI.label("Etapa " + op.getEtapa().paso() + " de 10 · " + op.getEtapa().nombre(), "cell-title"),
                UI.spacer(), ant, sig);
        cuerpo.getChildren().addAll(stepper, nav);

        GridPane datos = UI.grid(3);
        datos.add(UI.dato("Producto", op.getProductoNombre() + " ×" + op.getCantidad()), 0, 0, 2, 1);
        HBox tela = new HBox(8);
        tela.setAlignment(Pos.CENTER_LEFT);
        if (op.getTela() != null) tela.getChildren().add(new ColorSwatch(op.getTela().getHex(), op.getTela().getTipo(), 20));
        tela.getChildren().add(UI.label(op.getTelaNombre(), "dato-valor"));
        datos.add(UI.dato("Tela y color", tela), 2, 0);
        datos.add(UI.dato("Cliente", op.getClienteNombre()), 0, 1);
        datos.add(UI.dato("Responsable", op.getResponsable() == null ? "—" : op.getResponsable().getNombre()), 1, 1);
        datos.add(UI.dato("Prioridad", new StatusBadge(op.getPrioridad().nombre())), 2, 1);
        datos.add(UI.dato("Inicio", AppClockPMMD.fmt(op.getFechaInicio())), 0, 2);
        Label entrega = UI.label(AppClockPMMD.fmt(op.getFechaEntrega()) + (op.isRetrasada() ? " · vencida" : ""), "dato-valor");
        if (op.isRetrasada()) entrega.setStyle("-fx-text-fill: -is-danger-ink;");
        datos.add(UI.dato("Entrega", entrega), 1, 2);
        datos.add(UI.dato("Costo", MoneyPMMD.fmt(op.getCosto())), 2, 2);
        Node pedido;
        if (op.getPedidoCodigo() != null) {
            Hyperlink l = new Hyperlink(op.getPedidoCodigo());
            l.setOnAction(e -> navigatorPMMD.goTo("pedidos", Intent.abrir(op.getPedidoCodigo())));
            pedido = l;
        } else if (op.getRetapizadoCodigo() != null) {
            Hyperlink l = new Hyperlink(op.getRetapizadoCodigo());
            l.setOnAction(e -> navigatorPMMD.goTo("posventa", Intent.abrir(op.getRetapizadoCodigo())));
            pedido = l;
        } else pedido = UI.label("Stock de tienda", "dato-valor");
        datos.add(UI.dato("Pedido vinculado", pedido), 0, 3);
        if (op.getLargo() != null) datos.add(UI.dato("Medidas a pedido", op.getLargo() + " × " + op.getFondo() + " × " + op.getAlto() + " cm"), 1, 3, 2, 1);
        cuerpo.getChildren().addAll(UI.label("Datos de la orden", "card-title"), datos);
        if (op.getEspecificacion() != null) cuerpo.getChildren().add(UI.wrap(op.getEspecificacion(), "muted"));
        if (op.getAutorizacion() != null) cuerpo.getChildren().add(UI.banner("lock-banner", "VERIFIED_USER", op.getAutorizacion()));
        cuerpo.getChildren().add(materiales(op, refrescar));

        Button hoja = UI.secondary("Hoja de trabajo (PDF)", "PICTURE_AS_PDF");
        hoja.setOnAction(e -> hojaTrabajo(op));
        Button anular = UI.button("Anular orden", "BLOCK", "btn-danger-soft");
        anular.setDisable(op.getEtapa() == Etapa.LISTO || op.getAnulada());
        anular.setOnAction(e -> confirmPMMD.mostrar("¿Anular " + op.getCodigo() + "?",
                "La orden se cancela y se liberan sus reservas de material." + (op.getPedidoCodigo() != null ? " El pedido " + op.getPedidoCodigo() + " vuelve a Confirmado." : ""),
                "Anular orden", () -> {
                    String[] m = new String[1];
                    if (toastPMMD.intentar(() -> m[0] = produccionPMMD.anular(op.getId()))) {
                        toastPMMD.exito(m[0]);
                        panelPMMD.cerrar();
                        navigatorPMMD.datosCambiaron();
                        alCambiar.run();
                    }
                }));
        HBox pie = UI.hbox(8, anular, UI.spacer(), hoja);
        panelPMMD = overlaysPMMD.panel(op.getCodigo() + " · " + op.getProductoNombre(), op.getClienteNombre() + " · " + op.getEtapa().nombre(), cuerpo, pie, 680);
    }


    private Node materiales(OrdenProduccion op, Runnable refrescar) {
        VBox box = new VBox(10);
        box.getChildren().add(UI.label("Materiales · explosión de la ficha", "card-title"));
        if (op.isSinFicha()) {
            box.getChildren().add(UI.banner("lock-banner", "WARNING", op.getAutorizacion() != null
                    ? "Sin ficha · autorizado: la orden avanza sin reservar material."
                    : "Sin ficha aprobada: para pasar a producción se necesita autorización de Supervisor."));
            return box;
        }
        ProduccionServicePMMD.EstadoMateriales em = produccionPMMD.estadoMateriales(op);
        Explosion.Resultado r = em.explosion();
        long falt = em.faltantes();
        String estadoReserva = op.getEtapa() == Etapa.LISTO ? "Consumido" : em.completo() ? "Reservado"
                : em.lineas().stream().anyMatch(l -> l.reservado() > 0) ? "Parcial" : op.getEtapa() == Etapa.PENDIENTE ? "Pendiente" : "Faltante";
        box.getChildren().add(UI.hbox(10, UI.miniStat("Ficha", op.getFichaUid().replace("v", " v")), UI.miniStat("Materiales", String.valueOf(em.lineas().size())),
                UI.miniStat("Costo de materiales", MoneyPMMD.fmt(r.costoMateriales())), UI.miniStat("Reserva", estadoReserva + (falt > 0 ? " · " + falt + " faltan" : ""))));

        GridPane g = new GridPane();
        g.setHgap(10);
        g.setVgap(2);
        ColumnConstraints c0 = new ColumnConstraints();
        c0.setHgrow(Priority.ALWAYS);
        g.getColumnConstraints().addAll(c0, new ColumnConstraints(82), new ColumnConstraints(82), new ColumnConstraints(100), new ColumnConstraints(90));
        g.addRow(0, UI.label("MATERIAL", "dato-label"), UI.label("NECESARIO", "dato-label"), UI.label("RESERVADO", "dato-label"),
                UI.label("DISPONIBILIDAD", "dato-label"), UI.label("ESTADO", "dato-label"));
        int i = 1;
        for (var l : em.lineas()) {
            double cubre = l.necesario() <= 0 ? 1 : Math.min(1, Math.max(l.reservado(), "Disponible".equals(l.estado()) ? l.necesario() : Math.min(l.disponible(), l.necesario())) / l.necesario());
            String color = switch (l.estado()) {
                case "Reservado", "Disponible", "Consumido" -> "#16A34A";
                case "Parcial", "Faltará" -> "#F59E0B";
                default -> "#DC2626";
            };
            ProgressBarMini barra = new ProgressBarMini(cubre, color, 6);
            barra.setPrefWidth(90);
            Tooltip.install(barra, new Tooltip("Disponible para la OP: " + MoneyPMMD.numero(l.disponible()) + " " + l.material().getUnidad()));
            g.addRow(i++, new VBox(0, UI.label(l.material().getNombre(), "cell-title"), UI.label(l.material().getCodigo() + " · " + l.componentes(), "cell-sub")),
                    UI.label(MoneyPMMD.numero(l.necesario()) + " " + l.material().getUnidad(), "cell-sub"),
                    UI.label(MoneyPMMD.numero(l.reservado()), "cell-sub"), barra, new StatusBadge(l.estado()));
        }
        box.getChildren().add(g);

        Button reservar = UI.button("Reservar ahora", "LOCK", "btn-sm", "btn-secondary");
        reservar.setDisable(op.getEtapa() == Etapa.LISTO || em.completo());
        reservar.setOnAction(e -> {
            String[] m = new String[1];
            if (toastPMMD.intentar(() -> m[0] = op.getCodigo() + ": " + produccionPMMD.reservar(op.getId()).texto())) {
                toastPMMD.exito(m[0]);
                navigatorPMMD.datosCambiaron();
                refrescar.run();
            }
        });
        Button liberar = UI.button("Liberar reserva", "LOCK_OPEN", "btn-sm", "btn-secondary");
        liberar.setDisable(em.lineas().stream().noneMatch(l -> l.reservado() > 0) || op.getEtapa() == Etapa.LISTO);
        liberar.setOnAction(e -> confirmPMMD.mostrar(ConfirmDialog.Tipo.AVISO, "¿Liberar la reserva de " + op.getCodigo() + "?",
                "El material vuelve a estar disponible para otras órdenes.", "Liberar", () -> {
                    int[] n = new int[1];
                    if (toastPMMD.intentar(() -> n[0] = produccionPMMD.liberar(op.getId()))) {
                        toastPMMD.exito(n[0] + " reservas liberadas de " + op.getCodigo() + ".");
                        navigatorPMMD.datosCambiaron();
                        refrescar.run();
                    }
                }));
        Button oc = UI.button("Generar OC de faltantes", "SHOPPING_CART", "btn-sm", "btn-wood");
        oc.setDisable(falt == 0 || op.getEtapa() == Etapa.LISTO);
        oc.setOnAction(e -> generarOC(op));
        Button corte = UI.button("Hoja de corte", "CONTENT_CUT", "btn-sm", "btn-ghost");
        corte.setOnAction(e -> navigatorPMMD.goTo("corte", Intent.abrir(op.getCodigo())));
        box.getChildren().add(UI.hbox(8, reservar, liberar, oc, UI.spacer(), corte));
        return box;
    }


    public void generarOC(OrdenProduccion op) {
        List<MrpServicePMMD.Fila> filas = new ArrayList<>();
        Map<Long, Double> transito = mrpPMMD.transito();
        for (var f : produccionPMMD.faltantesSiReserva(op)) {
            double tr = transito.getOrDefault(f.material().getId(), 0d);
            double falta = Math.max(0, f.falta() - tr);
            if (falta > 0) filas.add(new MrpServicePMMD.Fila(f.material(), f.necesario(), tr, falta, AppClockPMMD.hoy(), f.material().getProveedor(), Set.of(op.getCodigo())));
        }
        if (filas.isEmpty()) {
            toastPMMD.info("Los faltantes de " + op.getCodigo() + " ya están cubiertos por compras en tránsito.");
            return;
        }
        List<String>[] cod = new List[1];
        if (toastPMMD.intentar(() -> cod[0] = mrpPMMD.generarOrdenes(filas, op.getCodigo()))) {
            toastPMMD.exito("Órdenes de compra en borrador: " + String.join(", ", cod[0]) + ".");
            navigatorPMMD.datosCambiaron();
        }
    }


    public void hojaTrabajo(OrdenProduccion op) {
        File f = Archivos.guardar(null, "Hoja-de-trabajo-" + op.getCodigo(), "PDF", "pdf");
        if (f == null) return;
        Tareas.ejecutar(overlaysPMMD, toastPMMD, "Generando hoja de trabajo…", () -> {
            exportPMMD.hojaTrabajo(op, produccionPMMD.estadoMateriales(op), f);
            return f;
        }, archivo -> {
            toastPMMD.exito("Hoja de trabajo guardada: " + archivo.getName());
            appWindowPMMD.hostServices().showDocument(archivo.toURI().toString());
        });
    }

    // ================================================================ nueva orden


    public void nueva(Runnable alCrear) {
        Validacion v = new Validacion();
        ComboBox<Producto> producto = Forms.combo(productosPMMD.listar(), null, p -> p.getCodigo() + " · " + p.getNombre());
        Spinner<Integer> cantidad = new Spinner<>(1, 50, 1);
        cantidad.setEditable(true);
        cantidad.setMaxWidth(Double.MAX_VALUE);
        List<Cliente> cs = new ArrayList<>();
        cs.add(null);
        cs.addAll(clientesPMMD.listar());
        ComboBox<Cliente> cliente = Forms.combo(cs, null, c -> c == null ? "Stock de tienda (sin cliente)" : c.getNombre());
        ComboBox<Empleado> responsable = Forms.combo(empleadosPMMD.taller(), null, e -> e.getNombre() + " · " + e.getCargo());
        ComboBox<Prioridad> prioridad = Forms.combo(List.of(Prioridad.values()), Prioridad.MEDIA, Prioridad::nombre);
        DatePicker inicio = Forms.fecha(AppClockPMMD.hoy());
        DatePicker entrega = Forms.fecha(AppClockPMMD.hoy().plusDays(15));
        List<Tela> ts = new ArrayList<>();
        ts.add(null);
        ts.addAll(telasPMMD.activas());
        ComboBox<Tela> tela = Forms.combo(ts, null, t -> t == null ? "Según ficha" : t.getNombre() + " · " + MoneyPMMD.numero(t.getStock()) + " m");
        Label costo = UI.label("Elige un modelo para ver el costo.", "muted");
        Runnable calcular = () -> {
            Producto p = producto.getValue();
            if (p == null) return;
            FichaTecnica f = fichasPMMD.aprobadaDe(p);
            if (f == null) {
                costo.setText("Costo estimado (sin ficha aprobada): " + MoneyPMMD.fmt(p.getCosto().multiply(java.math.BigDecimal.valueOf(cantidad.getValue())))
                        + " · necesitará autorización para producir.");
                costo.getStyleClass().setAll("label", "text-warn");
            } else {
                double c = fichasPMMD.explotar(f, cantidad.getValue(), pe.erp.sofas.service.calculo.Opciones.NINGUNA).costoEstandar();
                costo.setText("Costo estándar · ficha " + f.getEtiqueta() + ": " + MoneyPMMD.fmt(c));
                costo.getStyleClass().setAll("label", "text-primary", "semibold");
            }
        };
        producto.valueProperty().addListener((o, a, b) -> calcular.run());
        cantidad.valueProperty().addListener((o, a, b) -> calcular.run());
        FormGrid f = new FormGrid(2);
        f.nodo(v.campo("Modelo", producto, () -> Validacion.requerido(producto, "el modelo")), 2);
        f.nodo(v.campo("Cantidad", cantidad, null), 1);
        f.nodo(v.campo("Tela y color", tela, null), 1);
        f.nodo(v.campo("Cliente (opcional)", cliente, null), 1);
        f.nodo(v.campo("Responsable", responsable, () -> Validacion.requerido(responsable, "el responsable")), 1);
        f.nodo(v.campo("Prioridad", prioridad, null), 1);
        f.nodo(new Region(), 1);
        f.nodo(v.campo("Inicio", inicio, () -> inicio.getValue() == null ? "Elige la fecha de inicio." : null), 1);
        f.nodo(v.campo("Entrega", entrega, () -> entrega.getValue() == null ? "Elige la fecha de entrega."
                : inicio.getValue() != null && entrega.getValue().isBefore(inicio.getValue()) ? "La entrega debe ser después del inicio." : null), 1);
        v.escuchar(inicio.valueProperty());
        VBox cuerpo = new VBox(14, f, costo);
        Button cancelar = UI.secondary("Cancelar", null);
        Button ok = UI.primary("Crear orden", "ADD");
        v.enlazar(ok);
        var h = overlaysPMMD.modal("Nueva orden de producción", "Se crea en Pendiente; al pasar a producción se reservan los materiales.", cuerpo, 600, cancelar, ok);
        cancelar.setOnAction(e -> h.cerrar());
        ok.setOnAction(e -> {
            OrdenProduccion[] creada = new OrdenProduccion[1];
            if (toastPMMD.intentar(() -> creada[0] = produccionPMMD.crear(new ProduccionServicePMMD.NuevaOP(producto.getValue().getId(), null, cantidad.getValue(),
                    cliente.getValue() == null ? null : cliente.getValue().getId(), responsable.getValue().getId(), prioridad.getValue(),
                    inicio.getValue(), entrega.getValue(), tela.getValue() == null ? null : tela.getValue().getId(), null, Map.of(), List.of(),
                    null, null, null, Etapa.PENDIENTE, null, null)))) {
                h.cerrar();
                toastPMMD.exito(creada[0].getCodigo() + " creada en Pendiente" + (creada[0].isSinFicha() ? " (sin ficha aprobada)." : " con la ficha " + creada[0].getFichaUid().replace("v", " v") + "."));
                navigatorPMMD.datosCambiaron();
                alCrear.run();
            }
        });
    }
}

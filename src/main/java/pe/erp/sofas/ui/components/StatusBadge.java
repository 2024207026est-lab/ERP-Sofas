package pe.erp.sofas.ui.components;

import javafx.scene.control.Label;

import java.util.HashMap;
import java.util.Map;

/** Pildora de estado: fondo suave + texto del color del estado. */

public class StatusBadge extends Label {

    private static final Map<String, String> VARIANTESPMMD = new HashMap<>();

    static {
        for (String s : new String[]{"Disponible", "Normal", "Aprobada", "Entregado", "Pagado", "Resuelto", "En garantía", "Listo",
                "Reservado", "Suficiente", "Recibida", "Activo", "Consumido", "Listo para entrega", "Cubierto", "Materiales reservados",
                "Materiales consumidos"}) VARIANTESPMMD.put(s, "green");
        for (String s : new String[]{"Stock bajo", "Borrador", "Pendiente", "Por vencer", "Abierto", "Parcial", "Faltará", "Por cotizar",
                "Media", "Vacaciones", "Sin ficha aprobada", "Sin ficha · autorizado", "Registrado", "Nueva", "En reparación"}) VARIANTESPMMD.put(s, "amber");
        for (String s : new String[]{"Agotado", "Crítico", "Retrasado", "Cancelado", "Rechazado", "Fuera de garantía", "Faltante", "Sin stock",
                "Alta", "Cancelada", "Vencido", "SLA vencido"}) VARIANTESPMMD.put(s, "red");
        for (String s : new String[]{"En producción", "En diagnóstico", "Aprobado", "Confirmado", "Solicitada", "Cotizado", "En taller",
                "Factura", "Aprobada OC", "Materiales disponibles"}) VARIANTESPMMD.put(s, "blue");
        for (String s : new String[]{"Personalizado", "Exhibición", "Recojo programado", "A pedido", "Bajo pedido", "Compuesta", "Retazo",
                "MRP", "Calculadora"}) VARIANTESPMMD.put(s, "wood");
        for (String s : new String[]{"Obsoleta", "Descontinuada", "Inactivo", "Estándar", "Boleta", "Baja", "Stock de tienda"}) VARIANTESPMMD.put(s, "gray");
    }


    public StatusBadge(String estado) {
        this(estado, variante(estado));
    }


    public StatusBadge(String texto, String variante) {
        super(texto);
        getStyleClass().addAll("badge", "badge-" + variante);
        setMinWidth(USE_PREF_SIZE);
    }


    public static String variante(String estado) {
        if (estado == null) return "gray";
        if (estado.startsWith("Faltan") || estado.startsWith("Faltarán")) return "red";
        if (estado.startsWith("En garantía")) return "green";
        if (estado.startsWith("OP-")) return "wood";
        return VARIANTESPMMD.getOrDefault(estado, "gray");
    }


    public StatusBadge conIcono(String icono) {
        setGraphic(Ico.of(icono, 13));
        return this;
    }
}

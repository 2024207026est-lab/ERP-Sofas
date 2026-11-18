package pe.erp.sofas.ui.views.sala;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import pe.erp.sofas.core.*;
import pe.erp.sofas.model.*;
import pe.erp.sofas.service.*;
import pe.erp.sofas.ui.AppWindow;
import pe.erp.sofas.ui.components.*;
import pe.erp.sofas.ui.three.RoomBuilder;
import pe.erp.sofas.ui.three.SofaBuilder;
import pe.erp.sofas.ui.three.Viewer3D;

import javax.imageio.ImageIO;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.*;

/**
 * "Ver en mi sala": planta 2D (escala en cm, cuadricula de 50 cm) con muebles que se arrastran, rotan 90° y
 * eliminan; cotas al seleccionar y validaciones (fuera de la sala, choque, barrido de la puerta); vista 3D con
 * los mismos sofas del SofaBuilder; resumen con ocupacion, espacio libre y total.
 */

@Component
@Lazy
@Scope("prototype")
@RequiredArgsConstructor
public class SalaController implements Vista.ConCiclo {

    private enum Tipo { 
        SOFA, 
         ALFOMBRA, 
          PLANTA }

    /** Mueble en planta: centro (x, y) en cm desde la esquina superior izquierda (pared del fondo arriba). */

    private static final class Mueble {
        
        final String nombre;
        
        final Tipo tipo;
        
        final Producto producto;
        
        final ConfigSofa config;
        
        final double ancho, fondo, precio;
        
        double x, y;
        
        int rot;

        
        Mueble(String nombre, Tipo tipo, Producto producto, ConfigSofa config, double ancho, double fondo, double precio, double x, double y, int rot) {
            this.nombre = nombre;
            this.tipo = tipo;
            this.producto = producto;
            this.config = config;
            this.ancho = ancho;
            this.fondo = fondo;
            this.precio = precio;
            this.x = x;
            this.y = y;
            this.rot = rot;
        }

        
        double w() {
            return rot % 180 == 0 ? ancho : fondo;
        }

        
        double h() {
            return rot % 180 == 0 ? fondo : ancho;
        }

        
        double x0() {
            return x - w() / 2;
        }

        
        double y0() {
            return y - h() / 2;
        }
    }


    private final ProductoService productosPMMD;

    private final ConfiguradorService configuradorPMMD;

    private final TelaService telasPMMD;

    private final ClienteService clientesPMMD;

    private final ExportService exportPMMD;

    private final Overlays overlaysPMMD;

    private final Toast toastPMMD;

    private final Navigator navigatorPMMD;

    private final AppWindow appWindowPMMD;

    private final ThemeManager temaPMMD;


    @FXML private VBox raiz;


    private final List<Mueble> mueblesPMMD = new ArrayList<>();

    private Mueble seleccionPMMD;

    private double anchoSalaPMMD = 420, largoSalaPMMD = 360, altoSalaPMMD = 250;

    private int paredPuertaPMMD = 0;

    private double posPuertaPMMD = 40, anchoPuertaPMMD = 90;

    private int paredVentanaPMMD = 2;

    private double posVentanaPMMD = 130, anchoVentanaPMMD = 160;

    private final Canvas canvasPMMD = new Canvas(700, 520);

    private final StackPane areaPlanoPMMD = new StackPane();

    private final Viewer3D visor3dPMMD = new Viewer3D(false);

    private SegmentedControl vistaPMMD;

    private final VBox resumenPMMD = new VBox(10);

    private final VBox propiedadesPMMD = new VBox(10);

    private double escalaPMMD = 1, oxPMMD = 0, oyPMMD = 0;

    private double dragDxPMMD, dragDyPMMD;

    private boolean iniciadoPMMD;

    private String proyectoPMMD = "Sala · Rosa Quispe · sofá + sillón";


    @FXML
    public void initialize() {
        vistaPMMD = SegmentedControl.conIconos(new String[]{"Planta 2D", "Vista 3D"}, new String[]{"GRID_ON", "MDOAL_3D_ROTATION"});
        vistaPMMD.valorProperty().addListener((o, a, b) -> mostrarVista());
        Button rotar = UI.secondary("Rotar 90°", "ROTATE_RIGHT");
        rotar.setOnAction(e -> rotar());
        Button eliminar = UI.button("Eliminar", "DELETE_OUTLINE", "btn-danger-soft");
        eliminar.setOnAction(e -> eliminar());
        Button configurar = UI.secondary("Volver al configurador", "MDOAL_3D_ROTATION");
        configurar.setOnAction(e -> navigatorPMMD.goTo("configurador"));
        HBox barra = UI.hbox(10, UI.label(proyectoPMMD, "card-title"), UI.spacer(), vistaPMMD, rotar, eliminar, configurar);

        areaPlanoPMMD.getStyleClass().add("planner-host");
        areaPlanoPMMD.getChildren().add(canvasPMMD);
        areaPlanoPMMD.setMinSize(300, 300);
        areaPlanoPMMD.widthProperty().addListener((o, a, b) -> dibujar());
        areaPlanoPMMD.heightProperty().addListener((o, a, b) -> dibujar());
        canvasPMMD.widthProperty().bind(areaPlanoPMMD.widthProperty());
        canvasPMMD.heightProperty().bind(areaPlanoPMMD.heightProperty());
        canvasPMMD.setFocusTraversable(true);
        canvasPMMD.setOnMousePressed(e -> {
            canvasPMMD.requestFocus();
            double cx = (e.getX() - oxPMMD) / escalaPMMD, cy = (e.getY() - oyPMMD) / escalaPMMD;
            seleccionPMMD = null;
            for (int i = mueblesPMMD.size() - 1; i >= 0; i--) {
                Mueble m = mueblesPMMD.get(i);
                if (cx >= m.x0() && cx <= m.x0() + m.w() && cy >= m.y0() && cy <= m.y0() + m.h()) {
                    seleccionPMMD = m;
                    dragDxPMMD = cx - m.x;
                    dragDyPMMD = cy - m.y;
                    break;
                }
            }
            refrescar();
        });
        canvasPMMD.setOnMouseDragged(e -> {
            if (seleccionPMMD == null) return;
            double cx = (e.getX() - oxPMMD) / escalaPMMD, cy = (e.getY() - oyPMMD) / escalaPMMD;
            seleccionPMMD.x = Math.round((cx - dragDxPMMD) / 5) * 5;
            seleccionPMMD.y = Math.round((cy - dragDyPMMD) / 5) * 5;
            dibujar();
        });
        canvasPMMD.setOnMouseReleased(e -> refrescar());
        canvasPMMD.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.R) rotar();
            else if (e.getCode() == KeyCode.DELETE || e.getCode() == KeyCode.BACK_SPACE) eliminar();
        });
        visor3dPMMD.setVisible(false);
        StackPane centro = new StackPane(areaPlanoPMMD, visor3dPMMD);
        VBox.setVgrow(centro, Priority.ALWAYS);
        VBox izq = new VBox(10, barra, centro);
        HBox.setHgrow(izq, Priority.ALWAYS);

        VBox der = new VBox(14, salaCard(), catalogo(), propiedadesPMMD, resumenPMMD);
        ScrollPane spDer = UI.scroll(der);
        spDer.setPrefWidth(320);
        spDer.setMinWidth(300);
        spDer.setMaxWidth(320);
        HBox cuerpo = new HBox(14, izq, spDer);
        VBox.setVgrow(cuerpo, Priority.ALWAYS);
        raiz.getChildren().setAll(cuerpo);
    }


    @Override
    public void onShow(Intent intent) {
        if (!iniciadoPMMD) {
            iniciadoPMMD = true;
            productosPMMD.porCodigo("SF-001").ifPresent(p -> mueblesPMMD.add(new Mueble("Sofá Moderno 3 cuerpos (220 cm)", Tipo.SOFA, p, null, 220, 92, 2690, 210, 60, 0)));
            productosPMMD.porCodigo("SF-003").ifPresent(p -> mueblesPMMD.add(new Mueble(p.getNombre(), Tipo.SOFA, p, null, 95, 90, p.getPrecio().doubleValue(), 360, 185, 90)));
            mueblesPMMD.add(new Mueble("Alfombra 200 × 140", Tipo.ALFOMBRA, null, null, 200, 140, 0, 210, 190, 0));
            mueblesPMMD.add(new Mueble("Planta", Tipo.PLANTA, null, null, 40, 40, 0, 40, 40, 0));
        }
        if (intent.es("AGREGAR") && intent.dato() instanceof ConfigSofa c) {
            var cot = configuradorPMMD.cotizar(c);
            double fondo = c.conChaise() ? c.profundidad() + 65 : c.profundidad();
            Mueble m = new Mueble(c.modelo().nombre() + " a medida", Tipo.SOFA, null, c, c.largo(), fondo, cot.precio(), anchoSalaPMMD / 2, largoSalaPMMD / 2, 0);
            mueblesPMMD.add(m);
            seleccionPMMD = m;
            toastPMMD.info("Se agregó el sofá configurado (" + MoneyPMMD.fmt0(cot.precio()) + "). Arrástralo a su lugar.");
        }
        refrescar();
        if (intent.es("VISTA3D")) vistaPMMD.setValor("Vista 3D");
    }


    @Override
    public void onHide() {
        visor3dPMMD.detenerGiro();
    }


    private void mostrarVista() {
        boolean tres = "Vista 3D".equals(vistaPMMD.getValor());
        areaPlanoPMMD.setVisible(!tres);
        visor3dPMMD.setVisible(tres);
        if (tres) construir3D(true);
    }

    // ================================================================ panel derecho


    private Node salaCard() {
        TextField ancho = Forms.entero((int) anchoSalaPMMD), largo = Forms.entero((int) largoSalaPMMD), alto = Forms.entero((int) altoSalaPMMD);
        for (TextField t : List.of(ancho, largo, alto)) t.textProperty().addListener((o, a, b) -> {
            int v = Forms.entero(t);
            if (v < 150) return;
            anchoSalaPMMD = Forms.entero(ancho) >= 150 ? Forms.entero(ancho) : anchoSalaPMMD;
            largoSalaPMMD = Forms.entero(largo) >= 150 ? Forms.entero(largo) : largoSalaPMMD;
            altoSalaPMMD = Forms.entero(alto) >= 200 ? Forms.entero(alto) : altoSalaPMMD;
            refrescar();
        });
        List<String> paredes = List.of("Frente (abajo)", "Derecha", "Fondo (arriba)", "Izquierda");
        ComboBox<String> pPuerta = Forms.combo(paredes, paredes.get(paredPuertaPMMD), s -> s);
        TextField posP = Forms.entero((int) posPuertaPMMD);
        ComboBox<String> pVentana = Forms.combo(paredes, paredes.get(paredVentanaPMMD), s -> s);
        TextField posV = Forms.entero((int) posVentanaPMMD);
        pPuerta.valueProperty().addListener((o, a, b) -> {
            paredPuertaPMMD = paredes.indexOf(b);
            refrescar();
        });
        pVentana.valueProperty().addListener((o, a, b) -> {
            paredVentanaPMMD = paredes.indexOf(b);
            refrescar();
        });
        posP.textProperty().addListener((o, a, b) -> {
            posPuertaPMMD = Forms.entero(posP);
            refrescar();
        });
        posV.textProperty().addListener((o, a, b) -> {
            posVentanaPMMD = Forms.entero(posV);
            refrescar();
        });
        GridPane g = UI.grid(3);
        g.add(UI.field("Ancho (cm)", ancho), 0, 0);
        g.add(UI.field("Largo (cm)", largo), 1, 0);
        g.add(UI.field("Alto (cm)", alto), 2, 0);
        g.add(UI.field("Puerta en", pPuerta), 0, 1, 2, 1);
        g.add(UI.field("Desde (cm)", posP), 2, 1);
        g.add(UI.field("Ventana en", pVentana), 0, 2, 2, 1);
        g.add(UI.field("Desde (cm)", posV), 2, 2);
        return UI.card(UI.label("Medidas de la sala", "card-title"), g);
    }


    private Node catalogo() {
        VBox lista = new VBox(2);
        for (Producto p : productosPMMD.listar()) {
            if (p.getForma() == Forma.JUEGO) continue;
            double fondo = p.getFondo() == null ? 90 : p.getFondo();
            lista.getChildren().add(item("WEEKEND", p.getNombre(), p.getLargo() + " × " + Math.round(fondo) + " cm · " + MoneyPMMD.fmt0(p.getPrecio()),
                    () -> agregar(new Mueble(p.getNombre(), Tipo.SOFA, p, null, p.getLargo(), fondo, p.getPrecio().doubleValue(), anchoSalaPMMD / 2, largoSalaPMMD / 2, 0))));
        }
        lista.getChildren().add(item("CROP_LANDSCAPE", "Alfombra", "200 × 140 cm · decoración",
                () -> agregar(new Mueble("Alfombra 200 × 140", Tipo.ALFOMBRA, null, null, 200, 140, 0, anchoSalaPMMD / 2, largoSalaPMMD / 2, 0))));
        lista.getChildren().add(item("LOCAL_FLORIST", "Planta", "40 × 40 cm · decoración",
                () -> agregar(new Mueble("Planta", Tipo.PLANTA, null, null, 40, 40, 0, 40, largoSalaPMMD - 40, 0))));
        return UI.card(UI.cardHeader("Catálogo", "Clic para agregar a la sala"), lista);
    }


    private Node item(String icono, String titulo, String sub, Runnable r) {
        StackPane ic = new StackPane(Ico.of(icono, 16));
        ic.getStyleClass().add("popup-item-icon");
        HBox h = new HBox(10, ic, new VBox(1, UI.label(titulo, "cell-title"), UI.label(sub, "cell-sub")), UI.spacer(), Ico.of("ADD", 16));
        h.getStyleClass().add("catalog-item");
        h.setOnMouseClicked(e -> r.run());
        return h;
    }


    private void agregar(Mueble m) {
        mueblesPMMD.add(m);
        seleccionPMMD = m;
        refrescar();
    }


    private void rotar() {
        if (seleccionPMMD == null) {
            toastPMMD.info("Selecciona un mueble en la planta.");
            return;
        }
        seleccionPMMD.rot = (seleccionPMMD.rot + 90) % 360;
        refrescar();
    }


    private void eliminar() {
        if (seleccionPMMD == null) return;
        mueblesPMMD.remove(seleccionPMMD);
        toastPMMD.info(seleccionPMMD.nombre + " quitado de la sala.");
        seleccionPMMD = null;
        refrescar();
    }


    private void refrescar() {
        dibujar();
        renderPropiedades();
        renderResumen();
        if (visor3dPMMD.isVisible()) construir3D(false);
    }


    private void renderPropiedades() {
        if (seleccionPMMD == null) {
            propiedadesPMMD.getChildren().setAll(UI.card(UI.label("Selección", "card-title"), UI.wrap("Haz clic en un mueble para moverlo, rotarlo (R) o eliminarlo (Supr).", "muted")));
            return;
        }
        Mueble m = seleccionPMMD;
        List<String> problemas = problemas(m);
        VBox c = UI.card(UI.label(m.nombre, "card-title"),
                UI.label(Math.round(m.w()) + " × " + Math.round(m.h()) + " cm en planta · rotación " + m.rot + "°", "cell-sub"),
                UI.label("Posición: " + Math.round(m.x0()) + " cm desde la izquierda, " + Math.round(m.y0()) + " cm desde el fondo", "cell-sub"));
        if (problemas.isEmpty()) c.getChildren().add(UI.banner("ok-banner", "CHECK_CIRCLE", "Ubicación válida."));
        for (String p : problemas) c.getChildren().add(UI.banner(p.startsWith("Fuera") ? "danger-banner" : "lock-banner", "WARNING", p));
        propiedadesPMMD.getChildren().setAll(c);
    }


    private void renderResumen() {
        double area = anchoSalaPMMD * largoSalaPMMD;
        double ocupado = mueblesPMMD.stream().filter(m -> m.tipo != Tipo.ALFOMBRA).mapToDouble(m -> m.ancho * m.fondo).sum();
        double libre = Math.max(0, area - ocupado);
        double total = mueblesPMMD.stream().mapToDouble(m -> m.precio).sum();
        long conProblemas = mueblesPMMD.stream().filter(m -> !problemas(m).isEmpty()).count();
        Button cotizar = UI.primary("Crear cotización", "REQUEST_QUOTE");
        cotizar.setMaxWidth(Double.MAX_VALUE);
        cotizar.setOnAction(e -> crearCotizacion());
        VBox c = UI.card(UI.label("Resumen", "card-title"),
                UI.hbox(8, UI.miniStat("Ocupación del piso", String.format("%.0f %%", ocupado * 100 / area)), UI.miniStat("Libre para circular", String.format("%.1f m²", libre / 10000))),
                UI.hbox(8, UI.label("Total de la propuesta", "muted"), UI.spacer(), UI.label(MoneyPMMD.fmt0(total), "price-value")),
                conProblemas == 0 ? UI.banner("ok-banner", "CHECK_CIRCLE", "Todos los muebles caben y no bloquean la puerta.")
                        : UI.banner("lock-banner", "WARNING", conProblemas + (conProblemas == 1 ? " mueble con observaciones." : " muebles con observaciones.")),
                cotizar);
        resumenPMMD.getChildren().setAll(c);
    }

    // ================================================================ validaciones


    private double[] barridoPuerta() {
        double a = anchoPuertaPMMD;
        return switch (paredPuertaPMMD) {
            case 0 -> new double[]{posPuertaPMMD, largoSalaPMMD - a, a, a};
            case 2 -> new double[]{posPuertaPMMD, 0, a, a};
            case 1 -> new double[]{anchoSalaPMMD - a, posPuertaPMMD, a, a};
            default -> new double[]{0, posPuertaPMMD, a, a};
        };
    }


    private static boolean cruza(double ax, double ay, double aw, double ah, double bx, double by, double bw, double bh) {
        return ax < bx + bw - 0.5 && bx < ax + aw - 0.5 && ay < by + bh - 0.5 && by < ay + ah - 0.5;
    }


    private List<String> problemas(Mueble m) {
        List<String> l = new ArrayList<>();
        if (m.x0() < -0.5 || m.y0() < -0.5 || m.x0() + m.w() > anchoSalaPMMD + 0.5 || m.y0() + m.h() > largoSalaPMMD + 0.5) l.add("Fuera de la sala.");
        if (m.tipo != Tipo.ALFOMBRA) {
            for (Mueble o : mueblesPMMD) {
                if (o == m || o.tipo == Tipo.ALFOMBRA) continue;
                if (cruza(m.x0(), m.y0(), m.w(), m.h(), o.x0(), o.y0(), o.w(), o.h())) {
                    l.add("Choca con " + o.nombre + ".");
                    break;
                }
            }
            double[] b = barridoPuerta();
            if (cruza(m.x0(), m.y0(), m.w(), m.h(), b[0], b[1], b[2], b[3])) l.add("Bloquea el barrido de la puerta.");
        }
        return l;
    }

    // ================================================================ dibujo 2D


    private void dibujar() {
        double W = canvasPMMD.getWidth(), H = canvasPMMD.getHeight();
        if (W <= 0 || H <= 0) return;
        boolean dark = temaPMMD.isOscuro();
        GraphicsContext g = canvasPMMD.getGraphicsContext2D();
        g.clearRect(0, 0, W, H);
        double margen = 46;
        escalaPMMD = Math.min((W - 2 * margen) / anchoSalaPMMD, (H - 2 * margen) / largoSalaPMMD);
        oxPMMD = (W - anchoSalaPMMD * escalaPMMD) / 2;
        oyPMMD = (H - largoSalaPMMD * escalaPMMD) / 2;
        // piso y cuadricula de 50 cm
        g.setFill(Color.web(dark ? "#1E2A44" : "#FBF7F1"));
        g.fillRect(oxPMMD, oyPMMD, anchoSalaPMMD * escalaPMMD, largoSalaPMMD * escalaPMMD);
        g.setStroke(Color.web(dark ? "#24324D" : "#E7DFD3"));
        g.setLineWidth(1);
        for (double x = 50; x < anchoSalaPMMD; x += 50) g.strokeLine(oxPMMD + x * escalaPMMD, oyPMMD, oxPMMD + x * escalaPMMD, oyPMMD + largoSalaPMMD * escalaPMMD);
        for (double y = 50; y < largoSalaPMMD; y += 50) g.strokeLine(oxPMMD, oyPMMD + y * escalaPMMD, oxPMMD + anchoSalaPMMD * escalaPMMD, oyPMMD + y * escalaPMMD);
        // paredes
        g.setStroke(Color.web(dark ? "#CBD5E1" : "#14213D"));
        g.setLineWidth(6);
        g.strokeRect(oxPMMD, oyPMMD, anchoSalaPMMD * escalaPMMD, largoSalaPMMD * escalaPMMD);
        // ventana
        double[] v = tramo(paredVentanaPMMD, posVentanaPMMD, anchoVentanaPMMD);
        g.setStroke(Color.web("#93C5FD"));
        g.setLineWidth(6);
        g.strokeLine(v[0], v[1], v[2], v[3]);
        g.setStroke(Color.WHITE);
        g.setLineWidth(1.5);
        g.strokeLine(v[0], v[1], v[2], v[3]);
        // puerta: hueco y barrido
        double[] p = tramo(paredPuertaPMMD, posPuertaPMMD, anchoPuertaPMMD);
        g.setStroke(Color.web(dark ? "#1E2A44" : "#FBF7F1"));
        g.setLineWidth(7);
        g.strokeLine(p[0], p[1], p[2], p[3]);
        double[] b = barridoPuerta();
        g.setStroke(Color.web("#A47148"));
        g.setLineWidth(1.5);
        g.setLineDashes(5, 4);
        double r = anchoPuertaPMMD * escalaPMMD;
        double bx = oxPMMD + b[0] * escalaPMMD, by = oyPMMD + b[1] * escalaPMMD;
        switch (paredPuertaPMMD) {
            case 0 -> g.strokeArc(bx - r, by, 2 * r, 2 * r, 0, 90, ArcType.OPEN);
            case 2 -> g.strokeArc(bx - r, by - r, 2 * r, 2 * r, 270, 90, ArcType.OPEN);
            case 1 -> g.strokeArc(bx, by - r, 2 * r, 2 * r, 90, 90, ArcType.OPEN);
            default -> g.strokeArc(bx - r, by - r, 2 * r, 2 * r, 270, 90, ArcType.OPEN);
        }
        g.setLineDashes();
        // muebles (alfombras primero)
        List<Mueble> orden = new ArrayList<>(mueblesPMMD);
        orden.sort(Comparator.comparing(m -> m.tipo != Tipo.ALFOMBRA));
        for (Mueble m : orden) {
            List<String> pr = problemas(m);
            boolean fuera = pr.stream().anyMatch(s -> s.startsWith("Fuera"));
            Color relleno = switch (m.tipo) {
                case ALFOMBRA -> Color.web("#CBB89B", 0.45);
                case PLANTA -> Color.web("#3F7D4E", 0.75);
                default -> Color.web(m.config != null ? "#2563EB" : "#A47148", 0.82);
            };
            if (fuera) relleno = Color.web("#DC2626", 0.75);
            else if (!pr.isEmpty()) relleno = Color.web("#F59E0B", 0.8);
            double x = oxPMMD + m.x0() * escalaPMMD, y = oyPMMD + m.y0() * escalaPMMD, w = m.w() * escalaPMMD, h = m.h() * escalaPMMD;
            g.setFill(relleno);
            if (m.tipo == Tipo.PLANTA) g.fillOval(x, y, w, h);
            else g.fillRoundRect(x, y, w, h, 8, 8);
            if (m.tipo == Tipo.SOFA) {
                // respaldo: franja del lado posterior segun la rotacion
                g.setFill(Color.rgb(0, 0, 0, 0.18));
                double t = 18 * escalaPMMD;
                switch (m.rot) {
                    case 0 -> g.fillRect(x, y, w, t);
                    case 90 -> g.fillRect(x + w - t, y, t, h);
                    case 180 -> g.fillRect(x, y + h - t, w, t);
                    default -> g.fillRect(x, y, t, h);
                }
            }
            g.setStroke(m == seleccionPMMD ? Color.web("#2563EB") : Color.web(dark ? "#E5E7EB" : "#FFFFFF"));
            g.setLineWidth(m == seleccionPMMD ? 2.5 : 1);
            if (m.tipo == Tipo.PLANTA) g.strokeOval(x, y, w, h);
            else g.strokeRoundRect(x, y, w, h, 8, 8);
            if (w > 40 && h > 18) {
                g.setFill(m.tipo == Tipo.ALFOMBRA ? Color.web("#7A4E2B") : Color.WHITE);
                g.setFont(Font.font("Inter", FontWeight.BOLD, 11));
                g.setTextAlign(TextAlignment.CENTER);
                g.setTextBaseline(VPos.CENTER);
                g.fillText(recortar(m.nombre, w), x + w / 2, y + h / 2, w - 6);
            }
        }
        if (seleccionPMMD != null) cotas(g, seleccionPMMD, dark);
        // escala
        g.setFill(Color.web(dark ? "#94A3B8" : "#64748B"));
        g.setFont(Font.font("Inter", 11));
        g.setTextAlign(TextAlignment.LEFT);
        g.setTextBaseline(VPos.TOP);
        g.fillText("Sala " + Math.round(anchoSalaPMMD) + " × " + Math.round(largoSalaPMMD) + " cm · cuadrícula de 50 cm", oxPMMD, oyPMMD + largoSalaPMMD * escalaPMMD + 10);
        g.setTextAlign(TextAlignment.RIGHT);
        g.fillText("Fondo ↑", oxPMMD + anchoSalaPMMD * escalaPMMD, oyPMMD - 18);
    }


    private static String recortar(String s, double w) {
        int max = (int) (w / 6.5);
        return s.length() <= max ? s : s.substring(0, Math.max(3, max - 1)) + "…";
    }

    /** Extremos (px) de un tramo de pared. */

    private double[] tramo(int pared, double pos, double ancho) {
        return switch (pared) {
            case 0 -> new double[]{oxPMMD + pos * escalaPMMD, oyPMMD + largoSalaPMMD * escalaPMMD, oxPMMD + (pos + ancho) * escalaPMMD, oyPMMD + largoSalaPMMD * escalaPMMD};
            case 2 -> new double[]{oxPMMD + pos * escalaPMMD, oyPMMD, oxPMMD + (pos + ancho) * escalaPMMD, oyPMMD};
            case 1 -> new double[]{oxPMMD + anchoSalaPMMD * escalaPMMD, oyPMMD + pos * escalaPMMD, oxPMMD + anchoSalaPMMD * escalaPMMD, oyPMMD + (pos + ancho) * escalaPMMD};
            default -> new double[]{oxPMMD, oyPMMD + pos * escalaPMMD, oxPMMD, oyPMMD + (pos + ancho) * escalaPMMD};
        };
    }


    private void cotas(GraphicsContext g, Mueble m, boolean dark) {
        Color c = Color.web("#2563EB");
        g.setStroke(c);
        g.setFill(c);
        g.setLineWidth(1);
        g.setFont(Font.font("Inter", FontWeight.BOLD, 10.5));
        double x = oxPMMD + m.x0() * escalaPMMD, y = oyPMMD + m.y0() * escalaPMMD, w = m.w() * escalaPMMD, h = m.h() * escalaPMMD;
        // ancho y fondo
        g.strokeLine(x, y - 12, x + w, y - 12);
        g.setTextAlign(TextAlignment.CENTER);
        g.setTextBaseline(VPos.BOTTOM);
        g.fillText(Math.round(m.w()) + " cm", x + w / 2, y - 14);
        g.strokeLine(x - 12, y, x - 12, y + h);
        g.setTextBaseline(VPos.CENTER);
        g.setTextAlign(TextAlignment.RIGHT);
        g.fillText(Math.round(m.h()) + " cm", x - 16, y + h / 2);
        // distancias a las paredes
        g.setLineDashes(3, 3);
        g.setTextAlign(TextAlignment.CENTER);
        double izq = m.x0(), der = anchoSalaPMMD - m.x0() - m.w(), arr = m.y0(), aba = largoSalaPMMD - m.y0() - m.h();
        if (izq > 5) {
            g.strokeLine(oxPMMD, y + h / 2, x, y + h / 2);
            g.fillText(Math.round(izq) + "", (oxPMMD + x) / 2, y + h / 2 - 8);
        }
        if (der > 5) {
            g.strokeLine(x + w, y + h / 2, oxPMMD + anchoSalaPMMD * escalaPMMD, y + h / 2);
            g.fillText(Math.round(der) + "", (x + w + oxPMMD + anchoSalaPMMD * escalaPMMD) / 2, y + h / 2 - 8);
        }
        if (arr > 5) {
            g.strokeLine(x + w / 2, oyPMMD, x + w / 2, y);
            g.fillText(Math.round(arr) + "", x + w / 2 + 14, (oyPMMD + y) / 2);
        }
        if (aba > 5) {
            g.strokeLine(x + w / 2, y + h, x + w / 2, oyPMMD + largoSalaPMMD * escalaPMMD);
            g.fillText(Math.round(aba) + "", x + w / 2 + 14, (y + h + oyPMMD + largoSalaPMMD * escalaPMMD) / 2);
        }
        g.setLineDashes();
    }

    // ================================================================ 3D


    private Node nodo3D(Mueble m) {
        SofaBuilder b = new SofaBuilder();
        return switch (m.tipo) {
            case ALFOMBRA -> RoomBuilder.alfombra(m.ancho, m.fondo);
            case PLANTA -> RoomBuilder.planta();
            default -> {
                if (m.config != null) {
                    Tela t = m.config.telaId() == null ? null : telasPMMD.tela(m.config.telaId());
                    Tela tc = m.config.telaCojinesId() == null ? t : telasPMMD.tela(m.config.telaCojinesId());
                    yield b.build(SofaBuilder.Opciones.de(m.config, t, tc));
                }
                SofaBuilder.Opciones o = SofaBuilder.Opciones.de(m.producto);
                if (m.producto.getCodigo().equals("SF-001")) {
                    o = new SofaBuilder.Opciones(o.forma(), o.cuerpos(), m.ancho, o.fondo(), o.alto(), o.chaise(), o.brazo(), o.patas(), o.colorPatas(),
                            "#B9B7B1", o.tipoTela(), 2, "#C9972E", o.tipoCojines());
                }
                yield b.build(o);
            }
        };
    }


    private void construir3D(boolean recentrar) {
        List<RoomBuilder.Ubicado> ubicados = new ArrayList<>();
        for (Mueble m : mueblesPMMD) {
            // planta (x, y desde el fondo) → 3D (X = x, Z = largo − y); rotacion horaria en planta
            ubicados.add(new RoomBuilder.Ubicado(nodo3D(m), m.x, largoSalaPMMD - m.y, m.rot));
        }
        List<RoomBuilder.Abertura> ab = List.of(new RoomBuilder.Abertura(paredPuertaPMMD, posPuertaPMMD, anchoPuertaPMMD, true),
                new RoomBuilder.Abertura(paredVentanaPMMD, paredVentanaPMMD == 1 || paredVentanaPMMD == 3 ? largoSalaPMMD - posVentanaPMMD - anchoVentanaPMMD : posVentanaPMMD, anchoVentanaPMMD, false));
        visor3dPMMD.setContenido(RoomBuilder.build(anchoSalaPMMD, largoSalaPMMD, altoSalaPMMD, ab, ubicados), recentrar);
        if (recentrar) visor3dPMMD.fijarVista(-25, 38);
    }

    // ================================================================ cotizacion


    private byte[] png(Node n, javafx.scene.image.Image img) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            javafx.scene.image.WritableImage w;
            if (img instanceof javafx.scene.image.WritableImage wi) w = wi;
            else {
                SnapshotParameters sp = new SnapshotParameters();
                sp.setFill(Color.WHITE);
                w = n.snapshot(sp, null);
            }
            ImageIO.write(Snapshot.imagen(w), "png", out);
            return out.toByteArray();
        } catch (Exception e) {
            return null;
        }
    }


    private void crearCotizacion() {
        List<Cliente> cs = clientesPMMD.listar();
        ComboBox<Cliente> cliente = Forms.combo(cs, cs.stream().filter(c -> c.getCodigo().equals("C-0101")).findFirst().orElse(null), Cliente::getNombre);
        TextField nombre = UI.text(proyectoPMMD, "Nombre del proyecto");
        Button cancelar = UI.secondary("Cancelar", null);
        Button ok = UI.primary("Generar PDF", "PICTURE_AS_PDF");
        var h = overlaysPMMD.modal("Crear cotización de la sala", mueblesPMMD.size() + " muebles · " + MoneyPMMD.fmt0(mueblesPMMD.stream().mapToDouble(m -> m.precio).sum()),
                new VBox(12, UI.field("Cliente", cliente), UI.field("Proyecto", nombre)), 480, cancelar, ok);
        cancelar.setOnAction(e -> h.cerrar());
        ok.setOnAction(e -> {
            h.cerrar();
            File f = Archivos.guardar(canvasPMMD, "Propuesta-sala", "PDF", "pdf");
            if (f == null) return;
            Mueble sel = seleccionPMMD;
            seleccionPMMD = null;
            dibujar();
            byte[] plano = png(canvasPMMD, null);
            seleccionPMMD = sel;
            dibujar();
            List<RoomBuilder.Ubicado> ubicados = new ArrayList<>();
            for (Mueble m : mueblesPMMD) ubicados.add(new RoomBuilder.Ubicado(nodo3D(m), m.x, largoSalaPMMD - m.y, m.rot));
            List<RoomBuilder.Abertura> ab = List.of(new RoomBuilder.Abertura(paredPuertaPMMD, posPuertaPMMD, anchoPuertaPMMD, true),
                    new RoomBuilder.Abertura(paredVentanaPMMD, posVentanaPMMD, anchoVentanaPMMD, false));
            byte[] render = png(null, Viewer3D.render(RoomBuilder.build(anchoSalaPMMD, largoSalaPMMD, altoSalaPMMD, ab, ubicados), 900, 560, -25, 38));
            List<ExportService.ItemSala> items = mueblesPMMD.stream().map(m -> new ExportService.ItemSala(m.nombre,
                    Math.round(m.ancho) + " × " + Math.round(m.fondo) + " cm" + (m.config != null ? " · " + configuradorPMMD.especificacion(m.config) : ""), m.precio)).toList();
            String cli = cliente.getValue() == null ? "Por definir" : cliente.getValue().getNombre();
            String medidas = Math.round(anchoSalaPMMD) + " × " + Math.round(largoSalaPMMD) + " × " + Math.round(altoSalaPMMD) + " cm";
            Tareas.ejecutar(overlaysPMMD, toastPMMD, "Generando propuesta…", () -> {
                exportPMMD.cotizacionSala(nombre.getText(), cli, medidas, items, plano, render, f);
                return f;
            }, a -> {
                toastPMMD.exito("Propuesta de sala guardada: " + a.getName());
                appWindowPMMD.hostServices().showDocument(a.toURI().toString());
            });
        });
    }
}

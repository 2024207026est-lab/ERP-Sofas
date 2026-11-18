package pe.erp.sofas.ui.components;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.InnerShadow;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.*;
import javafx.scene.shape.Circle;
import pe.erp.sofas.model.TipoTela;

/**
 * Muestra circular de tela con su color, sombra interior y la textura del tipo: chenille (diagonal suave),
 * lino (trama cruzada), cuero (brillo radial), terciopelo (brillo lineal), boucle (puntos), microfibra (degradado).
 */

public class ColorSwatch extends StackPane {

    private final Canvas canvasPMMD;

    private final double size;


    public ColorSwatch(String hex, TipoTela tipo, double size) {
        this.size = size;
        canvasPMMD = new Canvas(size, size);
        getStyleClass().add("swatch");
        setMinSize(size, size);
        setPrefSize(size, size);
        setMaxSize(size, size);
        Circle clip = new Circle(size / 2, size / 2, size / 2);
        canvasPMMD.setClip(clip);
        InnerShadow sombra = new InnerShadow(size * 0.12, Color.rgb(0, 0, 0, 0.35));
        sombra.setOffsetY(size * 0.03);
        canvasPMMD.setEffect(sombra);
        getChildren().add(canvasPMMD);
        pintar(hex, tipo);
    }


    public void pintar(String hex, TipoTela tipo) {
        GraphicsContext g = canvasPMMD.getGraphicsContext2D();
        Color base = parse(hex);
        g.clearRect(0, 0, size, size);
        g.setFill(base);
        g.fillOval(0, 0, size, size);
        if (tipo == null) return;
        Color claro = base.deriveColor(0, 1, 1.25, 1);
        Color oscuro = base.deriveColor(0, 1, 0.78, 1);
        switch (tipo) {
            case CHENILLE -> {
                g.setStroke(Color.color(claro.getRed(), claro.getGreen(), claro.getBlue(), 0.35));
                g.setLineWidth(Math.max(1, size / 26));
                for (double d = -size; d < size * 2; d += size / 7) g.strokeLine(d, 0, d + size, size);
            }
            case LINO -> {
                g.setStroke(Color.color(oscuro.getRed(), oscuro.getGreen(), oscuro.getBlue(), 0.35));
                g.setLineWidth(Math.max(0.6, size / 60));
                for (double d = 0; d < size; d += size / 14) {
                    g.strokeLine(d, 0, d, size);
                    g.strokeLine(0, d, size, d);
                }
            }
            case CUERO -> {
                g.setFill(new RadialGradient(0, 0, 0.32, 0.28, 0.6, true, CycleMethod.NO_CYCLE,
                        new Stop(0, Color.rgb(255, 255, 255, 0.55)), new Stop(0.45, Color.rgb(255, 255, 255, 0.08)), new Stop(1, Color.TRANSPARENT)));
                g.fillOval(0, 0, size, size);
            }
            case TERCIOPELO -> {
                g.setFill(new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                        new Stop(0, Color.rgb(255, 255, 255, 0.0)), new Stop(0.45, Color.rgb(255, 255, 255, 0.32)),
                        new Stop(0.55, Color.rgb(255, 255, 255, 0.05)), new Stop(1, Color.rgb(0, 0, 0, 0.25))));
                g.fillOval(0, 0, size, size);
            }
            case BOUCLE -> {
                g.setFill(Color.color(oscuro.getRed(), oscuro.getGreen(), oscuro.getBlue(), 0.45));
                double paso = size / 7;
                for (double y = paso / 2; y < size; y += paso) {
                    for (double x = (y / paso) % 2 < 1 ? paso / 2 : paso; x < size; x += paso) {
                        g.fillOval(x - size / 40, y - size / 40, size / 20, size / 20);
                    }
                }
            }
            case MICROFIBRA -> {
                g.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                        new Stop(0, Color.color(claro.getRed(), claro.getGreen(), claro.getBlue(), 0.45)), new Stop(1, Color.color(oscuro.getRed(), oscuro.getGreen(), oscuro.getBlue(), 0.45))));
                g.fillOval(0, 0, size, size);
            }
        }
    }


    public static Color parse(String hex) {
        try {
            return Color.web(hex == null ? "#CBD5E1" : hex);
        } catch (IllegalArgumentException e) {
            return Color.web("#CBD5E1");
        }
    }


    public ColorSwatch seleccionada(boolean s) {
        pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("selected"), s);
        return this;
    }
}

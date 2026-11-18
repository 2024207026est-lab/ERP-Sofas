package pe.erp.sofas.service;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import com.lowagie.text.pdf.*;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import pe.erp.sofas.core.AppClockPMMD;
import pe.erp.sofas.core.MoneyPMMD;
import pe.erp.sofas.core.NegocioExceptionPMMD;
import pe.erp.sofas.model.*;
import pe.erp.sofas.service.calculo.Explosion;

import java.awt.Color;
import java.io.*;
import java.util.List;

/** Exportacion a PDF (OpenPDF) y Excel (Apache POI) con la identidad de SISTEMA ERP SOFAS. */

@Service
@RequiredArgsConstructor
public class ExportService {

    private static final Color NAVYPMMD = new Color(0x14, 0x21, 0x3D);

    private static final Color WOODPMMD = new Color(0xA4, 0x71, 0x48);

    private static final Color MUTEDPMMD = new Color(0x64, 0x74, 0x8B);

    private static final Color BORDERPMMD = new Color(0xE2, 0xE8, 0xF0);

    private static final Color SURFACE2PMMD = new Color(0xF1, 0xF5, 0xF9);


    private final ConfiguracionService configuracionPMMD;


    private BaseFont regularPMMD;

    private BaseFont boldPMMD;


    private BaseFont fuente(boolean negrita) {
        try {
            if (regularPMMD == null) {
                regularPMMD = cargar("/fonts/Inter-Regular.ttf");
                boldPMMD = cargar("/fonts/Inter-Bold.ttf");
            }
        } catch (Exception e) {
            try {
                regularPMMD = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, false);
                boldPMMD = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.CP1252, false);
            } catch (Exception ex) {
                throw new IllegalStateException(ex);
            }
        }
        return negrita ? boldPMMD : regularPMMD;
    }


    private BaseFont cargar(String ruta) throws IOException, DocumentException {
        try (InputStream in = getClass().getResourceAsStream(ruta)) {
            if (in == null) throw new IOException(ruta);
            return BaseFont.createFont(ruta.substring(ruta.lastIndexOf('/') + 1), BaseFont.IDENTITY_H, BaseFont.EMBEDDED, true, in.readAllBytes(), null);
        }
    }


    private Font f(float size, boolean negrita, Color color) {
        return new Font(fuente(negrita), size, Font.NORMAL, color);
    }

    // ================================================================ estructura comun


    private interface Contenido {
        
        void escribir(Document d) throws DocumentException, IOException;
    }


    private void pdf(File archivo, boolean horizontal, String titulo, String subtitulo, Contenido contenido) {
        Rectangle pagina = horizontal ? PageSize.A4.rotate() : PageSize.A4;
        Document d = new Document(pagina, 36, 36, 36, 42);
        try (OutputStream out = new FileOutputStream(archivo)) {
            PdfWriter w = PdfWriter.getInstance(d, out);
            w.setPageEvent(new PdfPageEventHelper() {

                @Override
                public void onEndPage(PdfWriter writer, Document document) {
                    PdfContentByte cb = writer.getDirectContent();
                    ColumnText.showTextAligned(cb, Element.ALIGN_LEFT,
                            new Phrase("SISTEMA ERP SOFAS S.A.C. · Documento generado el " + AppClockPMMD.fmt(AppClockPMMD.hoy()) + " · Sin validez tributaria", f(7.5f, false, MUTEDPMMD)),
                            document.left(), document.bottom() - 18, 0);
                    ColumnText.showTextAligned(cb, Element.ALIGN_RIGHT, new Phrase("Página " + writer.getPageNumber(), f(7.5f, false, MUTEDPMMD)),
                            document.right(), document.bottom() - 18, 0);
                }
            });
            d.addTitle(titulo);
            d.addAuthor("SISTEMA ERP SOFAS ERP · Sofás");
            d.open();
            cabecera(d, titulo, subtitulo);
            contenido.escribir(d);
            d.close();
        } catch (IOException | DocumentException e) {
            throw new NegocioExceptionPMMD("No se pudo generar el PDF: " + e.getMessage());
        }
    }


    private void cabecera(Document d, String titulo, String subtitulo) throws DocumentException {
        Empresa e = configuracionPMMD.empresa();
        PdfPTable t = new PdfPTable(new float[]{1.1f, 6, 4});
        t.setWidthPercentage(100);
        PdfPCell logo = new PdfPCell(new Phrase("IS", f(15, true, new Color(0xE4, 0xC9, 0xA4))));
        logo.setBackgroundColor(NAVYPMMD);
        logo.setHorizontalAlignment(Element.ALIGN_CENTER);
        logo.setVerticalAlignment(Element.ALIGN_MIDDLE);
        logo.setFixedHeight(34);
        logo.setBorder(Rectangle.NO_BORDER);
        t.addCell(logo);
        PdfPCell marca = new PdfPCell();
        marca.setBorder(Rectangle.NO_BORDER);
        marca.setPaddingLeft(8);
        marca.addElement(new Paragraph("SISTEMA ERP SOFAS", f(14, true, NAVYPMMD)));
        marca.addElement(new Paragraph("SOFÁS · JULIACA", f(7.5f, true, WOODPMMD)));
        t.addCell(marca);
        PdfPCell datos = new PdfPCell();
        datos.setBorder(Rectangle.NO_BORDER);
        Paragraph p = new Paragraph(e.getRazonSocial() + " · RUC " + (e.getRuc() == null ? "—" : e.getRuc()), f(7.5f, false, MUTEDPMMD));
        p.setAlignment(Element.ALIGN_RIGHT);
        datos.addElement(p);
        Paragraph p2 = new Paragraph((e.getDireccion() == null ? "" : e.getDireccion()) + " · " + (e.getTelefono() == null ? "" : e.getTelefono()), f(7.5f, false, MUTEDPMMD));
        p2.setAlignment(Element.ALIGN_RIGHT);
        datos.addElement(p2);
        t.addCell(datos);
        d.add(t);
        Paragraph tit = new Paragraph(titulo, f(16, true, NAVYPMMD));
        tit.setSpacingBefore(14);
        d.add(tit);
        if (subtitulo != null) {
            Paragraph sub = new Paragraph(subtitulo, f(9, false, MUTEDPMMD));
            sub.setSpacingAfter(10);
            d.add(sub);
        }
    }


    private PdfPTable tabla(List<String> columnas, boolean[] derecha, List<List<String>> filas, List<String> totales, float[] anchos) {
        PdfPTable t = anchos == null ? new PdfPTable(columnas.size()) : new PdfPTable(anchos);
        t.setWidthPercentage(100);
        t.setHeaderRows(1);
        for (int i = 0; i < columnas.size(); i++) {
            PdfPCell c = new PdfPCell(new Phrase(columnas.get(i).toUpperCase(), f(7, true, MUTEDPMMD)));
            c.setBackgroundColor(SURFACE2PMMD);
            c.setBorderColor(BORDERPMMD);
            c.setPadding(5);
            c.setHorizontalAlignment(derecha != null && derecha[i] ? Element.ALIGN_RIGHT : Element.ALIGN_LEFT);
            t.addCell(c);
        }
        for (List<String> fila : filas) {
            for (int i = 0; i < columnas.size(); i++) {
                PdfPCell c = new PdfPCell(new Phrase(i < fila.size() ? fila.get(i) : "", f(8, false, Color.BLACK)));
                c.setBorderColor(BORDERPMMD);
                c.setPadding(5);
                c.setHorizontalAlignment(derecha != null && derecha[i] ? Element.ALIGN_RIGHT : Element.ALIGN_LEFT);
                t.addCell(c);
            }
        }
        if (totales != null) {
            for (int i = 0; i < columnas.size(); i++) {
                PdfPCell c = new PdfPCell(new Phrase(i < totales.size() ? totales.get(i) : "", f(8, true, NAVYPMMD)));
                c.setBackgroundColor(SURFACE2PMMD);
                c.setBorderColor(BORDERPMMD);
                c.setPadding(5);
                c.setHorizontalAlignment(derecha != null && derecha[i] ? Element.ALIGN_RIGHT : Element.ALIGN_LEFT);
                t.addCell(c);
            }
        }
        return t;
    }


    private PdfPTable datos(String[][] pares, int columnas) {
        PdfPTable t = new PdfPTable(columnas);
        t.setWidthPercentage(100);
        t.setSpacingAfter(10);
        for (String[] par : pares) {
            PdfPCell c = new PdfPCell();
            c.setBorderColor(BORDERPMMD);
            c.setPadding(6);
            c.addElement(new Paragraph(par[0], f(7, false, MUTEDPMMD)));
            c.addElement(new Paragraph(par[1] == null || par[1].isBlank() ? "—" : par[1], f(9, true, Color.BLACK)));
            t.addCell(c);
        }
        int resto = pares.length % columnas;
        if (resto != 0) for (int i = resto; i < columnas; i++) {
            PdfPCell c = new PdfPCell(new Phrase(""));
            c.setBorderColor(BORDERPMMD);
            t.addCell(c);
        }
        return t;
    }


    private Paragraph seccion(String texto) {
        Paragraph p = new Paragraph(texto, f(10.5f, true, NAVYPMMD));
        p.setSpacingBefore(8);
        p.setSpacingAfter(6);
        return p;
    }


    private void imagen(Document d, byte[] png, float ancho) throws IOException, DocumentException {
        if (png == null) return;
        Image img = Image.getInstance(png);
        img.scaleToFit(ancho, ancho * 0.75f);
        img.setAlignment(Element.ALIGN_CENTER);
        img.setSpacingAfter(8);
        d.add(img);
    }


    private void imagenArchivoPMMD(Document d, String ruta, float ancho) throws IOException, DocumentException {
        if (ruta == null || !new File(ruta).exists()) return;
        Image img = Image.getInstance(ruta);
        img.scaleToFit(ancho, ancho * 0.66f);
        img.setAlignment(Element.ALIGN_CENTER);
        img.setSpacingAfter(8);
        d.add(img);
    }

    // ================================================================ reportes


    public void pdf(Tabla t, File archivo) {
        pdf(archivo, t.columnas().size() > 6, t.titulo(), t.subtitulo(),
                d -> d.add(tabla(t.columnas(), t.derecha(), t.filas(), t.totales(), null)));
    }


    public void excel(Tabla t, File archivo) {
        try (Workbook wb = new XSSFWorkbook(); OutputStream out = new FileOutputStream(archivo)) {
            Sheet s = wb.createSheet(t.titulo().length() > 30 ? t.titulo().substring(0, 30) : t.titulo());
            CellStyle titulo = wb.createCellStyle();
            org.apache.poi.ss.usermodel.Font ft = wb.createFont();
            ft.setBold(true);
            ft.setFontHeightInPoints((short) 14);
            titulo.setFont(ft);
            CellStyle cab = wb.createCellStyle();
            org.apache.poi.ss.usermodel.Font fc = wb.createFont();
            fc.setBold(true);
            fc.setColor(IndexedColors.WHITE.getIndex());
            cab.setFont(fc);
            cab.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            cab.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            CellStyle tot = wb.createCellStyle();
            org.apache.poi.ss.usermodel.Font fb = wb.createFont();
            fb.setBold(true);
            tot.setFont(fb);
            tot.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            tot.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            Row r0 = s.createRow(0);
            r0.createCell(0).setCellValue("SISTEMA ERP SOFAS S.A.C. · " + t.titulo());
            r0.getCell(0).setCellStyle(titulo);
            s.addMergedRegion(new CellRangeAddress(0, 0, 0, Math.max(1, t.columnas().size() - 1)));
            s.createRow(1).createCell(0).setCellValue(t.subtitulo() == null ? "" : t.subtitulo());
            Row h = s.createRow(3);
            for (int i = 0; i < t.columnas().size(); i++) {
                Cell c = h.createCell(i);
                c.setCellValue(t.columnas().get(i));
                c.setCellStyle(cab);
            }
            int fila = 4;
            for (List<String> f : t.filas()) {
                Row r = s.createRow(fila++);
                for (int i = 0; i < f.size(); i++) escribirCeldaPMMD(r.createCell(i), f.get(i));
            }
            if (t.totales() != null) {
                Row r = s.createRow(fila);
                for (int i = 0; i < t.totales().size(); i++) {
                    Cell c = r.createCell(i);
                    escribirCeldaPMMD(c, t.totales().get(i));
                    c.setCellStyle(tot);
                }
            }
            for (int i = 0; i < t.columnas().size(); i++) s.autoSizeColumn(i);
            wb.write(out);
        } catch (IOException e) {
            throw new NegocioExceptionPMMD("No se pudo generar el Excel: " + e.getMessage());
        }
    }

    /** Escribe montos y numeros como numericos para que se puedan sumar en Excel. */

    private static void escribirCeldaPMMD(Cell c, String v) {
        if (v == null) return;
        String limpio = v.replace("S/", "").replace(",", "").replace("%", "").trim();
        if (!limpio.isEmpty() && limpio.matches("[-+]?\\d+(\\.\\d+)?")) {
            c.setCellValue(Double.parseDouble(limpio));
        } else {
            c.setCellValue(v);
        }
    }

    // ================================================================ documentos


    public void cotizacionPedido(Pedido p, File archivo) {
        pdf(archivo, false, "Cotización " + p.getCodigo(), "Emitida el " + AppClockPMMD.fmt(AppClockPMMD.hoy()) + " · válida por 15 días", d -> {
            d.add(datos(new String[][]{
                    {"Cliente", p.getCliente().getNombre()}, {p.getCliente().getTipoDocumento(), p.getCliente().getDocumento()},
                    {"Teléfono", p.getCliente().getTelefono()}, {"Dirección", p.getCliente().getDireccion()},
                    {"Entrega estimada", AppClockPMMD.fmt(p.getFechaEntrega())}, {"Tipo", p.getTipo().nombre()}}, 3));
            if (p.getProducto() != null) imagenArchivoPMMD(d, p.getProducto().getImagenPrincipal(), 320);
            d.add(seccion("Especificación"));
            d.add(datos(new String[][]{
                    {"Modelo", p.getProductoNombre()}, {"Cantidad", String.valueOf(p.getCantidad())},
                    {"Medidas", p.getMedidas()}, {"Tela y color", p.getTela() == null ? "—" : p.getTela().getNombre()},
                    {"Detalle", p.getEspecificacion()}, {"Observaciones", p.getObservaciones()}}, 2));
            d.add(seccion("Importe"));
            var sub = pe.erp.sofas.service.calculo.IgvPMMD.subtotal(p.getPrecio());
            d.add(tabla(List.of("Concepto", "Importe"), new boolean[]{false, true}, List.of(
                    List.of("Subtotal", MoneyPMMD.fmt(sub)), List.of("IGV 18 %", MoneyPMMD.fmt(p.getPrecio().subtract(sub))),
                    List.of("Adelanto recibido", MoneyPMMD.fmt(p.getAdelanto()))),
                    List.of("Total (saldo " + MoneyPMMD.fmt(p.getSaldo()) + ")", MoneyPMMD.fmt(p.getPrecio())), new float[]{4, 1.4f}));
            Paragraph n = new Paragraph("Adelanto mínimo del 50 % para confirmar. Garantía: 12 meses en estructura y 6 meses en tapiz y espuma.", f(8, false, MUTEDPMMD));
            n.setSpacingBefore(10);
            d.add(n);
        });
    }


    public void cotizacionConfigurador(ConfiguradorService.Cotizacion c, String especificacion, String cliente, byte[] png, File archivo) {
        pdf(archivo, false, "Cotización · sofá a medida", "Emitida el " + AppClockPMMD.fmt(AppClockPMMD.hoy()) + " · " + c.referencia(), d -> {
            imagen(d, png, 420);
            d.add(datos(new String[][]{{"Cliente", cliente}, {"Fabricación estimada", c.dias() + " días"},
                    {"Configuración", especificacion}, {"Tela", c.tela() == null ? "Según ficha" : c.tela().getNombre()}}, 2));
            var total = java.math.BigDecimal.valueOf(c.precio());
            var sub = pe.erp.sofas.service.calculo.IgvPMMD.subtotal(total);
            d.add(seccion("Precio"));
            d.add(tabla(List.of("Concepto", "Importe"), new boolean[]{false, true},
                    List.of(List.of("Subtotal", MoneyPMMD.fmt(sub)), List.of("IGV 18 %", MoneyPMMD.fmt(total.subtract(sub)))),
                    List.of("Total", MoneyPMMD.fmt(total)), new float[]{4, 1.4f}));
        });
    }

    /** Item de la propuesta de "Ver en mi sala". */

    public record ItemSala(
      String nombre, 
        String detalle, 
          double precio) {
    }


    public void cotizacionSala(String proyecto, String cliente, String medidas, List<ItemSala> items, byte[] plano, byte[] render, File archivo) {
        pdf(archivo, false, "Propuesta de sala", proyecto + " · " + medidas, d -> {
            d.add(datos(new String[][]{{"Cliente", cliente}, {"Proyecto", proyecto}, {"Sala", medidas}, {"Fecha", AppClockPMMD.fmt(AppClockPMMD.hoy())}}, 2));
            imagen(d, render, 420);
            imagen(d, plano, 380);
            List<List<String>> filas = new java.util.ArrayList<>();
            double total = 0;
            for (ItemSala i : items) {
                filas.add(List.of(i.nombre(), i.detalle(), i.precio() > 0 ? MoneyPMMD.fmt(i.precio()) : "—"));
                total += i.precio();
            }
            d.add(seccion("Muebles de la propuesta"));
            d.add(tabla(List.of("Mueble", "Detalle", "Precio"), new boolean[]{false, false, true}, filas, List.of("Total", "", MoneyPMMD.fmt(total)), new float[]{3, 4, 1.4f}));
        });
    }


    public void cotizacionRetapizado(Retapizado r, RetapizadoService.Cotizacion c, File archivo) {
        pdf(archivo, false, "Cotización de retapizado " + r.getCodigo(), "Emitida el " + AppClockPMMD.fmt(AppClockPMMD.hoy()), d -> {
            imagenArchivoPMMD(d, r.getFoto(), 300);
            d.add(datos(new String[][]{{"Cliente", r.getCliente().getNombre()}, {"Sofá", r.getDescripcion()},
                    {"Medidas", r.getMedidas()}, {"Cuerpos", String.valueOf(r.getCuerpos())},
                    {"Estructura", r.getEstructura()}, {"Espuma", r.getEspuma()},
                    {"Tela nueva", r.getTela() == null ? "—" : r.getTela().getNombre()}, {"Metros de tela", MoneyPMMD.numero(c.metrosTelaPMMD()) + " m"}}, 2));
            d.add(seccion("Precio"));
            d.add(tabla(List.of("Concepto", "Importe"), new boolean[]{false, true}, List.of(List.of("Retapizado completo (materiales y mano de obra)", MoneyPMMD.fmt(c.precio()))),
                    List.of("Total", MoneyPMMD.fmt(c.precio())), new float[]{4, 1.4f}));
        });
    }


    public void hojaTrabajo(OrdenProduccion op, ProduccionServicePMMD.EstadoMateriales em, File archivo) {
        pdf(archivo, false, "Hoja de trabajo " + op.getCodigo(), op.getProductoNombre() + " × " + op.getCantidad(), d -> {
            d.add(datos(new String[][]{
                    {"Cliente", op.getClienteNombre()}, {"Responsable", op.getResponsable() == null ? "—" : op.getResponsable().getNombre()},
                    {"Prioridad", op.getPrioridad().nombre()}, {"Inicio", AppClockPMMD.fmt(op.getFechaInicio())},
                    {"Entrega", AppClockPMMD.fmt(op.getFechaEntrega())}, {"Etapa actual", op.getEtapa().nombre()},
                    {"Tela y color", op.getTelaNombre()}, {"Ficha", op.getFichaUid() == null ? "Sin ficha" : op.getFichaUid().replace("v", " v")},
                    {"Pedido", op.getPedidoCodigo()}}, 3));
            if (op.getEspecificacion() != null) {
                d.add(seccion("Especificación"));
                d.add(new Paragraph(op.getEspecificacion(), f(9, false, Color.BLACK)));
            }
            d.add(seccion("Etapas"));
            List<List<String>> etapas = new java.util.ArrayList<>();
            for (Etapa e : Etapa.values()) {
                etapas.add(List.of(e.ordinal() < op.getEtapa().ordinal() ? "[x]" : "[  ]", e.nombre(), "", ""));
            }
            d.add(tabla(List.of("", "Etapa", "Firma", "Fecha"), null, etapas, null, new float[]{0.4f, 4, 3, 2}));
            if (em != null && !em.lineas().isEmpty()) {
                d.add(seccion("Materiales"));
                List<List<String>> filas = new java.util.ArrayList<>();
                for (var l : em.lineas()) {
                    filas.add(List.of(l.material().getCodigo(), l.material().getNombre(), l.componentes(),
                            MoneyPMMD.numero(l.necesario()) + " " + l.material().getUnidad(), MoneyPMMD.numero(l.reservado()), l.estado()));
                }
                d.add(tabla(List.of("Código", "Material", "Componente", "Necesario", "Reservado", "Estado"),
                        new boolean[]{false, false, false, true, true, false}, filas, null, new float[]{1.3f, 3.4f, 2.4f, 1.6f, 1.4f, 1.4f}));
            }
        });
    }

    /** Fila de la hoja de corte impresa. */

    public record FilaCorte(
      String numeros, 
        String pieza, 
          String medidas, 
            int cantidad, 
              String origen) {
    }


    public void hojaCorte(String titulo, String subtitulo, String resumen, byte[] plano, List<FilaCorte> piezas, File archivo) {
        pdf(archivo, false, titulo, subtitulo, d -> {
            Paragraph r = new Paragraph(resumen, f(9, true, WOODPMMD));
            r.setSpacingAfter(8);
            d.add(r);
            imagen(d, plano, 520);
            List<List<String>> filas = new java.util.ArrayList<>();
            for (FilaCorte fc : piezas) filas.add(List.of("[  ]", fc.numeros(), fc.pieza(), fc.medidas(), String.valueOf(fc.cantidad()), fc.origen()));
            d.add(tabla(List.of("Cortada", "N.º", "Pieza", "Medidas (cm)", "Cant.", "Origen"),
                    new boolean[]{false, false, false, false, true, false}, filas, null, new float[]{0.9f, 1, 4, 2, 0.8f, 1.4f}));
        });
    }


    public void fichaTecnica(FichaTecnica ficha, Explosion.Resultado r, double precio, File archivo) {
        pdf(archivo, false, "Ficha técnica " + ficha.getEtiqueta(), ficha.getNombre() + " · " + ficha.getEstado().nombre(), d -> {
            d.add(datos(new String[][]{
                    {"Medidas base", ficha.getLargo() + " × " + ficha.getFondo() + " × " + ficha.getAlto() + " cm"},
                    {"Cuerpos", String.valueOf(ficha.getCuerpos())}, {"Aprobada por", ficha.getAprobadoPor()},
                    {"Fecha", AppClockPMMD.fmt(ficha.getFechaAprobacion())}, {"Indirectos", MoneyPMMD.numero(ficha.getIndirectosPct()) + " %"},
                    {"Costo estándar", MoneyPMMD.fmt(r.costoEstandar())}}, 3));
            d.add(seccion("Lista de materiales"));
            List<List<String>> filas = new java.util.ArrayList<>();
            for (Explosion.Linea l : r.lineas()) {
                filas.add(List.of(l.componente().nombre(), l.material().getCodigo() + " " + l.material().getNombre(),
                        MoneyPMMD.numero(l.base()), MoneyPMMD.numero(l.mermaPct()) + " %", MoneyPMMD.numero(l.bruta()) + " " + l.material().getUnidad(),
                        MoneyPMMD.fmt(l.costo())));
            }
            d.add(tabla(List.of("Componente", "Material", "Base", "Merma", "Bruto", "Costo"),
                    new boolean[]{false, false, true, true, true, true}, filas,
                    List.of("", "Materiales", "", "", "", MoneyPMMD.fmt(r.costoMateriales())), new float[]{1.6f, 4, 1, 1, 1.6f, 1.4f}));
            d.add(seccion("Mano de obra"));
            List<List<String>> mo = new java.util.ArrayList<>();
            for (OperacionMO o : ficha.getOperaciones()) mo.add(List.of(o.getEtapa().nombre(), MoneyPMMD.numero(o.getHoras()) + " h", MoneyPMMD.fmt(o.getCostoHora()) + "/h", MoneyPMMD.fmt(o.getCosto())));
            d.add(tabla(List.of("Etapa", "Horas", "Costo/hora", "Costo"), new boolean[]{false, true, true, true}, mo,
                    List.of("Total", MoneyPMMD.numero(r.horasMO()) + " h", "", MoneyPMMD.fmt(r.costoMO())), null));
            d.add(seccion("Resumen"));
            d.add(tabla(List.of("Concepto", "Importe"), new boolean[]{false, true}, List.of(
                    List.of("Materiales", MoneyPMMD.fmt(r.costoMateriales())), List.of("Mano de obra", MoneyPMMD.fmt(r.costoMO())),
                    List.of("Indirectos " + MoneyPMMD.numero(r.indirectosPct()) + " %", MoneyPMMD.fmt(r.indirectos())),
                    List.of("Precio de venta", MoneyPMMD.fmt(precio))), List.of("Costo estándar", MoneyPMMD.fmt(r.costoEstandar())), new float[]{4, 1.4f}));
        });
    }
}

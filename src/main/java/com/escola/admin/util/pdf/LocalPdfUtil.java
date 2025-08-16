package com.escola.admin.util.pdf;

//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//


import com.escola.admin.util.DataUtils;
import com.escola.admin.util.UtilBase64;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.*;
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;
import org.apache.commons.text.WordUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Date;

@Service
public class LocalPdfUtil {
    private ByteArrayOutputStream out;
    private PdfDocument pdfDoc;
    private Document doc;
    private Table table;
    private PdfDocumentInfo metadata;
    private int tamanhoFonte = 10;
    private int tamanhoFonteTabela = 8;
    private LocalPdfTable paramsTable;

    public void setTamanhoFonte(int tam) {
        this.tamanhoFonte = tam;
    }

    public void setTamanhoFonteTabela(int tam) {
        this.tamanhoFonteTabela = tam;
    }

//    public void iniciarRelatorio(boolean retrato) {
//        this.iniciarRelatorio(null, null, false, retrato);
//    }
//
//    public void iniciarRelatorio(String titulo) {
//        this.iniciarRelatorio(titulo, null, false, true);
//    }

    private void iniciarRelatorioLocal(LocalPdfParameters parametros) {
//        parametros.getTituloRelatorio(), parametros.getSubtituloRelatorio(), parametros.isRetrato(), parametros.isImmediateFlush()
        this.out = new ByteArrayOutputStream();
        this.pdfDoc = new PdfDocument(new PdfWriter(this.out));
        this.pdfDoc.setDefaultPageSize(parametros.isRetrato() ? PageSize.A4 : PageSize.A4.rotate());
        this.doc = new Document(this.pdfDoc, parametros.isRetrato() ? PageSize.A4 : PageSize.A4.rotate(), parametros.isImmediateFlush());
        this.doc.setFontSize(this.tamanhoFonte);
        this.doc.setTextAlignment(TextAlignment.JUSTIFIED);
        this.metadata = this.pdfDoc.getDocumentInfo();
        this.pdfDoc.getCatalog().put(PdfName.Lang, new PdfString("BR"));
        if (parametros.getTituloRelatorio() != null) {
            this.addCabecalho(parametros.getTituloRelatorio(), parametros.getSubtituloRelatorio(), parametros.getLogoBase64());
        }

    }

    public void iniciarRelatorio(LocalPdfParameters parametros) {
        this.iniciarRelatorioLocal(parametros);
        if (parametros.getMetadataTitle() != null) {
            this.metadata.setTitle(parametros.getMetadataTitle());
        }

        if (parametros.getMetadataSubject() != null) {
            this.metadata.setSubject(parametros.getMetadataSubject());
        }

        if (parametros.getMetadataAuthor() != null) {
            this.metadata.setAuthor(parametros.getMetadataAuthor());
        }

        if (parametros.getMetadataCreator() != null) {
            this.metadata.setCreator(parametros.getMetadataCreator());
        }

        if (parametros.getMetadataKeywords() != null) {
            this.metadata.setKeywords(parametros.getMetadataKeywords());
        }

    }

    private void addCabecalho(String titulo, String subtitulo, String logoBase64) {
        // Load the logo
        Image logo;
        if (logoBase64 != null) {
            logo = new Image(ImageDataFactory.create(UtilBase64.getImagem(logoBase64)));
        } else {
            logo = new Image(ImageDataFactory.create(UtilBase64.getLogoBase64()));
        }

        logo.setWidth(90.0F);

        // Create the header text
//        String cabecalho = "Tribunal Regional Do Trabalho Da 18˚ Região";

        // Create the table with 3 columns
        this.table = new Table(UnitValue.createPercentArray(new float[]{15.0F, 0.1F, 82.0F}));
        this.table.setWidth(UnitValue.createPercentValue(100.0F));

        // Add a border to the table
        this.table.setBorder(Border.NO_BORDER); // Add a solid border with 1-unit width
//        this.table.setBorder(new SolidBorder(1)); // Add a solid border with 1-unit width

        // Add the logo to the first cell
        this.table.addCell(new Cell().add(logo).setBorder(Border.NO_BORDER));

        // Add an empty cell for spacing
        this.table.addCell(new Cell().setBorder(Border.NO_BORDER));

        Table innerTable = new Table(UnitValue.createPercentArray(new float[]{100.0F}));
        innerTable.setWidth(UnitValue.createPercentValue(100.0F));
        innerTable.setBorder(Border.NO_BORDER);

        // Add the header text to the third cell
        Cell mainHeader = new Cell()
                .add(new Paragraph(titulo))
                .setFontSize(20.0F)
                .setTextAlignment(TextAlignment.CENTER) // Center-align the content
                .setBorder(Border.NO_BORDER); // Remove the border for this cell

        Cell secondHeader = new Cell()
                .add(new Paragraph(subtitulo))
                .setFontSize(14.0F)
                .setTextAlignment(TextAlignment.CENTER) // Center-align the content
                .setBorder(Border.NO_BORDER); // Remove the border for this cell

        innerTable.addCell(mainHeader);
        innerTable.addCell(secondHeader);

        Cell cellTable = new Cell().add(innerTable).setBorder(Border.NO_BORDER);
        this.table.addCell(cellTable);

        // Finalize the table
        this.encerrarTabela();
    }

    public void addParagrafo(String texto) {
        this.addParagrafo(texto, TextAlignment.JUSTIFIED);
    }

    public void addParagrafo(String texto, TextAlignment alignment) {
        this.addParagrafo((new Paragraph(texto)).setTextAlignment(alignment));
    }

    public void addParagrafo(Paragraph p) {
        this.doc.add(p);
    }

    public void addParagrafo(String texto, TextAlignment alignment, float fontSize) {
        this.addParagrafo((new Paragraph(texto)).setTextAlignment(alignment).setFontSize(fontSize));
    }

    public void addLinhaEmBranco() {
        addLinhaEmBranco(0.5f);
    }

    public void addLinhaEmBranco(float space) {
        Paragraph blankLine = new Paragraph("");
        blankLine.setMarginTop(space); // Adds 20 units of space above the blank line
        this.doc.add(blankLine);
    }

    public void addImagemBase64(String base64String, HorizontalAlignment alinhamento) {
        byte[] dataImgBase64 = UtilBase64.getImagem(base64String);
        Image img = new Image(ImageDataFactory.create(dataImgBase64));
        if (alinhamento != null) {
            img.setHorizontalAlignment(alinhamento);
        }

        this.doc.add(img);
    }

    public void addImagem(String caminho, HorizontalAlignment alinhamento) throws IOException {
        Resource imgFile = new ClassPathResource(caminho);
        Image logo = (new Image(ImageDataFactory.create(imgFile.getURL()))).setHorizontalAlignment(alinhamento);
        this.doc.add(logo);
    }

    public void addCelula(Image imagem) {
        this.table.addCell((new Cell()).add(imagem));
    }

    public void addSecao(String texto) {
        this.addLinhaEmBranco();
        this.addParagrafo((new Paragraph(texto)).setTextAlignment(TextAlignment.LEFT).setBold());
    }

    public void iniciarTabela(LocalPdfTable params) {
        if (params == null || params.getColunas().length == 0 || params.getCabecalhos().length == 0) {
            return;
        }
        this.paramsTable = params;
        float[] colunasFloat = new float[params.getColunas().length];

        for (Integer i = 0; i < params.getColunas().length; i = i + 1) {
            colunasFloat[i] = params.getColunas()[i];
        }

        this.table = new Table(UnitValue.createPercentArray(colunasFloat));
        this.table.setWidth(UnitValue.createPercentValue(100.0F));
        this.table.setTextAlignment(TextAlignment.CENTER);
        if (paramsTable.isBordaTabela()) {
            this.table.setBorder(new SolidBorder(1)); // Add a solid border with 1-unit width
        }
        if (params.isMostrarCabecalho()) {
            if (params.getTamanhoRealFonteTabela() == 0) {
                params.setTamanhoRealFonteTabela(params.getColunas().length < 9 ? this.tamanhoFonteTabela : 6);
            }
            String[] var11 = params.getCabecalhos();
            int var8 = params.getCabecalhos().length;

            for (int var9 = 0; var9 < var8; ++var9) {
                String cabecalho = var11[var9];
                Paragraph p = new Paragraph(cabecalho);
                p.setFontSize(params.getTamanhoRealFonteTabela());
                if (params.isBold()) {
                    p.setBold();
                }
                p.setFontColor(params.getCorFonteCabecalho());
                p.setBackgroundColor(params.getCorCabecalho());
                p.setTextAlignment(params.getAlignment());
                this.table.addCell((new Cell()).add(p));
            }
        }

    }

    public void addCelula(String texto) {
        if (texto == null) {
            texto = "";
        }
        this.addCelula(texto, TextAlignment.LEFT);
    }

    public void addCelula(String texto, TextAlignment alinhamento) {
        this.addCelula((new Paragraph(texto)).setTextAlignment(alinhamento));
    }

    public void addCelula(Paragraph p) {
        if (paramsTable.isBordaCelula()) {
            this.addCelula((new Cell()).add(p).setBorder(new SolidBorder(1)));
        } else {
            this.addCelula((new Cell()).add(p).setBorder(Border.NO_BORDER));
        }
    }

    public void addCelula(Cell c) {
        if (c == null) {
            return;
        }
        this.table.addCell(c.setFontSize(paramsTable.getTamanhoRealFonteTabela()));
    }

    public void addCelula(BlockElement<?> block) {
        this.table.addCell((new Cell()).add(block));
    }

    public void encerrarTabela(Div container) {
        container.add(this.table);
    }

    public void encerrarTabela() {
        this.doc.add(this.table);
    }

    public byte[] encerrarRelatorio() {
        this.doc.flush();
        this.pdfDoc.close();
        this.doc.close();
        return this.out.toByteArray();
    }

    public void addBlock(BlockElement<?> block) {
        this.doc.add(block);
    }

    public void addRodape() {
        this.addRodape("");
    }

    public void addRodape(String nome) {
        int numberOfPages = this.pdfDoc.getNumberOfPages();

        for (int i = 1; i <= numberOfPages; ++i) {
            Paragraph page = nome != null && !nome.isEmpty() ? new Paragraph(String.format("Página %s de %s - Usuário: %s", i, numberOfPages, WordUtils.capitalizeFully(nome))) : new Paragraph(String.format("Página %s de %s", i, numberOfPages));
            page.setFontSize(8.0F);
            this.doc.showTextAligned(page, 10.0F, 10.0F, i, TextAlignment.LEFT, VerticalAlignment.BOTTOM, 0.0F);
            Rectangle pageSize = this.pdfDoc.getPage(i).getPageSize();
            float x = pageSize.getWidth() - 10.0F;
            Paragraph date = new Paragraph(DataUtils.formatar(new Date(), "dd/MM/yyyy HH:mm:ss"));
            date.setFontSize(8.0F);
            this.doc.showTextAligned(date, x, 10.0F, i, TextAlignment.RIGHT, VerticalAlignment.BOTTOM, 0.0F);
        }

    }

    public void addCabecalho(BlockElement<?> block, float x, float y) {
        this.addCabecalho(block, x, y, TextAlignment.LEFT, VerticalAlignment.BOTTOM, 0.0F);
    }

    public void addCabecalho(BlockElement<?> block, float x, float y, TextAlignment textAlign, VerticalAlignment vertAlign, float radAngle) {
        Paragraph header = new Paragraph();
        header.add(block);

        for (int i = 1; i <= this.pdfDoc.getNumberOfPages(); ++i) {
            this.doc.showTextAligned(header, x, y, i, textAlign, vertAlign, radAngle);
        }

    }

    public void addLineSeparator() {
        SolidLine line = new SolidLine(1f);
        LineSeparator ls = new LineSeparator(line);
        addBlock(ls);
    }

    public Document getDoc() {
        return this.doc;
    }

    public void setDoc(Document doc) {
        this.doc = doc;
    }

    public PdfDocument getPdfDoc() {
        return this.pdfDoc;
    }

    public void setPdfDoc(PdfDocument pdfDoc) {
        this.pdfDoc = pdfDoc;
    }
}

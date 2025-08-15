package com.escola.admin.util.pdf;

import com.itextpdf.kernel.colors.Color;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.layout.properties.TextAlignment;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.springframework.context.annotation.Scope;

@Scope("prototype")
@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LocalPdfTable {
    Integer[] colunas;
    String[] cabecalhos;
    @Builder.Default // Use this annotation
    Color corCabecalho = ColorConstants.WHITE;
    @Builder.Default // Use this annotation
    Color corFonteCabecalho = ColorConstants.BLACK;
    @Builder.Default // Use this annotation
    TextAlignment alignment = TextAlignment.CENTER;
    @Builder.Default // Use this annotationÒ
    boolean mostrarCabecalho = true;
    float tamanhoRealFonteTabela;
    boolean bold;
    @Builder.Default // Use this annotationÒ
    boolean bordaTabela = false;
    @Builder.Default // Use this annotationÒ
    boolean bordaCelula = true;

}

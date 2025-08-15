package com.escola.admin.util.pdf;

//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.springframework.context.annotation.Scope;

//@Component
@Scope("prototype")
@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LocalPdfParameters {
    String metadataTitle;
    String metadataAuthor;
    String metadataSubject;
    String metadataKeywords;
    String metadataCreator;
    boolean immediateFlush;
    String tituloRelatorio;
    String subtituloRelatorio;
    boolean retrato;

}

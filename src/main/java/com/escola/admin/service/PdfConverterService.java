package com.escola.admin.service;

import java.io.IOException;

public interface PdfConverterService {

    String convertHtmlToPdfBase64(String htmlContent) throws IOException;
}

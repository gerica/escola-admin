package com.escola.admin.service;

import java.io.IOException;

public interface ItextPdfConverterService {

    String convertHtmlToPdfBase64(String htmlContent) throws IOException;
}

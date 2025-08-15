package com.escola.admin.model.request;

import com.escola.admin.service.report.TipoArquivoEnum;
import lombok.Builder;

@Builder
public record FiltroRelatorioRequest(
        String filtro,
        TipoArquivoEnum tipo
) {
}

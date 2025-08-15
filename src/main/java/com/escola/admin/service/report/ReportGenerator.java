package com.escola.admin.service.report;


import com.escola.admin.exception.BaseException;
import com.escola.admin.model.request.report.MetadadosRelatorioRequest;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.List;

public interface ReportGenerator<T> {
    ObjectNode build(List<T> entities, MetadadosRelatorioRequest metadados) throws BaseException;
}
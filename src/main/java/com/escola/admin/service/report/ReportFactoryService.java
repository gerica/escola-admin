package com.escola.admin.service.report;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class ReportFactoryService<T> {

    ApplicationContext applicationContext;

    public ReportGenerator<T> getReportGenerator(TipoArquivoEnum reportType, Class<T> entityClass) {
        String beanName = "report" + entityClass.getSimpleName() + reportType;
        return (ReportGenerator<T>) applicationContext.getBean(beanName);
    }
}
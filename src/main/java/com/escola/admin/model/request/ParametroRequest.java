package com.escola.admin.model.request;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Getter
@Setter
@Builder
public class ParametroRequest {
    String chave;
    String valor;
}

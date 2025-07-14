package com.escola.admin.model.response;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.time.OffsetDateTime;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Getter
@Setter
public class AppConfigResponse {
    String name;
    String description;
    String version;
    OffsetDateTime time;
}

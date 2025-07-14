package com.escola.admin.controller.help;

public record SortInput(
        String property,
        SortOrder direction) {
}
package com.atlassian.migration.app.zephyr.scale.model;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public record ScaleCustomFieldResponse(
        String name,
        int index,
        int id,
        String type,
        boolean required,
        List<ScaleCustomFieldOptionResponse> options
) {
    public Map<String, ScaleCustomFieldOptionResponse> getOptionsByName(){
        return options().stream().collect(Collectors.toMap(ScaleCustomFieldOptionResponse::name, Function.identity()));
    }
}
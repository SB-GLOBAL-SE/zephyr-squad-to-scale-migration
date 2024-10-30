package com.atlassian.migration.app.zephyr.scale.model;

import java.util.List;

public record ScaleCustomFieldPayload(
        String name,
        String category,
        String projectKey,
        String type
) {
    public static final String TYPE_SINGLE_LINE_TEXT = "SINGLE_LINE_TEXT";
    public static final String SINGLE_CHOICE_SELECT_LIST = "SINGLE_CHOICE_SELECT_LIST";
    public static final String MULTI_LINE_TEXT = "MULTI_LINE_TEXT";
}

package com.atlassian.migration.app.zephyr.scale.model;

import java.util.List;
import java.util.Map;

public record ScaleCustomFieldPayload(
        String name,
        String category,
        String projectKey,
        String type
) {
    public static final String TYPE_SINGLE_LINE_TEXT = "SINGLE_LINE_TEXT";
    public static final String SINGLE_CHOICE_SELECT_LIST = "SINGLE_CHOICE_SELECT_LIST";
    public static final String MULTI_LINE_TEXT = "MULTI_LINE_TEXT";
    public static final String CHECKBOX = "CHECKBOX";
    public static final String USER_LIST = "USER_LIST";
    public static final String DATE = "DATE";
    public static final String NUMBER = "NUMBER";
    public static final String DECIMAL = "DECIMAL";
    public static final String MULTI_CHOICE_SELECT_LIST = "MULTI_CHOICE_SELECT_LIST";

    public static final Map<String, String> SQUAD_SCALE_CUSTOM_FIELD_TYPE = Map.of(
            "LARGE_TEXT", MULTI_LINE_TEXT,
            "TEXT", TYPE_SINGLE_LINE_TEXT,
            "SINGLE_SELECT",SINGLE_CHOICE_SELECT_LIST,
            "CHECKBOX",MULTI_CHOICE_SELECT_LIST,
            "DATE",DATE,
            "DATE_TIME",DATE,
            "MULTI_SELECT",MULTI_CHOICE_SELECT_LIST,
            "NUMBER",NUMBER,
            "RADIO_BUTTON",SINGLE_CHOICE_SELECT_LIST
    );

}
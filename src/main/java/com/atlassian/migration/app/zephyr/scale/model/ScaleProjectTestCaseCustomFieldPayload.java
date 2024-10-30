package com.atlassian.migration.app.zephyr.scale.model;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.atlassian.migration.app.zephyr.scale.model.ScaleCustomFieldPayload.MULTI_LINE_TEXT;
import static com.atlassian.migration.app.zephyr.scale.model.ScaleCustomFieldPayload.SINGLE_CHOICE_SELECT_LIST;

public class ScaleProjectTestCaseCustomFieldPayload {

    //ETS ONLY collection of custom fields
    public static final ScaleTestCaseCustomField moduleLibraryCf = new ScaleTestCaseCustomField(
                    "Module Library",
                    SINGLE_CHOICE_SELECT_LIST,
                    "Select Yes if this TEST is part of Module Library/Generic Test Case",
                    List.of(
                            new ScaleTestCaseCustomFieldOption("Yes",0,false),
                            new ScaleTestCaseCustomFieldOption("No",1,false)
                    ));

    public static final ScaleTestCaseCustomField testTypeCf = new ScaleTestCaseCustomField(
            "Test Type",
            SINGLE_CHOICE_SELECT_LIST,
            "Type of TEST",
            List.of(
                    new ScaleTestCaseCustomFieldOption("Accessibility",0,false),
                    new ScaleTestCaseCustomFieldOption("Alpha",1,false),
                    new ScaleTestCaseCustomFieldOption("Dev Test",2,false),
                    new ScaleTestCaseCustomFieldOption("Functional",3,false),
                    new ScaleTestCaseCustomFieldOption("Integration",4,false),
                    new ScaleTestCaseCustomFieldOption("OT",5,false),
                    new ScaleTestCaseCustomFieldOption("Performance",6,false),
                    new ScaleTestCaseCustomFieldOption("PPV",7,false),
                    new ScaleTestCaseCustomFieldOption("Security",8,false),
                    new ScaleTestCaseCustomFieldOption("UAT",9,false)
            ));

    public static final ScaleTestCaseCustomField testTypeDetailCf = new ScaleTestCaseCustomField(
            "Test Type Detail",
            SINGLE_CHOICE_SELECT_LIST,
            "Type of TEST",
            List.of(
                    new ScaleTestCaseCustomFieldOption("Progression",0,false),
                    new ScaleTestCaseCustomFieldOption("Regression",1,false)
            ));

    public static final ScaleTestCaseCustomField testCaseClassificationCf = new ScaleTestCaseCustomField(
            "Test Case Classification",
            SINGLE_CHOICE_SELECT_LIST,
            "",
            List.of(
                    new ScaleTestCaseCustomFieldOption("Positive scenario",0,false),
                    new ScaleTestCaseCustomFieldOption("Negative scenario",1,false)
            ));

    public static final ScaleTestCaseCustomField testCaseCoverageCf = new ScaleTestCaseCustomField(
            "Test Case Coverage",
            SINGLE_CHOICE_SELECT_LIST,
            "",
            List.of(
                    new ScaleTestCaseCustomFieldOption("High",0,false),
                    new ScaleTestCaseCustomFieldOption("Medium",1,false),
                    new ScaleTestCaseCustomFieldOption("Low",2,false)
            ));

    public static final ScaleTestCaseCustomField testAutomationToolCf = new ScaleTestCaseCustomField(
            "Test Automation Tool",
            SINGLE_CHOICE_SELECT_LIST,
            "",
            List.of(
                    new ScaleTestCaseCustomFieldOption("N/A", 0, false),
                    new ScaleTestCaseCustomFieldOption("Appium - Android", 1, false),
                    new ScaleTestCaseCustomFieldOption("Appium - iOS", 2, false),
                    new ScaleTestCaseCustomFieldOption("Fusion Framework", 3, false),
                    new ScaleTestCaseCustomFieldOption("Latte", 4, false),
                    new ScaleTestCaseCustomFieldOption("Ready API", 5, false),
                    new ScaleTestCaseCustomFieldOption("Sahi Pro", 6, false),
                    new ScaleTestCaseCustomFieldOption("Selenium", 7, false),
                    new ScaleTestCaseCustomFieldOption("Soap UI", 8, false)
            ));

    public static final ScaleTestCaseCustomField automationStatusCf = new ScaleTestCaseCustomField(
            "Automation Status",
            SINGLE_CHOICE_SELECT_LIST,
            "Status of automation of TEST",
            List.of(
                    new ScaleTestCaseCustomFieldOption("To be reviewed",0,false),
                    new ScaleTestCaseCustomFieldOption("Not scriptable",1,false),
                    new ScaleTestCaseCustomFieldOption("To be scripted",2,false),
                    new ScaleTestCaseCustomFieldOption("Scripted",3,false),
                    new ScaleTestCaseCustomFieldOption("Update script",4,false)
            ));

    public static final ScaleTestCaseCustomField testCICf = new ScaleTestCaseCustomField(
            "Test CI",
            SINGLE_CHOICE_SELECT_LIST,
            "Denote if automation is done via continuous integration.",
            List.of(
                    new ScaleTestCaseCustomFieldOption("N/A",0,false),
                    new ScaleTestCaseCustomFieldOption("Yes",1,false),
                    new ScaleTestCaseCustomFieldOption("No",2,false)
            ));

    public static final ScaleTestCaseCustomField interfaceTouchPointCf = new ScaleTestCaseCustomField(
            "Interface Touch Point/s",
            MULTI_LINE_TEXT,
            "List of interfacing applications impacted by TEST",
            Collections.emptyList());

    public static final Map<String, ScaleTestCaseCustomField> CUSTOM_FIELD_ID_TO_NAMES = Map.of (
            "customfield_12200", moduleLibraryCf,
            "customfield_10202", testTypeCf,
            "customfield_23102", testTypeDetailCf,
            "customfield_15303", testCaseClassificationCf,
            "customfield_11111", testCaseCoverageCf,
            "customfield_15804", testAutomationToolCf,
            "customfield_11113", automationStatusCf,
            "customfield_23202", testCICf,
            "customfield_11118", interfaceTouchPointCf
    );
}

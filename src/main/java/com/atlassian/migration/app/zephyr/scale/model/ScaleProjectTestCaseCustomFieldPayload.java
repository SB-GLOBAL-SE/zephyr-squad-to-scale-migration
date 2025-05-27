package com.atlassian.migration.app.zephyr.scale.model;

import java.util.*;
import java.util.Map.Entry;


import static com.atlassian.migration.app.zephyr.scale.model.ScaleCustomFieldPayload.*;

public class ScaleProjectTestCaseCustomFieldPayload {

    public static final ScaleTestCaseCustomField testReviewerCf = new ScaleTestCaseCustomField(
            null,
            "Test Reviewer",
            SINGLE_USER_PICKER,
            "Test Reviewer",
            false,
            Collections.emptyList());

    public static final ScaleTestCaseCustomField appMnemonicCf = new ScaleTestCaseCustomField(
            null,
            "App Mnemonic",
            SINGLE_CHOICE_SELECT_LIST,
            "App Mnemonic",
            false,
            Collections.emptyList());

    public static final ScaleTestCaseCustomField automationStatusCf = new ScaleTestCaseCustomField(
            null,
            "Automation Status",
            SINGLE_CHOICE_SELECT_LIST,
            "Automation Status",
            false,
            List.of(
                    new ScaleTestCaseCustomFieldOption("Manual",0,false),
                    new ScaleTestCaseCustomFieldOption("Automated",1,false),
                    new ScaleTestCaseCustomFieldOption("To Be Automated",2,false),
                    new ScaleTestCaseCustomFieldOption("Cannot Be Automated",3,false)
                    ));

    public static final ScaleTestCaseCustomField testCaseTypeCf = new ScaleTestCaseCustomField(
            null,
            "Test Type",
            SINGLE_CHOICE_SELECT_LIST,
            "Test Type",
            false,
            List.of(
                    new ScaleTestCaseCustomFieldOption("Unit Test",0,false),
                    new ScaleTestCaseCustomFieldOption("System Integration Test (Functional)",1,false),
                    new ScaleTestCaseCustomFieldOption("Business Test (UAT)",2,false),
                    new ScaleTestCaseCustomFieldOption("Regression Test",3,false),
                    new ScaleTestCaseCustomFieldOption("Prod Parallel",4,false),
                    new ScaleTestCaseCustomFieldOption("Performance (Non-functional Test)",5,false),
                    new ScaleTestCaseCustomFieldOption("Security (Non-functional Test)",6,false),
                    new ScaleTestCaseCustomFieldOption("Backout Test",7,false),
                    new ScaleTestCaseCustomFieldOption("Post DR Test",8,false),
                    new ScaleTestCaseCustomFieldOption("Post Implementation Test",9,false)
                    ));

    // BNY PROD custom field ids.
    public static final String TEST_REVIEWER_FIELD_KEY = "customfield_15600";
    public static final String AUTOMATION_STATUS_FIELD_KEY = "customfield_19601";
    public static final String APP_MNEMONIC_FIELD_KEY = "customfield_10456";
    public static final String TEST_TYPE_FIELD_KEY = "customfield_16900";

    public static final String STATUS_SYSTEM_FIELD = "status";

    //BNY Field Ids
//    public static final String TEST_REVIEWER_FIELD_KEY = "customfield_15600";
//    public static final String AUTOMATION_STATUS_FIELD_KEY = "customfield_19601";
//    public static final String APP_MNEMONIC_FIELD_KEY = "customfield_10456";
//    public static final String TEST_TYPE_FIELD_KEY = "customfield_16900";

    //BNY Test Instance fields
//    public static final String TEST_REVIEWER_FIELD_KEY = "customfield_15600";
//    public static final String AUTOMATION_STATUS_FIELD_KEY = "customfield_19700";
//    public static final String APP_MNEMONIC_FIELD_KEY = "customfield_10456";
//    public static final String TEST_TYPE_FIELD_KEY = "customfield_19600";
    public static final Map<String, String> TEST_TYPE_FIELD_MAP =
            Map.ofEntries(
                    Map.entry("Unit test", "Unit Test"),
                    Map.entry("Systems Integration test", "System Integration Test (Functional)"),
                    Map.entry("Systems test (Functional test)", "System Integration Test (Functional)"),
                    Map.entry("System test (Functional test)", "System Integration Test (Functional)"),
                    Map.entry("User Acceptance test", "Business Test (UAT)"),
                    Map.entry("Regression test", "Regression Test"),
                    Map.entry("Prod Parallel test", "Prod Parallel"),
                    Map.entry("Performance test (Nonfunctional test)", "Performance (Non-functional Test)"),
                    Map.entry("Security test", "Security (Non-functional Test)"),
                    Map.entry("Backout test", "Backout Test"),
                    Map.entry("Byte to Byte Compare test", "System Integration Test (Functional)"),
                    Map.entry("Disaster recovery test", "Post DR Test"),
                    Map.entry("Production live test", "Post Implementation Test"),
                    Map.entry("Others", "System Integration Test (Functional)"),
                    Map.entry("Smoke test", "System Integration Test (Functional)")
            );

    public static final Map<String, String> AUTOMATION_STATUS_MAP =
            Map.ofEntries(
                    Map.entry("Manual", "Manual"),
                    Map.entry("Automated", "Automated"),
                    Map.entry("To be automated", "To Be Automated")
            );

    public static final Map<String, String> STATUS_SYSTEM_FIELD_MAP =
            Map.ofEntries(
                    Map.entry("Design", "Draft"),
                    Map.entry("Rework", "Draft"),
                    Map.entry("Ready for Review", "Review"),
                    Map.entry("Approved", "Approved"),
                    Map.entry("Retired", "Deprecated"),
                    Map.entry("RETIRED", "Deprecated"),
                    Map.entry("Rejected", "Deprecated")

            );

    public static final Map<String, ScaleTestCaseCustomField> CUSTOM_FIELD_ID_TO_NAMES = Map.of (
            TEST_REVIEWER_FIELD_KEY, testReviewerCf,
            AUTOMATION_STATUS_FIELD_KEY, automationStatusCf,
            APP_MNEMONIC_FIELD_KEY, appMnemonicCf,
            TEST_TYPE_FIELD_KEY, testCaseTypeCf
    );

    public static final Map<String, String> SCALE_CUSTOMFIELD_NAME_ID = new HashMap<>();

    public static final Map<String, Set<String>> SCALE_CUSTOMFIELD_NAME_OPTIONS = new HashMap<>();
    // This is to fetch the mapped values for specific custom fields in bny scale custom fields
    public static final Map<String, Map<String, String>> CUSTOM_FIELD_ID_TO_TARGETMAP = Map.of (
            AUTOMATION_STATUS_FIELD_KEY, AUTOMATION_STATUS_MAP,
            TEST_TYPE_FIELD_KEY, TEST_TYPE_FIELD_MAP,
            STATUS_SYSTEM_FIELD, STATUS_SYSTEM_FIELD_MAP
    );
}

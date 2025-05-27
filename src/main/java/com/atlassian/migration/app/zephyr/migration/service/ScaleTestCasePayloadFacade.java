package com.atlassian.migration.app.zephyr.migration.service;

import com.atlassian.migration.app.zephyr.jira.api.JiraApi;
import com.atlassian.migration.app.zephyr.jira.model.JiraIssueComponent;
import com.atlassian.migration.app.zephyr.jira.model.JiraIssuePriority;
import com.atlassian.migration.app.zephyr.jira.model.JiraIssueStatusResponse;
import com.atlassian.migration.app.zephyr.jira.model.JiraIssuesResponse;
import com.atlassian.migration.app.zephyr.scale.api.ScaleApi;
import com.atlassian.migration.app.zephyr.scale.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Stream;
import java.io.IOException;
import java.util.stream.Collectors;


public class ScaleTestCasePayloadFacade {

    private static final Logger logger = LoggerFactory.getLogger(ScaleTestCasePayloadFacade.class);

    private static final String DEFAULT_PRIORITY = "Medium";

    private static final String DEFAULT_STATUS = "Draft";

    private final JiraApi jiraApi;

    private final ScaleApi scaleApi;

    public ScaleTestCasePayloadFacade(JiraApi jiraApi, ScaleApi scaleApi) {
        this.jiraApi = jiraApi;
        this.scaleApi = scaleApi;
    }

    public ScaleTestCaseCreationPayload createTestCasePayload(JiraIssuesResponse issue, String projectKey) {
        var sanitizedPriority = sanitizePriority(issue.fields().priority);

        var sanitizedStatus = sanitizeStatus(issue.fields().status);

        var components = issue.fields().components;

        List<String> labels = issue.fields().labels;
        if(labels == null){
            labels = new LinkedList<String>();
        }
        String componentName = null;
        if(components != null && components.size() > 0){
            var firstComponent = components.get(0);
            componentName = firstComponent.name();
            if(components.size() > 1) {
                for (var comp : components.subList(1, components.size())) {
                    String cName = comp.name();
                    if (!labels.contains(cName)) {
                        labels.add(cName);
                    }
                }
            }
        }

        // Handle null reporter
        String reporterKey = (issue.fields().reporter != null && issue.fields().reporter.key() != null) 
                             ? issue.fields().reporter.key() 
                             : null;

        Map<String,Object> scaleCustomFields = new HashMap<>();

//        scaleCustomFields.put("components", getComponentsNames(issue));
//        scaleCustomFields.put("squadStatus", issue.fields().status.name());
//        scaleCustomFields.put("squadPriority", sanitizedPriority);
        scaleCustomFields.putAll(getCustomFieldsToMigrate(issue));

        return new ScaleTestCaseCreationPayload(
                projectKey,
                issue.fields().summary,
                convertJiraTextFormattingToHtml(issue.fields().description),
                labels,
                reporterKey, // Use the potentially null reporterKey
                getIssueLinksIds(issue),
                sanitizedPriority,
                sanitizedStatus,
                componentName,
                scaleCustomFields
        );
    }

    private Map<String, Object> getCustomFieldsToMigrate(JiraIssuesResponse issue) {

        Map<String, Object> mappedCustomFieldAndValues = new HashMap<>();
        var customFieldsToMigrate = ScaleProjectTestCaseCustomFieldPayload.CUSTOM_FIELD_ID_TO_NAMES.keySet();

        issue.fields().customFields.entrySet().stream()
                .filter( customField ->customFieldsToMigrate.contains(customField.getKey()))
                .forEach(customFieldMetadata -> {
                    String key = customFieldMetadata.getKey();
                    var customFieldProps = ScaleProjectTestCaseCustomFieldPayload
                            .CUSTOM_FIELD_ID_TO_NAMES
                            .get(key);

                    // This method is for BNY else use other way of fetching value
                    String customFieldValue = getFieldValue(customFieldMetadata, customFieldProps, key);
                    //Generally use this method to get values.
//                    String customFieldValue = getCustomFieldValue(customFieldMetadata, customFieldProps);

                    mappedCustomFieldAndValues.put(customFieldProps.name(),
                            customFieldValue);
                });

        return mappedCustomFieldAndValues;
    }

    private String getFieldValue(Map.Entry<String, Object> customFieldMetadata, ScaleTestCaseCustomField customFieldProps, String key) {
        String customFieldValue = getCustomFieldValue(customFieldMetadata, customFieldProps);
        if(key.equals(ScaleProjectTestCaseCustomFieldPayload.APP_MNEMONIC_FIELD_KEY)){
            try {
                String mnemonicCustomField = migrateMnemonic(customFieldValue);
                String fieldName = ScaleProjectTestCaseCustomFieldPayload.CUSTOM_FIELD_ID_TO_NAMES.get(key).name();
                String id = ScaleProjectTestCaseCustomFieldPayload.SCALE_CUSTOMFIELD_NAME_ID.get(fieldName);
                var existingOptions = ScaleProjectTestCaseCustomFieldPayload.SCALE_CUSTOMFIELD_NAME_OPTIONS.get(fieldName);
                if(mnemonicCustomField == null || mnemonicCustomField.equals("None")){
                    return null;
                }
                if(existingOptions == null || !existingOptions.contains(mnemonicCustomField)) {
                    scaleApi.addOptionToCustomField(id, new ScaleTestCaseCustomFieldOption(mnemonicCustomField, null, false));
                    ScaleProjectTestCaseCustomFieldPayload.SCALE_CUSTOMFIELD_NAME_OPTIONS.get(fieldName).add(mnemonicCustomField);
                }
                return mnemonicCustomField;
            }catch (Exception e){
                logger.error("Unable to add menmonic option to custom fields");
                return null;
            }
        }

        if(customFieldValue != null){
            var mappedValues = ScaleProjectTestCaseCustomFieldPayload.CUSTOM_FIELD_ID_TO_TARGETMAP.get(key);
            if(mappedValues != null && mappedValues.containsKey(customFieldValue)){
                customFieldValue = mappedValues.get(customFieldValue);
            }
        }
        return customFieldValue;
    }
    private String migrateMnemonic(String input) {
        if (input == null || input.trim().isEmpty() || input.length() < 3) {
            return null;
        }

        if (input.length() > 3) {
            return input.substring(0, 3);
        }

        return input;
    }

    private String getCustomFieldValue(Map.Entry<String, Object> customFieldMetadata, ScaleTestCaseCustomField customFieldProps){
        try {
            switch (customFieldProps.type()) {
                case ScaleCustomFieldPayload.SINGLE_CHOICE_SELECT_LIST -> {
                    if (customFieldMetadata.getValue() == null) {
                        return null;
                    }
                    if(customFieldMetadata.getValue() instanceof List<?>){
                        try {
                            var allValues = (List<Map<String, Object>>) customFieldMetadata.getValue();
                            if (allValues.size() > 0) {
                                var first = allValues.get(0);
                                return (String) first.get("value");
                            }
                        }catch (Exception e){
                            var allValues = (List<String>) customFieldMetadata.getValue();
                            if (allValues.size() > 0) {
                                var first = allValues.get(0);
                                return first;
                            }
                        }
                    }else {
                        var customFieldData = (Map<String, Object>) customFieldMetadata.getValue();
                        return (String) customFieldData.get("value");
                    }
                }
                case ScaleCustomFieldPayload.SINGLE_USER_PICKER -> {
                    if (customFieldMetadata.getKey() == null) {
                        return null;
                    }
                    var customFieldData = (Map<String, Object>) customFieldMetadata.getValue();
                    if(customFieldData == null){
                        return null;
                    }
                    return (String) customFieldData.get("key");
                }
                case ScaleCustomFieldPayload.MULTI_LINE_TEXT, ScaleCustomFieldPayload.TYPE_SINGLE_LINE_TEXT -> {
                    if (customFieldMetadata.getValue() == null) {
                        return "";
                    }
                    Object metadataValue = customFieldMetadata.getValue();
                    if(metadataValue instanceof Collection<?>){
                        return String.join(", ", (Collection<String>)metadataValue);
                    }
                    return (String) metadataValue;
                }
                default -> {
                    logger.warn("Custom field type not supported: " + customFieldProps.type());
                    return null;
                }
            }

        }catch (Exception e){
            System.out.println("Error: " + e.getMessage());
            System.out.println("customFieldMetadata: " + customFieldMetadata);

            System.exit(1);
        }

        return "";
    }

    public String sanitizePriority(JiraIssuePriority squadPriority) {

        if (squadPriority != null) {
            if (squadPriority.name() == null || squadPriority.name().isBlank()) {
                logger.warn("Priority with id:" + squadPriority.id() + " has an empty name.");
                return "";
            }
            return squadPriority.name();
        } else {
            return DEFAULT_PRIORITY;
        }
    }

    public String sanitizeStatus(JiraIssueStatusResponse squadStatus) {

        if (squadStatus != null) {
            String name = squadStatus.name();
            if (name == null || name.isBlank()) {
                logger.warn("Priority with id:" + squadStatus.id() + " has an empty name.");
                return "";
            }
            if(ScaleProjectTestCaseCustomFieldPayload.STATUS_SYSTEM_FIELD_MAP.containsKey(name)) {
                name = ScaleProjectTestCaseCustomFieldPayload.STATUS_SYSTEM_FIELD_MAP.get(name);
            }
            return name;
        } else {
            return DEFAULT_STATUS;
        }
    }

    private String convertJiraTextFormattingToHtml(String textToFormat) {
        if (textToFormat == null || textToFormat.isBlank()) {
            return textToFormat;
        }

        try {
            return jiraApi.convertJiraTextFormattingToHtml(textToFormat);
        } catch (IOException e) {
            logger.error("Failed to convert a Jira Text Formatting Notation text to HTML. Value:\nerror: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public List<String> getIssueLinksIds(JiraIssuesResponse issue) {
        return issue.fields().issuelinks.stream()
                .flatMap(e -> {
                    Stream<String> outward = e.outwardIssue() != null ? Stream.of(e.outwardIssue().key()) : Stream.empty();
                    Stream<String> inward = e.inwardIssue() != null ? Stream.of(e.inwardIssue().key()) : Stream.empty();
                    return Stream.concat(outward, inward);
                })
                .toList();
    }

    public List<String> getIssueLinksIssueIds(JiraIssuesResponse issue) {
        return issue.fields().issuelinks.stream()
                .flatMap(e -> {
                    Stream<String> outward = e.outwardIssue() != null ? Stream.of(e.outwardIssue().id()) : Stream.empty();
                    Stream<String> inward = e.inwardIssue() != null ? Stream.of(e.inwardIssue().id()) : Stream.empty();
                    return Stream.concat(outward, inward);
                })
                .toList();
    }

    private String getComponentsNames(JiraIssuesResponse issues) {
        return issues.fields().components.stream().map(JiraIssueComponent::name).collect(Collectors.joining(","));
    }
}

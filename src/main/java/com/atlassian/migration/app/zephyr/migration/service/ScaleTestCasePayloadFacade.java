package com.atlassian.migration.app.zephyr.migration.service;

import com.atlassian.migration.app.zephyr.jira.api.JiraApi;
import com.atlassian.migration.app.zephyr.jira.model.JiraIssueComponent;
import com.atlassian.migration.app.zephyr.jira.model.JiraIssuePriority;
import com.atlassian.migration.app.zephyr.jira.model.JiraIssueStatusResponse;
import com.atlassian.migration.app.zephyr.jira.model.JiraIssuesResponse;
import com.atlassian.migration.app.zephyr.scale.model.ScaleCustomFieldPayload;
import com.atlassian.migration.app.zephyr.scale.model.ScaleProjectTestCaseCustomFieldPayload;
import com.atlassian.migration.app.zephyr.scale.model.ScaleTestCaseCreationPayload;
import com.atlassian.migration.app.zephyr.scale.model.ScaleTestCaseCustomField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.stream.Stream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


public class ScaleTestCasePayloadFacade {

    private static final Logger logger = LoggerFactory.getLogger(ScaleTestCasePayloadFacade.class);

    private static final String DEFAULT_STATUS = "Draft";
    private static final String DEFAULT_PRIORITY = "Medium";

    private final JiraApi jiraApi;

    public ScaleTestCasePayloadFacade(JiraApi jiraApi) {
        this.jiraApi = jiraApi;
    }

    public ScaleTestCaseCreationPayload createTestCasePayload(JiraIssuesResponse issue, String projectKey) {
        var sanitizedPriority = sanitizePriority(issue.fields().priority);

        var sanitizedStatus = sanitizeStatus(issue.fields().status);
        // Handle null reporter
        String reporterKey = (issue.fields().reporter != null && issue.fields().reporter.key() != null) 
                             ? issue.fields().reporter.key() 
                             : null;

        Map<String,Object> scaleCustomFields = new HashMap<>();

        scaleCustomFields.put("components", getComponentsNames(issue));
        scaleCustomFields.put("squadStatus", issue.fields().status.name());
        scaleCustomFields.put("squadPriority", sanitizedPriority);
        scaleCustomFields.putAll(getCustomFieldsToMigrate(issue));

        return new ScaleTestCaseCreationPayload(
                projectKey,
                issue.fields().summary,
                convertJiraTextFormattingToHtml(issue.fields().description),
                issue.fields().labels,
                reporterKey, // Use the potentially null reporterKey
                getIssueLinksIds(issue),
                sanitizedStatus,
                scaleCustomFields
        );
    }

    private Map<String, Object> getCustomFieldsToMigrate(JiraIssuesResponse issue) {

        Map<String, Object> mappedCustomFieldAndValues = new HashMap<>();
        var customFieldsToMigrate = ScaleProjectTestCaseCustomFieldPayload.CUSTOM_FIELD_ID_TO_NAMES.keySet();

        issue.fields().customFields.entrySet().stream()
                .filter( customField ->customFieldsToMigrate.contains(customField.getKey()))
                .forEach(customFieldMetadata -> {
                    var customFieldProps = ScaleProjectTestCaseCustomFieldPayload
                            .CUSTOM_FIELD_ID_TO_NAMES
                            .get(customFieldMetadata.getKey());

                    mappedCustomFieldAndValues.put(customFieldProps.name(),
                            getCustomFieldValue(customFieldMetadata, customFieldProps));
                });

        return mappedCustomFieldAndValues;
    }

    private String getCustomFieldValue(Map.Entry<String, Object> customFieldMetadata, ScaleTestCaseCustomField customFieldProps){

        try {

            switch (customFieldProps.type()) {
                case ScaleCustomFieldPayload.SINGLE_CHOICE_SELECT_LIST -> {
                    if (customFieldMetadata.getValue() == null) {
                        return null;
                    }
                    var customFieldData = (Map<String, Object>) customFieldMetadata.getValue();
                    return (String) customFieldData.get("value");
                }
                case ScaleCustomFieldPayload.MULTI_LINE_TEXT -> {
                    if (customFieldMetadata.getValue() == null) {
                        return "";
                    }
                    return (String) customFieldMetadata.getValue();
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

    private String sanitizePriority(JiraIssuePriority squadPriority) {

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

    private List<String> getIssueLinksIds(JiraIssuesResponse issue) {
        return issue.fields().issuelinks.stream()
                .flatMap(e -> {
                    Stream<String> outward = e.outwardIssue() != null ? Stream.of(e.outwardIssue().key()) : Stream.empty();
                    Stream<String> inward = e.inwardIssue() != null ? Stream.of(e.inwardIssue().key()) : Stream.empty();
                    return Stream.concat(outward, inward);
                })
                .toList();
    }

    private String getComponentsNames(JiraIssuesResponse issues) {
        return issues.fields().components.stream().map(JiraIssueComponent::name).collect(Collectors.joining(","));
    }
}

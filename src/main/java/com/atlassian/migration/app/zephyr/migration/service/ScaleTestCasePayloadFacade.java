package com.atlassian.migration.app.zephyr.migration.service;

import com.atlassian.migration.app.zephyr.jira.api.JiraApi;
import com.atlassian.migration.app.zephyr.jira.model.JiraIssueComponent;
import com.atlassian.migration.app.zephyr.jira.model.JiraIssuePriority;
import com.atlassian.migration.app.zephyr.jira.model.JiraIssuesResponse;
import com.atlassian.migration.app.zephyr.scale.model.ScaleTestCaseCreationPayload;
import com.atlassian.migration.app.zephyr.scale.model.ScaleTestCaseCustomFieldPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;


public class ScaleTestCasePayloadFacade {

    private static final Logger logger = LoggerFactory.getLogger(ScaleTestCasePayloadFacade.class);

    private static final String DEFAULT_PRIORITY = "Medium";

    private final JiraApi jiraApi;

    public ScaleTestCasePayloadFacade(JiraApi jiraApi) {
        this.jiraApi = jiraApi;
    }

    public ScaleTestCaseCreationPayload createTestCasePayload(JiraIssuesResponse issue, String projectKey) {
        var sanitizedPriority = sanitizePriority(issue.fields().priority);
        
        // Handle null reporter
        String reporterKey = issue.fields().reporter != null ? issue.fields().reporter.key() : null;

        return new ScaleTestCaseCreationPayload(
                projectKey,
                issue.fields().summary,
                convertJiraTextFormattingToHtml(issue.fields().description),
                issue.fields().labels,
                reporterKey, // Use the potentially null reporterKey
                getIssueLinksIds(issue),
                new ScaleTestCaseCustomFieldPayload(
                        getComponentsNames(issue),
                        issue.fields().status.name(),
                        sanitizedPriority
                )
        );
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
                .filter(e -> e.outwardIssue() != null)
                .map(e -> e.outwardIssue().key())
                .toList();
    }

    private String getComponentsNames(JiraIssuesResponse issues) {
        return issues.fields().components.stream().map(JiraIssueComponent::name).collect(Collectors.joining(","));
    }
}

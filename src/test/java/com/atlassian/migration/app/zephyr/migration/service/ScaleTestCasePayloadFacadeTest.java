package com.atlassian.migration.app.zephyr.migration.service;

import com.atlassian.migration.app.zephyr.jira.api.JiraApi;
import com.atlassian.migration.app.zephyr.jira.model.*;
import com.atlassian.migration.app.zephyr.scale.model.ScaleTestCaseCreationPayload;
import com.atlassian.migration.app.zephyr.scale.model.ScaleMigrationTestCaseCustomFieldPayload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ScaleTestCasePayloadFacadeTest {
    @Mock
    private JiraApi jiraApiMock;

    private JiraIssuesResponse jiraIssuesResponseCompleteMock;

    private ScaleTestCasePayloadFacade sutScaleTestCasePayloadFacade;

    private JiraIssueFieldResponse jiraIssueFieldResponseMock;

    private final String projectKeyMock = "TEST";

    @BeforeEach
    void setup() {

        sutScaleTestCasePayloadFacade = new ScaleTestCasePayloadFacade(jiraApiMock);

        var author = new Author("self", "author", "author_key", "email", "name", false);

        jiraIssueFieldResponseMock = new JiraIssueFieldResponse();
        jiraIssueFieldResponseMock.issuetype = new JiraIssueTypeResponse(1, "type");
        jiraIssueFieldResponseMock.summary = "summary";
        jiraIssueFieldResponseMock.description = "description";
        jiraIssueFieldResponseMock.labels = Collections.emptyList();
        jiraIssueFieldResponseMock.reporter = new JiraReporterResponse(1, "REP", "reporter_name");
        jiraIssueFieldResponseMock.status = new JiraIssueStatusResponse("1", "status_name");

        var linkType = new LinkType("1", "link_name", "inward", "outward", "self");

        var statusCategory = new StatusCategory("self", 1, "key", "colorName", "name");
        var status = new Status("self", "description", "icon", "name", "1", statusCategory);
        var priority = new Priority("self", "icon", "name", "1");
        var issueType = new IssueType("self", "1", "Description", "icon", "name", false, 1);
        var fields = new Fields("summary", status, priority, issueType);

        jiraIssueFieldResponseMock.issuelinks = List.of(
                new IssueLink("1", "self",
                        linkType,
                        new RelatedIssue("1", "JIRA-99", "self", fields),
                        new RelatedIssue("2", "JIRA-100", "self", fields)));

        jiraIssueFieldResponseMock.components = List.of(
                new JiraIssueComponent(1, "component", "self"),
                new JiraIssueComponent(2, "component2", "self")
        );
        jiraIssueFieldResponseMock.priority = new JiraIssuePriority(1, "HIGH");
        jiraIssueFieldResponseMock.attachment = List.of(
                new Attachment("self", "1", "filename", author, "created", "123456", "mimetype")
        );

        jiraIssuesResponseCompleteMock = new JiraIssuesResponse("10100", "JIRA-1", jiraIssueFieldResponseMock);

    }

    @Test
    void shouldCreateTestCasePayload() throws IOException {

        when(jiraApiMock.convertJiraTextFormattingToHtml(any())).thenReturn("description");

        var expectedPayload = new ScaleTestCaseCreationPayload(
                projectKeyMock,
                "summary",
                "description",
                Collections.emptyList(),
                "REP",
                List.of("JIRA-100", "JIRA-99"),  // Updated to include both issue links),
                Map.of(
                        "components", "component,component2",
                        "squadStatus", "status_name",
                        "squadPriority", "HIGH"
                )
        );

        var receivedPayload = sutScaleTestCasePayloadFacade
                .createTestCasePayload(jiraIssuesResponseCompleteMock, projectKeyMock);

        assertEquals(expectedPayload, receivedPayload);
    }

    @Test
    void shouldCreateTestCaseWithDefaultPriorityIfPriorityIsNull() throws IOException {

        when(jiraApiMock.convertJiraTextFormattingToHtml(any())).thenReturn("description");

        jiraIssueFieldResponseMock.priority = null;

        var expectedPayload = new ScaleTestCaseCreationPayload(
                projectKeyMock,
                "summary",
                "description",
                Collections.emptyList(),
                "REP",
                List.of("JIRA-100", "JIRA-99"),  // Updated to include both issue links),
                Map.of(
                        "components", "component,component2",
                        "squadStatus", "status_name",
                        "squadPriority", "Medium"
                )
        );

        var receivedPayload = sutScaleTestCasePayloadFacade
                .createTestCasePayload(jiraIssuesResponseCompleteMock, projectKeyMock);

        assertEquals(expectedPayload, receivedPayload);
    }

    @Test
    void shouldCreateTestCaseWithEmptyPriorityIfPriorityNameIsNullOrEmpty() throws IOException {

        when(jiraApiMock.convertJiraTextFormattingToHtml(any())).thenReturn("description");

        jiraIssueFieldResponseMock.priority = new JiraIssuePriority(1, "");

        var expectedPayload = new ScaleTestCaseCreationPayload(
                projectKeyMock,
                "summary",
                "description",
                Collections.emptyList(),
                "REP",
                List.of("JIRA-100", "JIRA-99"),  // Updated to include both issue links),
                Map.of(
                        "components", "component,component2",
                        "squadStatus", "status_name",
                        "squadPriority", ""
                )
        );

        var receivedPayloadEmpty = sutScaleTestCasePayloadFacade
                .createTestCasePayload(jiraIssuesResponseCompleteMock, projectKeyMock);

        jiraIssueFieldResponseMock.priority = new JiraIssuePriority(1, null);

        var receivedPayloadNull = sutScaleTestCasePayloadFacade
                .createTestCasePayload(jiraIssuesResponseCompleteMock, projectKeyMock);

        assertEquals(expectedPayload, receivedPayloadEmpty);
        assertEquals(expectedPayload, receivedPayloadNull);
    }


    @Test
    void shouldNotCallConvertToHtmlIfDescriptionIsEmpty() throws IOException {

        var expectedPayload = new ScaleTestCaseCreationPayload(
                projectKeyMock,
                "summary",
                "",
                Collections.emptyList(),
                "REP",
                List.of("JIRA-100", "JIRA-99"),  // Updated to include both issue links),
                Map.of(
                        "components", "component,component2",
                        "squadStatus", "status_name",
                        "squadPriority", "HIGH"
                )
        );
        jiraIssueFieldResponseMock.description = "";
        var receivedPayload = sutScaleTestCasePayloadFacade
                .createTestCasePayload(jiraIssuesResponseCompleteMock, projectKeyMock);


        verify(jiraApiMock, never()).convertJiraTextFormattingToHtml(any());
        assertEquals(expectedPayload, receivedPayload);
    }
}

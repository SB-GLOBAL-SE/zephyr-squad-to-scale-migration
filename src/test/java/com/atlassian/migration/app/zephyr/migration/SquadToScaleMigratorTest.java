package com.atlassian.migration.app.zephyr.migration;

import com.atlassian.migration.app.zephyr.common.ApiConfiguration;
import com.atlassian.migration.app.zephyr.jira.api.JiraApi;
import com.atlassian.migration.app.zephyr.jira.model.*;
import com.atlassian.migration.app.zephyr.migration.execution.TestExecutionPostMigrator;
import com.atlassian.migration.app.zephyr.migration.testcase.TestCasePostMigrator;
import com.atlassian.migration.app.zephyr.scale.api.ScaleApi;
import com.atlassian.migration.app.zephyr.scale.model.GetAllProjectsResponse;
import com.atlassian.migration.app.zephyr.scale.model.GetProjectResponse;
import com.atlassian.migration.app.zephyr.scale.model.Option;
import com.atlassian.migration.app.zephyr.scale.model.ScaleTestResultCreatedPayload;
import com.atlassian.migration.app.zephyr.squad.api.SquadApi;
import com.atlassian.migration.app.zephyr.squad.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.mockito.Mockito.*;

class SquadToScaleMigratorTest {

    @Mock
    private SquadApi squadApiMock;

    @Mock
    private ScaleApi scaleApiMock;

    @Mock
    JiraApi jiraApiMock;

    @Mock
    AttachmentsMigrator attachmentsMigratorMock;

    @Mock
    TestCasePostMigrator testCasePostMigratorMock;
    @Mock
    TestExecutionPostMigrator testExecutionPostMigrator;
    @Mock
    private ApiConfiguration apiConfigurationMock;

    private MigrationConfiguration migConfigSpy;

    private SquadToScaleMigrator migratorSpy;
    private final JiraIssueFieldResponse fieldsMock = new JiraIssueFieldResponse();

    private final List<JiraIssuesResponse> issuesMock = new ArrayList<>();

    private FetchSquadExecutionParsedResponse emptyExecutionsMock;

    @BeforeEach
    void setup() throws IOException {
        MockitoAnnotations.openMocks(this);

        migConfigSpy = spy(new MigrationConfiguration(apiConfigurationMock,
                5,
                "CYCLE",
                "attachments_mapped.csv",
                "test_cases_Mapped.csv",
                "test_executions_Mapped.csv",
                "postgres",
                "/home/ubuntu"));

        migratorSpy = spy(new SquadToScaleMigrator(jiraApiMock, squadApiMock, scaleApiMock, attachmentsMigratorMock,
                testCasePostMigratorMock,
                testExecutionPostMigrator,
                migConfigSpy));

        when(jiraApiMock.getProjectByKey(any())).thenReturn(new GetProjectResponse("PROJECT", "1", Collections.emptyList()));
        doNothing().when(attachmentsMigratorMock).export(any(), any());

        fieldsMock.summary = "summary";
        fieldsMock.description = "desc";
        fieldsMock.labels = Collections.emptyList();
        fieldsMock.reporter = new JiraReporterResponse(1, "reporterKey", "reporter");
        fieldsMock.status = new JiraIssueStatusResponse("1", "status");
        fieldsMock.priority = new JiraIssuePriority(1, "priority");
        fieldsMock.issuelinks = Collections.emptyList();
        fieldsMock.components = Collections.emptyList();

        issuesMock.addAll(List.of(
                new JiraIssuesResponse("1", "KEY-1", fieldsMock),
                new JiraIssuesResponse("2", "KEY-2", fieldsMock),
                new JiraIssuesResponse("3", "KEY-3", fieldsMock),
                new JiraIssuesResponse("4", "KEY-4", fieldsMock),
                new JiraIssuesResponse("5", "KEY-5", fieldsMock)

        ));

        emptyExecutionsMock = new FetchSquadExecutionParsedResponse(Collections.emptyMap(),
                "10100",
                0,
                0,
                false,
                false,
                Collections.emptyList());

    }

    @Nested
    class CheckMigrationTriggers {
        @Test
        void shouldCallRunMigrationPerNumberOfProjects() throws IOException, ExecutionException, InterruptedException {

            var projectsMock = List.of(
                    new Option("true", "label", "PROJECT", "PROJECT-1"),
                    new Option("true", "label", "PROJECT", "PROJECT-2"),
                    new Option("true", "label", "PROJECT", "PROJECT-3")
            );

            var allProjects = new GetAllProjectsResponse(projectsMock);

            doReturn(allProjects).when(squadApiMock).getAllProjects();
            doReturn(new GetProjectResponse("PROJECT-1", "1", Collections.emptyList())).when(jiraApiMock).getProjectById(any());

            doNothing().when(migratorSpy).runMigration(any());
            migratorSpy.getProjectListAndRunMigration();

            verify(migratorSpy, times(projectsMock.size())).runMigration(any());

        }

        @Test
        void shouldSkipProjectIfThereAreNoIssuesWhenRunMigration() throws IOException, ExecutionException, InterruptedException {

            when(jiraApiMock.fetchTotalIssuesByProjectName(any())).thenReturn(0);

            migratorSpy.runMigration("PROJECT-1");

            verify(scaleApiMock, never()).enableProject(any());
        }

        @Test
        void shouldCallProcessPageBasedOnTotalIssuesFetched() throws IOException, ExecutionException, InterruptedException {

            var totalIssuesMock = 10;
            var interactionsExpected = totalIssuesMock / migConfigSpy.pageSteps();
            var projectResponseMock = new GetProjectResponse("PROJECT", "10000", null);

            when(jiraApiMock.fetchTotalIssuesByProjectName(any())).thenReturn(totalIssuesMock);

            when(jiraApiMock.fetchIssuesOrderedByCreatedDate(any(), any(), any())).thenReturn(Collections.emptyList());
            when(jiraApiMock.getProject(any())).thenReturn(projectResponseMock);
            migratorSpy.runMigration("PROJECT-1");

            //We are using a public method to count interactions. Each processPage calls attachments export once
            verify(attachmentsMigratorMock, times(interactionsExpected)).export(any(), any());

        }
    }

    @Nested
    class CheckNumberOfInteractionsForEachEntityBasedOnNumberOfIssues {

        @BeforeEach
        void setup() throws IOException {

            when(jiraApiMock.fetchTotalIssuesByProjectName(any())).thenReturn(issuesMock.size());

            when(jiraApiMock.fetchIssuesOrderedByCreatedDate(any(), any(), any())).thenReturn(issuesMock);

            when(scaleApiMock.createTestCases(any())).thenReturn("KEY-1");

            doNothing().when(scaleApiMock).updateTestStep(any(), any());

        }


        @Test
        void shouldCreateOneTestCasePerIssue() throws IOException, ExecutionException, InterruptedException {
            var projectResponseMock = new GetProjectResponse("PROJECT", "10000", null);

            when(squadApiMock.fetchLatestTestStepByTestCaseId(any())).thenReturn(new FetchSquadTestStepResponse(Collections.emptyList()));

            when(squadApiMock.fetchLatestExecutionByIssueId(any())).thenReturn(emptyExecutionsMock);

            when(jiraApiMock.getProject(any())).thenReturn(projectResponseMock);

            migratorSpy.runMigration("PROJECT");

            verify(scaleApiMock, times(issuesMock.size())).createTestCases(any());

        }

        @Test
        void shouldCallUpdateTestStepOncePerIssue() throws IOException, ExecutionException, InterruptedException {

            var stepBeanCollectionMock = List.of(
                    new SquadTestStepResponse("1", "order", "step", "data", "result", Collections.emptyList()),
                    new SquadTestStepResponse("2", "order", "step", "data", "result", Collections.emptyList()),
                    new SquadTestStepResponse("3", "order", "step", "data", "result", Collections.emptyList())
            );

            var fetchSquadTestStepResponseMock = new FetchSquadTestStepResponse(stepBeanCollectionMock);
            var projectResponseMock = new GetProjectResponse("PROJECT", "10000", null);

            when(squadApiMock.fetchLatestTestStepByTestCaseId(any())).thenReturn(fetchSquadTestStepResponseMock);

            when(squadApiMock.fetchLatestExecutionByIssueId(any())).thenReturn(emptyExecutionsMock);

            when(jiraApiMock.getProject(any())).thenReturn(projectResponseMock);
            migratorSpy.runMigration("PROJECT");

            verify(scaleApiMock, times(issuesMock.size())).updateTestStep(any(), any());

        }

        @Test
        void shouldCallCreateTestResultsOncePerExecution() throws IOException, ExecutionException, InterruptedException {

            when(squadApiMock.fetchLatestTestStepByTestCaseId(any())).thenReturn(new FetchSquadTestStepResponse(Collections.emptyList()));

            var statusMock = new SquadExecutionTypeResponse(1, "wip");

            var executionsMock = List.of(
                    new SquadExecutionItemParsedResponse("1",
                            statusMock, "createdOn",null, null,
                            "versionName", "comment", "executedOn",
                            "assignedTo", "assignedTo", "assigneeTo", "CYCLE-1", "folder",
                            List.of(new SquadExecutionDefectResponse("issueKey"))),
                    new SquadExecutionItemParsedResponse("2",
                            statusMock, "createdOn", null, null,
                            "versionName", "comment", "executedOn",
                            "assignedTo", "assignedTo", "assigneeTo", "CYCLE-2", "folder",
                            List.of(new SquadExecutionDefectResponse("issueKey"))),
                    new SquadExecutionItemParsedResponse("3",
                            statusMock, "createdOn", null, null,
                            "versionName", "comment", "executedOn",
                            "assignedTo", "assignedTo", "assigneeTo", "CYCLE-3", "folder",
                            List.of(new SquadExecutionDefectResponse("issueKey")))
            );

            var fetchSquadExecutionParsedResponseMock = new FetchSquadExecutionParsedResponse(Collections.emptyMap(),
                    "10100",
                    0,
                    0,
                    false,
                    false,
                    executionsMock);
            var projectResponseMock = new GetProjectResponse("PROJECT", "10000", null);

            when(squadApiMock.fetchLatestExecutionByIssueId(any())).thenReturn(fetchSquadExecutionParsedResponseMock);
            when(scaleApiMock.createTestExecution(any(), any())).thenReturn(new ScaleTestResultCreatedPayload("1"));
            when(jiraApiMock.getProject(any())).thenReturn(projectResponseMock);
            migratorSpy.runMigration("PROJECT");

            //each time a TestCase is processed, the executionsMock is returned, so we process executionsMock times the number of Test Cases
            verify(scaleApiMock, times(issuesMock.size() * executionsMock.size())).createTestExecution(any(), any());

        }

    }

}
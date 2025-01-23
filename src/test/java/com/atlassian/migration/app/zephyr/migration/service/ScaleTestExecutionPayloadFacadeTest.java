package com.atlassian.migration.app.zephyr.migration.service;

import com.atlassian.migration.app.zephyr.jira.api.JiraApi;
import com.atlassian.migration.app.zephyr.jira.model.AssignableUserResponse;
import com.atlassian.migration.app.zephyr.scale.model.ScaleExecutionCreationPayload;
import com.atlassian.migration.app.zephyr.scale.model.ScaleExecutionStepPayload;
import com.atlassian.migration.app.zephyr.scale.model.ScaleMigrationExecutionCustomFieldPayload;
import com.atlassian.migration.app.zephyr.squad.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ScaleTestExecutionPayloadFacadeTest {

    @Mock
    private JiraApi jiraApiMock;

    private final String testKeyMock = "TEST";
    private final String scaleTestCaseKeyMock = "TEST-1";

    private ScaleTestExecutionPayloadFacade sutTestExecFacade;


    @BeforeEach
    void setup() {
        sutTestExecFacade = new ScaleTestExecutionPayloadFacade(jiraApiMock);
    }

    @Test
    void shouldCreateScaleTestExecutionPayload() throws IOException {
        var squadExecutionPayloadMock = new SquadExecutionItemParsedResponse(
                "2",
                new SquadExecutionTypeResponse(1, "Pass"),
                "createdOn",
                "author",
                "author",
                "version",
                "html_content",
                "executed",
                "assignee",
                "assignee",
                "assignee",
                "cycle",
                "folder",
                List.of(new SquadExecutionDefectResponse("issueKey")));

        var expectedScaleExecutionPayload = new ScaleExecutionCreationPayload(
                "Pass",
                scaleTestCaseKeyMock,
                "author",
                "html_content",
                "version",
                List.of("issueKey"),
                List.of(new ScaleExecutionStepPayload(0, "status", "comment")),
                new ScaleMigrationExecutionCustomFieldPayload(
                        "executed",
                        "assignee",
                        "version",
                        "cycle",
                        "folder"
                )
        );

        var createdByUserMock = new AssignableUserResponse(
                "key", "author", "email", "author"
        );

        var assigneeUserMock = new AssignableUserResponse(
                "key", "assignee", "email", "assignee");

        var testExecutionStepResponseMock = new FetchSquadExecutionStepParsedResponse(List.of(new SquadExecutionStepParsedResponse(1, 0, new SquadExecutionTypeResponse(1, "status"), "comment", 0,null)));
        when(jiraApiMock.fetchAssignableUserByUsernameAndProject("author", testKeyMock))
                .thenReturn(List.of(createdByUserMock));

        when(jiraApiMock.fetchAssignableUserByUsernameAndProject("assignee", testKeyMock))
                .thenReturn(List.of(assigneeUserMock));


        var receivedPayload = sutTestExecFacade.buildPayload(squadExecutionPayloadMock, scaleTestCaseKeyMock, testKeyMock, testExecutionStepResponseMock);

        assertEquals(expectedScaleExecutionPayload, receivedPayload);

    }

    @Test
    void shouldTranslateSquadStatusToScaleStatus() throws IOException {
        var squadExecutionPayloadWipMock = new SquadExecutionItemParsedResponse(
                "2",
                new SquadExecutionTypeResponse(1, "wip"),
                "createdOn",
                "author",
                "author",
                "version",
                "html_content",
                "executed",
                "assignee",
                "assignee",
                "assignee",
                "cycle",
                "folder",
                List.of(new SquadExecutionDefectResponse("issueKey")));

        var squadExecutionPayloadUnexecutedMock = new SquadExecutionItemParsedResponse(
                "2",
                new SquadExecutionTypeResponse(1, "unexecuted"),
                "createdOn",
                "author",
                "author",
                "version",
                "html_content",
                "executed",
                "assignee",
                "assignee",
                "assignee",
                "cycle",
                "folder",
                List.of(new SquadExecutionDefectResponse("issueKey")));

        var expectedScaleExecutionPayloadWip = new ScaleExecutionCreationPayload(
                "In Progress",
                scaleTestCaseKeyMock,
                "author",
                "html_content",
                "version",
                List.of("issueKey"),
                List.of(new ScaleExecutionStepPayload(0, "status", "comment")),
                new ScaleMigrationExecutionCustomFieldPayload(
                        "executed",
                        "assignee",
                        "version",
                        "cycle",
                        "folder"
                )
        );

        var expectedScaleExecutionPayloadUnexecuted = new ScaleExecutionCreationPayload(
                "Not Executed",
                scaleTestCaseKeyMock,
                "author",
                "html_content",
                "version",
                List.of("issueKey"),
                List.of(new ScaleExecutionStepPayload(0, "status", "comment")),
                new ScaleMigrationExecutionCustomFieldPayload(
                        "executed",
                        "assignee",
                        "version",
                        "cycle",
                        "folder"
                )
        );

        var createdByUserMock = new AssignableUserResponse(
                "key", "author", "email", "author"
        );

        var assigneeUserMock = new AssignableUserResponse(
                "key", "assignee", "email", "assignee");

        var testExecutionStepResponseMock = new FetchSquadExecutionStepParsedResponse(List.of(new SquadExecutionStepParsedResponse(1, 0, new SquadExecutionTypeResponse(1, "status"), "comment", 1,null)));

        when(jiraApiMock.fetchAssignableUserByUsernameAndProject("author", testKeyMock))
                .thenReturn(List.of(createdByUserMock));

        when(jiraApiMock.fetchAssignableUserByUsernameAndProject("assignee", testKeyMock))
                .thenReturn(List.of(assigneeUserMock));

        var receivedPayloadWip = sutTestExecFacade
                .buildPayload(squadExecutionPayloadWipMock, scaleTestCaseKeyMock, testKeyMock, testExecutionStepResponseMock);

        assertEquals(expectedScaleExecutionPayloadWip, receivedPayloadWip);


        var receivedPayloadUnexecuted = sutTestExecFacade
                .buildPayload(squadExecutionPayloadUnexecutedMock, scaleTestCaseKeyMock, testKeyMock, testExecutionStepResponseMock);

        assertEquals(expectedScaleExecutionPayloadUnexecuted, receivedPayloadUnexecuted);

    }

    @Test
    void shouldTranslateSquadVersionToScaleVersion() throws IOException {
        var squadExecutionPayloadMock = new SquadExecutionItemParsedResponse(
                "2",
                new SquadExecutionTypeResponse(1, "Pass"),
                "createdOn",
                "author",
                "author",
                "unscheduled",
                "html_content",
                "executed",
                "assignee",
                "assignee",
                "assignee",
                "cycle",
                "folder",
                List.of(new SquadExecutionDefectResponse("issueKey")));

        var expectedScaleExecutionPayload = new ScaleExecutionCreationPayload(
                "Pass",
                scaleTestCaseKeyMock,
                "author",
                "html_content",
                null,
                List.of("issueKey"),
                List.of(new ScaleExecutionStepPayload(0, "status", "comment")),
                new ScaleMigrationExecutionCustomFieldPayload(
                        "executed",
                        "assignee",
                        null,
                        "cycle",
                        "folder"
                )
        );

        var createdByUserMock = new AssignableUserResponse(
                "key", "author", "email", "author"
        );

        var assigneeUserMock = new AssignableUserResponse(
                "key", "assignee", "email", "assignee");

        var testExecutionStepResponseMock = new FetchSquadExecutionStepParsedResponse(List.of(new SquadExecutionStepParsedResponse(1, 0, new SquadExecutionTypeResponse(1, "status"), "comment", 0, null)));

        when(jiraApiMock.fetchAssignableUserByUsernameAndProject("author", testKeyMock))
                .thenReturn(List.of(createdByUserMock));

        when(jiraApiMock.fetchAssignableUserByUsernameAndProject("assignee", testKeyMock))
                .thenReturn(List.of(assigneeUserMock));

        var receivedPayload = sutTestExecFacade.buildPayload(squadExecutionPayloadMock, scaleTestCaseKeyMock, testKeyMock, testExecutionStepResponseMock);

        assertEquals(expectedScaleExecutionPayload, receivedPayload);

    }

    @Test
    void shouldSetAssigneeToNoneIfUnassignable() throws IOException {
        var squadExecutionPayloadMock = new SquadExecutionItemParsedResponse(
                "2",
                new SquadExecutionTypeResponse(1, "Pass"),
                "createdOn",
                "author",
                "author",
                "version",
                "html_content",
                "executed",
                null,
                "",
                "unassignable",
                "cycle",
                "folder",
                List.of(new SquadExecutionDefectResponse("issueKey")));

        var expectedScaleExecutionPayload = new ScaleExecutionCreationPayload(
                "Pass",
                scaleTestCaseKeyMock,
                "author",
                "html_content",
                "version",
                List.of("issueKey"),
                List.of(new ScaleExecutionStepPayload(0, "status", "comment")),
                new ScaleMigrationExecutionCustomFieldPayload(
                        "executed",
                        "None",
                        "version",
                        "cycle",
                        "folder"
                )
        );

        var createdByUserMock = new AssignableUserResponse(
                "key", "author", "email", "author"
        );

        var testExecutionStepResponseMock = new FetchSquadExecutionStepParsedResponse(List.of(new SquadExecutionStepParsedResponse(1, 0, new SquadExecutionTypeResponse(1, "status"), "comment", 0, null)));

        when(jiraApiMock.fetchAssignableUserByUsernameAndProject("author", testKeyMock))
                .thenReturn(List.of(createdByUserMock));

        when(jiraApiMock.fetchAssignableUserByUsernameAndProject("unassignable", testKeyMock))
                .thenReturn(Collections.emptyList());

        var receivedPayload = sutTestExecFacade.buildPayload(squadExecutionPayloadMock, scaleTestCaseKeyMock, testKeyMock, testExecutionStepResponseMock);

        assertEquals(expectedScaleExecutionPayload, receivedPayload);

    }

    @Test
    void shouldSetAssigneeToNoneIfInactive() throws IOException {
        var squadExecutionPayloadMock = new SquadExecutionItemParsedResponse(
                "2",
                new SquadExecutionTypeResponse(1, "Pass"),
                "createdOn",
                "author",
                "author",
                "version",
                "html_content",
                "executed",
                "assignee",
                "assignee (Inactive)",
                "unassignable",
                "cycle",
                "folder",
                List.of(new SquadExecutionDefectResponse("issueKey")));

        var expectedScaleExecutionPayload = new ScaleExecutionCreationPayload(
                "Pass",
                scaleTestCaseKeyMock,
                "author",
                "html_content",
                "version",
                List.of("issueKey"),
                List.of(new ScaleExecutionStepPayload(0, "status", "comment")),
                new ScaleMigrationExecutionCustomFieldPayload(
                        "executed",
                        "None",
                        "version",
                        "cycle",
                        "folder"
                )
        );

        var createdByUserMock = new AssignableUserResponse(
                "key", "author", "email", "author"
        );

        var testExecutionStepResponseMock = new FetchSquadExecutionStepParsedResponse(List.of(new SquadExecutionStepParsedResponse(1, 0, new SquadExecutionTypeResponse(1, "status"), "comment", 0, null)));

        when(jiraApiMock.fetchAssignableUserByUsernameAndProject("author", testKeyMock))
                .thenReturn(List.of(createdByUserMock));

        var receivedPayload = sutTestExecFacade.buildPayload(squadExecutionPayloadMock, scaleTestCaseKeyMock, testKeyMock, testExecutionStepResponseMock);

        assertEquals(expectedScaleExecutionPayload, receivedPayload);

    }


    @Test
    void shouldUseCacheForAssignableUserAlreadyChecked() throws IOException {
        var squadExecutionPayloadMock = new SquadExecutionItemParsedResponse(
                "2",
                new SquadExecutionTypeResponse(1, "Pass"),
                "createdOn",
                "author",
                "author",
                "version",
                "html_content",
                "executed",
                "assignee",
                "assignee",
                "assignee",
                "cycle",
                "folder",
                List.of(new SquadExecutionDefectResponse("issueKey")));

        var createdByUserMock = new AssignableUserResponse(
                "key", "author", "email", "author"
        );

        var assigneeUserMock = new AssignableUserResponse(
                "key", "assignee", "email", "assignee");

        var testExecutionStepResponseMock = new FetchSquadExecutionStepParsedResponse(List.of(new SquadExecutionStepParsedResponse(1, 0, new SquadExecutionTypeResponse(1, "status"), "comment", 0,null)));

        when(jiraApiMock.fetchAssignableUserByUsernameAndProject("author", testKeyMock))
                .thenReturn(List.of(createdByUserMock));

        when(jiraApiMock.fetchAssignableUserByUsernameAndProject("assignee", testKeyMock))
                .thenReturn(List.of(assigneeUserMock));

        sutTestExecFacade.buildPayload(squadExecutionPayloadMock, scaleTestCaseKeyMock, testKeyMock, testExecutionStepResponseMock);

        sutTestExecFacade.buildPayload(squadExecutionPayloadMock, scaleTestCaseKeyMock, testKeyMock, testExecutionStepResponseMock);

        verify(jiraApiMock, times(2))
                .fetchAssignableUserByUsernameAndProject(any(), any());

        verify(jiraApiMock, times(1))
                .fetchAssignableUserByUsernameAndProject("author", testKeyMock);

        verify(jiraApiMock, times(1))
                .fetchAssignableUserByUsernameAndProject("assignee", testKeyMock);
    }


    @Test
    void shouldUseCacheForUnassignableUserAlreadyChecked() throws IOException {
        var squadExecutionPayloadMock = new SquadExecutionItemParsedResponse(
                "2",
                new SquadExecutionTypeResponse(1, "Pass"),
                "createdOn",
                "author",
                "author",
                "version",
                "html_content",
                "executed",
                "assignee",
                "assignee",
                "assignee",
                "cycle",
                "folder",
                List.of(new SquadExecutionDefectResponse("issueKey")));

        var createdByUserMock = new AssignableUserResponse(
                "key", "author", "email", "author"
        );

        var testExecutionStepResponseMock = new FetchSquadExecutionStepParsedResponse(List.of(new SquadExecutionStepParsedResponse(1, 0, new SquadExecutionTypeResponse(1, "status"), "comment", 0,null)));

        when(jiraApiMock.fetchAssignableUserByUsernameAndProject("author", testKeyMock))
                .thenReturn(List.of(createdByUserMock));

        when(jiraApiMock.fetchAssignableUserByUsernameAndProject("assignee", testKeyMock))
                .thenReturn(Collections.emptyList());

        sutTestExecFacade.buildPayload(squadExecutionPayloadMock, scaleTestCaseKeyMock, testKeyMock, testExecutionStepResponseMock);

        sutTestExecFacade.buildPayload(squadExecutionPayloadMock, scaleTestCaseKeyMock, testKeyMock, testExecutionStepResponseMock);

        verify(jiraApiMock, times(2))
                .fetchAssignableUserByUsernameAndProject(any(), any());

        verify(jiraApiMock, times(1))
                .fetchAssignableUserByUsernameAndProject("author", testKeyMock);

        verify(jiraApiMock, times(1))
                .fetchAssignableUserByUsernameAndProject("assignee", testKeyMock);
    }


}

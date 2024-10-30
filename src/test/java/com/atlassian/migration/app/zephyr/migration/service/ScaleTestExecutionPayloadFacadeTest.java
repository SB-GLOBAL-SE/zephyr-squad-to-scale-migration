package com.atlassian.migration.app.zephyr.migration.service;

import com.atlassian.migration.app.zephyr.jira.api.JiraApi;
import com.atlassian.migration.app.zephyr.jira.model.AssignableUserResponse;
import com.atlassian.migration.app.zephyr.scale.model.ScaleExecutionCreationPayload;
import com.atlassian.migration.app.zephyr.scale.model.ScaleMigrationExecutionCustomFieldPayload;
import com.atlassian.migration.app.zephyr.squad.model.SquadExecutionItemParsedResponse;
import com.atlassian.migration.app.zephyr.squad.model.SquadExecutionTypeResponse;
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
                "author",
                "author",
                "version",
                "html_content",
                "executed",
                "assignee",
                "assignee",
                "assignee",
                "cycle",
                "folder");

        var expectedScaleExecutionPayload = new ScaleExecutionCreationPayload(
                "Pass",
                scaleTestCaseKeyMock,
                "author",
                "html_content",
                "version",
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

        when(jiraApiMock.fetchAssignableUserByUsernameAndProject("author", testKeyMock))
                .thenReturn(List.of(createdByUserMock));

        when(jiraApiMock.fetchAssignableUserByUsernameAndProject("assignee", testKeyMock))
                .thenReturn(List.of(assigneeUserMock));

        var receivedPayload = sutTestExecFacade.buildPayload(squadExecutionPayloadMock, scaleTestCaseKeyMock, testKeyMock);

        assertEquals(expectedScaleExecutionPayload, receivedPayload);

    }

    @Test
    void shouldTranslateSquadStatusToScaleStatus() throws IOException {
        var squadExecutionPayloadWipMock = new SquadExecutionItemParsedResponse(
                "2",
                new SquadExecutionTypeResponse(1, "wip"),
                "author",
                "author",
                "version",
                "html_content",
                "executed",
                "assignee",
                "assignee",
                "assignee",
                "cycle",
                "folder");

        var squadExecutionPayloadUnexecutedMock = new SquadExecutionItemParsedResponse(
                "2",
                new SquadExecutionTypeResponse(1, "unexecuted"),
                "author",
                "author",
                "version",
                "html_content",
                "executed",
                "assignee",
                "assignee",
                "assignee",
                "cycle",
                "folder");

        var expectedScaleExecutionPayloadWip = new ScaleExecutionCreationPayload(
                "In Progress",
                scaleTestCaseKeyMock,
                "author",
                "html_content",
                "version",
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

        when(jiraApiMock.fetchAssignableUserByUsernameAndProject("author", testKeyMock))
                .thenReturn(List.of(createdByUserMock));

        when(jiraApiMock.fetchAssignableUserByUsernameAndProject("assignee", testKeyMock))
                .thenReturn(List.of(assigneeUserMock));

        var receivedPayloadWip = sutTestExecFacade
                .buildPayload(squadExecutionPayloadWipMock, scaleTestCaseKeyMock, testKeyMock);

        assertEquals(expectedScaleExecutionPayloadWip, receivedPayloadWip);


        var receivedPayloadUnexecuted = sutTestExecFacade
                .buildPayload(squadExecutionPayloadUnexecutedMock, scaleTestCaseKeyMock, testKeyMock);

        assertEquals(expectedScaleExecutionPayloadUnexecuted, receivedPayloadUnexecuted);

    }

    @Test
    void shouldTranslateSquadVersionToScaleVersion() throws IOException {
        var squadExecutionPayloadMock = new SquadExecutionItemParsedResponse(
                "2",
                new SquadExecutionTypeResponse(1, "Pass"),
                "author",
                "author",
                "unscheduled",
                "html_content",
                "executed",
                "assignee",
                "assignee",
                "assignee",
                "cycle",
                "folder");

        var expectedScaleExecutionPayload = new ScaleExecutionCreationPayload(
                "Pass",
                scaleTestCaseKeyMock,
                "author",
                "html_content",
                null,
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

        when(jiraApiMock.fetchAssignableUserByUsernameAndProject("author", testKeyMock))
                .thenReturn(List.of(createdByUserMock));

        when(jiraApiMock.fetchAssignableUserByUsernameAndProject("assignee", testKeyMock))
                .thenReturn(List.of(assigneeUserMock));

        var receivedPayload = sutTestExecFacade.buildPayload(squadExecutionPayloadMock, scaleTestCaseKeyMock, testKeyMock);

        assertEquals(expectedScaleExecutionPayload, receivedPayload);

    }

    @Test
    void shouldSetAssigneeToNoneIfUnassignable() throws IOException {
        var squadExecutionPayloadMock = new SquadExecutionItemParsedResponse(
                "2",
                new SquadExecutionTypeResponse(1, "Pass"),
                "author",
                "author",
                "version",
                "html_content",
                "executed",
                null,
                "",
                "unassignable",
                "cycle",
                "folder");

        var expectedScaleExecutionPayload = new ScaleExecutionCreationPayload(
                "Pass",
                scaleTestCaseKeyMock,
                "author",
                "html_content",
                "version",
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

        when(jiraApiMock.fetchAssignableUserByUsernameAndProject("author", testKeyMock))
                .thenReturn(List.of(createdByUserMock));

        when(jiraApiMock.fetchAssignableUserByUsernameAndProject("unassignable", testKeyMock))
                .thenReturn(Collections.emptyList());

        var receivedPayload = sutTestExecFacade.buildPayload(squadExecutionPayloadMock, scaleTestCaseKeyMock, testKeyMock);

        assertEquals(expectedScaleExecutionPayload, receivedPayload);

    }

    @Test
    void shouldSetAssigneeToNoneIfInactive() throws IOException {
        var squadExecutionPayloadMock = new SquadExecutionItemParsedResponse(
                "2",
                new SquadExecutionTypeResponse(1, "Pass"),
                "author",
                "author",
                "version",
                "html_content",
                "executed",
                "assignee",
                "assignee (Inactive)",
                "unassignable",
                "cycle",
                "folder");

        var expectedScaleExecutionPayload = new ScaleExecutionCreationPayload(
                "Pass",
                scaleTestCaseKeyMock,
                "author",
                "html_content",
                "version",
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

        when(jiraApiMock.fetchAssignableUserByUsernameAndProject("author", testKeyMock))
                .thenReturn(List.of(createdByUserMock));

        var receivedPayload = sutTestExecFacade.buildPayload(squadExecutionPayloadMock, scaleTestCaseKeyMock, testKeyMock);

        assertEquals(expectedScaleExecutionPayload, receivedPayload);

    }


    @Test
    void shouldUseCacheForAssignableUserAlreadyChecked() throws IOException {
        var squadExecutionPayloadMock = new SquadExecutionItemParsedResponse(
                "2",
                new SquadExecutionTypeResponse(1, "Pass"),
                "author",
                "author",
                "version",
                "html_content",
                "executed",
                "assignee",
                "assignee",
                "assignee",
                "cycle",
                "folder");

        var createdByUserMock = new AssignableUserResponse(
                "key", "author", "email", "author"
        );

        var assigneeUserMock = new AssignableUserResponse(
                "key", "assignee", "email", "assignee");

        when(jiraApiMock.fetchAssignableUserByUsernameAndProject("author", testKeyMock))
                .thenReturn(List.of(createdByUserMock));

        when(jiraApiMock.fetchAssignableUserByUsernameAndProject("assignee", testKeyMock))
                .thenReturn(List.of(assigneeUserMock));

        sutTestExecFacade.buildPayload(squadExecutionPayloadMock, scaleTestCaseKeyMock, testKeyMock);

        sutTestExecFacade.buildPayload(squadExecutionPayloadMock, scaleTestCaseKeyMock, testKeyMock);

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
                "author",
                "author",
                "version",
                "html_content",
                "executed",
                "assignee",
                "assignee",
                "assignee",
                "cycle",
                "folder");

        var createdByUserMock = new AssignableUserResponse(
                "key", "author", "email", "author"
        );

        when(jiraApiMock.fetchAssignableUserByUsernameAndProject("author", testKeyMock))
                .thenReturn(List.of(createdByUserMock));

        when(jiraApiMock.fetchAssignableUserByUsernameAndProject("assignee", testKeyMock))
                .thenReturn(Collections.emptyList());

        sutTestExecFacade.buildPayload(squadExecutionPayloadMock, scaleTestCaseKeyMock, testKeyMock);

        sutTestExecFacade.buildPayload(squadExecutionPayloadMock, scaleTestCaseKeyMock, testKeyMock);

        verify(jiraApiMock, times(2))
                .fetchAssignableUserByUsernameAndProject(any(), any());

        verify(jiraApiMock, times(1))
                .fetchAssignableUserByUsernameAndProject("author", testKeyMock);

        verify(jiraApiMock, times(1))
                .fetchAssignableUserByUsernameAndProject("assignee", testKeyMock);
    }


}

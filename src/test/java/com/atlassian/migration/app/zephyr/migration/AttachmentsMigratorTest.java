package com.atlassian.migration.app.zephyr.migration;

import com.atlassian.migration.app.zephyr.jira.api.JiraApi;
import com.atlassian.migration.app.zephyr.migration.model.*;
import com.atlassian.migration.app.zephyr.scale.api.ScaleApi;
import com.atlassian.migration.app.zephyr.scale.model.GetProjectResponse;
import com.atlassian.migration.app.zephyr.scale.model.ScaleGETStepItemPayload;
import com.atlassian.migration.app.zephyr.scale.model.ScaleGETStepsPayload;
import com.atlassian.migration.app.zephyr.scale.model.SquadGETStepItemPayload;
import com.atlassian.migration.app.zephyr.squad.api.SquadApi;
import com.atlassian.migration.app.zephyr.squad.model.FetchSquadAttachmentResponse;
import com.atlassian.migration.app.zephyr.squad.model.SquadAttachmentItemResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AttachmentsMigratorTest {

    @Mock
    private SquadApi squadApiMock;

    @Mock
    private ScaleApi scaleApiMock;

    @Mock
    private JiraApi jiraApiMock;

    @Mock
    private AttachmentsCsvExporter attachmentsCsvExporterMock;

    @Mock
    private AttachmentsCopier attachmentsCopier;
    @Mock
    private DriverManagerDataSource driverManagerDataSourceMock;

    private AttachmentsMigrator attachmentsMigratorSpy;

    private SquadToScaleEntitiesMap squadToScaleEntitiesMapMock;

    private final String projectKey = "PROJECT";

    private final SquadToScaleTestCaseMap testCaseMap = new SquadToScaleTestCaseMap();

    private final SquadToScaleTestStepMap testStepMap = new SquadToScaleTestStepMap();

    private final SquadToScaleTestExecutionMap testExecMap = new SquadToScaleTestExecutionMap();

    private final List<SquadAttachmentItemResponse> squadAttachmentsMockList = new ArrayList<>();

    @BeforeEach
    void setup() throws IOException, URISyntaxException {

        MockitoAnnotations.openMocks(this);

        attachmentsMigratorSpy = new AttachmentsMigrator(
                jiraApiMock,
                scaleApiMock,
                squadApiMock,
                driverManagerDataSourceMock,
                attachmentsCsvExporterMock,
                attachmentsCopier);

        testCaseMap.put(
                new SquadToScaleTestCaseMap.TestCaseMapKey("1", "SQUAD-1", "Squad created by", "squad created date",
                        "squad modified by", "squad modified date"), "SCALE-1"
        );

        doNothing().when(attachmentsCopier).copyAttachments(any(), any(), any());

        when(driverManagerDataSourceMock.getUrl()).thenReturn("jdbc:postgresql://localhost:5432/jira");

        var squadAttachmentItemResponseMock = new SquadAttachmentItemResponse(
                "name",
                "dataCreated",
                "comment",
                "123456",
                "icon",
                "author",
                "fileIcon",
                "comment",
                "1"
        );

        testStepMap.put("1", Map.of(
                new SquadToScaleTestStepMap.TestStepMapKey("1", "1"), Collections.emptyList()
        ));
        testStepMap.put("2", Map.of(
                new SquadToScaleTestStepMap.TestStepMapKey("1", "1"), Collections.emptyList()
        ));
        testStepMap.put("3", Map.of(
                new SquadToScaleTestStepMap.TestStepMapKey("1", "1"), Collections.emptyList()
        ));

        squadToScaleEntitiesMapMock = new SquadToScaleEntitiesMap(
                new SquadToScaleTestCaseMap(),
                testStepMap,
                new SquadToScaleTestExecutionMap(),
                new SquadToScaleExecutionStepMap());

        when(jiraApiMock.getProjectByKeyWithHistoricalKeys(projectKey)).thenReturn(new GetProjectResponse("PROJECT", "1", Collections.emptyList()));

        var stepItemPayloadMock = new SquadGETStepItemPayload();

        stepItemPayloadMock.steps = List.of(
                new ScaleGETStepItemPayload("desc", "data", "res", "1", "1")
        );

        var scaleGETStepsPayloadMock = new ScaleGETStepsPayload("KEY-1", "PROJECT", stepItemPayloadMock);

        when(scaleApiMock.fetchTestStepsFromTestCaseKey(any())).thenReturn(scaleGETStepsPayloadMock);

        doNothing().when(attachmentsCsvExporterMock).dump(any(), any(), any());
    }

    @Test
    void shouldCallCsvExportDumpOnlyOnce() throws IOException, URISyntaxException {

        attachmentsMigratorSpy.export(squadToScaleEntitiesMapMock, projectKey);

        verify(attachmentsCsvExporterMock, times(1)).dump(any(), any(), any());

    }

    @Test
    void shouldCallCpAttachmentsOnlyOnce() throws IOException {

        attachmentsMigratorSpy.export(squadToScaleEntitiesMapMock, projectKey);

        verify(attachmentsCopier, times(1)).copyAttachments(any(), any(), any());
    }

    @Test
    void shouldFetchTestStepFromScaleOncePerKey() throws IOException {

        attachmentsMigratorSpy.export(squadToScaleEntitiesMapMock, projectKey);

        verify(scaleApiMock, times(testStepMap.keySet().size())).fetchTestStepsFromTestCaseKey(any());
    }

    @Test
    void shouldFetchTestExecutionAttachmentsFromSquadOncePerExecutionId() throws IOException {

        attachmentsMigratorSpy.export(squadToScaleEntitiesMapMock, projectKey);

        verify(squadApiMock, times(testExecMap.keySet().size())).fetchTestExecutionAttachmentById(any());
    }

    @Test
    void shouldCreateListOfAttachmentsAssociationDataWithOneItemPerAttachmentFetched() throws IOException, URISyntaxException {

        ArgumentCaptor<List<AttachmentAssociationData>> captorListAttachmentsAssociationData = ArgumentCaptor.forClass(List.class);

        attachmentsMigratorSpy.export(squadToScaleEntitiesMapMock, projectKey);

        verify(attachmentsCsvExporterMock).dump(captorListAttachmentsAssociationData.capture(), any(), any());

        List<AttachmentAssociationData> captured = captorListAttachmentsAssociationData.getValue();

        //we are returning 2 attachments each time the process fetch it for each test execution
        assertEquals(captured.size(), testExecMap.keySet().size() * squadAttachmentsMockList.size());

    }

}
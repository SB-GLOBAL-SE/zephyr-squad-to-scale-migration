package com.atlassian.migration.app.zephyr.common;

import com.atlassian.migration.app.zephyr.scale.model.GetAllProjectsResponse;
import com.atlassian.migration.app.zephyr.scale.model.Option;
import com.atlassian.migration.app.zephyr.squad.api.SquadApi;
import com.atlassian.migration.app.zephyr.squad.model.*;
import com.google.gson.Gson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.net.http.HttpClient;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SquadApiTest {

    @Mock
    private ApiConfiguration apiConfMock;

    private SquadApi squadApiSpy;


    private static final String FETCH_ATTACHMENTS_RESPONSE_MOCK = "{data:[" +
            "{fileName:attachment1_file,dateCreated:'1970-01-01 00:00:00', htmlComment:comment, fileSize:123456, " +
            "fileIcon:icon.gif, author:author_key, fileIconAltText:text, comment:string_comment, fileId:1}," +
            "{fileName:attachment2_file,dateCreated:'1970-01-01 00:00:00', htmlComment:comment, fileSize:123456, " +
            "fileIcon:icon.gif, author:author_key, fileIconAltText:text, comment:string_comment, fileId:2}" +
            "]}";

    @BeforeEach
    void setup() {
        when(apiConfMock.httpVersion()).thenReturn("2");
        squadApiSpy = spy(new SquadApi(apiConfMock));
    }

    @Nested
    class whenCreating {

        @Test
        void shouldThrowExceptionWhenHttpVersionIsInvalid() {
            when(apiConfMock.httpVersion()).thenReturn("3");
            assertThrows(IllegalArgumentException.class, () -> new SquadApi(apiConfMock));
        }

        @Test
        void shouldSetTheHttpVersionTo2() {
            when(apiConfMock.httpVersion()).thenReturn("2");
            var sutScaleApi = new SquadApi(apiConfMock);
            assertEquals(sutScaleApi.client.version(), HttpClient.Version.HTTP_2);

            when(apiConfMock.httpVersion()).thenReturn("2.0");
            sutScaleApi = new SquadApi(apiConfMock);
            assertEquals(sutScaleApi.client.version(), HttpClient.Version.HTTP_2);
        }

        @Test
        void shouldSetTheHttpVersionTo1() {
            when(apiConfMock.httpVersion()).thenReturn("1");
            var sutScaleApi = new SquadApi(apiConfMock);
            assertEquals(sutScaleApi.client.version(), HttpClient.Version.HTTP_1_1);

            when(apiConfMock.httpVersion()).thenReturn("1.1");
            sutScaleApi = new SquadApi(apiConfMock);
            assertEquals(sutScaleApi.client.version(), HttpClient.Version.HTTP_1_1);
        }

    }

    @Nested
    class WhenFetchingProjects {

        private static final String FETCH_PROJECTS_MOCK_RESPONSE = "{options:[" +
                "{hasAccessToSoftware:true,label:some_label,type:some_type, value:some_value}," +
                "{hasAccessToSoftware:false,label:some_label,type:some_type, value:some_value}" +
                "]}";

        @Test
        void shouldGetAllProjects() throws IOException {

            var projectsExpected = List.of(
                    new Option("true", "some_label", "some_type", "some_value"),
                    new Option("false", "some_label", "some_type", "some_value")
            );


            var getAllProjectsResponseExpected = new GetAllProjectsResponse(projectsExpected);

            doReturn(FETCH_PROJECTS_MOCK_RESPONSE).when(squadApiSpy).sendHttpGet(any());

            var projectsFetched = squadApiSpy.getAllProjects();


            assertEquals(getAllProjectsResponseExpected, projectsFetched);
        }
    }

    @Nested
    class WhenFetchingTestSteps {

        private static final String FETCH_STEPS_RESPONSE_MOCK = "{stepBeanCollection:[" +
                "{id:1,orderId:1,htmlStep:html_step,htmlData:html_data,htmlResult:html_result,attachmentsMap:[]}," +
                "{id:2,orderId:2,htmlStep:html_step,htmlData:html_data,htmlResult:html_result,attachmentsMap:[]}" +
                "]}";

        @Test
        void shouldGetStepsByStepId() throws IOException {

            var testStepsExpected = List.of(
                    new SquadTestStepResponse("1", "1",
                            "html_step", "html_data", "html_result", Collections.emptyList()),
                    new SquadTestStepResponse("2", "2",
                            "html_step", "html_data", "html_result", Collections.emptyList())

            );

            var fetchSquadTestStepResponseExpected = new FetchSquadTestStepResponse(testStepsExpected);

            doReturn(FETCH_STEPS_RESPONSE_MOCK).when(squadApiSpy).sendHttpGet(any());

            var fetchSquadTestStepResponseFetched = squadApiSpy.fetchLatestTestStepByTestCaseId("10100");

            assertEquals(fetchSquadTestStepResponseExpected, fetchSquadTestStepResponseFetched);
        }

        @Test
        void shouldFetchAttachmentsFromTestSteps() throws ApiException {

            var attachmentsExpected = List.of(
                    new SquadAttachmentItemResponse("attachment1_file", "1970-01-01 00:00:00", "comment",
                            "123456", "icon.gif", "author_key", "text", "string_comment", "1"),
                    new SquadAttachmentItemResponse("attachment2_file", "1970-01-01 00:00:00", "comment",
                            "123456", "icon.gif", "author_key", "text", "string_comment", "2")
            );

            var fetchSquadAttachmentResponseExpected = new FetchSquadAttachmentResponse(attachmentsExpected);

            doReturn(FETCH_ATTACHMENTS_RESPONSE_MOCK).when(squadApiSpy).sendHttpGet(any());

            var fetchSquadAttachmentResponseFetched = squadApiSpy.fetchTestStepAttachmentById("1");

            assertEquals(fetchSquadAttachmentResponseExpected, fetchSquadAttachmentResponseFetched);
        }
    }

    @Nested
    class WhenFetchingTestExecution {

        private final Map<String, SquadExecutionStatusResponse> statusMap = Map.of(
                "1", new SquadExecutionStatusResponse("1", "Pass", "description", "color")
        );

        private Gson gson;

        @BeforeEach
        void setup() {
            gson = new Gson();
        }

        @Test
        void shouldFetchAttachmentsFromExecution() throws ApiException {

            var attachmentsExpected = List.of(
                    new SquadAttachmentItemResponse("attachment1_file", "1970-01-01 00:00:00", "comment",
                            "123456", "icon.gif", "author_key", "text", "string_comment", "1"),
                    new SquadAttachmentItemResponse("attachment2_file", "1970-01-01 00:00:00", "comment",
                            "123456", "icon.gif", "author_key", "text", "string_comment", "2")
            );

            var fetchSquadAttachmentResponseExpected = new FetchSquadAttachmentResponse(attachmentsExpected);

            doReturn(FETCH_ATTACHMENTS_RESPONSE_MOCK).when(squadApiSpy).sendHttpGet(any());

            var fetchSquadAttachmentResponseFetched = squadApiSpy.fetchTestExecutionAttachmentById("1");

            assertEquals(fetchSquadAttachmentResponseExpected, fetchSquadAttachmentResponseFetched);
        }

        @Test
        void shouldFetchExecutionsByIssueIdWithAllFields() throws ApiException {
            var squadExecutionItemParsedExpected = List.of(
                    new SquadExecutionItemResponse(
                            "2",
                            1,
                            "createdOn",
                            "author",
                            "author",
                            "version",
                            "html_content",
                            "cycle",
                            "folder",
                            "executed",
                            "assignee",
                            "assignee",
                            "assignee",
                            List.of(new SquadExecutionDefectResponse("issueKey"))));

            var fetchSquadExecutionResponseMock = new FetchSquadExecutionResponse(statusMap, "10100",
                    1, 1, false, false,
                    squadExecutionItemParsedExpected);

            var responseMock = gson.toJson(fetchSquadExecutionResponseMock);

            doReturn(responseMock).when(squadApiSpy).sendHttpGet(any());

            var fetchSquadExecutionParsedResponseExpected = new FetchSquadExecutionParsedResponse(
                    statusMap,
                    "10100", 1, 1, false, false,
                    List.of(
                            new SquadExecutionItemParsedResponse(
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
                                    List.of(new SquadExecutionDefectResponse("issueKey")))
                    )
            );

            var fetchSquadExecutionParsedResponseFetched = squadApiSpy.fetchLatestExecutionByIssueId("10100");

            assertEquals(fetchSquadExecutionParsedResponseExpected, fetchSquadExecutionParsedResponseFetched);
        }

        @Test
        void shouldFetchExecutionsByIssueIdWithNullFields() throws ApiException {

            var squadExecutionItemParsedExpected = List.of(
                    new SquadExecutionItemResponse(
                            "1",
                            1,
                            "createdOn",
                            "author",
                            "author",
                            "version",
                            "html_content",
                            "cycle",
                            null,
                            null,
                            null,
                            null,
                            null,
                            List.of(new SquadExecutionDefectResponse("issueKey"))));

            var fetchSquadExecutionResponseMock = new FetchSquadExecutionResponse(statusMap, "10100",
                    1, 1, false, false,
                    squadExecutionItemParsedExpected);


            var responseMock = gson.toJson(fetchSquadExecutionResponseMock);

            doReturn(responseMock).when(squadApiSpy).sendHttpGet(any());

            var fetchSquadExecutionParsedResponseExpected = new FetchSquadExecutionParsedResponse(
                    statusMap,
                    "10100", 1, 1, false, false,
                    List.of(
                            new SquadExecutionItemParsedResponse(
                                    "1",
                                    new SquadExecutionTypeResponse(1, "Pass"),
                                    "createdOn",
                                    "author",
                                    "author",
                                    "version",
                                    "html_content",
                                    null,
                                    null,
                                    null,
                                    null,
                                    "cycle",
                                    null,
                                    List.of(new SquadExecutionDefectResponse("issueKey")))
                    )
            );

            var fetchSquadExecutionParsedResponseFetched = squadApiSpy.fetchLatestExecutionByIssueId("10100");

            assertEquals(fetchSquadExecutionParsedResponseExpected, fetchSquadExecutionParsedResponseFetched);


        }
    }


}

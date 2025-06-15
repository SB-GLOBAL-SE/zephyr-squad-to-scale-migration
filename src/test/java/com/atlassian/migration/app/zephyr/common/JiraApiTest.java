package com.atlassian.migration.app.zephyr.common;

import com.atlassian.migration.app.zephyr.jira.api.JiraApi;
import com.atlassian.migration.app.zephyr.jira.model.*;
import com.atlassian.migration.app.zephyr.scale.model.GetProjectResponse;
import com.google.gson.Gson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.http.HttpClient;
import java.util.Collections;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JiraApiTest {
    @Mock
    private ApiConfiguration apiConfMock;

    private JiraApi jiraApiSpy;

    @BeforeEach
    void setup() {
        when(apiConfMock.httpVersion()).thenReturn("2");
        jiraApiSpy = spy(new JiraApi(apiConfMock));
    }

    @Nested
    class whenCreating {

        @Test
        void shouldThrowExceptionWhenHttpVersionIsInvalid() {
            when(apiConfMock.httpVersion()).thenReturn("3");
            assertThrows(IllegalArgumentException.class, () -> new JiraApi(apiConfMock));
        }


        @Test
        void shouldSetTheHttpVersionTo2() {
            when(apiConfMock.httpVersion()).thenReturn("2");
            var sutScaleApi = new JiraApi(apiConfMock);
            assertEquals(sutScaleApi.client.version(), HttpClient.Version.HTTP_2);

            when(apiConfMock.httpVersion()).thenReturn("2.0");
            sutScaleApi = new JiraApi(apiConfMock);
            assertEquals(sutScaleApi.client.version(), HttpClient.Version.HTTP_2);
        }

        @Test
        void shouldSetTheHttpVersionTo1() {
            when(apiConfMock.httpVersion()).thenReturn("1");
            var sutScaleApi = new JiraApi(apiConfMock);
            assertEquals(sutScaleApi.client.version(), HttpClient.Version.HTTP_1_1);

            when(apiConfMock.httpVersion()).thenReturn("1.1");
            sutScaleApi = new JiraApi(apiConfMock);
            assertEquals(sutScaleApi.client.version(), HttpClient.Version.HTTP_1_1);
        }

    }

    @Nested
    class WhenFetchingJiraIssues {

        private final JiraIssuesResponse issueExpected_1 = new JiraIssuesResponse("10101", "JIRA-1", null);
        private final JiraIssuesResponse issueExpected_2 = new JiraIssuesResponse("10102", "JIRA-10", null);

        private final Author author = new Author("self", "author", "author_key", "email", "name", false);

        private static final String RESPONSE_WITH_TWO_ISSUES_MOCK = "{startAt:0, total:2," +
                "issues:[{id:10101,key:JIRA-1}, {id:10102,key:JIRA-10}]}";

        private final Gson gson = new Gson();

        private JiraIssuesResponse jiraIssuesResponseComplete;

        @BeforeEach
        void setup() {
            var jiraIssueFieldResponse = new JiraIssueFieldResponse();
            jiraIssueFieldResponse.issuetype = new JiraIssueTypeResponse(1, "type");
            jiraIssueFieldResponse.summary = "summary";
            jiraIssueFieldResponse.description = "description";
            jiraIssueFieldResponse.labels = Collections.emptyList();
            jiraIssueFieldResponse.reporter = new JiraReporterResponse(1, "REP", "reporter_name");
            jiraIssueFieldResponse.status = new JiraIssueStatusResponse("1", "status_name");

            var linkType = new LinkType("1", "link_name", "inward", "outward", "self");

            var statusCategory = new StatusCategory("self", 1, "key", "colorName", "name");
            var status = new Status("self", "description", "icon", "name", "1", statusCategory);
            var priority = new Priority("self", "icon", "name", "1");
            var issueType = new IssueType("self", "1", "Description", "icon", "name", false, 1);
            var fields = new Fields("summary", status, priority, issueType);

            jiraIssueFieldResponse.issuelinks = List.of(
                    new IssueLink("1", "self",
                            linkType,
                            new RelatedIssue("1", "JIRA-99", "self", fields),
                            new RelatedIssue("2", "JIRA-100", "self", fields)));

            jiraIssueFieldResponse.components = List.of(
                    new JiraIssueComponent(1, "component", "self"));
            jiraIssueFieldResponse.priority = new JiraIssuePriority(1, "HIGH");
            jiraIssueFieldResponse.attachment = List.of(
                    new Attachment("self", "1", "filename", author, "created", "123456", "mimetype")
            );

            jiraIssuesResponseComplete = new JiraIssuesResponse("10100", "JIRA-1", jiraIssueFieldResponse);
        }

        @Test
        void shouldGetTotalNumberOfIssues() throws IOException {
            var mockPayload = "{startAt:0,total:10}";
            doReturn(mockPayload).when(jiraApiSpy)
                    .sendHttpGet(any());

            doReturn(gson.fromJson(mockPayload, FetchJiraIssuesResponse.class))
                    .when(jiraApiSpy).extractCustomFieldsFromResponse(any(), any());

            int totalIssues = jiraApiSpy.fetchTotalIssuesByProjectName("project");

            assertEquals(10, totalIssues);
        }

        @Test
        void shouldGetIssueById() throws IOException {

            var responseMock = gson.toJson(jiraIssuesResponseComplete);

            doReturn(responseMock).when(jiraApiSpy).sendHttpGet(any());

            var issueFetched = jiraApiSpy.getIssueById("10100");

            assertEquals(jiraIssuesResponseComplete, issueFetched);
        }

        @Test
        void shouldGetIssueAttachmentsByIssueId() throws IOException {

            var attachmentsExpected = List.of(
                    new Attachment("some_url", "1", "attach1", author, "1970-01-01 00:00:00", "123456", "type"),
                    new Attachment("some_url", "2", "attach2", author, "1970-01-01 00:00:00", "123456", "type")
            );

            var responseMock = "{id:10101,key:JIRA-1,fields:{" +
                    "attachment:[" +
                    "{self:some_url,id:1,filename:attach1,created:'1970-01-01 00:00:00',size:123456,mimetype:type," +
                    "author:{self:self,name:author,key:author_key,emailAddress:email,displayName:name,active:false}}," +
                    "{self:some_url,id:2,filename:attach2,created:'1970-01-01 00:00:00',size:123456,mimetype:type," +
                    "author:{self:self,name:author,key:author_key,emailAddress:email,displayName:name,active:false}}]}}";

            doReturn(responseMock).when(jiraApiSpy).sendHttpGet(any());

            var attachmentsFetched = jiraApiSpy.getIssueAttachmentsByIssueId("10101");
            assertEquals(attachmentsExpected, attachmentsFetched);
        }

        @Test
        void shouldGetNullIssueAttachmentsByIssueId() throws IOException {

            var attachmentsExpected = Collections.emptyList();
            var responseMock = "{id:10101,key:JIRA-1,fields:{}}";

            doReturn(responseMock).when(jiraApiSpy).sendHttpGet(any());

            var attachmentsFetched = jiraApiSpy.getIssueAttachmentsByIssueId("10101");
            assertEquals(attachmentsExpected, attachmentsFetched);
        }

        @Test
        void shouldGetIssuesOrderedByCreatedDateWithoutRetry() throws IOException {

            var issuesExpected = List.of(issueExpected_1, issueExpected_2);

            doReturn(RESPONSE_WITH_TWO_ISSUES_MOCK).when(jiraApiSpy).sendHttpGet(any());

            doReturn(gson.fromJson(RESPONSE_WITH_TWO_ISSUES_MOCK, FetchJiraIssuesResponse.class))
                    .when(jiraApiSpy).extractCustomFieldsFromResponse(any(), any());

            var issuesFetched = jiraApiSpy.fetchIssuesOrderedByCreatedDate("project", 0, 100);

            assertEquals(issuesExpected, issuesFetched);

        }

        @Test
        void shouldGetIssuesOrderedByCreatedDateWithRetry() throws IOException {

            var issuesExpected = List.of(issueExpected_1, issueExpected_2);

            doReturn(RESPONSE_WITH_TWO_ISSUES_MOCK).when(jiraApiSpy).sendHttpGet(any());

            doReturn(gson.fromJson(RESPONSE_WITH_TWO_ISSUES_MOCK, FetchJiraIssuesResponse.class))
                    .when(jiraApiSpy).extractCustomFieldsFromResponse(any(), any());

            var issuesFetched = jiraApiSpy.fetchIssuesOrderedByCreatedDate("project", 0, 100);

            assertEquals(issuesExpected, issuesFetched);

        }


        @Test
        void shouldFetchIssuesAndReadCustomFields() throws IOException {

            var responseMock = String.format("{startAt:0,total:10,issues:[%s]}", gson.toJson(jiraIssuesResponseComplete));

            doReturn(responseMock).when(jiraApiSpy).sendHttpGet(any());

            var issueFetched = jiraApiSpy.fetchTestCreatedOrderEntry("PROJECT", 0, 10);

            var fetchJiraIssuesResponseExpected = new FetchJiraIssuesResponse(0, 10, List.of(jiraIssuesResponseComplete));

            assertEquals(fetchJiraIssuesResponseExpected.issues().get(0).fields(), issueFetched.issues().get(0).fields());


        }
    }

    @Nested
    class WhenFetchingProjects {

        private final GetProjectResponse expectedProject = new GetProjectResponse("PROJECT", "10100", null, null);

        private static final String RESPONSE_WITH_SINGLE_PROJECT_MOCK = "{key:PROJECT,id:10100}";

        @BeforeEach
        void setupForProjects() throws ApiException {
            doReturn(RESPONSE_WITH_SINGLE_PROJECT_MOCK).when(jiraApiSpy).sendHttpGet(any());
        }

        @Test
        void shouldGetProjectById() throws IOException {
            var projectFetched = jiraApiSpy.getProjectById("10100");

            assertEquals(expectedProject, projectFetched);
        }

        @Test
        void shouldGetProjectByKey() throws IOException {
            var projectFetched = jiraApiSpy.getProjectByKey("10100");

            assertEquals(expectedProject, projectFetched);
        }

    }


    @Nested
    class BasicApiTests {
        @Test
        void shouldCorrectlyDecodeGzippedData() throws IOException {

            var testData = "test data";
            var byteArrayStream = new ByteArrayOutputStream();
            var gzipStream = new GZIPOutputStream(byteArrayStream);

            gzipStream.write(testData.getBytes());
            gzipStream.close();

            var encodedData = byteArrayStream.toByteArray();

            var decodedData = jiraApiSpy.decodeBody(encodedData, "gzip");

            assertEquals(testData, decodedData);
        }

        @Test
        void shouldKeepDataOriginalIfNotGzipped() throws IOException {

            var testData = "test data";

            var decodedData = jiraApiSpy.decodeBody(testData.getBytes(), "identity");

            assertEquals(testData, decodedData);

            decodedData = jiraApiSpy.decodeBody(testData.getBytes(), "");

            assertEquals(testData, decodedData);
        }
    }

}

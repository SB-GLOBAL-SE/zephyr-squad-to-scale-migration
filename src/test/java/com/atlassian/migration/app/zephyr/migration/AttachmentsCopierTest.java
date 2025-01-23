package com.atlassian.migration.app.zephyr.migration;

import com.atlassian.migration.app.zephyr.migration.model.AttachmentAssociationData;
import com.atlassian.migration.app.zephyr.migration.model.SquadOriginEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AttachmentsCopierTest {

    private AttachmentsCopier attachmentsCopierSpy;

    private final AttachmentAssociationData testCaseAttachMappedMock = AttachmentAssociationData.createAttachmentAssociationDataFromTestCase(
            "testCaseAttach",
            "1",
            "mime",
            "123456",
            "author",
            "10100",
            "1",
            new SquadOriginEntity("1", "JIRA-1"));

    private final AttachmentAssociationData testStepAttachMappedMock = AttachmentAssociationData.createAttachmentAssociationDataFromTestStep(
            "testStepAttach",
            "2",
            "mime",
            "123456",
            "author",
            "10100",
            "1",
            new SquadOriginEntity("1", ""));

    private final AttachmentAssociationData testExecAttachMappedMock = AttachmentAssociationData.createAttachmentAssociationDataFromTestExecution(
            "testStepAttach",
            "3",
            "mime",
            "123456",
            "author",
            "10100",
            "1",
            new SquadOriginEntity("1", ""));

    private final AttachmentAssociationData executionStepAttachMappedMock = AttachmentAssociationData.createAttachmentAssociationDataFromExecutionStep(
            "testStepAttach",
            "4",
            "mime",
            "123456",
            "author",
            "10100",
            "1",
            new SquadOriginEntity("1", ""));

    private final List<AttachmentAssociationData> attachmentsMappedMock = List.of(
            testCaseAttachMappedMock, testStepAttachMappedMock, testExecAttachMappedMock, executionStepAttachMappedMock);


    private final String mockProjectKey = "PROJECT";

    private final List<String> mockProjectHistoricalKeys = List.of("PROJECT");

    private static final String BASE_DIR = "/home/ubuntu/jira/data/attachments/";

    @BeforeEach
    void setup() throws IOException {
        attachmentsCopierSpy = spy(new AttachmentsCopier(BASE_DIR));

        doNothing().when(attachmentsCopierSpy).copyFile(any(), any());
    }

    @Test
    void shouldBuildOriginPathForTestCase() throws IOException {
        var expectedOriginPath = BASE_DIR + "PROJECT/10000/PROJECT-1/1";

        ArgumentCaptor<String> captureBaseDir = ArgumentCaptor.forClass(String.class);

        doReturn(new AttachmentsCopier.ProjectHistoricalKeys("PROJECT", "PROJECT", List.of("PROJECT")))
                .when(attachmentsCopierSpy).getOriginalProjectKey(any(), any());

        doReturn(true).when(attachmentsCopierSpy).isPathToAttachment(any());

        attachmentsCopierSpy.copyAttachments(List.of(testCaseAttachMappedMock), mockProjectKey,
                mockProjectHistoricalKeys);


        verify(attachmentsCopierSpy).copyFile(captureBaseDir.capture(), any());

        assertEquals(captureBaseDir.getValue(), expectedOriginPath);
    }

    @Test
    void shouldBuildOriginPathForTestStep() throws IOException {
        var expectedOriginPath = BASE_DIR + "PROJECT/teststep/1/2";

        ArgumentCaptor<String> captureBaseDir = ArgumentCaptor.forClass(String.class);

        doReturn(true).when(attachmentsCopierSpy).isPathToAttachment(any());

        attachmentsCopierSpy.copyAttachments(List.of(testStepAttachMappedMock), mockProjectKey, mockProjectHistoricalKeys);

        verify(attachmentsCopierSpy).copyFile(captureBaseDir.capture(), any());

        assertEquals(expectedOriginPath, captureBaseDir.getValue());
    }

    @Test
    void shouldBuildOriginPathForTestExecution() throws IOException {
        var expectedOriginPath = BASE_DIR + "PROJECT/schedule/1/3";

        ArgumentCaptor<String> captureBaseDir = ArgumentCaptor.forClass(String.class);

        doReturn(true).when(attachmentsCopierSpy).isPathToAttachment(any());

        attachmentsCopierSpy.copyAttachments(List.of(testExecAttachMappedMock), mockProjectKey, mockProjectHistoricalKeys);

        verify(attachmentsCopierSpy).copyFile(captureBaseDir.capture(), any());

        assertEquals(captureBaseDir.getValue(), expectedOriginPath);
    }

    @Test
    void shouldBuildOriginPathForExecutionStep() throws IOException {
        var expectedOriginPath = BASE_DIR + "PROJECT/teststepresult/1/4";

        ArgumentCaptor<String> captureBaseDir = ArgumentCaptor.forClass(String.class);

        doReturn(true).when(attachmentsCopierSpy).isPathToAttachment(any());

        attachmentsCopierSpy.copyAttachments(List.of(executionStepAttachMappedMock), mockProjectKey, mockProjectHistoricalKeys);

        verify(attachmentsCopierSpy).copyFile(captureBaseDir.capture(), any());

        assertEquals(captureBaseDir.getValue(), expectedOriginPath);
    }

    @Test
    void shouldCallCpFileOncePerAttachmentMapped() throws IOException {

        doReturn(true).when(attachmentsCopierSpy).isPathToAttachment(any());

        attachmentsCopierSpy.copyAttachments(attachmentsMappedMock, mockProjectKey, mockProjectHistoricalKeys);

        verify(attachmentsCopierSpy, times(4)).copyFile(any(), any());
    }

    @Test
    void shouldCalculateJiraIssueBucketCorrectly() {

        var expectedBucketPerIssue = Map.of(
                "JIRA-1", "10000",
                "JIRA-10000", "10000",
                "JIRA-10001", "20000",
                "JIRA-20000", "20000",
                "JIRA-20001", "30000",
                "JIRA-100000", "100000");

        expectedBucketPerIssue.forEach((issueNum, expectedBucket) -> assertEquals(expectedBucket,
                attachmentsCopierSpy.calculateBucket(Integer.parseInt(issueNum.split("-")[1]))));

    }


    @Test
    void shouldReturnOriginalKeyFromList() throws IOException {
        var mockProjectHistoricalKeys = List.of("KEY1", "KEY2", "KEY3");

        var path1 = Path.of("foo/bar/KEY1");
        var path2 = Path.of("foo/bar/KEY2");
        var path3 = Path.of("foo/bar/KEY3");

        var expectedOriginalKey = new AttachmentsCopier.ProjectHistoricalKeys(
                "KEY3",
                "KEY1",
                List.of("KEY1"));

        when(attachmentsCopierSpy.getProjectAttachmentsDir("KEY1")).thenReturn(path1);
        when(attachmentsCopierSpy.getProjectAttachmentsDir("KEY2")).thenReturn(path2);
        when(attachmentsCopierSpy.getProjectAttachmentsDir("KEY3")).thenReturn(path3);


        doReturn(true).when(attachmentsCopierSpy).holdsIssuesDir(path1);
        doReturn(false).when(attachmentsCopierSpy).holdsSquadDirs(path1);

        doReturn(false).when(attachmentsCopierSpy).holdsIssuesDir(path2);
        doReturn(false).when(attachmentsCopierSpy).holdsSquadDirs(path2);

        doReturn(false).when(attachmentsCopierSpy).holdsIssuesDir(path3);
        doReturn(false).when(attachmentsCopierSpy).holdsSquadDirs(path3);

        var returnedKey = attachmentsCopierSpy.getOriginalProjectKey("KEY3", mockProjectHistoricalKeys);

        assertEquals(expectedOriginalKey, returnedKey);
    }

    @Test
    void shouldReturnOriginalKeyAndHistoricalKeysHoldingDataFromList() throws IOException {
        var mockProjectHistoricalKeys = List.of("KEY1", "KEY2", "KEY3");

        var path1 = Path.of("foo/bar/KEY1");
        var path2 = Path.of("foo/bar/KEY2");
        var path3 = Path.of("foo/bar/KEY3");

        var expectedOriginalKey = new AttachmentsCopier.ProjectHistoricalKeys(
                "KEY3",
                "KEY1",
                List.of("KEY1", "KEY2"));

        when(attachmentsCopierSpy.getProjectAttachmentsDir("KEY1")).thenReturn(path1);
        when(attachmentsCopierSpy.getProjectAttachmentsDir("KEY2")).thenReturn(path2);
        when(attachmentsCopierSpy.getProjectAttachmentsDir("KEY3")).thenReturn(path3);


        doReturn(true).when(attachmentsCopierSpy).holdsIssuesDir(path1);
        doReturn(false).when(attachmentsCopierSpy).holdsSquadDirs(path1);

        doReturn(false).when(attachmentsCopierSpy).holdsIssuesDir(path2);
        doReturn(true).when(attachmentsCopierSpy).holdsSquadDirs(path2);

        doReturn(false).when(attachmentsCopierSpy).holdsIssuesDir(path3);
        doReturn(false).when(attachmentsCopierSpy).holdsSquadDirs(path3);

        var returnedKey = attachmentsCopierSpy.getOriginalProjectKey("KEY3", mockProjectHistoricalKeys);

        assertEquals(expectedOriginalKey, returnedKey);
    }

    @Test
    void shouldSkipAttachmentsCopyingIfProjectOriginalKeyIsNotFound() throws IOException {
        var mockProjectHistoricalKeys = List.of("KEY1", "KEY2", "KEY3");

        var path1 = Path.of("foo/bar/KEY1");
        var path2 = Path.of("foo/bar/KEY2");
        var path3 = Path.of("foo/bar/KEY3");

        when(attachmentsCopierSpy.getProjectAttachmentsDir("KEY1")).thenReturn(path1);
        when(attachmentsCopierSpy.getProjectAttachmentsDir("KEY2")).thenReturn(path2);
        when(attachmentsCopierSpy.getProjectAttachmentsDir("KEY3")).thenReturn(path3);

        doReturn(false).when(attachmentsCopierSpy).holdsIssuesDir(path1);
        doReturn(false).when(attachmentsCopierSpy).holdsSquadDirs(path1);

        doReturn(false).when(attachmentsCopierSpy).holdsIssuesDir(path2);
        doReturn(true).when(attachmentsCopierSpy).holdsSquadDirs(path2);

        doReturn(false).when(attachmentsCopierSpy).holdsIssuesDir(path3);
        doReturn(true).when(attachmentsCopierSpy).holdsSquadDirs(path3);

        assertNull(attachmentsCopierSpy.getOriginalProjectKey("KEY1", mockProjectHistoricalKeys));

        attachmentsCopierSpy.copyAttachments(List.of(testStepAttachMappedMock), mockProjectKey, mockProjectHistoricalKeys);

        verify(attachmentsCopierSpy, times(0))
                .copyFile(any(), any());

    }

    @Test
    void shouldNotCallCopyFileIfSquadEntityAttachmentIsMissing() throws IOException {

        doReturn(false).when(attachmentsCopierSpy).isPathToAttachment(any());

        attachmentsCopierSpy.copyAttachments(List.of(testStepAttachMappedMock), mockProjectKey, mockProjectHistoricalKeys);

        verify(attachmentsCopierSpy, times(0))
                .copyFile(any(), any());

        attachmentsCopierSpy.copyAttachments(List.of(testExecAttachMappedMock), mockProjectKey, mockProjectHistoricalKeys);

        verify(attachmentsCopierSpy, times(0))
                .copyFile(any(), any());

    }

    @Test
    void shouldNotCallCopyFileIfJiraEntityAttachmentIsMissing() throws IOException {

        doReturn(false).when(attachmentsCopierSpy).isPathToAttachment(any());

        attachmentsCopierSpy.copyAttachments(List.of(testCaseAttachMappedMock), mockProjectKey, mockProjectHistoricalKeys);

        verify(attachmentsCopierSpy, times(0))
                .copyFile(any(), any());

    }
}
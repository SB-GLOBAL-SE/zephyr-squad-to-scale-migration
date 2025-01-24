package com.atlassian.migration.app.zephyr.migration.execution;

import com.atlassian.migration.app.zephyr.migration.AttachmentsMigrator;
import com.atlassian.migration.app.zephyr.migration.model.data.TestExecutionAssociatedData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.supercsv.io.CsvBeanWriter;
import org.supercsv.prefs.CsvPreference;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class TestExecutionCsvExporter {

    private static final Logger logger = LoggerFactory.getLogger(TestExecutionCsvExporter.class);
    private final String fileName;
    private Path destinationPath = null;

    public TestExecutionCsvExporter(String fileName) {
        this.fileName = fileName;
    }

    public void dump(List<TestExecutionAssociatedData> testExecutionsMap, String[] headers, String[] headerMapping) throws URISyntaxException, IOException {

        if (destinationPath == null) {
            destinationPath = setupFile(this.fileName, headers);
        }

        try (CsvBeanWriter csvBeanWriter = new CsvBeanWriter(
                new BufferedWriter(new FileWriter(destinationPath.toString(), StandardCharsets.UTF_8, true))
                , CsvPreference.STANDARD_PREFERENCE
        )) {

            for (var testExecution : testExecutionsMap) {

                csvBeanWriter.write(testExecution, headerMapping);
            }

        } catch (IOException e) {
            logger.error("Failed to export Attachments Map to CSV file " + e.getMessage(), e);
        }

    }

    private Path setupFile(String fileName, String[] headers) throws URISyntaxException, IOException {

        Path destinationPath = getCurrentPath().resolve(fileName);
        logger.info("Creating file for mapped executions at: " + destinationPath);
        Files.deleteIfExists(destinationPath);
        Files.createFile(destinationPath);

        try (CsvBeanWriter csvBeanWriter = new CsvBeanWriter(
                new BufferedWriter(new FileWriter(destinationPath.toString(), StandardCharsets.UTF_8, true))
                , CsvPreference.STANDARD_PREFERENCE
        )) {
            csvBeanWriter.writeHeader(headers);

            return destinationPath;

        } catch (IOException e) {
            logger.error("Failed to create CSV file to receive mapped test executions " + e.getMessage(), e);
            throw e;
        }
    }

    private Path getCurrentPath() throws URISyntaxException {
        return Paths.get(AttachmentsMigrator.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParent();
    }
}

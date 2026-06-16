package com.atlassian.migration.app.zephyr.migration;

import com.atlassian.migration.app.zephyr.migration.model.AttachmentAssociationData;
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

public class AttachmentsCsvExporter {

    private static final Logger logger = LoggerFactory.getLogger(AttachmentsCsvExporter.class);
    private final String fileName;
    private Path destinationPath = null;

    public AttachmentsCsvExporter(String fileName) {
        this.fileName = fileName;

    }

    public void dump(List<AttachmentAssociationData> attachmentAssociationData, String[] headers, String[] headerMapping)
            throws URISyntaxException, IOException {

        if (destinationPath == null) {
            destinationPath = setupFile(this.fileName, headers);
        }

        try (CsvBeanWriter csvBeanWriter = new CsvBeanWriter(
                new BufferedWriter(new FileWriter(destinationPath.toString(), StandardCharsets.UTF_8, true))
                , CsvPreference.STANDARD_PREFERENCE
        )) {

            for (var attachmentMapped : attachmentAssociationData) {
                if(attachmentMapped.getCreatedOn() != null ){
                    String yearMonthSubDir = AttachmentUtils.getYearMonthSubDir(attachmentMapped.getCreatedOn());
                    if(yearMonthSubDir != null && !yearMonthSubDir.isBlank() && !yearMonthSubDir.equals("unknown")){
                        attachmentMapped.setFileName(yearMonthSubDir + "/" + attachmentMapped.getFileName());
                    }
                }
                csvBeanWriter.write(attachmentMapped, headerMapping);
            }

        } catch (IOException e) {
            logger.error("Failed to export Attachments Map to CSV file " + e.getMessage(), e);
        }
    }

    private Path setupFile(String fileName, String[] headers) throws URISyntaxException, IOException {

        Path destinationPath = getCurrentPath().resolve(fileName);
        logger.info("Creating file for mapped attachments at: " + destinationPath);
        Files.deleteIfExists(destinationPath);
        Files.createFile(destinationPath);

        try (CsvBeanWriter csvBeanWriter = new CsvBeanWriter(
                new BufferedWriter(new FileWriter(destinationPath.toString(), StandardCharsets.UTF_8, true))
                , CsvPreference.STANDARD_PREFERENCE
        )) {
            csvBeanWriter.writeHeader(headers);

            return destinationPath;

        } catch (IOException e) {
            logger.error("Failed to create CSV file to receive mapped attachments " + e.getMessage(), e);
            throw e;
        }
    }

    private Path getCurrentPath() throws URISyntaxException {
        return Paths.get(AttachmentsMigrator.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParent();
    }
}

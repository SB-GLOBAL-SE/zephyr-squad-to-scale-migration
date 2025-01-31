package com.atlassian.migration.app.zephyr;

import com.atlassian.migration.app.zephyr.common.ApiConfiguration;
import com.atlassian.migration.app.zephyr.common.DataSourceFactory;
import com.atlassian.migration.app.zephyr.common.PropertySanitizer;
import com.atlassian.migration.app.zephyr.jira.api.JiraApi;
import com.atlassian.migration.app.zephyr.migration.*;
import com.atlassian.migration.app.zephyr.migration.execution.TestExecutionCsvExporter;
import com.atlassian.migration.app.zephyr.migration.execution.TestExecutionPostMigrator;
import com.atlassian.migration.app.zephyr.migration.testcase.TestCaseCsvExporter;
import com.atlassian.migration.app.zephyr.migration.testcase.TestCasePostMigrator;
import com.atlassian.migration.app.zephyr.scale.api.ScaleApi;
import com.atlassian.migration.app.zephyr.squad.api.SquadApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class ApplicationMain {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationMain.class);

    public static void main(String[] args) {

        if (args.length < 2) {
            logger.error("Username and password are required as command line arguments.");
            System.exit(1);
        }

        logger.info("Loading properties file...");
        try (FileInputStream input = new FileInputStream("app.properties")) {

            var migrationConfig = loadMigrationConfiguration(args, input);

            logger.info("Starting migration...");

            SquadToScaleMigrator migrator = createSquadToScaleMigrator(migrationConfig);

            if (args.length == 3) {
                var projectKey = args[2].toUpperCase();
                migrator.runMigration(projectKey);
            } else {
                migrator.getProjectListAndRunMigration();
            }

            logger.info("Migration completed.");

        } catch (Exception ex) {
            logger.error("Failed to execute the migration: " + ex.getMessage());
            System.exit(1);
        }
    }

    private static MigrationConfiguration loadMigrationConfiguration(String[] args, FileInputStream input) throws IOException {
        Properties prop = new Properties();
        prop.load(input);

        var host = PropertySanitizer.sanitizeHostAddress(prop.getProperty("host"));
        var pageSteps = Integer.parseInt(prop.getProperty("batchSize"));
        var cycleNamePlaceHolder = prop.getProperty("cycleNamePlaceHolder", "");
        var attachmentsMappedCsvFile = prop.getProperty("attachmentsMappedCsvFile");
        var testCaseCSVFile = prop.getProperty("testCaseMappedCsvFile");
        var testExecutionCSVFile = prop.getProperty("testExecutionMappedCsvFile");
        var databaseType = prop.getProperty("database");
        var httpVersion = prop.getProperty("httpVersion");
        var updateDatabaseFieldsPostMigration = Boolean.parseBoolean(prop.getProperty("updateDatabaseFieldsPostMigration"));
        var attachmentsBaseFolder = PropertySanitizer.sanitizeAttachmentsBaseFolder(prop.getProperty("attachmentsBaseFolder"));

        var username = args[0];
        var password = args[1];

        var apiConfig = new ApiConfiguration(host, username, password.toCharArray(), httpVersion);

        return new MigrationConfiguration(apiConfig, pageSteps, cycleNamePlaceHolder,
                attachmentsMappedCsvFile, testCaseCSVFile, testExecutionCSVFile, databaseType,
                updateDatabaseFieldsPostMigration, attachmentsBaseFolder);
    }

    private static SquadToScaleMigrator createSquadToScaleMigrator(MigrationConfiguration migrationConfig) throws IOException {
        var jiraApi = new JiraApi(migrationConfig.apiConfiguration());
        var squadApi = new SquadApi(migrationConfig.apiConfiguration());
        var scaleApi = new ScaleApi(migrationConfig.apiConfiguration());
        var csvExporter = new AttachmentsCsvExporter(migrationConfig.attachmentsMappedCsvFile());
        var testCaseCsvExporter = new TestCaseCsvExporter(migrationConfig.testCaseCSVFile());
        var testExecutionCsvExporter = new TestExecutionCsvExporter(migrationConfig.testExecutionCSVFile());
        var attachmentsCopier = new AttachmentsCopier(migrationConfig.attachmentsBaseFolder());

        var dataSourceFactory = new DataSourceFactory();
        var dataSource = dataSourceFactory.createDataSourceFromDatabaseName(migrationConfig.databaseType());

        var attachmentsCsvExporter = new AttachmentsMigrator(jiraApi, scaleApi, squadApi, dataSource,
                csvExporter, attachmentsCopier);
        var testCaseMigrator = new TestCasePostMigrator(jiraApi, testCaseCsvExporter);
        var testExecutionMigrator = new TestExecutionPostMigrator(jiraApi, testExecutionCsvExporter);

        return new SquadToScaleMigrator(jiraApi, squadApi, scaleApi, attachmentsCsvExporter,
                testCaseMigrator,
                testExecutionMigrator,
                migrationConfig);
    }
}
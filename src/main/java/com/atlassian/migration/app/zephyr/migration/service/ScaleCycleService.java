package com.atlassian.migration.app.zephyr.migration.service;

import com.atlassian.migration.app.zephyr.common.ZephyrApiException;
import com.atlassian.migration.app.zephyr.scale.api.ScaleApi;
import com.atlassian.migration.app.zephyr.scale.model.GetCycleResponse;
import com.atlassian.migration.app.zephyr.scale.model.ScaleCycleTraceLinkPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class ScaleCycleService implements Resettable {

    private static final Logger logger = LoggerFactory.getLogger(ScaleCycleService.class);

    private final Map<String, String> mapCreatedScaleCycles = new HashMap<>();
    private final Map<String, Set<String>> mapCycleLinks = new HashMap<>();
    private final ScaleApi scaleApi;
    private final String defaultCycleKey;

    public ScaleCycleService(ScaleApi scaleApi, String defaultCycleKey) {
        this.scaleApi = scaleApi;
        this.defaultCycleKey = defaultCycleKey;
    }

    @Override
    public void reset() {
        mapCreatedScaleCycles.clear();
        mapCycleLinks.clear();
    }

    public String getCycleKeyBySquadCycleName(String squadCycleName, String projectKey, String versionName) {

        var squadCycleVersion = translateSquadToScaleVersion(versionName);

        if (!mapCreatedScaleCycles.containsKey(squadCycleName)) {
            var newScaleCycleKey = createNewScaleCycle(squadCycleName, projectKey, squadCycleVersion);
            mapCreatedScaleCycles.put(squadCycleName, newScaleCycleKey);
        }

        return mapCreatedScaleCycles.get(squadCycleName);
    }

    private String translateSquadToScaleVersion(String versionName) {
        if (versionName.equalsIgnoreCase("unscheduled")) {
            return null;
        }
        return versionName;
    }

    private String createNewScaleCycle(String squadCycleName, String projectKey, String cycleVersion) {
        try {
            var scaleCycleName = defaultCycleKey.isBlank() ? squadCycleName : defaultCycleKey;

            logger.info("Creating test cycle...");

            var scaleCycleKey = scaleApi.createMigrationTestCycle(projectKey, scaleCycleName, cycleVersion);

            logger.info("Test Cycle created successfully");

            return scaleCycleKey;
        } catch (IOException exception) {
            logger.error("Failed to create new Scale cycle." + exception.getMessage(), exception);
            throw new RuntimeException(exception);
        }

    }

    public void updateIssueLinksforCycle(String projectKey) throws ZephyrApiException {
        var allCyclesInfo = scaleApi.fetchTestRunsbyProjectKey(projectKey);
        if(allCyclesInfo == null || allCyclesInfo.results() == null || allCyclesInfo.results().size() < 0){
            logger.info(String.format("No cycles found in given project %s", projectKey));
        }
        logger.info(String.format("Creating Cycle trace links for project %s", projectKey));
        var cycleIdByKey = allCyclesInfo.results().stream().collect(Collectors.toMap(GetCycleResponse::key, GetCycleResponse::id));

        mapCycleLinks.forEach( (cycleKey, issueLinks) -> {
            List<ScaleCycleTraceLinkPayload> alltraceLinksforCycleKey = new LinkedList<>();
            if(issueLinks != null && issueLinks.size() > 0 && cycleIdByKey.containsKey(cycleKey)){
                issueLinks.forEach( issueLink -> {
                    alltraceLinksforCycleKey.add(new ScaleCycleTraceLinkPayload(cycleIdByKey.get(cycleKey), issueLink, 2));
                });

                try {
                    scaleApi.createTestRunTraceLinks(alltraceLinksforCycleKey);
                    logger.info(String.format("Tracelinks updated for cycleKey: %s", cycleKey));
                } catch (ZephyrApiException e) {
                    logger.info(String.format("Unable to update Tracelinks updated for cycleKey: %s", cycleKey));
                }
            }
        });
        logger.info(String.format("All Cycle trace links created for project %s", projectKey));

    }
    public void addIssueLinkstoCycleService(String cycleKey, Set<String> issueLinks){
        if(issueLinks == null || issueLinks.size() < 0){
            return;
        }
        if(mapCycleLinks.containsKey(cycleKey)){
            mapCycleLinks.get(cycleKey).addAll(issueLinks);
        }else{
            mapCycleLinks.put(cycleKey, issueLinks);
        }
    }


}

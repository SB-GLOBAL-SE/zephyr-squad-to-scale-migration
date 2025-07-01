package com.atlassian.migration.app.zephyr.squad.model;


import java.util.List;
import java.util.Map;

public record FetchSquadExecutionStepParsedResponse(

        List<SquadExecutionStepParsedResponse> executionSteps) { }
package com.atlassian.migration.app.zephyr.migration.model;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SquadToScaleTestCaseMap extends HashMap<SquadToScaleTestCaseMap.TestCaseMapKey,String> {

    public List<Map.Entry<SquadToScaleTestCaseMap.TestCaseMapKey, String>> getListOfAllEntriesOrdered(){
        return this.entrySet().stream().sorted(Map.Entry.comparingByKey(
                Comparator.comparingInt(key -> Integer.parseInt(key.testCaseId))
        )).toList();
    }

    public record TestCaseMapKey(
            String testCaseId,
            String testCaseKey,
            Object createdBy,
            String createdOn,
            String modifiedBy,
            String modifiedOn
    ){}


}

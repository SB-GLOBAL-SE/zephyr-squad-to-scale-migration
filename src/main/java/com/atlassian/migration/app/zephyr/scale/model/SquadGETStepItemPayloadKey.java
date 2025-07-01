package com.atlassian.migration.app.zephyr.scale.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SquadGETStepItemPayloadKey {
    public String type = "STEP_BY_STEP";
    public List<ScaleGETStepItemPayload> steps = new ArrayList<>();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SquadGETStepItemPayload that = (SquadGETStepItemPayload) o;
        return Objects.equals(steps, that.steps);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, steps);
    }
}
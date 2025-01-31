package com.atlassian.migration.app.zephyr.migration.database;

public class TestExecutionMapper {
    private int id;
    private int SCALE_EXECUTION_ID;
    private String CREATED_BY;
    private String CREATED_ON;
    private String MODIFIED_BY;
    private String MODIFIED_ON;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getSCALE_EXECUTION_ID() {
        return SCALE_EXECUTION_ID;
    }

    public void setSCALE_EXECUTION_ID(int SCALE_EXECUTION_ID) {
        this.SCALE_EXECUTION_ID = SCALE_EXECUTION_ID;
    }

    public String getCREATED_BY() {
        return CREATED_BY;
    }

    public void setCREATED_BY(String CREATED_BY) {
        this.CREATED_BY = CREATED_BY;
    }

    public String getCREATED_ON() {
        return CREATED_ON;
    }

    public void setCREATED_ON(String CREATED_ON) {
        this.CREATED_ON = CREATED_ON;
    }

    public String getMODIFIED_BY() {
        return MODIFIED_BY;
    }

    public void setMODIFIED_BY(String MODIFIED_BY) {
        this.MODIFIED_BY = MODIFIED_BY;
    }

    public String getMODIFIED_ON() {
        return MODIFIED_ON;
    }

    public void setMODIFIED_ON(String MODIFIED_ON) {
        this.MODIFIED_ON = MODIFIED_ON;
    }

}

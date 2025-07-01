package com.atlassian.migration.app.zephyr.migration.database;

public class AttachmentMapper {
    private String FILE_NAME;
    private long FILE_SIZE;
    private String NAME;
    private int PROJECT_ID;
    private String USER_KEY;
    private boolean TEMPORARY;
    private String CREATED_ON;
    private String MIME_TYPE;
    private int TEST_CASE_ID;
    private int STEP_ID;
    private int TEST_RESULT_ID;
    private int TEST_SCRIPT_RESULT_ID;

    // Getters
    public String getFILE_NAME() {
        return FILE_NAME;
    }

    public long getFILE_SIZE() {
        return FILE_SIZE;
    }

    public String getNAME() {
        return NAME;
    }

    public int getPROJECT_ID() {
        return PROJECT_ID;
    }

    public String getUSER_KEY() {
        return USER_KEY;
    }

    public boolean isTEMPORARY() {
        return TEMPORARY;
    }

    public String getCREATED_ON() {
        return CREATED_ON;
    }

    public String getMIME_TYPE() {
        return MIME_TYPE;
    }

    public int getTEST_CASE_ID() {
        return TEST_CASE_ID;
    }

    public int getSTEP_ID() {
        return STEP_ID;
    }

    public int getTEST_RESULT_ID() {
        return TEST_RESULT_ID;
    }

    public int getTEST_SCRIPT_RESULT_ID() {
        return TEST_SCRIPT_RESULT_ID;
    }

    // Setters
    public void setFILE_NAME(String FILE_NAME) {
        this.FILE_NAME = FILE_NAME;
    }

    public void setFILE_SIZE(long FILE_SIZE) {
        this.FILE_SIZE = FILE_SIZE;
    }

    public void setNAME(String NAME) {
        this.NAME = NAME;
    }

    public void setPROJECT_ID(int PROJECT_ID) {
        this.PROJECT_ID = PROJECT_ID;
    }

    public void setUSER_KEY(String USER_KEY) {
        this.USER_KEY = USER_KEY;
    }

    public void setTEMPORARY(boolean TEMPORARY) {
        this.TEMPORARY = TEMPORARY;
    }

    public void setCREATED_ON(String CREATED_ON) {
        this.CREATED_ON = CREATED_ON;
    }

    public void setMIME_TYPE(String MIME_TYPE) {
        this.MIME_TYPE = MIME_TYPE;
    }

    public void setTEST_CASE_ID(int TEST_CASE_ID) {
        this.TEST_CASE_ID = TEST_CASE_ID;
    }

    public void setSTEP_ID(int STEP_ID) {
        this.STEP_ID = STEP_ID;
    }

    public void setTEST_RESULT_ID(int TEST_RESULT_ID) {
        this.TEST_RESULT_ID = TEST_RESULT_ID;
    }

    public void setTEST_SCRIPT_RESULT_ID(int TEST_SCRIPT_RESULT_ID) {
        this.TEST_SCRIPT_RESULT_ID = TEST_SCRIPT_RESULT_ID;
    }
}
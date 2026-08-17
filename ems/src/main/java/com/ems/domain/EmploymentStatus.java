package com.ems.domain;

/** Employment lifecycle state of an employee record. */
public enum EmploymentStatus {

    ACTIVE("Active"),
    ON_LEAVE("On leave"),
    TERMINATED("Terminated");

    private final String label;

    EmploymentStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}

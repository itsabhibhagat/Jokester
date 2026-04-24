package com.application.jokester.config.datasource;

public enum DataSourceType {
    PRIMARY,   // master — for writes
    REPLICA    // slave  — for reads
}

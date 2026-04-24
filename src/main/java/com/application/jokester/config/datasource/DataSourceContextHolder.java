package com.application.jokester.config.datasource;

public class DataSourceContextHolder {

    // ThreadLocal stores which datasource to use for THIS request
    // Each thread (request) has its own independent value
    // Why ThreadLocal? Because requests run in parallel threads
    // Without ThreadLocal: one request could change datasource for another request
    private static final ThreadLocal<DataSourceType> contextHolder =
            new ThreadLocal<>();

    public static void setDataSourceType(DataSourceType type) {
        contextHolder.set(type);
    }

    public static DataSourceType getDataSourceType() {
        return contextHolder.get();
    }

    public static void clearDataSourceType() {
        contextHolder.remove(); // MUST clear after request to prevent memory leaks
    }
}
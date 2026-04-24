package com.application.jokester.config.datasource;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

public class RoutingDataSource extends AbstractRoutingDataSource {

    // Spring calls this method on EVERY database operation
    // Return value determines which datasource to use
    @Override
    protected Object determineCurrentLookupKey() {
        DataSourceType type = DataSourceContextHolder.getDataSourceType();

        // If nothing set → default to PRIMARY (safe for writes)
        return type != null ? type : DataSourceType.PRIMARY;
    }
}

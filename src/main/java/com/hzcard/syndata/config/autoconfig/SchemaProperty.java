package com.hzcard.syndata.config.autoconfig;

import java.util.Map;

/**
 * Created by zhangwei on 2017/2/25.
 */
public class SchemaProperty {

    /**
     * table名称，跟reporsitory映射
     */
    private Map<String,TableRepository> tableRepository;

    private DataSourcePro sourceDataSource;

    private DataSourcePro targetDataSource;

    public Map<String, TableRepository> getTableRepository() {
        return tableRepository;
    }

    public String includeSynTables;

    public void setTableRepository(Map<String, TableRepository> tableRepository) {
        this.tableRepository = tableRepository;
    }

    public DataSourcePro getSourceDataSource() {
        return sourceDataSource;
    }

    public void setSourceDataSource(DataSourcePro sourceDataSource) {
        this.sourceDataSource = sourceDataSource;
    }

    public DataSourcePro getTargetDataSource() {
        return targetDataSource;
    }

    public String getIncludeSynTables() {
        return includeSynTables;
    }

    public void setIncludeSynTables(String includeSynTables) {
        this.includeSynTables = includeSynTables;
    }

    public void setTargetDataSource(DataSourcePro targetDataSource) {
        this.targetDataSource = targetDataSource;
    }
}

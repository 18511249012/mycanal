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


    private DataSourcePro targetDataSource;

    public Map<String, TableRepository> getTableRepository() {
        return tableRepository;
    }

    public String includeSynTables;

    public String excludeSynTables;

    /**
     * 表名映射
     */
    private Map<String,String> tableNameMappings;

    public void setTableRepository(Map<String, TableRepository> tableRepository) {
        this.tableRepository = tableRepository;
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

    public String getExcludeSynTables() {
        return excludeSynTables;
    }

    public void setExcludeSynTables(String excludeSynTables) {
        this.excludeSynTables = excludeSynTables;
    }

    public Map<String, String> getTableNameMappings() {
        return tableNameMappings;
    }

    public void setTableNameMappings(Map<String, String> tableNameMappings) {
        this.tableNameMappings = tableNameMappings;
    }
}

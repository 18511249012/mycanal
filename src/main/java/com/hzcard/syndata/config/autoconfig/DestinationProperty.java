package com.hzcard.syndata.config.autoconfig;

public class DestinationProperty {


    private String includeSchemas;

    /**
     * 数据库配置
     */
    private MysqlClientProperties mysql;


    public String getIncludeSchemas() {
        return includeSchemas;
    }

    public void setIncludeSchemas(String includeSchemas) {
        this.includeSchemas = includeSchemas;
    }

    public MysqlClientProperties getMysql() {
        return mysql;
    }

    public void setMysql(MysqlClientProperties mysql) {
        this.mysql = mysql;
    }

}

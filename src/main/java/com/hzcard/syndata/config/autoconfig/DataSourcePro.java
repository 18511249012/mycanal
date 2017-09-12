package com.hzcard.syndata.config.autoconfig;

/**
 * Created by zhangwei on 2017/2/22.
 */
public class DataSourcePro {

    private String driverClassName;

    private String username;
    private String password;
    private String url;
    private String basePackage;

    public String getDriverClassName() {
        return driverClassName;
    }

    public void setDriverClassName(String driverClassName) {
        this.driverClassName = driverClassName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUrl() {
        return url;
    }

    public String getBasePackage() {
        return basePackage;
    }

    public void setBasePackage(String basePackage) {
        this.basePackage = basePackage;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}

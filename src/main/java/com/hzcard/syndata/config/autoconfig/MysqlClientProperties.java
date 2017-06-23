package com.hzcard.syndata.config.autoconfig;

/**
 * Created by zhangwei on 2017/4/27.
 */
public class MysqlClientProperties {

    /**
     * 通道唯一标识
     */
    private String myChannel;

    /**
     * 当前slave的serverId，不要跟master的serverId一样
     */
    private Long serverId;

    private Long keepalive;

    private String host;

    private Integer port;

    private String user;

    private String password;

    private Long timeout;

    private String whitelist;

    private String blacklist;



    private Long preloadTimeOut;

    private String preloadDatabases;

    /**
     * 是否包含数据
     */
    private Boolean isIncludeData;

    /**
     * 包含数据的情况下是否加密
     */
    private Boolean isEncryptor;

    public Long getServerId() {
        return serverId;
    }

    public void setServerId(Long serverId) {
        this.serverId = serverId;
    }

    public Long getKeepalive() {
        return keepalive;
    }

    public void setKeepalive(Long keepalive) {
        this.keepalive = keepalive;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Long getTimeout() {
        return timeout;
    }

    public void setTimeout(Long timeout) {
        this.timeout = timeout;
    }

    public String getWhitelist() {
        return whitelist;
    }

    public void setWhitelist(String whitelist) {
        this.whitelist = whitelist;
    }

    public String getBlacklist() {
        return blacklist;
    }

    public void setBlacklist(String blacklist) {
        this.blacklist = blacklist;
    }



    public Long getPreloadTimeOut() {
        return preloadTimeOut;
    }

    public void setPreloadTimeOut(Long preloadTimeOut) {
        this.preloadTimeOut = preloadTimeOut;
    }

    public String getPreloadDatabases() {
        return preloadDatabases;
    }

    public void setPreloadDatabases(String preloadDatabases) {
        this.preloadDatabases = preloadDatabases;
    }

    public String getMyChannel() {
        return myChannel;
    }

    public void setMyChannel(String myChannel) {
        this.myChannel = myChannel;
    }

    public Boolean getIncludeData() {
        return isIncludeData;
    }

    public void setIncludeData(Boolean includeData) {
        isIncludeData = includeData;
    }

    public Boolean getEncryptor() {
        return isEncryptor;
    }

    public void setEncryptor(Boolean encryptor) {
        isEncryptor = encryptor;
    }
}

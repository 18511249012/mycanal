package com.hzcard.syndata.config.autoconfig;

/**
 * Created by zhangwei on 2017/5/2.
 */
public class Encryptor {
    /**
     * 加密消息字段
     */
    private String encryptFields;
    /**
     * 加密超时时间
     */
    private Long timeOut;

    /**
     * 加密算法
     */
    private String cipher="AES";

    /**
     * 加密key
     */
    private String key ="yRVCuDlryZdWhSUDwLNugQ==";

    public String getEncryptFields() {
        return encryptFields;
    }

    public void setEncryptFields(String encryptFields) {
        this.encryptFields = encryptFields;
    }

    public Long getTimeOut() {
        return timeOut;
    }

    public void setTimeOut(Long timeOut) {
        this.timeOut = timeOut;
    }

    public String getCipher() {
        return cipher;
    }

    public void setCipher(String cipher) {
        this.cipher = cipher;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }
}

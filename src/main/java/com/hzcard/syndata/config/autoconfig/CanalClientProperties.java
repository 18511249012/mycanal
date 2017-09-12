package com.hzcard.syndata.config.autoconfig;

import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "hzcard.syndata")
public class CanalClientProperties {

	private String zkServers;
	
	
	private Map<String, DestinationProperty> destinations;

	private Map<String,SchemaProperty>  schemas;

	private Encryptor encryptor;

	private Integer transCacheCount = 1024;


	public String getZkServers() {
		return zkServers;
	}


	public void setZkServers(String zkServers) {
		this.zkServers = zkServers;
	}


	public Map<String, DestinationProperty> getDestinations() {
		return destinations;
	}


	public void setDestinations(Map<String, DestinationProperty> destinations) {
		this.destinations = destinations;
	}

	public Map<String, SchemaProperty> getSchemas() {
		return schemas;
	}

	public void setSchemas(Map<String, SchemaProperty> schemas) {
		this.schemas = schemas;
	}

	public Encryptor getEncryptor() {
		return encryptor;
	}

	public void setEncryptor(Encryptor encryptor) {
		this.encryptor = encryptor;
	}

	public Integer getTransCacheCount() {
		return transCacheCount;
	}

	public void setTransCacheCount(Integer transCacheCount) {
		this.transCacheCount = transCacheCount;
	}
}

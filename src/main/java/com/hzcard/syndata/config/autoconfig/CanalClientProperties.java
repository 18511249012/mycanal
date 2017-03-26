package com.hzcard.syndata.config.autoconfig;

import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "hzcard.syndata")
public class CanalClientProperties {

	private String zkServers;
	
	
	private Map<String, DestinationProperty> destinations;

	private Map<String,SchemaProperty>  schemas;


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
}

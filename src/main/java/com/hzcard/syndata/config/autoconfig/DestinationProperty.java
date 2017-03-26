package com.hzcard.syndata.config.autoconfig;

public class DestinationProperty {
	


//	private Map<String,PhysicalInstanceProperty> instanceConfig;
	
	private String userName;
	
	private String password;

	private String includeSchemas;


	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getIncludeSchemas() {
		return includeSchemas;
	}

	public void setIncludeSchemas(String includeSchemas) {
		this.includeSchemas = includeSchemas;
	}
}

package com.hzcard.syndata.config.autoconfig;

public class DataBaseProperty {

	/**
	 * userName、password、jdbcUrl等
	 */
	private String userName;
	
	private String password;
	
	private String jdbcUrl;

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

	public String getJdbcUrl() {
		return jdbcUrl;
	}

	public void setJdbcUrl(String jdbcUrl) {
		this.jdbcUrl = jdbcUrl;
	}

	
	
	
}

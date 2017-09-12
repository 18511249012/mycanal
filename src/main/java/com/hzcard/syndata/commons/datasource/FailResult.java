package com.hzcard.syndata.commons.datasource;

public class FailResult {

	private String result="fail";
	
	private String errmsg;
	
	public FailResult(String errmsg){
		this.errmsg = errmsg;
	}

	public String getResult() {
		return result;
	}

	public String getErrmsg() {
		return errmsg;
	}
	
}

package com.hzcard.syndata.config.autoconfig;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.ApplicationContext;

import com.hzcard.syndata.datadeal.AbstractCanalClientScala;

;

public class CanalClientContext implements Serializable,DisposableBean {

	private static Logger logger = LoggerFactory.getLogger(CanalClientContext.class);

	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
 
	
	private CanalClientContext(){}
	
	private Map<String,AbstractCanalClientScala> clients = new HashMap<>();
	
	/**
	 * reset endPoint 与 repository 的class映射
	 */
	private Map<String,Class> resetEndPoint = new ConcurrentHashMap<String,Class>();
	
	/**
	 *  repository 的class与 destination映射
	 */
	private Map<Class,String> repositoryDestination = new ConcurrentHashMap<Class,String>();
	
	private ApplicationContext applicationContext;
	
	private CanalClientProperties canalClientProperties;
	
	
	
	public CanalClientProperties getCanalClientProperties() {
		return canalClientProperties;
	}

	
	public CanalClientContext(ApplicationContext applicationContext,CanalClientProperties canalClientProperties) {
		this.canalClientProperties = canalClientProperties;
		this.applicationContext = applicationContext;
	}
	
	public ApplicationContext getApplicationContext(){
		return this.applicationContext;
	}

	public AbstractCanalClientScala getCanalClient(String destination){
		return clients.get(destination);
	}
	
	public boolean isExsits(String destination){
		return clients.containsKey(destination);
	}
	
	protected AbstractCanalClientScala regiestSimpleCanalClient(String destination,AbstractCanalClientScala client){
		logger.error("client length is:{}",this.clients.size());
		return this.clients.put(destination,client);
	}

	@Override
	public void destroy() throws Exception {
		logger.error("canalclientContext destroy");
		for(AbstractCanalClientScala client :clients.values()){
			client.stop();
		}
	}

	protected Class regiestResetEndPoint(String resetEndPoint,Class repository){
		return this.resetEndPoint.computeIfAbsent(resetEndPoint, k->repository);
	}
	
	public Class getRepositoryByEndPoint(String resetEndPoint){
		return this.resetEndPoint.get(resetEndPoint);
	}
	
	public String getDestinationByRepository(Class repository){
		return this.repositoryDestination.get(repository);
	}

	protected void startClient() {
		for(AbstractCanalClientScala client :this.clients.values()){
			logger.error("canal client start");
			client.start();
		}
		
	}

}

package com.hzcard.syndata;

import com.hzcard.syndata.config.autoconfig.CanalClientContext;
import mousio.etcd4j.EtcdClient;
import mousio.etcd4j.transport.EtcdNettyClient;
import mousio.etcd4j.transport.EtcdNettyConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.web.support.SpringBootServletInitializer;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.etcd.EtcdProperties;
import org.springframework.cloud.etcd.discovery.EtcdDiscoveryClientConfiguration;
import org.springframework.cloud.etcd.discovery.EtcdLifecycle;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.net.URI;

@SpringBootApplication(exclude = { DataSourceAutoConfiguration.class,DataSourceTransactionManagerAutoConfiguration.class })
@EnableElasticsearchRepositories(basePackages = "com/hzcard/syndata")
@EnableTransactionManagement
@EnableDiscoveryClient
@ComponentScan("com.hzcard")
public class Application   implements CommandLineRunner{

	public static void main(String[] args) throws ClassNotFoundException {
		SpringApplication.run(Application.class, args);
	}

	@Autowired
	EtcdProperties etcdProperties;

	@Bean
	public EtcdClient etcdClient() {
		EtcdNettyConfig config = new EtcdNettyConfig();
		config.setMaxFrameSize(1024 * 1024); // Desired max size
		EtcdNettyClient nettyClient = new EtcdNettyClient(config, etcdProperties.getUris().toArray(new URI[] {}));
		return new EtcdClient(nettyClient);
	}

	@Autowired
	CanalClientContext canalClientContext;

	Logger logger = LoggerFactory.getLogger(Application.class);
	@Override
	public void run(String... args) throws Exception {
		logger.info("runner canalClientContext start");
		canalClientContext.start();
	}
}

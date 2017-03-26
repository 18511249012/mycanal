package com.hzcard.syndata;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication(exclude = { DataSourceAutoConfiguration.class,DataSourceTransactionManagerAutoConfiguration.class })
@EnableDiscoveryClient
@EnableElasticsearchRepositories(basePackages = "com/hzcard/syndata")
@EnableTransactionManagement
@ComponentScan("com.hzcard")
public class Application {

	public static void main(String[] args) throws ClassNotFoundException {
		SpringApplication.run(Application.class, args);
	}



}

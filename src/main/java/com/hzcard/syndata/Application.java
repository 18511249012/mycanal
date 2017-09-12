package com.hzcard.syndata;

import java.net.URI;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.etcd.EtcdProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import mousio.etcd4j.EtcdClient;
import mousio.etcd4j.transport.EtcdNettyClient;
import mousio.etcd4j.transport.EtcdNettyConfig;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class, DataSourceTransactionManagerAutoConfiguration.class})
@EnableElasticsearchRepositories(basePackages = "com/hzcard/syndata")
@EnableTransactionManagement
@EnableDiscoveryClient
@ComponentScan("com.hzcard")
public class Application implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) throws ClassNotFoundException {
        ConfigurableApplicationContext context = SpringApplication.run(Application.class, args);
    }

    @Autowired
    EtcdProperties etcdProperties;

    @Bean
    public GracefulShutdown gracefulShutdown(){
        return new GracefulShutdown(2L,TimeUnit.SECONDS);
    }

    @Bean
    public EtcdClient etcdClient() {
        EtcdNettyConfig config = new EtcdNettyConfig();
        config.setMaxFrameSize(1024 * 1024); // Desired max size
        EtcdNettyClient nettyClient = new EtcdNettyClient(config, etcdProperties.getUris().toArray(new URI[]{}));
        return new EtcdClient(nettyClient);
    }

    @Autowired
    CanalClientContext canalClientContext;


    @Override
    public void run(String... args) throws Exception {
        logger.info("runner canalClientContext start");
        canalClientContext.start();
    }
}

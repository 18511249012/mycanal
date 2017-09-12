package com.hzcard.syndata.config.autoconfig;

import org.apache.tomcat.jdbc.pool.PoolProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.embedded.EmbeddedServletContainerInitializedEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.lookup.MapDataSourceLookup;

@Configuration
@EnableConfigurationProperties(CanalClientProperties.class)
public class CanalClientInstanceAutoConfig implements ApplicationListener<EmbeddedServletContainerInitializedEvent> {

    @Autowired
    private CanalClientProperties canalClientProperties;

    private static int serverPort;


    @Bean("mapDataSource")
    public MapDataSourceLookup initAllDataSource() {
        MapDataSourceLookup mapDataSource = new MapDataSourceLookup();

        for (String schema : canalClientProperties.getSchemas().keySet()) {

            DataSourcePro dataSourcePro = canalClientProperties.getSchemas().get(schema).getSourceDataSource();
            DataSourcePro targetDataSourcePro = canalClientProperties.getSchemas().get(schema).getTargetDataSource();
            if (dataSourcePro != null) {
//        		mapDataSource.addDataSource("source" + schema, DataSourceBuilder.create().driverClassName(dataSourcePro.getDriverClassName()).username(dataSourcePro.getUsername()).password(dataSourcePro.getPassword()).url(dataSourcePro.getUrl()).build());
            	PoolProperties poolProp = new PoolProperties();
            	poolProp.setDriverClassName(dataSourcePro.getDriverClassName());
            	poolProp.setUrl(dataSourcePro.getUrl());
        		poolProp.setUsername(dataSourcePro.getUsername());
        		poolProp.setPassword(dataSourcePro.getPassword());
        		poolProp.setMaxActive(200);
        		poolProp.setTestOnBorrow(true);
        		poolProp.setValidationQuery("SELECT 1");
        		org.apache.tomcat.jdbc.pool.DataSource dataSource = new org.apache.tomcat.jdbc.pool.DataSource(poolProp);
        		mapDataSource.addDataSource("source" + schema, dataSource);
            }
            	
            if (targetDataSourcePro != null){
//                mapDataSource.addDataSource("target" + schema, DataSourceBuilder.create().driverClassName(targetDataSourcePro.getDriverClassName()).username(targetDataSourcePro.getUsername()).password(targetDataSourcePro.getPassword()).url(targetDataSourcePro.getUrl()).build());
                PoolProperties poolProp = new PoolProperties();
            	poolProp.setDriverClassName(targetDataSourcePro.getDriverClassName());
            	poolProp.setUrl(targetDataSourcePro.getUrl());
        		poolProp.setUsername(targetDataSourcePro.getUsername());
        		poolProp.setPassword(targetDataSourcePro.getPassword());
        		poolProp.setMaxActive(1000);
        		poolProp.setMaxWait(60 * 1000);
                poolProp.setTestOnBorrow(true);
                poolProp.setValidationQuery("SELECT 1");
        		org.apache.tomcat.jdbc.pool.DataSource dataSource = new org.apache.tomcat.jdbc.pool.DataSource(poolProp);
        		mapDataSource.addDataSource("target" + schema, dataSource);
            }
        }

        return mapDataSource;
    }


    @Override
    public void onApplicationEvent(EmbeddedServletContainerInitializedEvent event) {
    	CanalClientInstanceAutoConfig.serverPort = event.getEmbeddedServletContainer().getPort();
    }

    public static int getPort() {
        return serverPort;
    }

}

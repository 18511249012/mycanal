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

        for (DestinationProperty dp : canalClientProperties.getDestinations().values()) {
            MysqlClientProperties properties = dp.getMysql();
            PoolProperties poolProp = new PoolProperties();
            poolProp.setDriverClassName("com.mysql.jdbc.Driver");
            poolProp.setUrl("jdbc:mysql://"+properties.getHost()+":"+properties.getPort()+"/mysql?useUnicode=true&characterEncoding=utf8");
            poolProp.setUsername(properties.getUser());
            poolProp.setPassword(properties.getPassword());
            poolProp.setMaxActive(2);
            poolProp.setInitialSize(1);
            poolProp.setTestOnBorrow(true);
            poolProp.setValidationQuery("SELECT 1");
            poolProp.setDefaultAutoCommit(true);
            org.apache.tomcat.jdbc.pool.DataSource dataSource = new org.apache.tomcat.jdbc.pool.DataSource(poolProp);
            mapDataSource.addDataSource("channel" + properties.getMyChannel(), dataSource);
        }

        for (String schema : canalClientProperties.getSchemas().keySet()) {

            DataSourcePro targetDataSourcePro = canalClientProperties.getSchemas().get(schema).getTargetDataSource();
            if (targetDataSourcePro != null) {
                PoolProperties poolProp = new PoolProperties();
                poolProp.setDriverClassName(targetDataSourcePro.getDriverClassName());
                poolProp.setUrl(targetDataSourcePro.getUrl());
                poolProp.setUsername(targetDataSourcePro.getUsername());
                poolProp.setPassword(targetDataSourcePro.getPassword());
                poolProp.setMaxActive(5);
                poolProp.setMaxWait(60 * 1000);
                poolProp.setTestOnBorrow(true);
                poolProp.setDefaultAutoCommit(true);
                if (targetDataSourcePro.getDriverClassName().indexOf("oracle") >= 0)
                    poolProp.setValidationQuery("select 1 from dual");
                else
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

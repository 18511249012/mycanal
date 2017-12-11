package com.hzcard.syndata.controller;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import com.hzcard.syndata.CanalClientContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.data.repository.Repository;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.TypeInformation;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.datasource.lookup.MapDataSourceLookup;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.hzcard.syndata.commons.datasource.FailResult;
import com.hzcard.syndata.commons.datasource.SuccessResult;
import com.hzcard.syndata.config.autoconfig.CanalClientProperties;
import com.hzcard.syndata.config.autoconfig.TableRepository;

@RestController
@RequestMapping("reset")
public class ResetDomainController {

    private static Logger logger = LoggerFactory.getLogger(ResetDomainController.class);

    @Autowired
    private CanalClientContext clientContext;

    private int PAGE_SIZE = 1000;

    protected static boolean isRestDomain = true;

    @Autowired
    private CanalClientProperties clientPropertis;

    @Autowired
    private MapDataSourceLookup sources;
    private ResultSet set;

    @Autowired
    private ApplicationContext applicationContext;

    @RequestMapping(path = "/{reporsitory}", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<Object> resetCreateIndex(@PathVariable String reporsitory) {
        Class repositoryClass = clientContext.getRepositoryByEndPoint(reporsitory);
        if (repositoryClass == null)
            return ResponseEntity.ok(new FailResult("找不到对应的入口点"));
        Connection conn = null;
        try {
            for (String schema : clientPropertis.getSchemas().keySet()) {
                if (clientPropertis.getSchemas().get(schema).getTableRepository() != null)
                    for (String tableName : clientPropertis.getSchemas().get(schema).getTableRepository().keySet()) {
                        TableRepository table = clientPropertis.getSchemas().get(schema).getTableRepository()
                                .get(tableName);
                        if (table != null && table.getResetEndPoint() != null)
                            if (table.getResetEndPoint().equals(reporsitory)) {
                                // 查询数据库
                                DataSource dataSource = sources.getDataSource("source" + schema);
                                conn = dataSource.getConnection();

                                Statement stm = conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
                                String sql = "select * from " + tableName + " Order By ID ASC";
                                if ("com.hzcard.syndata.points.repositories.OperationOrderRepository".equals(table.getRepository()) || "com.hzcard.syndata.points.repositories.OrderRepository".equals(table.getRepository()))
                                    sql = "select *,code as code_search,card_no as card_no_search from " + schema + "." + tableName + " Order By ID ASC";
                                String sqlCount = "select count(1) from " + tableName;
                                ResultSet set = stm.executeQuery(sqlCount);
                                int dataCount = 0;
                                while (set.next()) {
                                    dataCount = set.getInt(1);
                                }
                                int totalPageNum = (dataCount + PAGE_SIZE - 1) / PAGE_SIZE;

                                // "+clientPropertis.getDestinations().get(key)\
                                set.close();
                                stm.close();
                                conn.close();
                                for (int pageNum = 1; pageNum <= totalPageNum; pageNum++) {
                                    conn = dataSource.getConnection();
                                    stm = conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
                                    stm.setFetchSize(PAGE_SIZE);
                                    stm.setMaxRows(PAGE_SIZE);
                                    set = stm.executeQuery(sql + " Limit " + (pageNum - 1) * PAGE_SIZE + ", " + PAGE_SIZE);
                                    ClassTypeInformation cT = ClassTypeInformation.from(repositoryClass); // 解析获得domaiin
                                    List<TypeInformation<?>> arguments = cT.getSuperTypeInformation(Repository.class)
                                            .getTypeArguments();
                                    BeanPropertyRowMapper dd = new BeanPropertyRowMapper(arguments.get(0).getType()); // domain\BeanPropertyRowMapper
                                    int rowNum = 0;
                                    List results = new ArrayList();
                                    while (set.next()) {
                                        results.add(dd.mapRow(set, rowNum++));
                                    }
                                    ElasticsearchRepository rep = (ElasticsearchRepository) applicationContext
                                            .getBean(repositoryClass);
                                    rep.save(results); // 存储es
                                    set.close();
                                    stm.close();
                                    conn.close();
                                }
                            }

                    }
            }
        } catch (SQLException e) {
            logger.error("数据库操作失败", e);

            return ResponseEntity.ok(new FailResult(e.getMessage()));
        } catch (Exception e) {
            logger.error("重建索引失败", e);

            return ResponseEntity.ok(new FailResult(e.getMessage()));
        } finally {
            if (conn != null)
                try {
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
        }
        return ResponseEntity.ok(new SuccessResult());
    }

}

package com.hzcard.syndata.exchange.repositories;

import com.hzcard.syndata.exchange.index.SearchProductVo;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface ProductRepository extends ElasticsearchRepository<SearchProductVo, String> {

}

package com.hzcard.syndata.reorder.repositories;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import com.hzcard.syndata.reorder.index.ReOperationOrder;

public interface ReoperationOrderRepository extends ElasticsearchRepository<ReOperationOrder, String>{

}

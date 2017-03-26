package com.hzcard.syndata.points.repositories;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import com.hzcard.syndata.points.index.OperationOrderIndex;

@Repository
public interface OperationOrderRepository extends ElasticsearchRepository<OperationOrderIndex, String> {

}

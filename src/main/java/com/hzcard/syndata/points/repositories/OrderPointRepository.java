package com.hzcard.syndata.points.repositories;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import com.hzcard.syndata.points.index.OrderPointIndex;

@Repository
public interface OrderPointRepository extends ElasticsearchRepository<OrderPointIndex, String> {

}

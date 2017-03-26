package com.hzcard.syndata.points.repositories;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import com.hzcard.syndata.points.index.OrderOfferExprIndex;

@Repository
public interface OrderOfferExprRepository extends ElasticsearchRepository<OrderOfferExprIndex, String> {

}

package com.hzcard.syndata.reorder.repositories;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import com.hzcard.syndata.reorder.index.ReOrder;


public interface ReorderRepository extends ElasticsearchRepository<ReOrder, String>{

}

package com.hzcard.syndata.points.index;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldIndex;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Document(indexName="order_offer_expr",type = "order_offer_expr")
public class OrderOfferExprIndex {
	
	public static final String INDICE = "order_offer_expr";
	public static final String TYPE = "order_offer_expr";

	@Id
	@Field(index = FieldIndex.not_analyzed, store = true)
	private String id;
	@Field(type=FieldType.String, index = FieldIndex.not_analyzed, store = true)
	//@JsonProperty("card_no")
	private String card_no;
	@Field(type=FieldType.String, index = FieldIndex.not_analyzed, store = true)
	//@JsonProperty("order_id")
	private String order_id;
	@Field(type=FieldType.String, index = FieldIndex.not_analyzed, store = true)
	//@JsonProperty("offer_x_id")
	private String offer_x_id;
	@Field(type=FieldType.String, index = FieldIndex.not_analyzed, store = true)
	//@JsonProperty("offer_y_id")
	private String offer_y_id;
	
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getCard_no() {
		return card_no;
	}
	public void setCard_no(String card_no) {
		this.card_no = card_no;
	}
	public String getOrder_id() {
		return order_id;
	}
	public void setOrder_id(String order_id) {
		this.order_id = order_id;
	}
	public String getOffer_x_id() {
		return offer_x_id;
	}
	public void setOffer_x_id(String offer_x_id) {
		this.offer_x_id = offer_x_id;
	}
	public String getOffer_y_id() {
		return offer_y_id;
	}
	public void setOffer_y_id(String offer_y_id) {
		this.offer_y_id = offer_y_id;
	}
	
}

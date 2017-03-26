package com.hzcard.syndata.points.index;

import java.util.Date;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldIndex;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Document(indexName="operation_order_point",type = "operation_order_point")
public class OperationOrderPointIndex {
	
	public static final String INDICE = "operation_order_point";
	public static final String TYPE ="operation_order_point";
	
	@Id
	@Field(type = FieldType.String, index = FieldIndex.not_analyzed, store = true)
	private String id;
	
	@Field(type = FieldType.String, index = FieldIndex.not_analyzed, store = true)
	private String operation_order_id;
	
	@Field(type = FieldType.String, index = FieldIndex.not_analyzed, store = true)
	private String order_point_code;
	
//	@Field(type = FieldType.String, index = FieldIndex.not_analyzed, store = true)
//	private String orderPointId;
	@Field(type = FieldType.String, index = FieldIndex.not_analyzed, store = true)
	private String order_id;
	
	@Field(type = FieldType.String, index = FieldIndex.not_analyzed, store = true)
	private String order_code;
	
	@Field(type = FieldType.String, index = FieldIndex.not_analyzed, store = true)
	private String partner_id;
	
	@Field(type = FieldType.String, index = FieldIndex.not_analyzed, store = true)
	private String partner_code;
	
	@Field(type = FieldType.String, index = FieldIndex.not_analyzed, store = true)
	private String shop_id;
	
	@Field(type = FieldType.String, index = FieldIndex.not_analyzed, store = true)
	private String shop_code;
	
	@Field(type = FieldType.String, index = FieldIndex.not_analyzed, store = true)
	private String description;
	
	@Field(type = FieldType.Long, index = FieldIndex.not_analyzed, store = true)
	private long basic_point;
	
	@Field(type = FieldType.Long, index = FieldIndex.not_analyzed, store = true)
	private long reward_point;
	
	@Field(type = FieldType.Long, index = FieldIndex.not_analyzed, store = true)
	private long total_point;
	
	@Field(type = FieldType.Date, index = FieldIndex.not_analyzed, store = true)
	private Date trade_time;
	
	@Field(type = FieldType.Date, index = FieldIndex.not_analyzed, store = true)
	private Date point_time;
	
	@Field(type = FieldType.Integer, index = FieldIndex.not_analyzed, store = true)
	private int offer_type;
	
	@Field(type = FieldType.String, index = FieldIndex.not_analyzed, store = true)
	//@JsonProperty("offer_id")
	private String offer_id;
	
	@Field(type = FieldType.String, index = FieldIndex.not_analyzed, store = true)
	//@JsonProperty("offer_code")
	private String offer_code;
	
	@Field(type = FieldType.String, index = FieldIndex.not_analyzed, store = true)
	//@JsonProperty("card_no")
	private String card_no;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getOperation_order_id() {
		return operation_order_id;
	}

	public void setOperation_order_id(String operation_order_id) {
		this.operation_order_id = operation_order_id;
	}

	public String getOrder_point_code() {
		return order_point_code;
	}

	public void setOrder_point_code(String order_point_code) {
		this.order_point_code = order_point_code;
	}

	public String getOrder_id() {
		return order_id;
	}

	public void setOrder_id(String order_id) {
		this.order_id = order_id;
	}

	public String getOrder_code() {
		return order_code;
	}

	public void setOrder_code(String order_code) {
		this.order_code = order_code;
	}

	public String getPartner_id() {
		return partner_id;
	}

	public void setPartner_id(String partner_id) {
		this.partner_id = partner_id;
	}

	public String getPartner_code() {
		return partner_code;
	}

	public void setPartner_code(String partner_code) {
		this.partner_code = partner_code;
	}

	public String getShop_id() {
		return shop_id;
	}

	public void setShop_id(String shop_id) {
		this.shop_id = shop_id;
	}

	public String getShop_code() {
		return shop_code;
	}

	public void setShop_code(String shop_code) {
		this.shop_code = shop_code;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public long getBasic_point() {
		return basic_point;
	}

	public void setBasic_point(long basic_point) {
		this.basic_point = basic_point;
	}

	public long getReward_point() {
		return reward_point;
	}

	public void setReward_point(long reward_point) {
		this.reward_point = reward_point;
	}

	public long getTotal_point() {
		return total_point;
	}

	public void setTotal_point(long total_point) {
		this.total_point = total_point;
	}

	public Date getTrade_time() {
		return trade_time;
	}

	public void setTrade_time(Date trade_time) {
		this.trade_time = trade_time;
	}

	public Date getPoint_time() {
		return point_time;
	}

	public void setPoint_time(Date point_time) {
		this.point_time = point_time;
	}

	public int getOffer_type() {
		return offer_type;
	}

	public void setOffer_type(int offer_type) {
		this.offer_type = offer_type;
	}

	public String getOffer_id() {
		return offer_id;
	}

	public void setOffer_id(String offer_id) {
		this.offer_id = offer_id;
	}

	public String getOffer_code() {
		return offer_code;
	}

	public void setOffer_code(String offer_code) {
		this.offer_code = offer_code;
	}

	public String getCard_no() {
		return card_no;
	}

	public void setCard_no(String card_no) {
		this.card_no = card_no;
	}
	
}

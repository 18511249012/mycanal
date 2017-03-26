package com.hzcard.syndata.points.index;

import java.math.BigDecimal;
import java.util.Date;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldIndex;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Document(indexName = "order_product", type = "order_product")
public class OrderProductIndex {
	
	public static final String INDICE = "order_product";
	public static final String TYPE = "order_product";

	@Id
	@Field(type = FieldType.String, index = FieldIndex.not_analyzed, store = true)
	private String id;
	@Field(type = FieldType.String, index = FieldIndex.not_analyzed, store = true)
	//@JsonProperty("card_no")
	private String card_no;
	@Field(type = FieldType.String, index = FieldIndex.not_analyzed, store = true)
	private String code;	// 订单明细编号
	@Field(type = FieldType.String, index = FieldIndex.not_analyzed, store = true)
	//@JsonProperty("order_id")
	private String order_id;	// 订单id
	@Field(type = FieldType.String, index = FieldIndex.not_analyzed, store = true)
	//@JsonProperty("order_code")
	private String order_code;	// 订单编号
	@Field(type = FieldType.String, index = FieldIndex.not_analyzed, store = true)
	//@JsonProperty("partner_id")
	private String partner_id;	// 合作伙伴id
	@Field(type = FieldType.String, index = FieldIndex.not_analyzed, store = true)
	//@JsonProperty("partner_code")
	private String partner_code;	// 合作伙伴编码
	@Field(type = FieldType.String, index = FieldIndex.not_analyzed, store = true)
	//@JsonProperty("shop_id")
	private String shop_id;	// 分店id
	@Field(type = FieldType.String, index = FieldIndex.not_analyzed, store = true)
	//@JsonProperty("shop_code")
	private String shop_code;	// 分店编码
	@Field(type = FieldType.String, index = FieldIndex.not_analyzed, store = true)
	//@JsonProperty("partner_category_id")
	private String partner_category_id;	// 商家品类id
	@Field(type = FieldType.String, index = FieldIndex.not_analyzed, store = true)
	//@JsonProperty("partner_category_code")
	private String partner_category_code;	// 商家品类编码
	@Field(type = FieldType.String, index = FieldIndex.not_analyzed, store = true)
	//@JsonProperty("partner_product_id")
	private String partner_product_id;	// 商家商品id
	@Field(type = FieldType.String, index = FieldIndex.not_analyzed, store = true)
	//@JsonProperty("partner_product_code")
	private String partner_product_code;	// 商家商品编码
	@Field(type = FieldType.String, index = FieldIndex.not_analyzed, store = true)
	//@JsonProperty("description")
	private String description;	// 商品描述
	@Field(type = FieldType.Double, index = FieldIndex.not_analyzed, store = true)
	//@JsonProperty("amount")
	private BigDecimal amount;	// 单品数量
	@Field(type = FieldType.Double, index = FieldIndex.not_analyzed, store = true)
	//@JsonProperty("unit_price")
	private BigDecimal unit_price;	// 单价
	@Field(type = FieldType.Double, index = FieldIndex.not_analyzed, store = true)
	//@JsonProperty("total_price")
	private BigDecimal total_price;	// 单品总价
	@Field(type = FieldType.Date, index = FieldIndex.not_analyzed, store = true)
	//@JsonProperty("trade_time")
	private Date trade_time;	// 交易时间
	@Field(type = FieldType.Date, index = FieldIndex.not_analyzed, store = true)
	//@JsonProperty("point_time")
	private Date point_time;	// 积分时间
	@Field(type = FieldType.String, index = FieldIndex.not_analyzed, store = true)
	private String remark;
	
	@Field(type = FieldType.Integer, index = FieldIndex.not_analyzed, store = true)
	private Integer earn;
	
	
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
	public String getCode() {
		return code;
	}
	public void setCode(String code) {
		this.code = code;
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
	public String getPartner_category_id() {
		return partner_category_id;
	}
	public void setPartner_category_id(String partner_category_id) {
		this.partner_category_id = partner_category_id;
	}
	public String getPartner_category_code() {
		return partner_category_code;
	}
	public void setPartner_category_code(String partner_category_code) {
		this.partner_category_code = partner_category_code;
	}
	public String getPartner_product_id() {
		return partner_product_id;
	}
	public void setPartner_product_id(String partner_product_id) {
		this.partner_product_id = partner_product_id;
	}
	public String getPartner_product_code() {
		return partner_product_code;
	}
	public void setPartner_product_code(String partner_product_code) {
		this.partner_product_code = partner_product_code;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public BigDecimal getAmount() {
		return amount;
	}
	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}
	public BigDecimal getUnit_price() {
		return unit_price;
	}
	public void setUnit_price(BigDecimal unit_price) {
		this.unit_price = unit_price;
	}
	public BigDecimal getTotal_price() {
		return total_price;
	}
	public void setTotal_price(BigDecimal total_price) {
		this.total_price = total_price;
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
	public String getRemark() {
		return remark;
	}
	public void setRemark(String remark) {
		this.remark = remark;
	}
	public Integer getEarn() {
		return earn;
	}
	public void setEarn(Integer earn) {
		this.earn = earn;
	}
	
}

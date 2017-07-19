package com.hzcard.syndata.points.index;

import java.math.BigDecimal;
import java.util.Date;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldIndex;
import org.springframework.data.elasticsearch.annotations.FieldType;
/**
 * 
 * elasticsearch index
 *
 */
@Document(indexName="order_main",type = "order_main")
public class OrderIndex {
	
	public static final String INDICE = "order_main";
	public static final String TYPE = "order_main";
	
	@Id
	@Field(index = FieldIndex.not_analyzed, store = true)
	private String id;

	@Field(type=FieldType.String, index = FieldIndex.not_analyzed, store = true)
	private String code;

	@Field(type=FieldType.String, index = FieldIndex.not_analyzed, store = true)
	//@JsonProperty("trade_vif_id")
	private String trade_vif_id;
	
	@Field(type=FieldType.String, index = FieldIndex.not_analyzed, store = true)
	//@JsonProperty("partner_sheet_id")
	private String partner_sheet_id; //POS机的小票流水号

	@Field(type=FieldType.Integer, index = FieldIndex.not_analyzed, store = true)
	private int type;

	@Field(type=FieldType.String, index = FieldIndex.not_analyzed, store = true)
	//@JsonProperty("point_account_id")
	private String point_account_id;

	@Field(type=FieldType.String, index = FieldIndex.not_analyzed, store = true)
	//@JsonProperty("card_no")
	private String card_no;

	@Field(type=FieldType.String, index = FieldIndex.not_analyzed, store = true)
	//@JsonProperty("mobile_phone")
	private String mobile_phone;

	@Field(type=FieldType.String, index = FieldIndex.not_analyzed, store = true)
	//@JsonProperty("partner_id")
	private String partner_id;

	@Field(type=FieldType.String, index = FieldIndex.not_analyzed, store = true)
	//@JsonProperty("partner_code")
	private String partner_code;

	@Field(type=FieldType.String, index = FieldIndex.not_analyzed, store = true)
	//@JsonProperty("shop_id")
	private String shop_id;

	@Field(type=FieldType.String, index = FieldIndex.not_analyzed, store = true)
	//@JsonProperty("shop_code")
	private String shop_code;

	@Field(type=FieldType.Double, index = FieldIndex.not_analyzed, store = true)
	//@JsonProperty("total_amount")
	private BigDecimal total_amount;

	@Field(type=FieldType.Double, index = FieldIndex.not_analyzed, store = true)
	//@JsonProperty("total_price")
	private BigDecimal total_price;

	@Field(type=FieldType.Double, index = FieldIndex.not_analyzed, store = true)
	//@JsonProperty("pay_value")
	private BigDecimal pay_value;

	@Field(type=FieldType.String, index = FieldIndex.not_analyzed, store = true)
	//@JsonProperty("currency_unit")
	private String currency_unit;

	@Field(type=FieldType.Date, index = FieldIndex.not_analyzed, store = true)
	//@JsonProperty("trade_time")
	private Date trade_time;

	@Field(type=FieldType.Integer, index = FieldIndex.not_analyzed, store = true)
	//@JsonProperty("pay_mode")
	private int pay_mode;
	
	@Field(type=FieldType.Integer, index = FieldIndex.not_analyzed, store = true)
	//@JsonProperty("sale_type")
	private int sale_type;

	@Field(type=FieldType.Integer, index = FieldIndex.not_analyzed, store = true)
	//@JsonProperty("point_type")
	private int point_type;
	
	@Field(type=FieldType.Long, index = FieldIndex.not_analyzed, store = true)
	//@JsonProperty("total_basic_point")
	private long total_basic_point;

	@Field(type=FieldType.Long, index = FieldIndex.not_analyzed, store = true)
	//@JsonProperty("total_reward_point")
	private long total_reward_point;

	@Field(type=FieldType.Long, index = FieldIndex.not_analyzed, store = true)
	//@JsonProperty("total_point")
	private long total_point;

	@Field(type=FieldType.Date, index = FieldIndex.not_analyzed, store = true)
	//@JsonProperty("point_time")
	private Date point_time;

	@Field(type=FieldType.String, index = FieldIndex.not_analyzed, store = true)
	//@JsonProperty("parent_id")
	private String parent_id;
	
	@Field(type=FieldType.String, index = FieldIndex.not_analyzed, store = true)
	//@JsonProperty("parent_code")
	private String parent_code;

	@Field(type=FieldType.String, index = FieldIndex.not_analyzed, store = true)
	//@JsonProperty("remark")
	private String remark;
	
	@Field(type=FieldType.Integer, index = FieldIndex.not_analyzed, store = true)
	//@JsonProperty("cancelled")
	private int cancelled;
	
	@Field(type=FieldType.Integer, index = FieldIndex.not_analyzed, store = true)
	//@JsonProperty("client_type")
	private Integer client_type;

	@Field(type=FieldType.String, index = FieldIndex.analyzed, store = true)
	//@JsonProperty("card_no_search")
	private String card_no_search;

	@Field(type=FieldType.String, index = FieldIndex.analyzed, store = true)
	//@JsonProperty("code_search")
	private String code_search;
	
	@Field(type=FieldType.Double, index = FieldIndex.not_analyzed, store = true)
	private BigDecimal no_total_amount;

	@Field(type=FieldType.Double, index = FieldIndex.not_analyzed, store = true)
	private BigDecimal no_total_price;

	@Field(type=FieldType.Double, index = FieldIndex.not_analyzed, store = true)
	private BigDecimal no_pay_value;

	@Field(type=FieldType.String, index = FieldIndex.not_analyzed, store = true)
	private String pos_id;

	@Field(type=FieldType.String, index = FieldIndex.not_analyzed, store = true)
	private String cashier_id;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getTrade_vif_id() {
		return trade_vif_id;
	}

	public void setTrade_vif_id(String trade_vif_id) {
		this.trade_vif_id = trade_vif_id;
	}

	public String getPartner_sheet_id() {
		return partner_sheet_id;
	}

	public void setPartner_sheet_id(String partner_sheet_id) {
		this.partner_sheet_id = partner_sheet_id;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public String getPoint_account_id() {
		return point_account_id;
	}

	public void setPoint_account_id(String point_account_id) {
		this.point_account_id = point_account_id;
	}

	public String getCard_no() {
		return card_no;
	}

	public void setCard_no(String card_no) {
		this.card_no = card_no;
	}

	public String getMobile_phone() {
		return mobile_phone;
	}

	public void setMobile_phone(String mobile_phone) {
		this.mobile_phone = mobile_phone;
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

	public BigDecimal getTotal_amount() {
		return total_amount;
	}

	public void setTotal_amount(BigDecimal total_amount) {
		this.total_amount = total_amount;
	}

	public BigDecimal getTotal_price() {
		return total_price;
	}

	public void setTotal_price(BigDecimal total_price) {
		this.total_price = total_price;
	}

	public BigDecimal getPay_value() {
		return pay_value;
	}

	public void setPay_value(BigDecimal pay_value) {
		this.pay_value = pay_value;
	}

	public String getCurrency_unit() {
		return currency_unit;
	}

	public void setCurrency_unit(String currency_unit) {
		this.currency_unit = currency_unit;
	}

	public Date getTrade_time() {
		return trade_time;
	}

	public void setTrade_time(Date trade_time) {
		this.trade_time = trade_time;
	}

	public int getPay_mode() {
		return pay_mode;
	}

	public void setPay_mode(int pay_mode) {
		this.pay_mode = pay_mode;
	}

	public int getSale_type() {
		return sale_type;
	}

	public void setSale_type(int sale_type) {
		this.sale_type = sale_type;
	}

	public int getPoint_type() {
		return point_type;
	}

	public void setPoint_type(int point_type) {
		this.point_type = point_type;
	}

	public long getTotal_basic_point() {
		return total_basic_point;
	}

	public void setTotal_basic_point(long total_basic_point) {
		this.total_basic_point = total_basic_point;
	}

	public long getTotal_reward_point() {
		return total_reward_point;
	}

	public void setTotal_reward_point(long total_reward_point) {
		this.total_reward_point = total_reward_point;
	}

	public long getTotal_point() {
		return total_point;
	}

	public void setTotal_point(long total_point) {
		this.total_point = total_point;
	}

	public Date getPoint_time() {
		return point_time;
	}

	public void setPoint_time(Date point_time) {
		this.point_time = point_time;
	}

	public String getParent_id() {
		return parent_id;
	}

	public void setParent_id(String parent_id) {
		this.parent_id = parent_id;
	}

	public String getParent_code() {
		return parent_code;
	}

	public void setParent_code(String parent_code) {
		this.parent_code = parent_code;
	}

	public String getRemark() {
		return remark;
	}

	public void setRemark(String remark) {
		this.remark = remark;
	}

	public int getCancelled() {
		return cancelled;
	}

	public void setCancelled(int cancelled) {
		this.cancelled = cancelled;
	}

	public Integer getClient_type() {
		return client_type;
	}

	public void setClient_type(Integer client_type) {
		this.client_type = client_type;
	}

	public String getCard_no_search() {
		return card_no_search;
	}

	public void setCard_no_search(String card_no_search) {
		this.card_no_search = card_no_search;
	}

	public String getCode_search() {
		return code_search;
	}

	public void setCode_search(String code_search) {
		this.code_search = code_search;
	}

	public BigDecimal getNo_total_amount() {
		return no_total_amount;
	}

	public void setNo_total_amount(BigDecimal no_total_amount) {
		this.no_total_amount = no_total_amount;
	}

	public BigDecimal getNo_total_price() {
		return no_total_price;
	}

	public void setNo_total_price(BigDecimal no_total_price) {
		this.no_total_price = no_total_price;
	}

	public BigDecimal getNo_pay_value() {
		return no_pay_value;
	}

	public void setNo_pay_value(BigDecimal no_pay_value) {
		this.no_pay_value = no_pay_value;
	}

	public String getPos_id() {
		return pos_id;
	}

	public void setPos_id(String pos_id) {
		this.pos_id = pos_id;
	}

	public String getCashier_id() {
		return cashier_id;
	}

	public void setCashier_id(String cashier_id) {
		this.cashier_id = cashier_id;
	}
}

package com.hzcard.syndata.points.index;

import java.math.BigDecimal;
import java.util.Date;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldIndex;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Document(indexName="operation_order_main",type = "operation_order_main")
public class OperationOrderIndex {
	
	public static final String INDICE = "operation_order_main";
	public static final String TYPE ="operation_order_main";
	@Id
	@Field(type = FieldType.String, index = FieldIndex.not_analyzed, store = true)
	private String id;
	
	@Field(type = FieldType.String, index = FieldIndex.not_analyzed, store = true)
	private String code;
	
	@Field(type = FieldType.String, index = FieldIndex.not_analyzed, store = true)
	private String operator;
	
	@Field(type = FieldType.Date, index = FieldIndex.not_analyzed, store = true)
	private Date operation_time;
	
	@Field(type = FieldType.Integer, index = FieldIndex.not_analyzed, store = true)
	private int operation_type;
	
	@Field(type = FieldType.String, index = FieldIndex.not_analyzed, store = true)
	private String operation_comment;
	
	@Field(type = FieldType.String, index = FieldIndex.not_analyzed, store = true)
	private String order_id;
	
	@Field(type = FieldType.String, index = FieldIndex.not_analyzed, store = true)
	private String order_code;
	
	@Field(type = FieldType.String, index = FieldIndex.not_analyzed, store = true)
	private String trade_vif_id;
	
	@Field(type = FieldType.String, index = FieldIndex.not_analyzed, store = true)
	private String partner_sheet_id;
	
	@Field(type = FieldType.Integer, index = FieldIndex.not_analyzed, store = true)
	private int type;
	
	@Field(type = FieldType.String, index = FieldIndex.not_analyzed, store = true)
	private String point_account_id;
	
	@Field(type = FieldType.String, index = FieldIndex.not_analyzed, store = true)
	private String card_no;
	
	@Field(type = FieldType.String, index = FieldIndex.not_analyzed, store = true)
	private String mobile_phone;
	
	@Field(type = FieldType.String, index = FieldIndex.not_analyzed, store = true)
	private String partner_id;
	
	@Field(type = FieldType.String, index = FieldIndex.not_analyzed, store = true)
	private String partner_code;
	
	@Field(type = FieldType.String, index = FieldIndex.not_analyzed, store = true)
	private String shop_id;
	
	@Field(type = FieldType.String, index = FieldIndex.not_analyzed, store = true)
	private String shop_code;
	
	@Field(type = FieldType.Double, index = FieldIndex.not_analyzed, store = true)
	private BigDecimal total_amount;
	
	@Field(type = FieldType.Double, index = FieldIndex.not_analyzed, store = true)
	private BigDecimal total_price;
	
	@Field(type = FieldType.Double, index = FieldIndex.not_analyzed, store = true)
	private BigDecimal pay_value;
	
	@Field(type = FieldType.Date, index = FieldIndex.not_analyzed, store = true)
	private Date trade_time;
	
	@Field(type = FieldType.Integer, index = FieldIndex.not_analyzed, store = true)
	private int pay_mode;
	
	@Field(type = FieldType.Integer, index = FieldIndex.not_analyzed, store = true)
	private int point_type;
	
	@Field(type = FieldType.Long, index = FieldIndex.not_analyzed, store = true)
	private long total_basic_point;
	
	@Field(type = FieldType.Long, index = FieldIndex.not_analyzed, store = true)
	private long total_reward_point;
	
	@Field(type = FieldType.Long, index = FieldIndex.not_analyzed, store = true)
	private long total_point;
	
	@Field(type = FieldType.Date, index = FieldIndex.not_analyzed, store = true)
	private Date point_time;
	
	@Field(type = FieldType.String, index = FieldIndex.not_analyzed, store = true)
	private String parent_order_code;
	
	@Field(type = FieldType.String, index = FieldIndex.not_analyzed, store = true)
	private String order_remark;
	
	@Field(type = FieldType.Integer, index = FieldIndex.not_analyzed, store = true)
	private int order_cancelled;
	
	@Field(type = FieldType.Integer, index = FieldIndex.not_analyzed, store = true)
	private Integer client_type;
	
	@Field(type = FieldType.String, index = FieldIndex.analyzed, store = true)
	private String code_search;
	
	@Field(type = FieldType.String, index = FieldIndex.analyzed, store = true)
	private String card_no_search;
	
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

	public String getOperator() {
		return operator;
	}

	public void setOperator(String operator) {
		this.operator = operator;
	}

	public Date getOperation_time() {
		return operation_time;
	}

	public void setOperation_time(Date operation_time) {
		this.operation_time = operation_time;
	}

	public int getOperation_type() {
		return operation_type;
	}

	public void setOperation_type(int operation_type) {
		this.operation_type = operation_type;
	}

	public String getOperation_comment() {
		return operation_comment;
	}

	public void setOperation_comment(String operation_comment) {
		this.operation_comment = operation_comment;
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

	public String getParent_order_code() {
		return parent_order_code;
	}

	public void setParent_order_code(String parent_order_code) {
		this.parent_order_code = parent_order_code;
	}

	public String getOrder_remark() {
		return order_remark;
	}

	public void setOrder_remark(String order_remark) {
		this.order_remark = order_remark;
	}

	public int getOrder_cancelled() {
		return order_cancelled;
	}

	public void setOrder_cancelled(int order_cancelled) {
		this.order_cancelled = order_cancelled;
	}

	public Integer getClient_type() {
		return client_type;
	}

	public void setClient_type(Integer client_type) {
		this.client_type = client_type;
	}

	public String getCode_search() {
		return code_search;
	}

	public void setCode_search(String code_search) {
		this.code_search = code_search;
	}

	public String getCard_no_search() {
		return card_no_search;
	}

	public void setCard_no_search(String card_no_search) {
		this.card_no_search = card_no_search;
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

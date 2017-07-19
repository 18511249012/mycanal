package com.hzcard.syndata.reorder.index;


import java.math.BigDecimal;
import java.util.Date;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldIndex;
import org.springframework.data.elasticsearch.annotations.FieldType;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;


@Document(indexName="reorder",type = "re_order",createIndex=true)
public class ReOrder {

	@JsonCreator
    public ReOrder(@JsonProperty("id") String id) {
		this.id = id;
	}
	public ReOrder(){}

	@Id
    @Field(type = FieldType.String, index = FieldIndex.not_analyzed, store = true)
    private String id;
    
    @Field(type = FieldType.String, index = FieldIndex.not_analyzed, store = true)
    private String code;

    @Field(type = FieldType.Integer, index = FieldIndex.not_analyzed, store = true)
    private Integer type;

    @Field(type = FieldType.String, index = FieldIndex.not_analyzed, store = true)
    private String card_no;

    @Field(type = FieldType.String, index = FieldIndex.not_analyzed, store = true)
    private String partner_id;

    @Field(type = FieldType.String, index = FieldIndex.not_analyzed, store = true)
    private String partner_code;

	@Field(type = FieldType.String, index = FieldIndex.not_analyzed, store = true)
    private String shop_code;
    
    @Field(type = FieldType.String, index = FieldIndex.not_analyzed, store = true)
    private String partner_name;

    @Field(type = FieldType.String, index = FieldIndex.not_analyzed, store = true)
    private String product_id;

    @Field(type = FieldType.String, index = FieldIndex.not_analyzed, store = true)
    private String partner_product_code;

    @Field(type = FieldType.String, index = FieldIndex.not_analyzed, store = true)
    private String product_redeem_code;

    @Field(type = FieldType.String, index = FieldIndex.not_analyzed, store = true)
    private String product_name;

    @Field(type = FieldType.Long, index = FieldIndex.not_analyzed, store = true)
    private Long unit_point;

    @Field(type = FieldType.Integer, index = FieldIndex.not_analyzed, store = true)
    private Integer total_amount;

    @Field(type = FieldType.Long, index = FieldIndex.not_analyzed, store = true)
    private Long total_point;

    @Field(type = FieldType.String, index = FieldIndex.not_analyzed, store = true)
    private String partner_order_id;

    @Field(type = FieldType.String, index = FieldIndex.not_analyzed, store = true)
    private String vif_order_id;

    @Field(type = FieldType.Integer, index = FieldIndex.not_analyzed, store = true)
    private Integer redeem_channel;

    @Field(type = FieldType.Integer, index = FieldIndex.not_analyzed, store = true)
    private Integer status;
   
    @Field(type = FieldType.Date, index = FieldIndex.not_analyzed, store = true,format=DateFormat.date_time)
    @JsonFormat(shape = JsonFormat.Shape.STRING,timezone="GMT+8", pattern="yyyy-MM-dd'T'HH:mm:ss.SSSZZ")
    private Date redeemed_time;

    @Field(type = FieldType.String, index = FieldIndex.not_analyzed, store = true)
    private String payment_flow_id;

    @Field(type = FieldType.Integer, index = FieldIndex.not_analyzed, store = true)
    private Integer distribution_type;

    @Field(type = FieldType.String, index = FieldIndex.not_analyzed, store = true)
    private String receiver;

    @Field(type = FieldType.String, index = FieldIndex.not_analyzed, store = true)
    private String delivery_address;

    @Field(type = FieldType.String, index = FieldIndex.not_analyzed, store = true)
    private String tel_num;

    @Field(type = FieldType.String, index = FieldIndex.not_analyzed, store = true)
    private String post_code;

    @Field(type = FieldType.String, index = FieldIndex.not_analyzed, store = true)
    private String logistics_company;

    @Field(type = FieldType.String, index = FieldIndex.not_analyzed, store = true)
    private String logistics_serial_no;
   
    @Field(type = FieldType.String, index = FieldIndex.not_analyzed, store = true)
    private String logistics_info;
   
    @Field(type = FieldType.String, index = FieldIndex.not_analyzed, store = true)
    private String remark;
    
    @Field(type = FieldType.Date, index = FieldIndex.not_analyzed, store = true,format=DateFormat.date_time)
    @JsonFormat(shape = JsonFormat.Shape.STRING,timezone="GMT+8", pattern="yyyy-MM-dd'T'HH:mm:ss.SSSZZ")
    private Date receipt_time;
    
    @Field(type = FieldType.Date, index = FieldIndex.not_analyzed, store = true,format=DateFormat.date_time)
    @JsonFormat(shape = JsonFormat.Shape.STRING,timezone="GMT+8", pattern="yyyy-MM-dd'T'HH:mm:ss.SSSZZ")
    private Date receipt_time_default;
    
    @Field(type = FieldType.Integer, index = FieldIndex.not_analyzed, store = true)
    private Integer receipted_auto;
   
    @Field(type = FieldType.String, index = FieldIndex.not_analyzed, store = true)
    private String cancelled;

    @Field(type = FieldType.String, index = FieldIndex.not_analyzed, store = true)
    private String is_reparir;
    
    @Field(type = FieldType.String, index = FieldIndex.not_analyzed, store = true)
    private String operator;
    
    @Field(type = FieldType.Double, index = FieldIndex.not_analyzed, store = true)
    private BigDecimal cash;
    
    @Field(type = FieldType.String, index = FieldIndex.not_analyzed, store = true)
    private RepaymentCashChannel cash_channel;
    
    @Field(type = FieldType.String, index = FieldIndex.not_analyzed, store = true)
    private String cash_account;

	@Field(type=FieldType.String, index = FieldIndex.not_analyzed, store = true)
	private String pos_id;

	@Field(type=FieldType.String, index = FieldIndex.not_analyzed, store = true)
	private String cashier_id;

    public String getShop_code() {
        return shop_code;
    }

    public void setShop_code(String shop_code) {
        this.shop_code = shop_code;
    }

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
		this.code = code == null ? null : code.trim();
	}
	public Integer getType() {
		return type;
	}
	public void setType(Integer type) {
		this.type = type;
	}
	public String getCard_no() {
		return card_no;
	}
	public void setCard_no(String card_no) {
		this.card_no = card_no == null ? null : card_no.trim();
	}
	public String getPartner_id() {
		return partner_id;
	}
	public void setPartner_id(String partner_id) {
		this.partner_id = partner_id == null ? null : partner_id.trim();
	}
	public String getPartner_code() {
		return partner_code;
	}
	public void setPartner_code(String partner_code) {
		this.partner_code = partner_code == null ? null : partner_code.trim();
	}
	public String getPartner_name() {
		return partner_name;
	}
	public void setPartner_name(String partner_name) {
		this.partner_name = partner_name == null ? null : partner_name.trim();
	}
	public String getProduct_id() {
		return product_id;
	}
	public void setProduct_id(String product_id) {
		this.product_id = product_id == null ? null : product_id.trim();
	}
	public String getPartner_product_code() {
		return partner_product_code;
	}
	public void setPartner_product_code(String partner_product_code) {
		this.partner_product_code = partner_product_code == null ? null : partner_product_code.trim();
	}
	public String getProduct_redeem_code() {
		return product_redeem_code;
	}
	public void setProduct_redeem_code(String product_redeem_code) {
		this.product_redeem_code = product_redeem_code == null ? null : product_redeem_code.trim();
	}
	public String getProduct_name() {
		return product_name;
	}
	public void setProduct_name(String product_name) {
		this.product_name = product_name == null ? null : product_name.trim();
	}
	public Long getUnit_point() {
		return unit_point;
	}
	public void setUnit_point(Long unit_point) {
		this.unit_point = unit_point;
	}
	public Integer getTotal_amount() {
		return total_amount;
	}
	public void setTotal_amount(Integer total_amount) {
		this.total_amount = total_amount;
	}
	public Long getTotal_point() {
		return total_point;
	}
	public void setTotal_point(Long total_point) {
		this.total_point = total_point;
	}
	public String getPartner_order_id() {
		return partner_order_id;
	}
	public void setPartner_order_id(String partner_order_id) {
		this.partner_order_id = partner_order_id;
	}
	public String getVif_order_id() {
		return vif_order_id;
	}
	public void setVif_order_id(String vif_order_id) {
		this.vif_order_id = vif_order_id == null ? null : vif_order_id.trim();
	}
	public Integer getRedeem_channel() {
		return redeem_channel;
	}
	public void setRedeem_channel(Integer redeem_channel) {
		this.redeem_channel = redeem_channel;
	}
	public Integer getStatus() {
		return status;
	}
	public void setStatus(Integer status) {
		this.status = status;
	}
	public Date getRedeemed_time() {
		return redeemed_time;
	}
	public void setRedeemed_time(Date redeemed_time) {
		this.redeemed_time = redeemed_time;
	}
	public String getPayment_flow_id() {
		return payment_flow_id;
	}
	public void setPayment_flow_id(String payment_flow_id) {
		this.payment_flow_id = payment_flow_id == null ? null : payment_flow_id.trim();
	}
	public Integer getDistribution_type() {
		return distribution_type;
	}
	public void setDistribution_type(Integer distribution_type) {
		this.distribution_type = distribution_type;
	}
	public String getReceiver() {
		return receiver;
	}
	public void setReceiver(String receiver) {
		this.receiver = receiver == null ? null : receiver.trim();
	}
	public String getDelivery_address() {
		return delivery_address;
	}
	public void setDelivery_address(String delivery_address) {
		this.delivery_address = delivery_address == null ? null : delivery_address.trim();
	}
	public String getTel_num() {
		return tel_num;
	}
	public void setTel_num(String tel_num) {
		this.tel_num = tel_num == null ? null : tel_num.trim();
	}
	public String getPost_code() {
		return post_code;
	}
	public void setPost_code(String post_code) {
		this.post_code = post_code == null ? null : post_code.trim();
	}
	public String getLogistics_company() {
		return logistics_company;
	}
	public void setLogistics_company(String logistics_company) {
		this.logistics_company = logistics_company == null ? null : logistics_company.trim();
	}
	public String getLogistics_serial_no() {
		return logistics_serial_no;
	}
	public void setLogistics_serial_no(String logistics_serial_no) {
		this.logistics_serial_no = logistics_serial_no == null ? null : logistics_serial_no.trim();
	}
	public String getLogistics_info() {
		return logistics_info;
	}
	public void setLogistics_info(String logistics_info) {
		this.logistics_info = logistics_info == null ? null : logistics_info.trim();
	}
	public String getRemark() {
		return remark;
	}
	public void setRemark(String remark) {
		this.remark = remark == null ? null : remark.trim();
	}
	public Date getReceipt_time() {
		return receipt_time;
	}
	public void setReceipt_time(Date receipt_time) {
		this.receipt_time = receipt_time;
	}
	public Date getReceipt_time_default() {
		return receipt_time_default;
	}
	public void setReceipt_time_default(Date receipt_time_default) {
		this.receipt_time_default = receipt_time_default;
	}
	public Integer getReceipted_auto() {
		return receipted_auto;
	}
	public void setReceipted_auto(Integer receipted_auto) {
		this.receipted_auto = receipted_auto;
	}
	public String getCancelled() {
		return cancelled;
	}
	public void setCancelled(String cancelled) {
		this.cancelled = cancelled;
	}
	public String getIs_reparir() {
		return is_reparir;
	}
	public void setIs_reparir(String is_reparir) {
		this.is_reparir = is_reparir;
	}
	public String getOperator() {
		return operator;
	}
	public void setOperator(String operator) {
		this.operator = operator == null ? null : operator.trim();
	}
	public BigDecimal getCash() {
		return cash;
	}
	public void setCash(BigDecimal cash) {
		this.cash = cash;
	}
	public RepaymentCashChannel getCash_channel() {
		return cash_channel;
	}
	public void setCash_channel(RepaymentCashChannel cash_channel) {
		this.cash_channel = cash_channel;
	}
	public String getCash_account() {
		return cash_account;
	}
	public void setCash_account(String cash_account) {
		this.cash_account = cash_account;
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

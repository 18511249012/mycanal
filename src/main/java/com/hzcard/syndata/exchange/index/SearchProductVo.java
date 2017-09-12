package com.hzcard.syndata.exchange.index;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldIndex;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.math.BigDecimal;


@Document(indexName = "exchange-product-index", type = "exchange-product_type")
public class SearchProductVo {
	
	public static final String INDICE = "exchange-product-index";
	public static final String TYPE = "exchange-product_type";
	
	@Id
	@Field(type = FieldType.String, index = FieldIndex.not_analyzed, store = true)
	private String id;
	
	@Field(type = FieldType.String, index = FieldIndex.not_analyzed, store = true)
	private String redeem_code;
	
	@Field(type = FieldType.Integer, index = FieldIndex.not_analyzed, store = true)
	private Integer type;
	
	@Field(type = FieldType.String, index = FieldIndex.not_analyzed, store = true)
	private String partner_id;
	
	@Field(type = FieldType.String, index = FieldIndex.not_analyzed, store = true)
	private String partner_code;
	
	@Field(type = FieldType.String, index = FieldIndex.not_analyzed, store = true)
	private String partner_product_code;
	
	@Field(type = FieldType.String, index = FieldIndex.not_analyzed, store = true)
	private String name;
	
	@Field(type = FieldType.String, index = FieldIndex.not_analyzed, store = true)
	private String brand;
	
	@Field(type = FieldType.String, index = FieldIndex.not_analyzed, store = true)
	private String description;
	
	@Field(type = FieldType.String, index = FieldIndex.not_analyzed, store = true)
	private String pic;
	
	@Field(type = FieldType.String, index = FieldIndex.not_analyzed, store = true)
	private BigDecimal unit_price;
	
	@Field(type = FieldType.Long, index = FieldIndex.not_analyzed, store = true)
	private Long unit_point;
	
	@Field(type = FieldType.Integer, index = FieldIndex.not_analyzed, store = true)
	private Integer total_amount;
	
	@Field(type = FieldType.Integer, index = FieldIndex.not_analyzed, store = true)
	private Integer surplus_amount;
	
	@Field(type = FieldType.Long, index = FieldIndex.not_analyzed, store = true)
	private Long begin_date;
	
	@Field(type = FieldType.Long, index = FieldIndex.not_analyzed, store = true)
	private Long end_date;
	
	@Field(type = FieldType.String, index = FieldIndex.not_analyzed, store = true)
	private String instruction;
	
	@Field(type = FieldType.Integer, index = FieldIndex.not_analyzed, store = true)
	private Integer display_flag;
	
	@Field(type = FieldType.String, index = FieldIndex.not_analyzed, store = true)
	private String partner_category1_id;
	
	@Field(type = FieldType.String, index = FieldIndex.not_analyzed, store = true)
	private String partner_category1_name;
	
	@Field(type = FieldType.String, index = FieldIndex.not_analyzed, store = true)
	private String partner_category2_id;
	
	@Field(type = FieldType.String, index = FieldIndex.not_analyzed, store = true)
	private String partner_category2_name;
	
	@Field(type = FieldType.String, index = FieldIndex.not_analyzed, store = true)
	private String partner_category3_id;
	
	@Field(type = FieldType.String, index = FieldIndex.not_analyzed, store = true)
	private String partner_category3_name;

	@Field(type = FieldType.Integer, index = FieldIndex.not_analyzed, store = true)
	private int deleted;

	@Field(type = FieldType.Integer, index = FieldIndex.not_analyzed, store = true)
	private int version;

	public int getDeleted() {
		return deleted;
	}

	public void setDeleted(int deleted) {
		this.deleted = deleted;
	}

	public int getVersion() {
		return version;
	}

	public void setVersion(int version) {
		this.version = version;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getRedeem_code() {
		return redeem_code;
	}

	public void setRedeem_code(String redeem_code) {
		this.redeem_code = redeem_code;
	}

	public Integer getType() {
		return type;
	}

	public void setType(Integer type) {
		this.type = type;
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

	public String getPartner_product_code() {
		return partner_product_code;
	}

	public void setPartner_product_code(String partner_product_code) {
		this.partner_product_code = partner_product_code;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getBrand() {
		return brand;
	}

	public void setBrand(String brand) {
		this.brand = brand;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getPic() {
		return pic;
	}

	public void setPic(String pic) {
		this.pic = pic;
	}

	public BigDecimal getUnit_price() {
		return unit_price;
	}

	public void setUnit_price(BigDecimal unit_price) {
		this.unit_price = unit_price;
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

	public Integer getSurplus_amount() {
		return surplus_amount;
	}

	public void setSurplus_amount(Integer surplus_amount) {
		this.surplus_amount = surplus_amount;
	}

	public Long getBegin_date() {
		return begin_date;
	}

	public void setBegin_date(Long begin_date) {
		this.begin_date = begin_date;
	}

	public Long getEnd_date() {
		return end_date;
	}

	public void setEnd_date(Long end_date) {
		this.end_date = end_date;
	}

	public String getInstruction() {
		return instruction;
	}

	public void setInstruction(String instruction) {
		this.instruction = instruction;
	}

	public Integer getDisplay_flag() {
		return display_flag;
	}

	public void setDisplay_flag(Integer display_flag) {
		this.display_flag = display_flag;
	}

	public String getPartner_category1_id() {
		return partner_category1_id;
	}

	public void setPartner_category1_id(String partner_category1_id) {
		this.partner_category1_id = partner_category1_id;
	}

	public String getPartner_category1_name() {
		return partner_category1_name;
	}

	public void setPartner_category1_name(String partner_category1_name) {
		this.partner_category1_name = partner_category1_name;
	}

	public String getPartner_category2_id() {
		return partner_category2_id;
	}

	public void setPartner_category2_id(String partner_category2_id) {
		this.partner_category2_id = partner_category2_id;
	}

	public String getPartner_category2_name() {
		return partner_category2_name;
	}

	public void setPartner_category2_name(String partner_category2_name) {
		this.partner_category2_name = partner_category2_name;
	}

	public String getPartner_category3_id() {
		return partner_category3_id;
	}

	public void setPartner_category3_id(String partner_category3_id) {
		this.partner_category3_id = partner_category3_id;
	}

	public String getPartner_category3_name() {
		return partner_category3_name;
	}

	public void setPartner_category3_name(String partner_category3_name) {
		this.partner_category3_name = partner_category3_name;
	}
}

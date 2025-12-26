package org.example.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Data Transfer Object for Discount Rules
 * Matches the API's DiscountRuleDTO structure exactly
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DiscountRuleDTO {
    private Long id;
    private String name;
    private String description;
    private String ruleType;
    private Double percentOff;
    private String category;
    private Integer buyQuantity;
    private Integer freeQuantity;
    private String itemKeyword;
    private Integer requiredQuantity;
    private Double bundlePrice;
    private Boolean active;
    private Integer priority;
    // Removed LocalDateTime fields - we don't need them in the UI
    // and they require additional Jackson dependencies

    // Constructors
    public DiscountRuleDTO() {
        // Set default values
        this.requiredQuantity = 1;
        this.percentOff = 0.0;
        this.buyQuantity = 1;
        this.freeQuantity = 0;
        this.bundlePrice = 0.0;
        this.priority = 1;
        this.active = true;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getRuleType() {
        return ruleType;
    }

    public void setRuleType(String ruleType) {
        this.ruleType = ruleType;
    }

    public Double getPercentOff() {
        return percentOff;
    }

    public void setPercentOff(Double percentOff) {
        this.percentOff = percentOff;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Integer getBuyQuantity() {
        return buyQuantity;
    }

    public void setBuyQuantity(Integer buyQuantity) {
        this.buyQuantity = buyQuantity;
    }

    public Integer getFreeQuantity() {
        return freeQuantity;
    }

    public void setFreeQuantity(Integer freeQuantity) {
        this.freeQuantity = freeQuantity;
    }

    public String getItemKeyword() {
        return itemKeyword;
    }

    public void setItemKeyword(String itemKeyword) {
        this.itemKeyword = itemKeyword;
    }

    public Integer getRequiredQuantity() {
        return requiredQuantity;
    }

    public void setRequiredQuantity(Integer requiredQuantity) {
        this.requiredQuantity = requiredQuantity;
    }

    public Double getBundlePrice() {
        return bundlePrice;
    }

    public void setBundlePrice(Double bundlePrice) {
        this.bundlePrice = bundlePrice;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public boolean isActive() {
        return active != null ? active : false;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    @Override
    public String toString() {
        return "DiscountRuleDTO{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", ruleType='" + ruleType + '\'' +
                ", category='" + category + '\'' +
                ", active=" + active +
                '}';
    }
}
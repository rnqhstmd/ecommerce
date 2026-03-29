package com.loopers.infrastructure.search;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.loopers.domain.product.Product;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.ZonedDateTime;

@Document(indexName = "products", createIndex = false)
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductDocument {

    @Id
    private Long id;

    @Field(type = FieldType.Text)
    private String name;

    @Field(type = FieldType.Long)
    private Long brandId;

    @Field(type = FieldType.Text)
    private String brandName;

    @Field(type = FieldType.Long)
    private Long categoryId;

    @Field(type = FieldType.Text)
    private String categoryName;

    @Field(type = FieldType.Long)
    private Long price;

    @Field(type = FieldType.Long)
    private Long likeCount;

    @Field(type = FieldType.Date)
    private ZonedDateTime createdAt;

    @Field(type = FieldType.Date)
    private ZonedDateTime deletedAt;

    public static ProductDocument from(Product product, String brandName, String categoryName) {
        return ProductDocument.builder()
                .id(product.getId())
                .name(product.getName())
                .brandId(product.getBrandId())
                .brandName(brandName)
                .categoryId(product.getCategoryId())
                .categoryName(categoryName)
                .price(product.getPriceValue())
                .likeCount(product.getLikeCount())
                .createdAt(product.getCreatedAt())
                .deletedAt(product.getDeletedAt())
                .build();
    }
}

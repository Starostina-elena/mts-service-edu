package ru.aigul.mts_service.search;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Data
@NoArgsConstructor
@Document(indexName = "tariffs")
public class TariffDocument {
    @Id
    @Field(type = FieldType.Long)
    private Long id;

    @Field(type = FieldType.Text)
    private String name;

    @Field(type = FieldType.Text)
    private String description;

    @Field(type = FieldType.Keyword)
    private String category;

    @Field(type = FieldType.Double)
    private Double basePrice;

    @Field(type = FieldType.Integer)
    private Integer speedMbps;

    @Field(type = FieldType.Integer)
    private Integer tvChannels;

    @Field(type = FieldType.Integer)
    private Integer internetGb;

    @Field(type = FieldType.Integer)
    private Integer callsMinutes;

    @Field(type = FieldType.Integer)
    private Integer smsCount;

    @Field(type = FieldType.Boolean)
    private Boolean forFamily;

    @Field(type = FieldType.Boolean)
    private Boolean noSubscriptionFee;

    @Field(type = FieldType.Keyword)
    private String deviceType;

    @Field(type = FieldType.Integer)
    private Integer localMinutes;

    @Field(type = FieldType.Integer)
    private Integer intercityMinutes;

    @Field(type = FieldType.Integer)
    private Integer mobileMinutes;

    @Field(type = FieldType.Integer)
    private Integer channelsTotal;

    @Field(type = FieldType.Integer)
    private Integer channelsHd;

    @Field(type = FieldType.Nested)
    private List<Map<String, Object>> services;

    @Field(type = FieldType.Keyword)
    private String status;

    @Field(type = FieldType.Keyword)
    private String createdAt;
}

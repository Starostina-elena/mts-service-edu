package ru.aigul.mts_service.search;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import lombok.Data;

@Document(indexName = "tariffs")
@Data
public class TariffDocument {

    @Id
    private Long id;
    @Field(type = FieldType.Text)
    private String name;
    @Field(type = FieldType.Text)
    private String description;
    @Field(type = FieldType.Keyword)
    private String category;
    private Double basePrice;

    private Integer speedMbps;
    private Integer tvChannels;
    private Integer internetGb;
    private Integer callsMinutes;
    private Integer smsCount;
    private Boolean forFamily;
    @Field(type = FieldType.Keyword)
    private String deviceType;
    private Integer localMinutes;
    private Integer intercityMinutes;
    private Integer mobileMinutes;
    private Integer channelsTotal;
    private Integer channelsHd;

    @Field(type = FieldType.Object)
    private java.util.List<java.util.Map<String, Object>> services;

    @Field(type = FieldType.Keyword)
    private String status;
    @Field(type = FieldType.Date, format = org.springframework.data.elasticsearch.annotations.DateFormat.date_time)
    private String createdAt;
}

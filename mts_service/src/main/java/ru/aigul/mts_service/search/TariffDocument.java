package ru.aigul.mts_service.search;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import lombok.Data;

import java.time.LocalDateTime;

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
    @Field(type = FieldType.Keyword)
    private String status;
    @Field(type = FieldType.Date, format = org.springframework.data.elasticsearch.annotations.DateFormat.date_time)
    private String createdAt;
}

package ru.aigul.mts_service.search;

import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.core.query.StringQuery;
import org.springframework.stereotype.Service;
import ru.aigul.mts_service.dto.CursorPage;
import ru.aigul.mts_service.model.Tariff;
import ru.aigul.mts_service.model.TariffStatus;
import ru.aigul.mts_service.repository.TariffRepository;

@Service
@RequiredArgsConstructor
public class TariffSearchService {

  private static final Logger log =
      LoggerFactory.getLogger(TariffSearchService.class);

  private final ElasticsearchOperations es;
  private final TariffRepository tariffRepository;

  private static final String STATUS_ACTIVE_FILTER =
      "{\"term\":{\"status.keyword\":\"ACTIVE\"}}";

  public void index(Tariff tariff) {
    TariffDocument d = toDocument(tariff);
    es.save(d);
  }

  public void delete(Long id) {
    es.delete(String.valueOf(id), TariffDocument.class);
  }

  public SearchResult<TariffDocument> search(String qStr, String categoryStr,
                                             Double priceMax, Long after,
                                             int limit) {
    String boolQueryJson = buildBoolQueryJson(qStr, categoryStr, priceMax);

    NativeQueryBuilder queryBuilder =
        new NativeQueryBuilder()
            .withQuery(new StringQuery(boolQueryJson))
            .withPageable(PageRequest.of(0, limit + 1))
            .withSort(Sort.by(Sort.Direction.ASC, "id"));

    if (after != null) {
      queryBuilder.withSearchAfter(List.of(after));
    }

    Query q = queryBuilder.build();

    List<TariffDocument> items;
    try {
      SearchHits<TariffDocument> hits = es.search(q, TariffDocument.class);
      items = hits.getSearchHits()
                  .stream()
                  .map(SearchHit::getContent)
                  .collect(Collectors.toList());
      log.info(
          "Elasticsearch returned {} hits for q='{}' category='{}' after='{}'",
          items.size(), qStr, categoryStr, after);
    } catch (Exception ex) {
      log.warn("Elasticsearch search failed: {}", ex.toString());
      items = List.of();
    }

    return new SearchResult<>(
        CursorPage.of(items, limit, t -> t, TariffDocument::getId), "es");
  }

  private String buildBoolQueryJson(String qStr, String categoryStr,
                                    Double priceMax) {
    StringBuilder filter = new StringBuilder();
    filter.append(STATUS_ACTIVE_FILTER);

    if (categoryStr != null && !categoryStr.isBlank()) {
      filter.append(",")
          .append("{\"term\":{\"category.keyword\":\"")
          .append(escapeJson(categoryStr.toUpperCase()))
          .append("\"}}");
    }

    if (priceMax != null) {
      filter.append(",")
          .append("{\"range\":{\"basePrice\":{\"lte\":")
          .append(priceMax)
          .append("}}}");
    }

    StringBuilder must = new StringBuilder();
    if (qStr != null && !qStr.isBlank()) {
      must.append("{\"query_string\":{\"query\":\"*")
          .append(escapeJson(qStr.toLowerCase()))
          .append("*\",\"fields\":[\"name\",\"description\"]}}");
    }

    return "{\"bool\":{\"filter\":[" + filter + "],\"must\":[" + must + "]}}";
  }

  private String escapeJson(String value) {
    return value.replace("\\", "\\\\").replace("\"", "\\\"");
  }

  public int reindexAll() {
    List<Tariff> all = tariffRepository.findAll();
    List<TariffDocument> docs =
        all.stream().map(this::toDocument).collect(Collectors.toList());
    int success = 0;
    for (TariffDocument d : docs) {
      try {
        es.save(d);
        success++;
      } catch (Exception ex) {
        System.out.println("Failed to index tariff id=" + d.getId() + ": " +
                           ex.getMessage());
      }
    }
    System.out.println("Reindexed " + success + " / " + docs.size() +
                       " tariffs into Elasticsearch");
    return success;
  }

  private TariffDocument toDocument(Tariff t) {
    TariffDocument d = new TariffDocument();
    d.setId(t.getId());
    d.setName(t.getName());
    d.setDescription(t.getDescription());
    d.setCategory(t.getCategory() != null ? t.getCategory().name() : null);
    d.setBasePrice(t.getBasePrice() != null ? t.getBasePrice().doubleValue()
                                            : null);

    d.setSpeedMbps(t.getSpeedMbps());
    d.setTvChannels(t.getTvChannels());
    d.setInternetGb(t.getInternetGb());
    d.setCallsMinutes(t.getCallsMinutes());
    d.setSmsCount(t.getSmsCount());
    d.setForFamily(t.isForFamily());
    d.setDeviceType(t.getDeviceType() != null ? t.getDeviceType().name()
                                              : null);
    d.setLocalMinutes(t.getLocalMinutes());
    d.setIntercityMinutes(t.getIntercityMinutes());
    d.setMobileMinutes(t.getMobileMinutes());
    d.setChannelsTotal(t.getChannelsTotal());
    d.setChannelsHd(t.getChannelsHd());

    if (t.getServices() != null && !t.getServices().isEmpty()) {
      List<java.util.Map<String, Object>> serviceDocs =
          t.getServices()
              .stream()
              .map(s -> {
                java.util.Map<String, Object> map = new java.util.HashMap<>();
                map.put("id", s.getId());
                map.put("name", s.getName());
                return map;
              })
              .collect(Collectors.toList());
      d.setServices(serviceDocs);
    }

    d.setStatus(t.getStatus() != null ? t.getStatus().name() : null);
    if (t.getCreatedAt() != null) {
      d.setCreatedAt(
          t.getCreatedAt().atOffset(java.time.ZoneOffset.UTC).toString());
    }
    return d;
  }
}

package ru.aigul.mts_service.search;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.query.StringQuery;
import ru.aigul.mts_service.dto.CursorPage;
import ru.aigul.mts_service.model.Tariff;
import ru.aigul.mts_service.model.TariffCategory;
import ru.aigul.mts_service.model.TariffStatus;
import ru.aigul.mts_service.repository.TariffRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TariffSearchService {

    private static final Logger log = LoggerFactory.getLogger(TariffSearchService.class);

    private final ElasticsearchOperations es;
    private final TariffRepository tariffRepository;

    public void index(Tariff tariff) {
        TariffDocument d = toDocument(tariff);
        es.save(d);
    }

    public void delete(Long id) {
        es.delete(String.valueOf(id), TariffDocument.class);
    }

    public SearchResult<TariffDocument> search(String qStr, String categoryStr, Double priceMax, Long after, int limit) {
        org.elasticsearch.index.query.BoolQueryBuilder bool = QueryBuilders.boolQuery();

        bool.filter(QueryBuilders.termQuery("status.keyword", TariffStatus.ACTIVE.name()));

        if (qStr != null && !qStr.isBlank()) {
            bool.must(QueryBuilders.queryStringQuery("*" + qStr.toLowerCase() + "*")
                    .field("name")
                    .field("description"));
        }

        if (categoryStr != null && !categoryStr.isBlank()) {
            bool.filter(QueryBuilders.termQuery("category.keyword", categoryStr.toUpperCase()));
        }

        if (priceMax != null) {
            bool.filter(QueryBuilders.rangeQuery("basePrice").lte(priceMax));
        }

        if (after != null) {
            bool.filter(QueryBuilders.rangeQuery("id").gt(after));
        }

        StringQuery q = new StringQuery(bool.toString());
        q.setPageable(PageRequest.of(0, limit + 1, Sort.by(Sort.Direction.ASC, "id")));

        List<TariffDocument> items = null;
        try {
            SearchHits<TariffDocument> hits = es.search(q, TariffDocument.class);
            items = hits.getSearchHits().stream().map(SearchHit::getContent).collect(Collectors.toList());
            log.info("Elasticsearch returned {} hits for q='{}' category='{}' after='{}'", items.size(), qStr, categoryStr, after);
        } catch (Exception ex) {
            log.warn("Elasticsearch search failed: {}", ex.toString());
            items = null;
        }

        if (items != null && !items.isEmpty()) {
            log.info("Using Elasticsearch results ({} items)", items.size());
            return new SearchResult<>(CursorPage.of(items, limit, t -> t, TariffDocument::getId), "es");
        }

        try {
            TariffCategory category = null;
            if (categoryStr != null && !categoryStr.isBlank()) {
                try {
                    category = TariffCategory.valueOf(categoryStr);
                } catch (IllegalArgumentException ignored) {
                }
            }
            log.info("Falling back to JPA search for q='{}' category='{}' after='{}'", qStr, categoryStr, after);
            List<Tariff> found = tariffRepository.searchFallback(qStr, category, after, org.springframework.data.domain.Limit.of(limit + 1));
            List<TariffDocument> docs = found.stream().map(this::toDocument).collect(Collectors.toList());
            log.info("Fallback returned {} items", docs.size());
            return new SearchResult<>(CursorPage.of(docs, limit, t -> t, TariffDocument::getId), "jpa");
        } catch (Exception ex) {
            log.error("Fallback search failed: {}", ex.toString());
            return new SearchResult<>(new CursorPage<>(List.of(), null), "none");
        }
    }

    public long countIndexed() {
        StringQuery q = new StringQuery(QueryBuilders.matchAllQuery().toString());
        return es.count(q, TariffDocument.class);
    }

    public List<TariffDocument> sampleIndexed(int limit) {
        StringQuery q = new StringQuery(QueryBuilders.matchAllQuery().toString());
        q.setPageable(PageRequest.of(0, limit));
        SearchHits<TariffDocument> hits = es.search(q, TariffDocument.class);
        return hits.getSearchHits().stream().map(SearchHit::getContent).collect(Collectors.toList());
    }

    public int reindexAll() {
        List<Tariff> all = tariffRepository.findAll();
        List<TariffDocument> docs = all.stream().map(this::toDocument).collect(Collectors.toList());
        int success = 0;
        for (TariffDocument d : docs) {
            try {
                es.save(d);
                success++;
            } catch (Exception ex) {
                System.out.println("Failed to index tariff id=" + d.getId() + ": " + ex.getMessage());
            }
        }
        System.out.println("Reindexed " + success + " / " + docs.size() + " tariffs into Elasticsearch");
        return success;
    }

    private TariffDocument toDocument(Tariff t) {
        TariffDocument d = new TariffDocument();
        d.setId(t.getId());
        d.setName(t.getName());
        d.setDescription(t.getDescription());
        d.setCategory(t.getCategory() != null ? t.getCategory().name() : null);
        d.setBasePrice(t.getBasePrice() != null ? t.getBasePrice().doubleValue() : null);

        d.setSpeedMbps(t.getSpeedMbps());
        d.setTvChannels(t.getTvChannels());
        d.setInternetGb(t.getInternetGb());
        d.setCallsMinutes(t.getCallsMinutes());
        d.setSmsCount(t.getSmsCount());
        d.setForFamily(t.isForFamily());
        d.setDeviceType(t.getDeviceType() != null ? t.getDeviceType().name() : null);
        d.setLocalMinutes(t.getLocalMinutes());
        d.setIntercityMinutes(t.getIntercityMinutes());
        d.setMobileMinutes(t.getMobileMinutes());
        d.setChannelsTotal(t.getChannelsTotal());
        d.setChannelsHd(t.getChannelsHd());

        if (t.getServices() != null && !t.getServices().isEmpty()) {
            List<java.util.Map<String, Object>> serviceDocs = t.getServices().stream().map(s -> {
                java.util.Map<String, Object> map = new java.util.HashMap<>();
                map.put("id", s.getId());
                map.put("name", s.getName());
                return map;
            }).collect(Collectors.toList());
            d.setServices(serviceDocs);
        }

        d.setStatus(t.getStatus() != null ? t.getStatus().name() : null);
        if (t.getCreatedAt() != null) {
            d.setCreatedAt(t.getCreatedAt().atOffset(java.time.ZoneOffset.UTC).toString());
        }
        return d;
    }
}

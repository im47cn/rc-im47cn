package com.rc.notification.domain.publisher;

import java.util.List;

public interface PublisherRepository {
    Publisher findById(Long id);
    Publisher findByPublisherCode(String publisherCode);
    Publisher findByApiKey(String apiKey);
    List<Publisher> findByFilters(String keyword, Integer status, int page, int size);
    long countByFilters(String keyword, Integer status);
    Publisher save(Publisher publisher);
    Publisher update(Publisher publisher);
    boolean existsByPublisherCode(String publisherCode);
}

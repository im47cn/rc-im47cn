package com.rc.notification.application.admin;

import com.rc.notification.domain.publisher.Publisher;
import com.rc.notification.domain.publisher.PublisherRepository;
import com.rc.notification.interfaces.admin.dto.PageResult;
import com.rc.notification.interfaces.admin.dto.PublisherCreateRequest;
import com.rc.notification.interfaces.admin.dto.PublisherDto;
import com.rc.notification.interfaces.admin.dto.PublisherUpdateRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * 发布方管理服务
 */
@Service
public class PublisherAdminService {

    private static final Logger log = LoggerFactory.getLogger(PublisherAdminService.class);

    private final PublisherRepository publisherRepository;

    public PublisherAdminService(PublisherRepository publisherRepository) {
        this.publisherRepository = publisherRepository;
    }

    /**
     * 分页查询发布方列表
     */
    public PageResult<PublisherDto> listPublishers(String keyword, Integer status, int page, int size) {
        List<Publisher> publishers = publisherRepository.findByFilters(keyword, status, page, size);
        long total = publisherRepository.countByFilters(keyword, status);

        List<PublisherDto> dtoList = publishers.stream()
                .map(this::toDto)
                .toList();

        return new PageResult<>(dtoList, total, page, size);
    }

    /**
     * 查询单个发布方
     */
    public PublisherDto getPublisher(Long id) {
        Publisher publisher = publisherRepository.findById(id);
        if (publisher == null) {
            throw new IllegalArgumentException("发布方不存在: id=" + id);
        }
        return toDto(publisher);
    }

    /**
     * 新增发布方
     */
    public PublisherDto createPublisher(PublisherCreateRequest request) {
        if (publisherRepository.existsByPublisherCode(request.getPublisherCode())) {
            throw new IllegalArgumentException("发布方编码已存在: " + request.getPublisherCode());
        }

        Publisher publisher = new Publisher();
        publisher.setPublisherCode(request.getPublisherCode());
        publisher.setPublisherName(request.getPublisherName());
        publisher.setContactInfo(request.getContactInfo());
        publisher.setApiKey("pk_" + UUID.randomUUID().toString().replace("-", ""));
        publisher.setStatus(1);

        Publisher saved = publisherRepository.save(publisher);
        log.info("新增发布方: publisherCode={}", request.getPublisherCode());

        return toDto(saved);
    }

    /**
     * 更新发布方
     */
    public PublisherDto updatePublisher(Long id, PublisherUpdateRequest request) {
        Publisher publisher = publisherRepository.findById(id);
        if (publisher == null) {
            throw new IllegalArgumentException("发布方不存在: id=" + id);
        }

        if (request.getPublisherName() != null) {
            publisher.setPublisherName(request.getPublisherName());
        }
        if (request.getContactInfo() != null) {
            publisher.setContactInfo(request.getContactInfo());
        }
        if (request.getStatus() != null) {
            publisher.setStatus(request.getStatus());
        }

        Publisher updated = publisherRepository.update(publisher);
        log.info("更新发布方: id={}, publisherCode={}", id, publisher.getPublisherCode());

        return toDto(updated);
    }

    /**
     * 轮换 API Key
     */
    public PublisherDto rotateApiKey(Long id) {
        Publisher publisher = publisherRepository.findById(id);
        if (publisher == null) {
            throw new IllegalArgumentException("发布方不存在: id=" + id);
        }

        publisher.setApiKey("pk_" + UUID.randomUUID().toString().replace("-", ""));
        Publisher updated = publisherRepository.update(publisher);
        log.info("轮换 API Key: id={}, publisherCode={}", id, publisher.getPublisherCode());

        return toDto(updated);
    }

    private PublisherDto toDto(Publisher publisher) {
        PublisherDto dto = new PublisherDto();
        dto.setId(publisher.getId());
        dto.setPublisherCode(publisher.getPublisherCode());
        dto.setPublisherName(publisher.getPublisherName());
        dto.setApiKey(publisher.getApiKey());
        dto.setStatus(publisher.getStatus());
        dto.setContactInfo(publisher.getContactInfo());
        dto.setCreateTime(publisher.getCreateTime());
        dto.setUpdateTime(publisher.getUpdateTime());
        return dto;
    }
}

package com.rc.notification.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rc.notification.domain.publisher.Publisher;
import com.rc.notification.domain.publisher.PublisherRepository;
import com.rc.notification.infrastructure.persistence.entity.PublisherEntity;
import com.rc.notification.infrastructure.persistence.mapper.PublisherMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 发布方 Repository 实现
 * <p>
 * 基于 MyBatis-Plus，完成 Entity ↔ Publisher 转换
 */
@Repository
public class PublisherRepositoryImpl implements PublisherRepository {

    private final PublisherMapper publisherMapper;

    public PublisherRepositoryImpl(PublisherMapper publisherMapper) {
        this.publisherMapper = publisherMapper;
    }

    @Override
    public Publisher findById(Long id) {
        PublisherEntity entity = publisherMapper.selectById(id);
        return Publisher.fromEntity(entity);
    }

    @Override
    public Publisher findByPublisherCode(String publisherCode) {
        LambdaQueryWrapper<PublisherEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PublisherEntity::getPublisherCode, publisherCode);
        PublisherEntity entity = publisherMapper.selectOne(wrapper);
        return Publisher.fromEntity(entity);
    }

    @Override
    public Publisher findByApiKey(String apiKey) {
        LambdaQueryWrapper<PublisherEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PublisherEntity::getApiKey, apiKey);
        PublisherEntity entity = publisherMapper.selectOne(wrapper);
        return Publisher.fromEntity(entity);
    }

    @Override
    public List<Publisher> findByFilters(String keyword, Integer status, int page, int size) {
        LambdaQueryWrapper<PublisherEntity> wrapper = buildFilterWrapper(keyword, status);
        wrapper.orderByDesc(PublisherEntity::getUpdateTime);

        IPage<PublisherEntity> pageResult = publisherMapper.selectPage(
                new Page<>(page, size), wrapper);

        return pageResult.getRecords().stream()
                .map(Publisher::fromEntity)
                .toList();
    }

    @Override
    public long countByFilters(String keyword, Integer status) {
        LambdaQueryWrapper<PublisherEntity> wrapper = buildFilterWrapper(keyword, status);
        return publisherMapper.selectCount(wrapper);
    }

    @Override
    public Publisher save(Publisher publisher) {
        PublisherEntity entity = publisher.toEntity();
        publisherMapper.insert(entity);
        publisher.setId(entity.getId());
        return publisher;
    }

    @Override
    public Publisher update(Publisher publisher) {
        PublisherEntity entity = publisher.toEntity();
        publisherMapper.updateById(entity);
        return publisher;
    }

    @Override
    public boolean existsByPublisherCode(String publisherCode) {
        LambdaQueryWrapper<PublisherEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PublisherEntity::getPublisherCode, publisherCode);
        return publisherMapper.selectCount(wrapper) > 0;
    }

    private LambdaQueryWrapper<PublisherEntity> buildFilterWrapper(String keyword, Integer status) {
        LambdaQueryWrapper<PublisherEntity> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w
                    .like(PublisherEntity::getPublisherCode, keyword)
                    .or()
                    .like(PublisherEntity::getPublisherName, keyword));
        }
        if (status != null) {
            wrapper.eq(PublisherEntity::getStatus, status);
        }
        return wrapper;
    }
}

package com.genifast.dms.service;

import jakarta.annotation.Nullable;
import java.io.Serializable;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import com.genifast.dms.common.exception.ResourceNotFoundException;
import com.genifast.dms.entity.BaseEntity;
import com.genifast.dms.repository.BaseRepository;
import com.genifast.dms.repository.specifications.CommonSpecifications;

public class BaseServiceImpl<T extends BaseEntity, ID extends Serializable, R extends BaseRepository<T, ID>>
    implements BaseService<T, ID> {

  @Autowired
  protected R repository;

  @Override
  public T createOrUpdate(T model) {
    if (model.getId() == null) {
      model.setIsDeleted(false);
    }
    return repository.save(model);
  }

  @Override
  public T create(T model) {
    model.setIsDeleted(false);
    return repository.save(model);
  }

  @Override
  public T update(T model) {
    return repository.save(model);
  }

  @Override
  public List<T> createOrUpdate(List<T> models) {
    return (List<T>) repository.saveAll(models);
  }

  @Override
  public void delete(ID id) {
    Optional<T> modelOpt = repository.findByIdAndIsDeletedFalse(id);
    T model = modelOpt.orElseThrow(ResourceNotFoundException::new);
    model.setIsDeleted(true);
    repository.save(model);
  }

  @Override
  public T findOne(ID id) {
    return repository.findByIdAndIsDeletedFalse(id).orElseThrow(ResourceNotFoundException::new);
  }

  @Override
  public Optional<T> findOneOpt(ID id) {
    return repository.findByIdAndIsDeletedFalse(id);
  }

  @Override
  public List<T> findAll() {
    CommonSpecifications<T> spec = new CommonSpecifications<>();
    Specification<T> undeletedSpec = spec.unDeleted();
    return repository.findAll(undeletedSpec);
  }

  @Override
  public List<T> findIdListIn(List<ID> idLists) {
    return repository.findAllByIdInAndIsDeletedFalse(idLists);
  }

  @Override
  public List<T> findAll(Specification<T> spec) {
    return repository.findAll(spec);
  }

  @Override
  public Page<T> findAll(@Nullable Specification<T> spec, Pageable page) {
    return repository.findAll(spec, page);
  }

  @Override
  public Page<T> findUndeletedAll(Pageable page) {
    CommonSpecifications<T> spec = new CommonSpecifications<>();
    Specification<T> undeletedSpec = spec.unDeleted();
    return repository.findAll(undeletedSpec, page);
  }

  @Override
  public boolean exists(ID id) {
    return repository.existsByIdAndIsDeletedFalse(id);
  }

  @Override
  public Iterable<T> updateAll(Iterable<T> modelList) {
    return repository.saveAll(modelList);
  }

  public List<T> findAll(Specification<T> spec, Sort sort) {
    return repository.findAll(spec, sort);
  }

  @Override
  public void physicalDelete(T model) {
    repository.delete(model);
  }

  @Override
  public void deleteAllByModels(List<T> modelList) {
    repository.deleteAll(modelList);
  }
}

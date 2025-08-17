package com.genifast.dms.service;

import jakarta.annotation.Nullable;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import com.genifast.dms.entity.BaseEntity;

public interface BaseService<T extends BaseEntity, ID> {

  // default Specification<T> undeletedSpec() {
  // return (root, query, criteriaBuilder) ->
  // criteriaBuilder.equal(root.get(BaseEntity_.DELETED), false);
  // }

  T createOrUpdate(T model);

  T create(T model);

  T update(T model);

  List<T> createOrUpdate(List<T> models);

  void delete(ID id);

  T findOne(ID id);

  Optional<T> findOneOpt(ID id);

  List<T> findAll();

  List<T> findIdListIn(List<ID> idLists);

  default Map<Long, T> findAllMap() {
    return findAll().stream().collect(Collectors.toMap(T::getId, v -> v));
  }

  default Map<Long, T> findAllMap(List<ID> idLists) {
    return findIdListIn(idLists).stream().collect(Collectors.toMap(T::getId, v -> v));
  }

  List<T> findAll(@Nullable Specification<T> spec);

  Page<T> findAll(@Nullable Specification<T> spec, Pageable page);

  Page<T> findUndeletedAll(Pageable page);

  boolean exists(ID id);

  Iterable<T> updateAll(Iterable<T> modelList);

  List<T> findAll(@Nullable Specification<T> spec, Sort sort);

  void physicalDelete(T model);

  void deleteAllByModels(List<T> modelList);

  default void setDoubleOrNull(PreparedStatement preparedStatement, int index, Double value)
      throws SQLException {
    if (value == null) {
      preparedStatement.setNull(index, Types.NULL);
    } else {
      preparedStatement.setDouble(index, value);
    }
  }

  default void setLongOrNull(PreparedStatement preparedStatement, int index, Long value)
      throws SQLException {
    if (value == null) {
      preparedStatement.setNull(index, Types.NULL);
    } else {
      preparedStatement.setLong(index, value);
    }
  }

  default void setIntegerOrNull(PreparedStatement preparedStatement, int index, Integer value)
      throws SQLException {
    if (value == null) {
      preparedStatement.setNull(index, Types.NULL);
    } else {
      preparedStatement.setInt(index, value);
    }
  }
}

package com.teamroy.model.dao;

import java.util.List;

public interface GenericDao<T> {
    void Create(T entity);

    T GetByID(int id);

    List<T> GetAll();

    void Update(T entity);

    void Delete(int id);
}




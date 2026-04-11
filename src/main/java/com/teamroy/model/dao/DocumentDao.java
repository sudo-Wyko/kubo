package com.teamroy.model.dao;

import com.teamroy.model.entity.Document;
import java.util.List;

public interface DocumentDao extends GenericDao<Document> {
    List<Document> GetByTenantID(int tenantId);

    List<Document> GetByTitle(String title);

    Document GetByFilePath(String filePath);

    void DeleteByTenantID(int tenantId);
}

package com.teamroy.model.dao;

import com.teamroy.model.entity.Document;
import java.time.LocalDateTime;
import java.util.*;
import java.sql.*;

public class DocumentDaoImpl implements DocumentDao {
    private Connection conn;

    public DocumentDaoImpl(Connection conn) {
        this.conn = conn;
    }

    @Override
    public void Create(Document document) {
        String sql = "INSERT INTO DOCUMENT (tenant_id, title, file_path, uploaded_at) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, document.GetTenantID());
            ps.setString(2, document.GetTitle());
            ps.setString(3, document.GetFilePath());
            ps.setObject(4, document.GetTimeUploadedAt());
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next())
                    document.SetDocumentID(rs.getInt(1));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Document GetByID(int documentId) {
        String sql = "SELECT * FROM DOCUMENT WHERE document_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, documentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    return ResultSetToDocument(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public List<Document> GetAll() {
        List<Document> docs = new ArrayList<>();
        String sql = "SELECT * FROM DOCUMENT ORDER BY uploaded_at DESC";
        try (Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next())
                docs.add(ResultSetToDocument(rs));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return docs;
    }

    @Override
    public void Update(Document document) {
        String sql = "UPDATE DOCUMENT SET tenant_id=?, title=?, file_path=?, uploaded_at=? WHERE document_id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, document.GetTenantID());
            ps.setString(2, document.GetTitle());
            ps.setString(3, document.GetFilePath());
            ps.setObject(4, document.GetTimeUploadedAt());
            ps.setInt(5, document.GetDocumentID());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void Delete(int id) {
        String sql = "DELETE FROM DOCUMENT WHERE document_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<Document> GetByTenantID(int tenantId) {
        List<Document> docs = new ArrayList<>();
        String sql = "SELECT * FROM DOCUMENT WHERE tenant_id = ? ORDER BY uploaded_at DESC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, tenantId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    docs.add(ResultSetToDocument(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return docs;
    }

    @Override
    public List<Document> GetByTitle(String title) {
        List<Document> docs = new ArrayList<>();
        String sql = "SELECT * FROM DOCUMENT WHERE title LIKE ? ORDER BY uploaded_at DESC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "%" + title + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    docs.add(ResultSetToDocument(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return docs;
    }

    @Override
    public Document GetByFilePath(String filePath) {
        String sql = "SELECT * FROM DOCUMENT WHERE file_path = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, filePath);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    return ResultSetToDocument(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void DeleteByTenantID(int tenantId) {
        String sql = "DELETE FROM DOCUMENT WHERE tenant_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, tenantId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private Document ResultSetToDocument(ResultSet rs) throws SQLException {
        Document doc = new Document();
        doc.SetDocumentID(rs.getInt("document_id"));
        doc.SetTenantID(rs.getInt("tenant_id"));
        doc.SetTitle(rs.getString("title"));
        doc.SetFilePath(rs.getString("file_path"));
        doc.SetTimeUploadedAt(rs.getObject("uploaded_at", LocalDateTime.class));
        return doc;
    }
}
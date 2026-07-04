package com.example.oms.platform.repository;

import com.example.oms.platform.entity.Permission;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PermissionMapper {
    @Select("SELECT code, label, description, created_at FROM oms_ai_permissions ORDER BY code")
    List<Permission> findAll();
}

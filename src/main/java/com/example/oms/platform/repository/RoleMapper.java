package com.example.oms.platform.repository;

import com.example.oms.platform.entity.Role;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface RoleMapper {
    @Select("SELECT code, label, created_at FROM oms_ai_roles ORDER BY code")
    List<Role> findAll();
}

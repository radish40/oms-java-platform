package com.example.oms.platform.repository;

import com.example.oms.platform.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper {
    @Select("SELECT username, password_hash, display_name, status, created_at, updated_at FROM oms_ai_users WHERE username = #{username} LIMIT 1")
    User findByUsername(@Param("username") String username);
}

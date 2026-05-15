package com.baoying.vehicleapplication.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baoying.vehicleapplication.dto.response.UserInfoResponse;
import com.baoying.vehicleapplication.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserMapper extends BaseMapper<SysUser> {

    @Select("SELECT * FROM sys_user WHERE username = #{username}")
    SysUser selectByUsername(String username);

    @Select("SELECT realname FROM sys_user WHERE id = #{id}")
    String selectRealnameById(Long id);

    @Select("SELECT * from sys_user")
    List<UserInfoResponse> selectAll();
}
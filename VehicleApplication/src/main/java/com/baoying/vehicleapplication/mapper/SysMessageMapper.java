package com.baoying.vehicleapplication.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baoying.vehicleapplication.entity.SysMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 系统消息Mapper
 */
@Mapper
public interface SysMessageMapper extends BaseMapper<SysMessage> {
    
    /**
     * 标记消息为已读
     */
    @Update("UPDATE sys_message SET is_read = 1, read_time = NOW() WHERE id = #{id}")
    void markAsRead(@Param("id") Long id);
    
    /**
     * 批量标记用户的所有消息为已读
     */
    @Update("UPDATE sys_message SET is_read = 1, read_time = NOW() WHERE user_id = #{userId} AND is_read = 0")
    void markAllAsRead(@Param("userId") Long userId);
}

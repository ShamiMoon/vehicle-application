package com.baoying.vehicleapplication.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baoying.vehicleapplication.entity.CarApplication;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface CarApplicationMapper extends BaseMapper<CarApplication> {

    /**
     * 查询待审批列表（用户作为审批人）
     */
    @Select("SELECT * FROM process_apply " +
            "WHERE status IN (1, 2) " +
            "AND FIND_IN_SET(#{userId}, current_approver_ids) > 0 " +
            "ORDER BY create_time DESC")
    List<CarApplication> selectPendingListByUserId(@Param("userId") Long userId);

    /**
     * 更新申请状态
     */
    @Update("UPDATE process_apply SET status = #{status}, update_time = NOW() WHERE id = #{id}")
    void updateStatus(@Param("id") Long id, @Param("status") Integer status);

    /**
     * 更新当前节点
     */
    @Update("UPDATE process_apply SET current_node = #{nodeOrder}, " +
            "current_approver_ids = #{approverIds}, update_time = NOW() " +
            "WHERE id = #{id}")
    void updateCurrentNode(@Param("id") Long id,
                           @Param("nodeOrder") Integer nodeOrder,
                           @Param("approverIds") String approverIds);
}
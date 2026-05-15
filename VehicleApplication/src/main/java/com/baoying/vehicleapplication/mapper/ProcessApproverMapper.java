package com.baoying.vehicleapplication.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baoying.vehicleapplication.entity.ProcessApprover;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ProcessApproverMapper extends BaseMapper<ProcessApprover> {

    /**
     * 获取模板某个节点的所有预置审批人
     */
    @Select("SELECT user_id FROM process_approver " +
            "WHERE template_id = #{templateId} AND node_order = #{nodeOrder}")
    List<Long> selectUserIdsByTemplateAndNode(@Param("templateId") Integer templateId,
                                               @Param("nodeOrder") Integer nodeOrder);

    /**
     * 获取模板所有节点的预置审批人（按节点分组）
     */
    @Select("SELECT template_id, node_order, user_id FROM process_approver " +
            "WHERE template_id = #{templateId} ORDER BY node_order, user_id")
    List<ProcessApprover> selectByTemplateId(@Param("templateId") Integer templateId);

    /**
     * 批量删除模板的所有预置审批人
     */
    @Select("DELETE FROM process_approver WHERE template_id = #{templateId}")
    void deleteByTemplateId(@Param("templateId") Integer templateId);

    /**
     * 检查某个用户是否是某个模板节点的预置审批人
     */
    @Select("SELECT COUNT(*) FROM process_approver " +
            "WHERE template_id = #{templateId} AND node_order = #{nodeOrder} AND user_id = #{userId}")
    int countByTemplateNodeAndUser(@Param("templateId") Integer templateId,
                                    @Param("nodeOrder") Integer nodeOrder,
                                    @Param("userId") Long userId);
}
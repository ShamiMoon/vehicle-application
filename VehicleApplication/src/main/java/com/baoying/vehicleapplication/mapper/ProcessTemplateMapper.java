package com.baoying.vehicleapplication.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baoying.vehicleapplication.entity.ProcessTemplate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ProcessTemplateMapper extends BaseMapper<ProcessTemplate> {
    
    @Select("SELECT COUNT(*) FROM process_apply WHERE template_id = #{templateId} AND status IN (0,1,2)")
    Integer countUnfinishedApplyByTemplateId(Integer templateId);
}
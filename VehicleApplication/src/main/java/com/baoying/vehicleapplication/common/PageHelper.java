package com.baoying.vehicleapplication.common;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.beans.BeanUtils;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 通用分页查询助手类
 * 用于简化分页查询的代码编写
 * 
 * @param <E> 实体类型
 * @param <D> DTO/响应类型
 */
public class PageHelper<E, D> {
    
    /**
     * 执行分页查询并转换结果
     * 
     * @param service MyBatis-Plus Service
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @param queryWrapper 查询条件
     * @param converter 实体转DTO的转换器
     * @return 分页结果
     */
    public static <E, D> Page<D> queryPage(
            IService<E> service,
            int pageNum,
            int pageSize,
            LambdaQueryWrapper<E> queryWrapper,
            Function<E, D> converter) {
        
        // 创建分页对象
        Page<E> page = new Page<>(pageNum, pageSize);
        
        // 执行分页查询
        IPage<E> resultPage = service.page(page, queryWrapper);
        
        // 转换结果
        Page<D> dtoPage = new Page<>(resultPage.getCurrent(), resultPage.getSize(), resultPage.getTotal());
        List<D> dtoList = resultPage.getRecords().stream()
                .map(converter)
                .collect(Collectors.toList());
        dtoPage.setRecords(dtoList);
        
        return dtoPage;
    }
    
    /**
     * 执行分页查询（使用默认转换器）
     * 
     * @param service MyBatis-Plus Service
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @param queryWrapper 查询条件
     * @param dtoClass DTO类
     * @return 分页结果
     */
    public static <E, D> Page<D> queryPage(
            IService<E> service,
            int pageNum,
            int pageSize,
            LambdaQueryWrapper<E> queryWrapper,
            Class<D> dtoClass) {
        
        // 创建分页对象
        Page<E> page = new Page<>(pageNum, pageSize);
        
        // 执行分页查询
        IPage<E> resultPage = service.page(page, queryWrapper);
        
        // 转换结果（使用BeanUtils自动复制）
        Page<D> dtoPage = new Page<>(resultPage.getCurrent(), resultPage.getSize(), resultPage.getTotal());
        List<D> dtoList = resultPage.getRecords().stream()
                .map(entity -> {
                    try {
                        D dto = dtoClass.getDeclaredConstructor().newInstance();
                        BeanUtils.copyProperties(entity, dto);
                        return dto;
                    } catch (Exception e) {
                        throw new RuntimeException("DTO转换失败", e);
                    }
                })
                .collect(Collectors.toList());
        dtoPage.setRecords(dtoList);
        
        return dtoPage;
    }
    
    /**
     * 构建分页查询对象（链式调用）
     */
    public static <E, D> PageQueryBuilder<E, D> builder(IService<E> service) {
        return new PageQueryBuilder<>(service);
    }
    
    /**
     * 分页查询构建器（支持链式调用）
     */
    public static class PageQueryBuilder<E, D> {
        private final IService<E> service;
        private int pageNum = 1;
        private int pageSize = 10;
        private LambdaQueryWrapper<E> queryWrapper;
        private Function<E, D> converter;
        private Class<D> dtoClass;
        
        public PageQueryBuilder(IService<E> service) {
            this.service = service;
        }
        
        /**
         * 设置页码
         */
        public PageQueryBuilder<E, D> pageNum(int pageNum) {
            this.pageNum = pageNum;
            return this;
        }
        
        /**
         * 设置每页大小
         */
        public PageQueryBuilder<E, D> pageSize(int pageSize) {
            this.pageSize = pageSize;
            return this;
        }
        
        /**
         * 设置查询条件
         */
        public PageQueryBuilder<E, D> queryWrapper(LambdaQueryWrapper<E> queryWrapper) {
            this.queryWrapper = queryWrapper;
            return this;
        }
        
        /**
         * 设置转换器（Lambda表达式）
         */
        public PageQueryBuilder<E, D> converter(Function<E, D> converter) {
            this.converter = converter;
            return this;
        }
        
        /**
         * 设置DTO类（使用默认转换）
         */
        public PageQueryBuilder<E, D> dtoClass(Class<D> dtoClass) {
            this.dtoClass = dtoClass;
            return this;
        }
        
        /**
         * 执行查询
         */
        public Page<D> execute() {
            if (queryWrapper == null) {
                queryWrapper = new LambdaQueryWrapper<>();
            }
            
            if (converter != null) {
                return queryPage(service, pageNum, pageSize, queryWrapper, converter);
            } else if (dtoClass != null) {
                return queryPage(service, pageNum, pageSize, queryWrapper, dtoClass);
            } else {
                throw new IllegalStateException("必须设置converter或dtoClass");
            }
        }
    }
}

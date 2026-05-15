package com.baoying.vehicleapplication.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 权限控制注解
 * 用于标记需要特定角色或数据权限才能访问的接口
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirePermission {
    
    /**
     * 需要的角色ID列表（可选）
     * 如果指定，则用户必须拥有其中任意一个角色
     */
    int[] roles() default {};
    
    /**
     * 数据权限范围
     * self: 仅个人数据
     * dept: 本部门数据
     * dept_and_sub: 本部门及下级部门数据
     * all: 全部数据
     */
    String dataScope() default "self";
    
    /**
     * 是否需要检查角色
     */
    boolean checkRole() default true;
}

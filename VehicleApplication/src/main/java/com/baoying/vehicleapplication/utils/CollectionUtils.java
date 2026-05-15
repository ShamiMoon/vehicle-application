package com.baoying.vehicleapplication.utils;

import java.util.*;
import java.util.stream.Collectors;

public class CollectionUtils {

    /**
     * 对Long类型列表去重（保持原有顺序）
     */
    public static List<Long> distinct(List<Long> list) {
        if (list == null || list.isEmpty()) {
            return new ArrayList<>();
        }
        Set<Long> linkedSet = new LinkedHashSet<>(list);
        return new ArrayList<>(linkedSet);
    }

    /**
     * 对任意类型列表去重（保持原有顺序）
     */
    public static <T> List<T> distinct(List<T> list, Comparator<T> comparator) {
        if (list == null || list.isEmpty()) {
            return new ArrayList<>();
        }
        return list.stream()
                .distinct()
                .collect(Collectors.toList());
    }
}
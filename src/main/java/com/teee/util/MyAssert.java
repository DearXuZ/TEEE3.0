package com.teee.util;

import com.baomidou.mybatisplus.core.toolkit.Assert;
import com.baomidou.mybatisplus.core.toolkit.ExceptionUtils;
import com.teee.exception.BusinessException;
import com.teee.project.ProjectCode;

public class MyAssert {
    public static void isTrue(boolean expression, String message, Object... params) {
        if (!expression) {
            throw new BusinessException(ProjectCode.CODE_EXCEPTION_BUSSINESS, message);
        }
    }
    public static void notNull(Object obj, String message, Object... params) {
        isTrue(obj !=null, message);
    }
}
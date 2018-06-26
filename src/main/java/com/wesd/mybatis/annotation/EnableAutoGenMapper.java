package com.wesd.mybatis.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface EnableAutoGenMapper {
    //model包
    String[] modelPackageName();

    //待生成的mapper包
    String mapperPackageName();

    //生成的mapper前缀
    String mapperPrefix() default "";

    //生成的mapper继承类
    String superMapperClassName();
}

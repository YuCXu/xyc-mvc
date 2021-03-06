package com.xyc.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)  //定义注解的作用目标,接口、类、枚举、注解
@Retention(RetentionPolicy.RUNTIME)  //定义注解的保留策略,注解会在class字节码文件中存在，在运行时可以通过反射获取到
@Documented //说明该注解将被包含在javadoc中
public @interface XycService {
	String value() default "";
}

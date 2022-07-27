package com.mashibing.selfAutowired;

@Target({
    ElementType.CONSTRUCTOR,
    ElementType.METHOD,
    ElementType.PARAMETER,
    ElementType.FIELD,
    ElementType.ANNOTATION_TYPE
})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Lian {

    boolean required() default true;
}

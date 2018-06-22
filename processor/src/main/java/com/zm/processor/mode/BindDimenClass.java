package com.zm.processor.mode;
import com.zm.binddimen.BindDimen;

import java.util.Arrays;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;

/**
 * Author: lhy
 * Date: 2018/6/21
 */
public class BindDimenClass {
    private String path;
    private String[] dimenFit;

    public BindDimenClass(Element element) throws IllegalArgumentException {
        if (element.getKind() != ElementKind.CLASS) {
            throw new IllegalArgumentException(String.format("Only fields can be annotated with @%s", BindDimen.class.getSimpleName()));
        }
        TypeElement typeElement = (TypeElement) element;
        path = typeElement.getAnnotation(BindDimen.class).normal();
        dimenFit = typeElement.getAnnotation(BindDimen.class).dimenFit();
        if (path == null || path.isEmpty()) {
            throw new IllegalArgumentException("'path' parameter is null or empty,please set parameter 'path'.");
        }
        if(dimenFit == null || dimenFit.length == 0){
            throw new IllegalArgumentException("'dimenFit' parameter is null or empty,please set parameter 'dimenFit'.");
        }
    }

    public String getPath() {
        return path;
    }

    public String[] getDimenFit() {
        return dimenFit;
    }

    @Override
    public String toString() {
        return "BindDimenClass{" +
                "path='" + path + '\'' +
                ", dimenFit=" + Arrays.toString(dimenFit) +
                '}';
    }
}

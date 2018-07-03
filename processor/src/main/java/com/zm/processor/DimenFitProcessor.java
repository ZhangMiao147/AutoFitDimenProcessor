package com.zm.processor;

import com.google.auto.service.AutoService;
import com.zm.binddimen.BindDimen;
import com.zm.processor.mode.BindDimenClass;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;

/**
 * Author: lhy
 * Date: 2018/6/20
 */
@AutoService(Processor.class)
public class DimenFitProcessor extends AbstractProcessor {

    private Messager mMessager;//日志相关的辅助类
    private Map<String, BindDimenClass> mAnnotatedClassMap = new HashMap<>();

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        mMessager = processingEnv.getMessager();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> types = new LinkedHashSet<>();
        types.add(BindDimen.class.getCanonicalName());
        return types;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        mAnnotatedClassMap.clear();
        try {
            processBindDimen(roundEnv);
        } catch (IllegalArgumentException e) {
            error(e.getMessage());
            return true;
        }
        for (BindDimenClass bindDimenClass : mAnnotatedClassMap.values()) {
            info("\nbindDimenClass:" + bindDimenClass);
            String path = bindDimenClass.getPath();
            String[] dimenFit = bindDimenClass.getDimenFit();
            createFile(path,dimenFit);
        }
        return true;
    }

    private void processBindDimen(RoundEnvironment environment) {
        for (Element element : environment.getElementsAnnotatedWith(BindDimen.class)) {
            TypeElement typeElement = (TypeElement) element;
            String fullClassName = typeElement.getQualifiedName().toString();
            BindDimenClass bindDimenClass = mAnnotatedClassMap.get(fullClassName);
            if (bindDimenClass == null) {
                bindDimenClass = new BindDimenClass(element);
                mAnnotatedClassMap.put(fullClassName, bindDimenClass);
            }
        }
    }

    private void createFile(String path,String[] dimenFit) {
        if (path == null || path.isEmpty() || dimenFit == null || dimenFit.length == 0) {
            return;
        }
        int endIndex = path.lastIndexOf("/");
        int firstIndex = path.indexOf("values");
        String sub = path.substring(firstIndex, endIndex);
        info("\nsub:" + sub);
        StringBuffer numStr = new StringBuffer();
        for (int i = 0; i < sub.length(); i++) {
            char c = sub.charAt(i);
            if (c >= '0' && c <= '9') {
                numStr.append(c);
            } else {
                if (c == 'x') {
                    break;
                }
            }
        }
        info("\nnum:" + numStr.toString());
        int valueNum = Integer.parseInt(numStr.toString());

        File file = new File(path);
        BufferedReader reader = null;

        int length = dimenFit.length;
        double[] multiple = new double[length];
        String[] pathArray = new String[length];
        for (int i = 0; i<length; i++){
            String fit = dimenFit[i];
            info("i:"+i+",fit:"+fit);
            int index = fit.indexOf("x");
            double size = Double.parseDouble(fit.substring(0,index));
            multiple[i] = size/valueNum;
            pathArray[i] = "values-" + fit;
        }

        StringBuilder[] builders = new StringBuilder[multiple.length];
        for (int i = 0; i < multiple.length; i++) {
            builders[i] = new StringBuilder();
        }

        try {
            reader = new BufferedReader(new FileReader(file));
            String tempString;
            while ((tempString = reader.readLine()) != null) {
                if (tempString.contains("</dimen>")) {
                    String start = tempString.substring(0, tempString.indexOf(">") + 1);
                    String end = tempString.substring(tempString.lastIndexOf("<") - 2);
                    int num = Integer.valueOf(tempString.substring(tempString.indexOf(">") + 1, tempString.indexOf("</dimen>") - 2));

                    for (int i = 0; i < builders.length; i++) {
                        builders[i].append(start).append((int) Math.round(num * multiple[i])).append(end).append("\n");
                    }
                } else {

                    for (int i = 0; i < builders.length; i++) {
                        builders[i].append(tempString).append("\n");
                    }
                }
            }


             List<String> pathList = new ArrayList<>();
            String firstStr = path.substring(0, firstIndex);
            String endStr = path.substring(endIndex, path.length());
            for (int i = 0; i < pathArray.length; i++) {
                String s = firstStr + pathArray[i] + endStr;
                pathList.add(s);
            }

            for (int i = 0; i < pathList.size(); i++) {
                String p = pathList.get(i);
                File dimenFile = new File(p);
                int index = p.lastIndexOf("/");
                String dirPath = p.substring(0, index);
                File dirFile = new File(dirPath);
                if (!dirFile.exists()) {
                    dirFile.mkdirs();
                }
                if (!dimenFile.exists()) {
                    dimenFile.createNewFile();
                }
                FileOutputStream fos = null;
                PrintWriter pw = null;
                try {
                    fos = new FileOutputStream(dimenFile);
                    pw = new PrintWriter(fos);
                    pw.write(builders[i].toString().toCharArray());
                    pw.flush();

                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (fos != null) {
                        fos.close();
                    }
                    if (pw != null) {
                        pw.close();
                    }
                }
            }


        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }


    private void error(String msg, Object... args) {
        mMessager.printMessage(Diagnostic.Kind.ERROR, String.format(msg, args));
    }

    private void info(String msg, Object... args) {
        mMessager.printMessage(Diagnostic.Kind.NOTE, String.format(msg, args));
    }
}

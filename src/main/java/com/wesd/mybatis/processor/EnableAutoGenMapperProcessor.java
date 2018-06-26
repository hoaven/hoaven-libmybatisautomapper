package com.wesd.mybatis.processor;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.*;
import com.wesd.mybatis.annotation.EnableAutoGenMapper;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.persistence.Table;
import java.io.File;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 编译时生成Mapper代码。
 * 注解处理器运行在它自己的 JVM 中。javac 启动了一个完整的 java 虚拟机来运行注解处理器。
 */
@AutoService(Processor.class)
public class EnableAutoGenMapperProcessor extends AbstractProcessor {
    /**
     * 使用这个类来创建文件
     */
    private Filer filer;

    /**
     * Element帮助类，Element代表程序中的元素，比如说 包，类，方法，属性等。
     * 换个角度来看源代码。它只是结构化的文本而已。类似XML文件。或者一棵编译中创建的抽象语法树。
     */
    public Elements elementUtils;

    /**
     * 定义你的注解处理器要处理哪些注解。
     *
     * @return
     */
    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> types = new LinkedHashSet<>();
        //返回com.wesd.mybatis.annotation.EnableAutoGenMapper
        types.add(EnableAutoGenMapper.class.getCanonicalName());
        return types;
    }

    /**
     * 指定你使用的 java 版本。
     * 通常返回SourceVersion.latestSupported()。
     *
     * @return
     */
    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    /**
     * 它会被注解处理工具调用，以ProcessingEnvironment作为参数。
     * ProcessingEnvironment 提供了一些实用的工具类Elements, Types和Filer
     *
     * @param processingEnv
     */
    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        filer = processingEnv.getFiler();
        elementUtils = processingEnv.getElementUtils();
        //typeUtils = processingEnv.getTypeUtils();  TypeMirror工具类
        // elementUtils代表源代码，TypeElement代表源代码中的元素类型，例如类。
        // 然后，TypeElement并不包含类的相关信息。你可以从TypeElement获取类的名称，
        // 但你不能获取类的信息，比如说父类。这些信息可以通过TypeMirror获取。
        // 你可以通过调用element.asType()来获取一个Element的TypeMirror
    }

    /**
     * 这类似于每个处理器的main()方法。
     * 你可以在这个方法里面编码实现扫描，处理注解，生成 java 文件。
     * 使用RoundEnvironment 参数，你可以查询被特定注解标注的元素
     *
     * @param annotations
     * @param roundEnv
     * @return
     */
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        //roundEnv.getElementsAnnotatedWith(EnableAutoGenMapper.class)返回一个被@EnableAutoGenMapper注解的元素列表(类、方法、变量等)
        //由于@EnableAutoGenMapper用在类上，所以rootElement一定是类元素
        //遍历应用项目使用EnableAutoGenMapper注解的类，每个lib项目一般用在***BaseDao类上
        for (Element rootElement : roundEnv.getElementsAnnotatedWith(EnableAutoGenMapper.class)) {
            EnableAutoGenMapper genMapper = rootElement.getAnnotation(EnableAutoGenMapper.class);

            //存放modelPackageName数组元素
            Set<String> packageNames = new LinkedHashSet<>();
            for (String modelPackageName : genMapper.modelPackageName()) {
                //当前包，由于当前包除了子包外，可能还有model，所以modelPackageName得加上
                packageNames.add(modelPackageName);
                //当前包的子包
                packageNames.addAll(getPackageNames(modelPackageName));
            }

            //存放所有modelPackageName数组元素包下使用了Table注解(model和数据库表的映射)的model
            Set<TypeElement> typeElements = new LinkedHashSet<>();
            for (String modelPackageName : packageNames) {
                //获取model所在包
                PackageElement packageElement = elementUtils.getPackageElement(modelPackageName);
                for (Element findElement : packageElement.getEnclosedElements()) {
                    TypeElement typeElement = (TypeElement) findElement;
                    if (typeElement.getAnnotation(Table.class) == null) {
                        continue;
                    }
                    typeElements.add(typeElement);
                }
            }

            //遍历model生成mapper
            for (TypeElement typeElement : typeElements) {
                try {
                    //获取model名
                    String shortClassName = getShortClassName(typeElement);
                    //待生成的mapper名
                    String mapperName = genMapper.mapperPrefix() + shortClassName + "Mapper";
                    //待继承的mapper--一般为GenericMapper
                    ClassName basemapper = ClassName.get(getMapper(genMapper.superMapperClassName(), true), getMapper(genMapper.superMapperClassName(), false));
                    ClassName model = ClassName.get(typeElement);

                    //使用JavaPoet在编译期间生成Java文件
                    //继承的父接口，ParameterizedTypeName指参数化类型 --> basemapper<model>
                    TypeName superinterface = ParameterizedTypeName.get(basemapper, model);
                    //Class定义
                    TypeSpec mapper = TypeSpec.interfaceBuilder(mapperName)
                            .addModifiers(Modifier.PUBLIC)
                            .addSuperinterface(superinterface)
                            .build();

                    //写入java文件，JavaFile表示一个Java文件
                    JavaFile javaFile = JavaFile.builder(genMapper.mapperPackageName(), mapper)
                            .build();
                    javaFile.writeTo(filer);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return true;
    }

    public String getMapper(String fullClass, boolean packageflag) {
        int index = fullClass.lastIndexOf(".");
        if (packageflag) {
            return fullClass.substring(0, index);
        }
        return fullClass.substring(index + 1);
    }

    private Set<String> getPackageNames(String rootPackageName) {
        Set<String> packageNames = new LinkedHashSet<>();
        //当前包的系统路径
        StringBuilder dirUrl = new StringBuilder(System.getProperty("user.dir"));
        dirUrl.append(File.separator).append("src");
        dirUrl.append(File.separator).append("main");
        dirUrl.append(File.separator).append("java");
        dirUrl.append(File.separator).append(rootPackageName.replace(".", File.separator));
        String rootPath = dirUrl.toString();
        File rootFile = new File(rootPath);
        if (rootFile.exists() && rootFile.isDirectory()) {
            //遍历当前包的子file
            for (File file : rootFile.listFiles()) {
                if (file.isDirectory()) {
                    //获取子包相对于当前包的后面一部分路径，且以.分割
                    String dirname = file.getAbsolutePath().replace(rootPath, "").replace(File.separator, ".");
                    String packageName = rootPackageName + dirname;
                    packageNames.add(packageName);
                    packageNames.addAll(getPackageNames(packageName));
                }
            }
        }
        return packageNames;
    }

    private String getPackageName(Element element) {
        String packageName = elementUtils.getPackageOf(element).getQualifiedName().toString();
        return packageName.substring(0, packageName.lastIndexOf("."));
    }

    private String getShortClassName(Element element) {
        String packageName = elementUtils.getPackageOf(element).getQualifiedName().toString();
        int packageLen = packageName.length() + 1;
        return getFullClassName(element).substring(packageLen).replace('.', '$');
    }

    public String getFullClassName(Element element) {
        return ((TypeElement) element).getQualifiedName().toString();
    }
}

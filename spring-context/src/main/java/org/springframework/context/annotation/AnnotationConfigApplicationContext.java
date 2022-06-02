/*
 * Copyright 2002-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.context.annotation;

import java.util.function.Supplier;

import org.springframework.beans.factory.config.BeanDefinitionCustomizer;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Standalone application context, accepting <em>component classes</em> as input &mdash;
 * in particular {@link Configuration @Configuration}-annotated classes, but also plain
 * {@link org.springframework.stereotype.Component @Component} types and JSR-330 compliant
 * classes using {@code javax.inject} annotations.
 *
 * <p>Allows for registering classes one by one using {@link #register(Class...)}
 * as well as for classpath scanning using {@link #scan(String...)}.
 *
 * <p>In case of multiple {@code @Configuration} classes, {@link Bean @Bean} methods
 * defined in later classes will override those defined in earlier classes. This can
 * be leveraged to deliberately override certain bean definitions via an extra
 * {@code @Configuration} class.
 *
 * <p>See {@link Configuration @Configuration}'s javadoc for usage examples.
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @see #register
 * @see #scan
 * @see AnnotatedBeanDefinitionReader
 * @see ClassPathBeanDefinitionScanner
 * @see org.springframework.context.support.GenericXmlApplicationContext
 * @since 3.0
 */
public class AnnotationConfigApplicationContext extends GenericApplicationContext implements AnnotationConfigRegistry {

    /*
     * AnnotationConfigApplicationContext对象，主要是做了3个主要的操作
     *
     * 1、创建一个new DefaultListableBeanFactory() 是一个Bean工厂容器（GenericApplicationContext的beanFactory，beanFactory主要是用来存放Spring管理的Bean对象，一个Bean存放的工厂）
     * 2、创建一个new AnnotatedBeanDefinitionReader(this)，是Bean的读取器
     * 3、创建一个new ClassPathBeanDefinitionScanner(this)，是Bean的扫描器
     *
     *
     * AnnotationConfigApplicationContext extends GenericApplicationContext implements BeanDefinitionRegistry
     * 可以得出 AnnotationConfigApplicationContext也是一个BeanDefinitionRegistry，对BeanDefinition有增删改查操作
     *
     */

    /**
     * 读取器,读取一个被加了注解的bean,在构造方法中实例化
     * 对加了特定注解（如 @Service、@Repository）的类进行读取转化成 BeanDefinition 对象（BeanDefinition 是 Spring 中极其重要的一个概念，
     * 它存储了 bean 对象的所有特征信息，如是否单例，是否懒加载，factoryBeanName 等），那么就需要一个注解配置读取器
     */
    private final AnnotatedBeanDefinitionReader reader;
    /**
     * 扫描器，扫描所有加了注解的bean,在构造方法中实例化
     * 对用户指定的包目录进行扫描查找 bean 对象，那么还需要一个路径扫描器
     */
    private final ClassPathBeanDefinitionScanner scanner;

    /**
     * 初始化一个bean读取器和扫描器
     * 默认构造函数，如果直接调用这个默认构造方法，需要在稍后通过调用其register() 去注册配置类（javaconfig），
     * 并调用refresh()方法刷新容器，
     * 触发容器对注解Bean的载入、解析和注册过程
     * Create a new AnnotationConfigApplicationContext that needs to be populated
     * through {@link #register} calls and then manually {@linkplain #refresh refreshed}.
     */
    public AnnotationConfigApplicationContext() {
        /*
         * 创建一个读取注解的Bean定义读取器(bean定义，即为BeanDefinition)
         */
        this.reader = new AnnotatedBeanDefinitionReader(this);
        this.scanner = new ClassPathBeanDefinitionScanner(this);
    }

    /**
     * Create a new AnnotationConfigApplicationContext with the given DefaultListableBeanFactory.
     *
     * @param beanFactory the DefaultListableBeanFactory instance to use for this context
     */
    public AnnotationConfigApplicationContext(DefaultListableBeanFactory beanFactory) {
        super(beanFactory);
        this.reader = new AnnotatedBeanDefinitionReader(this);
        this.scanner = new ClassPathBeanDefinitionScanner(this);
    }

    /**
     * Create a new AnnotationConfigApplicationContext, deriving bean definitions
     * from the given component classes and automatically refreshing the context.
     *
     * @param componentClasses one or more component classes &mdash; for example,
     *                         {@link Configuration @Configuration} classes
     */
    public AnnotationConfigApplicationContext(Class<?>... componentClasses) {
        this();
        register(componentClasses);
        refresh();
    }

    /**
     * Create a new AnnotationConfigApplicationContext, scanning for components
     * in the given packages, registering bean definitions for those components,
     * and automatically refreshing the context.
     *
     * @param basePackages the packages to scan for component classes
     */
    public AnnotationConfigApplicationContext(String... basePackages) {
        this();
        scan(basePackages);
        refresh();
    }


    /**
     * Propagate the given custom {@code Environment} to the underlying
     * {@link AnnotatedBeanDefinitionReader} and {@link ClassPathBeanDefinitionScanner}.
     */
    @Override
    public void setEnvironment(ConfigurableEnvironment environment) {
        super.setEnvironment(environment);
        this.reader.setEnvironment(environment);
        this.scanner.setEnvironment(environment);
    }

    /**
     * Provide a custom {@link BeanNameGenerator} for use with {@link AnnotatedBeanDefinitionReader}
     * and/or {@link ClassPathBeanDefinitionScanner}, if any.
     * <p>Default is {@link AnnotationBeanNameGenerator}.
     * <p>Any call to this method must occur prior to calls to {@link #register(Class...)}
     * and/or {@link #scan(String...)}.
     *
     * @see AnnotatedBeanDefinitionReader#setBeanNameGenerator
     * @see ClassPathBeanDefinitionScanner#setBeanNameGenerator
     * @see AnnotationBeanNameGenerator
     * @see FullyQualifiedAnnotationBeanNameGenerator
     */
    public void setBeanNameGenerator(BeanNameGenerator beanNameGenerator) {
        this.reader.setBeanNameGenerator(beanNameGenerator);
        this.scanner.setBeanNameGenerator(beanNameGenerator);
        getBeanFactory().registerSingleton(AnnotationConfigUtils.CONFIGURATION_BEAN_NAME_GENERATOR, beanNameGenerator);
    }

    /**
     * Set the {@link ScopeMetadataResolver} to use for registered component classes.
     * <p>The default is an {@link AnnotationScopeMetadataResolver}.
     * <p>Any call to this method must occur prior to calls to {@link #register(Class...)}
     * and/or {@link #scan(String...)}.
     */
    public void setScopeMetadataResolver(ScopeMetadataResolver scopeMetadataResolver) {
        this.reader.setScopeMetadataResolver(scopeMetadataResolver);
        this.scanner.setScopeMetadataResolver(scopeMetadataResolver);
    }


    //---------------------------------------------------------------------
    // Implementation of AnnotationConfigRegistry
    //---------------------------------------------------------------------

    /**
     * Register one or more component classes to be processed.
     * <p>Note that {@link #refresh()} must be called in order for the context
     * to fully process the new classes.
     *
     * @param componentClasses one or more component classes &mdash; for example,
     *                         {@link Configuration @Configuration} classes
     * @see #scan(String...)
     * @see #refresh()
     */
    @Override
    public void register(Class<?>... componentClasses) {
        Assert.notEmpty(componentClasses, "At least one component class must be specified");
        this.reader.register(componentClasses);
    }

    /**
     * Perform a scan within the specified base packages.
     * <p>Note that {@link #refresh()} must be called in order for the context
     * to fully process the new classes.
     *
     * @param basePackages the packages to scan for component classes
     * @see #register(Class...)
     * @see #refresh()
     */
    @Override
    public void scan(String... basePackages) {
        Assert.notEmpty(basePackages, "At least one base package must be specified");
        this.scanner.scan(basePackages);
    }


    //---------------------------------------------------------------------
    // Adapt superclass registerBean calls to AnnotatedBeanDefinitionReader
    //---------------------------------------------------------------------

    @Override
    public <T> void registerBean(@Nullable String beanName, Class<T> beanClass, @Nullable Supplier<T> supplier, BeanDefinitionCustomizer... customizers) {

        this.reader.registerBean(beanClass, beanName, supplier, customizers);
    }

}
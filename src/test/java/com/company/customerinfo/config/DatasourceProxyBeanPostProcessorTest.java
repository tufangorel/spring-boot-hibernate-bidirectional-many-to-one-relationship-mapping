package com.company.customerinfo.config;

import net.ttddyy.dsproxy.support.ProxyDataSource;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DatasourceProxyBeanPostProcessorTest {

    interface DummyInvocationMethod {
        Object notOnDataSource();
    }

    @Test
    void postProcessAfterInitializationWrapsRegularDataSource() {
        DatasourceProxyBeanPostProcessor processor = new DatasourceProxyBeanPostProcessor();
        DataSource dataSource = mock(DataSource.class);

        Object processed = processor.postProcessAfterInitialization(dataSource, "testDataSource");

        assertThat(processed).isNotSameAs(dataSource);
    }

    @Test
    void postProcessAfterInitializationKeepsProxyDataSourceUnchanged() {
        DatasourceProxyBeanPostProcessor processor = new DatasourceProxyBeanPostProcessor();
        DataSource target = mock(DataSource.class);
        ProxyDataSource proxyDataSource = ProxyDataSourceBuilder.create(target).build();

        Object processed = processor.postProcessAfterInitialization(proxyDataSource, "proxyDataSource");

        assertThat(processed).isSameAs(proxyDataSource);
    }

    @Test
    void postProcessBeforeInitializationReturnsSameBean() {
        DatasourceProxyBeanPostProcessor processor = new DatasourceProxyBeanPostProcessor();
        Object bean = new Object();

        Object processed = processor.postProcessBeforeInitialization(bean, "bean");

        assertThat(processed).isSameAs(bean);
    }

    @Test
    void interceptorInvokeDelegatesToProxyDataSourceMethodWhenAvailable() throws Throwable {
        Object interceptor = createInterceptor(mock(DataSource.class));
        Method invokeMethod = interceptor.getClass().getDeclaredMethod("invoke", MethodInvocation.class);
        invokeMethod.setAccessible(true);

        MethodInvocation invocation = mock(MethodInvocation.class);
        when(invocation.getMethod()).thenReturn(Object.class.getMethod("toString"));
        when(invocation.getArguments()).thenReturn(new Object[0]);

        Object result = invokeMethod.invoke(interceptor, invocation);

        assertThat(result).isNotNull();
    }

    @Test
    void interceptorInvokeFallsBackToProceedWhenMethodMissing() throws Throwable {
        Object interceptor = createInterceptor(mock(DataSource.class));
        Method invokeMethod = interceptor.getClass().getDeclaredMethod("invoke", MethodInvocation.class);
        invokeMethod.setAccessible(true);

        MethodInvocation invocation = mock(MethodInvocation.class);
        when(invocation.getMethod()).thenReturn(DummyInvocationMethod.class.getMethod("notOnDataSource"));
        when(invocation.getArguments()).thenReturn(new Object[0]);
        when(invocation.proceed()).thenReturn("fallback");

        Object result = invokeMethod.invoke(interceptor, invocation);

        assertThat(result).isEqualTo("fallback");
    }

    private Object createInterceptor(DataSource dataSource) throws Exception {
        Class<?> clazz = Class.forName("com.company.customerinfo.config.DatasourceProxyBeanPostProcessor$ProxyDataSourceInterceptor");
        Constructor<?> constructor = clazz.getDeclaredConstructor(DataSource.class);
        constructor.setAccessible(true);
        return constructor.newInstance(dataSource);
    }
}

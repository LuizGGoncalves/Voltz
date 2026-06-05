package com.treinamento.clientes.config

import org.slf4j.LoggerFactory
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.AsyncConfigurer
import java.lang.reflect.Method

@Configuration
class AsyncConfig : AsyncConfigurer {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun getAsyncUncaughtExceptionHandler(): AsyncUncaughtExceptionHandler =
        AsyncUncaughtExceptionHandler { throwable: Throwable, method: Method, params: Array<out Any> ->
            log.error("Exceção não tratada em @Async {}.{}: {}",
                method.declaringClass.simpleName, method.name, throwable.message, throwable)
        }
}

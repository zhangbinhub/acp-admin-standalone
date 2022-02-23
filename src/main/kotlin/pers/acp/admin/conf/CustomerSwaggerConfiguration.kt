package pers.acp.admin.conf

import com.github.xiaoymin.knife4j.spring.annotations.EnableKnife4j
import io.github.zhangbinhub.acp.boot.conf.SwaggerConfiguration
import org.springframework.beans.BeansException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.beans.factory.config.BeanPostProcessor
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.util.ReflectionUtils
import org.springframework.web.servlet.mvc.method.RequestMappingInfoHandlerMapping
import pers.acp.admin.base.BaseSwaggerConfiguration
import springfox.documentation.spring.web.plugins.WebFluxRequestHandlerProvider
import springfox.documentation.spring.web.plugins.WebMvcRequestHandlerProvider
import springfox.documentation.swagger2.annotations.EnableSwagger2

/**
 * @author zhang by 27/12/2018
 * @since JDK 11
 */
@Configuration(proxyBeanMethods = false)
@EnableSwagger2
@EnableKnife4j
class CustomerSwaggerConfiguration @Autowired
constructor(
    @Value("\${info.version}")
    version: String?,
    swaggerConfiguration: SwaggerConfiguration
) : BaseSwaggerConfiguration(version, swaggerConfiguration) {

    @Bean
    fun createRestApi() = buildDocket("pers.acp.admin.controller", "Server RESTful API")

    @Bean
    fun springfoxHandlerProviderBeanPostProcessor(): BeanPostProcessor? {
        return object : BeanPostProcessor {
            @Throws(BeansException::class)
            override fun postProcessAfterInitialization(bean: Any, beanName: String): Any {
                if (bean is WebMvcRequestHandlerProvider || bean is WebFluxRequestHandlerProvider) {
                    customizeSpringfoxHandlerMappings(getHandlerMappings(bean))
                }
                return bean
            }

            private fun customizeSpringfoxHandlerMappings(mappings: Any?) {
                if (mappings is ArrayList<*>) {
                    mappings.removeIf { mapping ->
                        if (mapping is RequestMappingInfoHandlerMapping) {
                            mapping.patternParser != null
                        } else {
                            false
                        }
                    }
                }
            }

            private fun getHandlerMappings(bean: Any): Any? =
                try {
                    ReflectionUtils.findField(bean.javaClass, "handlerMappings")?.let {
                        it.isAccessible = true
                        it[bean]
                    } ?: mutableListOf<RequestMappingInfoHandlerMapping>()
                } catch (e: IllegalArgumentException) {
                    throw IllegalStateException(e)
                } catch (e: IllegalAccessException) {
                    throw IllegalStateException(e)
                }
        }
    }
}
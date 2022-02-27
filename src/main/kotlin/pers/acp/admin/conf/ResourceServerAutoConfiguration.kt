package pers.acp.admin.conf

import io.github.zhangbinhub.acp.core.CommonTools
import io.github.zhangbinhub.acp.core.log.LogFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.web.ServerProperties
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter
import org.springframework.security.oauth2.config.annotation.web.configurers.ResourceServerSecurityConfigurer
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.access.AccessDeniedHandler
import pers.acp.admin.constant.RestPrefix

/**
 * Oauth2 资源服务配置
 *
 * @author zhangbin by 11/04/2018 15:13
 * @since JDK 11
 */
@Configuration
@EnableResourceServer
class ResourceServerAutoConfiguration @Autowired constructor(
    private val acpOauthConfiguration: AcpOauthConfiguration,
    private val entryPointMap: Map<String, AuthenticationEntryPoint>,
    private val accessDeniedHandlerMap: Map<String, AccessDeniedHandler>,
    serverProperties: ServerProperties
) : ResourceServerConfigurerAdapter() {

    private val log = LogFactory.getInstance(this.javaClass)

    private val contextPath: String =
        if (CommonTools.isNullStr(serverProperties.servlet.contextPath)) "" else serverProperties.servlet.contextPath

    /**
     * 设置 token 验证服务
     * 设置自定义异常处理
     *
     * @param resources 资源服务安全验证配置对象
     */
    override fun configure(resources: ResourceServerSecurityConfigurer) {
        // 自定义 token 异常处理
        if (entryPointMap.isNotEmpty()) {
            if (entryPointMap.size > 1) {
                if (!CommonTools.isNullStr(acpOauthConfiguration.authExceptionEntryPoint)) {
                    resources.authenticationEntryPoint(entryPointMap[acpOauthConfiguration.authExceptionEntryPoint])
                } else {
                    log.warn("Find more than one authenticationEntryPoint, please specify explicitly in the configuration 'acp.cloud.auth.auth-exception-entry-point'")
                }
            } else {
                resources.authenticationEntryPoint(entryPointMap.entries.iterator().next().value)
            }
        }
        // 自定义权限异常处理
        if (accessDeniedHandlerMap.isNotEmpty()) {
            if (accessDeniedHandlerMap.size > 1) {
                if (!CommonTools.isNullStr(acpOauthConfiguration.accessDeniedHandler)) {
                    resources.accessDeniedHandler(accessDeniedHandlerMap[acpOauthConfiguration.accessDeniedHandler])
                } else {
                    log.warn("Find more than one accessDeniedHandler, please specify explicitly in the configuration 'acp.cloud.auth.access-denied-handler'")
                }
            } else {
                resources.accessDeniedHandler(accessDeniedHandlerMap.entries.iterator().next().value)
            }
        }
    }

    /**
     * http 验证策略配置
     *
     * @param http http 安全验证对象
     * @throws Exception 异常
     */
    @Throws(Exception::class)
    override fun configure(http: HttpSecurity) {
        val permitAll = ArrayList<String>()
        val security = ArrayList<String>()
        permitAll.add("$contextPath/error")
        permitAll.add("$contextPath/favicon.ico")
        permitAll.add("$contextPath/actuator")
        permitAll.add("$contextPath/actuator/**")
        permitAll.add("$contextPath/v2/api-docs")
        permitAll.add("$contextPath/v3/api-docs")
        permitAll.add("$contextPath/configuration/ui")
        permitAll.add("$contextPath/swagger-resources/**")
        permitAll.add("$contextPath/configuration/security")
        permitAll.add("$contextPath/swagger-ui.html")
        permitAll.add("$contextPath/doc.html")
        permitAll.add("$contextPath/webjars/**")
        permitAll.add("$contextPath/swagger-resources/configuration/ui")
        permitAll.add("$contextPath/oauth/authorize")
        permitAll.add("$contextPath/oauth/token")
        permitAll.add("$contextPath/oauth/error")
        acpOauthConfiguration.resourceServerPermitAllPath.forEach { path -> permitAll.add(contextPath + path) }
        acpOauthConfiguration.resourceServerSecurityPath.forEach { path -> security.add(contextPath + path) }
        permitAll.add(contextPath + RestPrefix.Open + "/**")
        permitAll.forEach { uri -> log.info("permitAll uri: $uri") }
        security.forEach { uri -> log.info("security uri: $uri") }
        log.info("security uri: other any")
        // match 匹配的url，赋予全部权限（不进行拦截）
        http.csrf().disable().authorizeRequests().antMatchers(*security.toTypedArray()).authenticated()
            .antMatchers(*permitAll.toTypedArray()).permitAll().anyRequest().authenticated()
    }

}

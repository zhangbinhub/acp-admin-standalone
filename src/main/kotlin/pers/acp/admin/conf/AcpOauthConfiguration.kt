package pers.acp.admin.conf

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

/**
 * 鉴权相关配置
 *
 * @author zhang by 14/01/2019 17:30
 * @since JDK 11
 */
@Configuration(proxyBeanMethods = false)
@ConfigurationProperties(prefix = "acp.oauth")
class AcpOauthConfiguration {
    /**
     * 自定义异常处理的 Bean Name
     */
    var authExceptionEntryPoint: String? = null

    /**
     * 自定义权限处理 Bean Name
     */
    var accessDeniedHandler: String? = null

    /**
     * 不进行权限校验的 url path，当 resourceServer=true 时有效
     */
    var resourceServerPermitAllPath: MutableList<String> = mutableListOf()

    /**
     * 进行权限保护的 url path，当 resourceServer=true 时有效
     */
    var resourceServerSecurityPath: MutableList<String> = mutableListOf()

}

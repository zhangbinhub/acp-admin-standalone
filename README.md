# acp-admin-standalone

###### v1.0.0 [版本更新日志](doc/version_history.md)

- 使用Application Construction Platform 应用构建平台作为脚手架
- 基于 Spring Boot 的单机版，基于 Spring Cloud 版本请查看[这里](https://github.com/zhangbinhub/acp-admin-cloud)
- 该项目是前后端分离架构中的“后端部分”。前端工程[v1.0.0](https://github.com/zhangbinhub/acp-admin-standalone-web)

## 相关组件版本

- [Spring Boot 2.6.2](https://projects.spring.io/spring-boot)
- [Acp 2021.0.0](https://github.com/zhangbinhub/acp)

## 技术栈

- joda-time
- okhttp
- netty
- xstream
- hibernate
- jackson
- poi
- swagger2
- junit5

## 一、环境要求

- jdk 11
- gradle 6.5+
- kotlin 1.5+

## 二、gradle 脚本配置及使用

### （一）配置文件

##### 1.[gradle/dependencies.gradle](gradle/dependencies.gradle)

定义外部依赖版本号

##### 2.[gradle/environment.gradle](gradle/environment.gradle)

编译时定义的环境变量

##### 3.[settings.gradle](settings.gradle)

定义项目/模块结构

##### 4.[project.properties](project.properties)

gradle全局参数：

- gradleVersion：gradle版本号
- group：对应打包时的最外层groupid，最终的groupid还会加上模块路径，例如`groupid.acp.core`
- version：版本号
- encoding：编译字符集
- mavenCentralUrl：maven中央仓库地址
- javaVersion：jdk版本号

##### 5.[build.gradle](build.gradle)

公共构建脚本

### （二）自定义任务

- clearPj 清理所有输出文件
- release 编译、打包并输出
    - 如需编译打包对应环境，命令中使用参数 active，例如
  ```
  gradlew project:release -Pactive=test
  ```

### （三）升级命令

```
    gradlew wrapper --gradle-version=7.4 --distribution-type=all
```

## 三、工程说明

- 工程全局默认使用 UTF-8 字符集
- gradle 目录下为相关配置文件
- swagger url : /doc.html

## 四、启停 SpringBoot 应用

- [jvm 参考参数](doc/jvm-params.txt)
- [启停脚本模板(Linux)](doc/script/server.model)，根据实际情况修改第2行 APP_NAME 和第3行 JVM_PARAM 的值即可，和 SpringBoot 应用的 .jar 放在同一路径下
- [启停脚本(windows)](doc/script/server.bat)，根据实际情况修改第1行末尾需要执行的 jar 名称，和SpringBoot应用的 .jar 放在同一路径下
- windows：修改[server.bat](doc/script/server.bat)内相关参数后，直接运行即可
- Linux 命令：

| 命令                  | 描述       |
|---------------------|----------|
| ./server.sh         | 查看可用参数   |
| ./server.sh status  | 查看系统运行状态 |
| ./server.sh start   | 启动应用     |
| ./server.sh stop    | 停止应用     |
| ./server.sh restart | 重启应用     |

## 五、说明

- 统一认证服务，集成 oauth2
- 提供全套权限体系服务，包含客户端应用管理、用户管理、机构管理、角色管理、权限管理、token管理、运行参数管理等
- 统一认证服务：token 存储于 Redis，user 及 client 信息可扩展配置

| url                   | 描述                                                   |
|-----------------------|------------------------------------------------------|
| /oauth/authorize      | 申请授权，basic认证保护                                       |
| /oauth/token          | 获取token的服务，url中没有client_id和client_secret的，走basic认证保护 |
| /oauth/check_token    | 资源服务器用来校验token，basic认证保护                             |
| /oauth/confirm_access | 授权确认，basic认证保护                                       |
| /oauth/error          | 认证失败，无认证保护                                           |

[查看认证过程](doc/oauth2.0认证.md)

##### （一）数据初始化

执行 oauth-server 模块下的 pers.acp.admin.oauth.nobuild.InitData.doInitAll() 单元测试

##### （二）接口功能

提供生成token、验证token、应用管理、机构管理、参数管理、角色管理、用户管理、菜单管理、权限功能管理等接口，接口详情请在浏览器中访问 /doc.html 页面

##### （三）运行参数

| 名称                            | 值          | 描述                                                                    | 备注                     |
|-------------------------------|------------|-----------------------------------------------------------------------|------------------------|
| PASSWORD_COMPLEXITY_POLICY    | 0          | 密码复杂度策略；0：不限制，1：数字+字母，2：数字+字母+英文特殊符号`~!@#$%^&*()+=&#124;{}':;,\"[].<> | 默认0                    |
| PASSWORD_UPDATE_INTERVAL_TIME | 7776000000 | 修改密码间隔时间，单位：毫秒                                                        | 密码过期之后，会要求强制修改密码；默认90天 |

##### （四）自定义认证方式

- 1、新建
  AuthenticationToken，参考参考[UserPasswordAuthenticationToken](src/main/kotlin/pers/acp/admin/token/UserPasswordAuthenticationToken.kt)
- 2、新建认证 AuthenticationProvider，并在 WebSecurityConfiguration
  中进行配置，参考[UserPasswordAuthenticationProvider](src/main/kotlin/pers/acp/admin/token/granter/UserPasswordAuthenticationProvider.kt)
- 3、新建发布器 UserPasswordTokenGranter，设置自定义grantType，并在 AuthorizationServerConfiguration.getDefaultTokenGranters
  方法中进行配置，参考[UserPasswordTokenGranter](src/main/kotlin/pers/acp/admin/token/granter/UserPasswordTokenGranter.kt)
- 4、[SecurityClientDetailsService](src/main/kotlin/pers/acp/admin/security/SecurityClientDetailsService.kt)
  中将自定义grantType加入client中

## 六、环境变量及启动参数

| 变量名                | 描述      | 默认值                                                                                                                     | 说明                                                                                                                                          |
|--------------------|---------|-------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------|
| acp_profile_active | 激活的配置环境 | dev                                                                                                                     | 服务器部署时建议java启动命令加入参数 -Dacp_profile_active 或 --acp_profile_active；容器部署时指定环境变量即可                                                              |
| acp_server_port    | 服务启动端口  | 0（随机端口）                                                                                                                 | 服务器部署时建议java启动命令加入参数 -Dacp_server_port 或 --acp_server_port；容器部署时指定环境变量即可。服务不需要外部直接访问时，建议保持默认值。注：admin-server默认值：9099，gateway-server默认值：8771 |
| acp_log_path       | 日志路径    | logs/${spring.application.name}                                                                                         | 服务器部署时建议java启动命令加入参数 -Dacp_log_path 或 --acp_log_path；容器部署时指定环境变量即可                                                                          |
| acp_jvm_param      | JVM启动参数 | -server -XX:+UnlockExperimentalVMOptions -XX:+UseZGC -Xms256m -Xmx512m -Djava.library.path=./libs -Dfile.encoding=utf-8 | 该环境变量在容器部署时使用                                                                                                                               |

## 七、JVM启动参数

```shell
-server -XX:+UnlockExperimentalVMOptions -XX:+UseZGC -Xms256m -Xmx512m -Djava.library.path=./libs -Dfile.encoding=utf-8
```

## 八、打包OCI镜像

```
bootBuildImage {
    docker {
        host = "tcp://localhost:2375"
        tlsVerify = false
    }
    imageName = "${group}/${project.name}:${version}"
    environment = [
            "BP_JVM_VERSION"              : "11.*",
            "BPE_APPEND_JAVA_TOOL_OPTIONS": " -XX:+UnlockExperimentalVMOptions -XX:+UseZGC -Dfile.encoding=utf-8"
    ]
}
```
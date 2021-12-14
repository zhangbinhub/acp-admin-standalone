package pers.acp.admin.vo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

/**
 * @author zhangbin by 2018-1-17 16:53
 * @since JDK 11
 */
@ApiModel("角色详细信息")
data class RoleVo(

        @ApiModelProperty("角色ID")
        var id: String? = null,

        @ApiModelProperty(value = "应用ID", position = 1)
        var appId: String? = null,

        @ApiModelProperty(value = "角色名称", position = 2)
        var name: String? = null,

        @ApiModelProperty(value = "角色编码", position = 3)
        var code: String? = null,

        @ApiModelProperty(value = "角色级别", position = 4)
        var levels: Int = 1,

        @ApiModelProperty(value = "序号", position = 5)
        var sort: Int = 0,

        @ApiModelProperty(value = "关联用户ID", position = 5)
        var userIds: MutableList<String> = mutableListOf(),

        @ApiModelProperty(value = "关联菜单ID", position = 6)
        var menuIds: MutableList<String> = mutableListOf(),

        @ApiModelProperty(value = "关联模块功能ID", position = 7)
        var moduleFuncIds: MutableList<String> = mutableListOf()

)

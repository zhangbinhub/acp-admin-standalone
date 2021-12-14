package pers.acp.admin.repo

import pers.acp.admin.base.BaseRepository
import pers.acp.admin.entity.Role

/**
 * @author zhangbin by 2018-1-17 17:48
 * @since JDK 11
 */
interface RoleRepository : BaseRepository<Role, String> {

    fun findAllByCodeInOrderBySortAsc(code: List<String>): MutableList<Role>

    fun findAllByOrderBySortAsc(): MutableList<Role>

    fun findByAppIdOrderBySortAsc(appId: String): MutableList<Role>

    fun findByAppIdAndLevelsGreaterThanOrderBySortAsc(appId: String, level: Int): MutableList<Role>

    fun deleteByIdIn(idList: MutableList<String>)

}

package pers.acp.admin.repo

import pers.acp.admin.base.BaseRepository
import pers.acp.admin.entity.Menu

/**
 * @author zhangbin by 2018-1-17 17:46
 * @since JDK 11
 */
interface MenuRepository : BaseRepository<Menu, String> {

    fun findAllByOrderBySortAsc(): MutableList<Menu>

    fun findByAppId(appId: String): MutableList<Menu>

    fun findByParentIdIn(idList: MutableList<String>): MutableList<Menu>

    fun deleteByIdInAndCovert(idList: MutableList<String>, covert: Boolean)

}

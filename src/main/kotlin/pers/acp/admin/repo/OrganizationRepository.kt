package pers.acp.admin.repo

import pers.acp.admin.base.BaseRepository
import pers.acp.admin.entity.Organization

/**
 * @author zhangbin by 2018-1-17 17:45
 * @since JDK 11
 */
interface OrganizationRepository : BaseRepository<Organization, String> {

    fun findAllByOrderBySortAsc(): MutableList<Organization>

    fun findByParentIdIn(idList: MutableList<String>): MutableList<Organization>

    fun findAllByCodeLikeOrNameLikeOrderBySortAsc(code: String, name: String): MutableList<Organization>

    fun deleteByIdIn(idList: MutableList<String>)

}

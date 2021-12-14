package pers.acp.admin.repo

import pers.acp.admin.base.BaseRepository
import pers.acp.admin.entity.RuntimeConfig
import java.util.Optional

/**
 * @author zhangbin by 2018-1-16 23:46
 * @since JDK 11
 */
interface RuntimeConfigRepository : BaseRepository<RuntimeConfig, String> {

    fun findByName(name: String): Optional<RuntimeConfig>

    fun deleteByIdInAndCovert(idList: MutableList<String>, covert: Boolean)

}

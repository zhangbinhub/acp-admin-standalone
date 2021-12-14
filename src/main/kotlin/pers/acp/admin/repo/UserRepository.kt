package pers.acp.admin.repo

import pers.acp.admin.base.BaseRepository
import pers.acp.admin.entity.User
import java.util.Optional

/**
 * @author zhangbin by 2018-1-17 17:48
 * @since JDK 11
 */
interface UserRepository : BaseRepository<User, String> {

    fun findByLoginNo(loginNo: String): Optional<User>

    fun findByMobile(mobile: String): Optional<User>

    fun findByLoginNoAndIdNot(loginNo: String, userId: String): Optional<User>

    fun findByMobileAndIdNot(mobile: String, userId: String): Optional<User>

    fun findByLevelsGreaterThan(currLevels: Int): MutableList<User>

    fun findByLoginNoLikeOrNameLikeOrderByLoginNoAsc(loginNo: String, name: String): MutableList<User>

    fun deleteByIdIn(idList: MutableList<String>)

}

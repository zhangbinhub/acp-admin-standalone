package pers.acp.admin.domain

import io.github.zhangbinhub.acp.boot.exceptions.ServerException
import io.github.zhangbinhub.acp.core.CommonTools
import org.springframework.beans.BeanUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Page
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import pers.acp.admin.base.OauthBaseDomain
import pers.acp.admin.component.UserPasswordEncrypt
import pers.acp.admin.entity.Organization
import pers.acp.admin.entity.Role
import pers.acp.admin.entity.User
import pers.acp.admin.po.UserPo
import pers.acp.admin.po.UserQueryPo
import pers.acp.admin.repo.ApplicationRepository
import pers.acp.admin.repo.OrganizationRepository
import pers.acp.admin.repo.RoleRepository
import pers.acp.admin.repo.UserRepository
import pers.acp.admin.token.SecurityTokenService
import pers.acp.admin.vo.OrganizationVo
import pers.acp.admin.vo.RoleVo
import pers.acp.admin.vo.UserVo
import javax.persistence.criteria.JoinType
import javax.persistence.criteria.Predicate

/**
 * @author zhang by 19/12/2018
 * @since JDK 11
 */
@Service
@Transactional(readOnly = true)
class UserDomain @Autowired
constructor(
    userRepository: UserRepository,
    private val userPasswordEncrypt: UserPasswordEncrypt,
    private val applicationRepository: ApplicationRepository,
    private val organizationRepository: OrganizationRepository,
    private val roleRepository: RoleRepository,
    private val securityTokenService: SecurityTokenService
) : OauthBaseDomain(userRepository) {

    @Throws(ServerException::class)
    private fun formatUserVo(user: User) = UserVo().apply {
        BeanUtils.copyProperties(user, this)
        this.organizationSet = user.organizationSet.map { org ->
            OrganizationVo().apply { BeanUtils.copyProperties(org, this) }
        }.toMutableSet()
        this.organizationMngSet = user.organizationMngSet.map { org ->
            OrganizationVo().apply { BeanUtils.copyProperties(org, this) }
        }.toMutableSet()
        this.roleSet = user.roleSet.map { role ->
            RoleVo().apply { BeanUtils.copyProperties(role, this) }
        }.toMutableSet()
    }

    @Throws(ServerException::class)
    private fun validatePermit(loginNo: String, userPo: UserPo, roleSet: Set<Role>, isCreate: Boolean) {
        val currUserInfo = getUserInfoByLoginNo(loginNo) ?: throw ServerException("??????????????????????????????")
        if (!isSuper(currUserInfo)) {
            if (currUserInfo.levels >= userPo.levels!!) {
                throw ServerException("?????????????????????????????????????????????")
            }
            getAllOrgList(organizationRepository, currUserInfo.organizationMngSet.toMutableList()).map { it.id }.let {
                userPo.orgIds.forEach { orgId ->
                    if (!it.contains(orgId)) {
                        throw ServerException("?????????????????????????????????????????????????????????????????????")
                    }
                }
                userPo.orgMngIds.forEach { orgId ->
                    if (!it.contains(orgId)) {
                        throw ServerException("?????????????????????????????????????????????????????????????????????")
                    }
                }
            }
            val roleMinLevel = getRoleMinLevel(currUserInfo)
            roleSet.forEach {
                if (!roleMinLevel.containsKey(it.appId) || roleMinLevel.getValue(it.appId) >= it.levels) {
                    throw ServerException("???????????????????????????${it.name}??????????????????????????????")
                }
            }
        } else {
            if (isCreate) {
                if (currUserInfo.levels >= userPo.levels!!) {
                    throw ServerException("???????????????????????????????????????")
                }
            }
        }
    }

    private fun doSave(user: User, userPo: UserPo): User =
        doSaveUser(
            user.copy(
                mobile = userPo.mobile!!,
                name = userPo.name!!,
                enabled = userPo.enabled!!,
                levels = userPo.levels!!,
                sort = userPo.sort,
                organizationSet = organizationRepository.findAllById(userPo.orgIds).toMutableSet(),
                organizationMngSet = organizationRepository.findAllById(userPo.orgMngIds).toMutableSet()
            )
        )

    @Transactional
    fun doSaveUser(user: User): User = userRepository.save(user)

    fun getMobileForOtherUser(mobile: String, userId: String): User? =
        userRepository.findByMobileAndIdNot(mobile, userId).orElse(null)

    @Throws(ServerException::class)
    fun findModifiableUserList(loginNo: String): MutableList<UserVo> {
        val user = getUserInfoByLoginNo(loginNo) ?: throw ServerException("??????????????????????????????")
        return if (isSuper(user)) {
            userRepository.findAll().map { item -> formatUserVo(item) }
                .toMutableList()
        } else {
            user.let {
                userRepository.findByLevelsGreaterThan(it.levels)
                    .map { item -> formatUserVo(item) }.toMutableList()
            }
        }
    }

    @Transactional
    @Throws(ServerException::class)
    fun doCreate(loginNo: String, userPo: UserPo): User {
        val roleSet = roleRepository.findAllById(userPo.roleIds).toMutableSet()
        validatePermit(loginNo, userPo, roleSet, true)
        var checkUser = userRepository.findByLoginNo(userPo.loginNo!!).orElse(null)
        if (checkUser != null) {
            throw ServerException("???????????????????????????????????????")
        }
        checkUser = userRepository.findByMobile(userPo.mobile!!).orElse(null)
        if (checkUser != null) {
            throw ServerException("???????????????????????????????????????")
        }
        return doSave(
            User(
                loginNo = userPo.loginNo!!,
                password = userPasswordEncrypt.encrypt(userPo.loginNo!!, DEFAULT_PASSWORD),
                roleSet = roleSet
            ), userPo
        )
    }

    @Transactional
    @Throws(ServerException::class)
    fun doUpdate(loginNo: String, userPo: UserPo): User {
        val roleSet = roleRepository.findAllById(userPo.roleIds).toMutableSet()
        validatePermit(loginNo, userPo, roleSet, false)
        return doSave(userRepository.getById(userPo.id!!).apply {
            var checkUser = userRepository.findByLoginNoAndIdNot(userPo.loginNo!!, this.id).orElse(null)
            if (checkUser != null) {
                throw ServerException("???????????????????????????????????????")
            }
            checkUser = userRepository.findByMobileAndIdNot(userPo.mobile!!, this.id).orElse(null)
            if (checkUser != null) {
                throw ServerException("???????????????????????????????????????")
            }
            if (this.loginNo != userPo.loginNo) {
                this.loginNo = userPo.loginNo!!
                this.password = userPasswordEncrypt.encrypt(userPo.loginNo!!, DEFAULT_PASSWORD)
                this.lastUpdatePasswordTime = null
                removeToken(userPo.loginNo!!)
            }
            this.roleSet = roleSet
        }, userPo)
    }

    @Transactional
    @Throws(ServerException::class)
    fun doUpdatePwd(loginNo: String, userId: String): User =
        userRepository.getById(userId).apply {
            (getUserInfoByLoginNo(loginNo) ?: throw ServerException("???????????????????????????")).let {
                if (!isSuper(it)) {
                    if (it.levels >= this.levels) {
                        throw ServerException("??????????????????????????????????????????????????????")
                    }
                }
                this.password = userPasswordEncrypt.encrypt(this.loginNo, DEFAULT_PASSWORD)
                this.lastUpdatePasswordTime = null
                userRepository.save(this)
                removeToken(this.loginNo)
            }
        }

    @Transactional
    @Throws(ServerException::class)
    fun doDelete(loginNo: String, idList: MutableList<String>) {
        val user = getUserInfoByLoginNo(loginNo) ?: throw ServerException("???????????????????????????")
        if (idList.contains(user.id)) {
            throw ServerException("??????????????????")
        }
        val userList = userRepository.findAllById(idList)
        if (!isSuper(user)) {
            userList.forEach {
                if (user.levels >= it.levels) {
                    throw ServerException("???????????????????????????????????????????????????")
                }
            }
        }
        userRepository.deleteByIdIn(idList)
        userList.forEach { item -> removeToken(item.loginNo) }
    }

    @Transactional
    @Throws(ServerException::class)
    fun disableUser(loginNo: String) = getUserInfoByLoginNo(loginNo, true)?.apply {
        this.enabled = false
    }?.apply {
        doSaveUser(this)
    } ?: throw ServerException("????????????????????????$loginNo???")

    private fun removeToken(loginNo: String) {
        applicationRepository.findAllByOrderByIdentifyAscAppNameAsc()
            .forEach { application -> securityTokenService.removeTokensByAppIdAndLoginNo(application.id, loginNo) }
    }

    fun doQuery(userQueryPo: UserQueryPo): Page<UserVo> =
        userRepository.findAll({ root, query, criteriaBuilder ->
            val predicateList = ArrayList<Predicate>()
            if (!CommonTools.isNullStr(userQueryPo.loginNo)) {
                predicateList.add(
                    criteriaBuilder.equal(
                        root.get<Any>("loginNo").`as`(String::class.java),
                        userQueryPo.loginNo
                    )
                )
            }
            if (!CommonTools.isNullStr(userQueryPo.name)) {
                predicateList.add(
                    criteriaBuilder.like(
                        root.get<Any>("name").`as`(String::class.java),
                        "%" + userQueryPo.name + "%"
                    )
                )
            }
            if (userQueryPo.enabled != null) {
                predicateList.add(criteriaBuilder.equal(root.get<Any>("enabled"), userQueryPo.enabled))
            }
            if (!CommonTools.isNullStr(userQueryPo.orgName)) {
                val subQuery = query.subquery(User::class.java)
                val subRoot = subQuery.from(User::class.java)
                val joinOrg = subRoot.join<User, Organization>("organizationSet", JoinType.LEFT)
                subQuery.select(subRoot.get("id")).where(
                    criteriaBuilder.like(
                        joinOrg.get<Any>("name").`as`(String::class.java),
                        "%" + userQueryPo.orgName + "%"
                    )
                )
                predicateList.add(root.get<Any>("id").`in`(subQuery))
            }
            if (!CommonTools.isNullStr(userQueryPo.roleName)) {
                val subQuery = query.subquery(User::class.java)
                val subRoot = subQuery.from(User::class.java)
                val joinRole = subRoot.join<User, Role>("roleSet", JoinType.LEFT)
                subQuery.select(subRoot.get("id")).where(
                    criteriaBuilder.like(
                        joinRole.get<Any>("name").`as`(String::class.java),
                        "%" + userQueryPo.roleName + "%"
                    )
                )
                predicateList.add(root.get<Any>("id").`in`(subQuery))
            }
            criteriaBuilder.and(*predicateList.toTypedArray())
        }, buildPageRequest(userQueryPo.queryParam!!))
            .map { user -> formatUserVo(user) }

    fun getUserInfoById(userId: String): User? = userRepository.findById(userId).orElse(null)

    @Throws(ServerException::class)
    fun getUserVoById(userId: String): UserVo = userRepository.findById(userId).let {
        if (it.isEmpty) throw ServerException("?????????????????????")
        formatUserVo(it.get())
    }

    @Throws(ServerException::class)
    fun getUserVoByLoginNo(loginNo: String): UserVo = userRepository.findByLoginNo(loginNo).let {
        if (it.isEmpty) throw ServerException("?????????????????????")
        formatUserVo(it.get())
    }

    /**
     * ??????ID??????????????????
     */
    @Throws(ServerException::class)
    fun getUserVoListByIdList(idList: MutableList<String>): MutableList<UserVo> =
        userRepository.findAllById(idList).map { item -> formatUserVo(item) }.toMutableList()

    /**
     * ??????????????????????????????????????????
     */
    @Throws(ServerException::class)
    fun getUserVoListByLoginNoOrName(loginNoOrName: String, findAll: Boolean): MutableList<UserVo> =
        userRepository.findByLoginNoLikeOrNameLikeOrderByLoginNoAsc("%$loginNoOrName%", "%$loginNoOrName%")
            .filter { item -> findAll || item.enabled }.map { item -> formatUserVo(item) }.toMutableList()

    /**
     * ??????????????????????????????????????????????????????
     */
    private fun getUserVoListInOrgListByRoleCode(
        organizations: Collection<Organization>,
        roleCode: List<String>
    ): MutableList<UserVo> =
        mutableListOf<UserVo>().apply {
            organizations.forEach { org ->
                this.addAll(org.userSet.filter { user -> user.roleSet.any { role -> roleCode.contains(role.code) } }
                    .map { item -> formatUserVo(item) }
                    .toMutableList())
            }
        }.let {
            getUserVoListDistinct(it)
        }

    /**
     * User?????????????????????List
     */
    private fun getUserVoListDistinct(users: Collection<UserVo>): MutableList<UserVo> =
        mutableListOf<UserVo>().apply {
            val userIdList = mutableListOf<String>()
            users.forEach { user ->
                if (!userIdList.contains(user.id)) {
                    this.add(user)
                    userIdList.add(user.id!!)
                }
            }
        }

    @Throws(ServerException::class)
    fun getUserVoListByCurrOrgAndRole(loginNo: String, roleCode: List<String>): MutableList<UserVo> =
        getUserInfoByLoginNo(loginNo)?.let { currUser ->
            getUserVoListInOrgListByRoleCode(currUser.organizationSet, roleCode)
        } ?: throw ServerException("??????????????????????????????")

    /**
     * ???????????????????????????????????????
     * @param orgLevelList >0 ???????????????1????????????2?????????...???=0???????????????<0 ???????????????-1????????????-2?????????...
     */
    @Throws(ServerException::class)
    fun getUserVoListByRelativeOrgAndRole(
        loginNo: String,
        orgLevelList: List<Int>,
        roleCode: List<String>
    ): MutableList<UserVo> =
        getUserInfoByLoginNo(loginNo)?.let { currUser ->
            val orgList = mutableListOf<Organization>()
            orgLevelList.forEach { orgLevel ->
                when {
                    orgLevel > 0 -> { // ????????????
                        val tmpOrg = currUser.organizationSet.toMutableList()
                        for (index in 1..orgLevel) {
                            val children = getRelativeOrgList(index, tmpOrg)
                            if (children.isNotEmpty()) {
                                tmpOrg.clear()
                                tmpOrg.addAll(children)
                            } else {
                                tmpOrg.clear()
                                break
                            }
                        }
                        orgList.addAll(tmpOrg)
                    }
                    orgLevel < 0 -> { // ????????????
                        val tmpOrg = currUser.organizationSet.toMutableList()
                        for (index in orgLevel until 0) {
                            val parent = getRelativeOrgList(index, tmpOrg)
                            if (parent.isNotEmpty()) {
                                tmpOrg.clear()
                                tmpOrg.addAll(parent)
                            } else {
                                tmpOrg.clear()
                                break
                            }
                        }
                        orgList.addAll(tmpOrg)
                    }
                    else -> { // ?????????
                        orgList.addAll(currUser.organizationSet)
                    }
                }
            }
            getUserVoListInOrgListByRoleCode(orgList, roleCode)
        } ?: throw ServerException("??????????????????????????????")

    fun getUserVoListByOrgCodeAndRole(orgCode: List<String>, roleCode: List<String>): MutableList<UserVo> =
        getUserVoListInOrgListByRoleCode(
            organizationRepository.findAllByCodeLikeOrNameLikeOrderBySortAsc(
                "%$orgCode%",
                "%$orgCode%"
            ), roleCode
        )

    fun getUserVoListByRole(roleCode: List<String>): MutableList<UserVo> =
        getUserVoListDistinct(roleRepository.findAllByCodeInOrderBySortAsc(roleCode)
            .flatMap { role -> role.userSet }
            .map { item -> formatUserVo(item) }
            .toMutableList())

    /**
     * ????????????????????????
     * @param flag ?????????>0?????????<0??????
     * @param orgList ??????????????????
     * @return ??????????????????
     */
    private fun getRelativeOrgList(flag: Int, orgList: Collection<Organization>): Collection<Organization> = when {
        flag > 0 -> { // ????????????
            organizationRepository.findByParentIdIn(orgList.map { org -> org.id }.toMutableList())
        }
        flag < 0 -> { // ????????????
            mutableListOf<Organization>().apply {
                orgList.forEach { org ->
                    val parent = organizationRepository.findById(org.parentId)
                    if (parent.isPresent) {
                        this.add(parent.get())
                    }
                }
            }
        }
        else -> {
            orgList
        }
    }

    companion object {
        private const val DEFAULT_PASSWORD = "000000"
    }

}

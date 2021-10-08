package com.mikuac.yuri.repository

import com.mikuac.yuri.entity.GroupBlackListEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Component
import java.util.*

@Component
interface GroupBlackListRepository : JpaRepository<GroupBlackListEntity, Int> {

    fun findByGroupId(groupId: Long): Optional<GroupBlackListEntity>

}
package com.mikuac.yuri.repository

import com.mikuac.yuri.entity.DriftBottleEntity
import jakarta.transaction.Transactional
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Component

@Component
interface DriftBottleRepository : JpaRepository<DriftBottleEntity, Int> {

    fun findAllByOpenIsFalseAndUserIdNotLikeAndGroupIdNotLike(userId: Long, groupId: Long): List<DriftBottleEntity>

    fun countAllByOpenIsFalse(): Int

    @Transactional
    override fun <S : DriftBottleEntity> save(entity: S): S {
        TODO("Not yet implemented")
    }

}
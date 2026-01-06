package com.gartenplan.pro.data.repository

import com.gartenplan.pro.data.local.dao.CompostDao
import com.gartenplan.pro.data.mapper.*
import com.gartenplan.pro.domain.model.Compost
import com.gartenplan.pro.domain.model.CompostEntry
import com.gartenplan.pro.domain.model.CompostStatus
import com.gartenplan.pro.domain.repository.CompostRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CompostRepositoryImpl @Inject constructor(
    private val compostDao: CompostDao
) : CompostRepository {

    override fun getAllComposts(): Flow<List<Compost>> {
        return compostDao.getAllCompostsWithEntries().map { list ->
            list.map { compostWithEntries ->
                val entries = compostWithEntries.entries.map { it.toDomain() }
                val greenCount = entries.count { it.isGreen }
                val brownCount = entries.count { !it.isGreen }
                val total = greenCount + brownCount
                val greenRatio = if (total > 0) greenCount.toFloat() / total else 0f
                val brownRatio = if (total > 0) brownCount.toFloat() / total else 0f
                
                compostWithEntries.compost.toDomain(
                    entries = entries,
                    greenRatio = greenRatio,
                    brownRatio = brownRatio
                )
            }
        }
    }

    override fun getActiveComposts(): Flow<List<Compost>> {
        return compostDao.getActiveComposts().map { composts ->
            composts.map { it.toDomain() }
        }
    }

    override fun observeCompostById(id: String): Flow<Compost?> {
        return compostDao.getCompostWithEntries(id).map { compostWithEntries ->
            compostWithEntries?.let {
                val entries = it.entries.map { entry -> entry.toDomain() }
                val greenCount = entries.count { e -> e.isGreen }
                val brownCount = entries.count { e -> !e.isGreen }
                val total = greenCount + brownCount
                val greenRatio = if (total > 0) greenCount.toFloat() / total else 0f
                val brownRatio = if (total > 0) brownCount.toFloat() / total else 0f
                
                it.compost.toDomain(
                    entries = entries,
                    greenRatio = greenRatio,
                    brownRatio = brownRatio
                )
            }
        }
    }

    override fun getEntriesByCompost(compostId: String): Flow<List<CompostEntry>> {
        return compostDao.getEntriesByCompost(compostId).map { entries ->
            entries.map { it.toDomain() }
        }
    }

    override suspend fun getCompostById(id: String): Compost? {
        return compostDao.getCompostById(id)?.toDomain()
    }

    override suspend fun createCompost(compost: Compost): String {
        compostDao.insertCompost(compost.toEntity())
        return compost.id
    }

    override suspend fun updateCompost(compost: Compost) {
        compostDao.updateCompost(compost.toEntity())
    }

    override suspend fun deleteCompost(compostId: String) {
        compostDao.deleteCompostById(compostId)
    }

    override suspend fun updateCompostStatus(compostId: String, status: CompostStatus) {
        compostDao.updateCompostStatus(compostId, status.toEntityCompostStatus())
    }

    override suspend fun addEntry(entry: CompostEntry): String {
        compostDao.insertEntry(entry.toEntity())
        return entry.id
    }

    override suspend fun deleteEntry(entryId: String) {
        compostDao.deleteEntryById(entryId)
    }

    override suspend fun getGreenBrownRatio(compostId: String): Pair<Float, Float> {
        val greenLiters = compostDao.getTotalGreenLiters(compostId)
        val brownLiters = compostDao.getTotalBrownLiters(compostId)
        val total = greenLiters + brownLiters
        
        return if (total > 0) {
            Pair(greenLiters / total, brownLiters / total)
        } else {
            Pair(0f, 0f)
        }
    }
}

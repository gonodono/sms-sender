package com.gonodono.smssender.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Entity(tableName = "send_tasks")
class SendTask(
    @PrimaryKey
    val id: UUID,
    var state: State = State.Running,
    var error: String? = null
) {
    enum class State { Running, Succeeded, Failed, Cancelled }
}

@Dao
interface SendTaskDao {

    @get:Query("SELECT * FROM send_tasks ORDER BY rowid DESC LIMIT 1")
    val latestSendTask: Flow<SendTask?>

    @Insert
    suspend fun insert(task: SendTask)

    @Update
    suspend fun update(task: SendTask)

    @Query("SELECT state FROM send_tasks WHERE id=:id")
    suspend fun getTaskState(id: UUID): SendTask.State?

    @Query("UPDATE send_tasks SET state='Cancelled' WHERE id=:id")
    suspend fun setTaskCancelled(id: UUID): Int
}
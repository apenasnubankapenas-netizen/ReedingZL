package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.SegmentChatMessage
import kotlinx.coroutines.flow.Flow

@Dao
interface SegmentChatMessageDao {
    @Query("""
        SELECT * FROM segment_chat_messages 
        WHERE bookId = :bookId AND pageNumber = :pageNumber AND segmentIndex = :segmentIndex 
        ORDER BY timestamp ASC
    """)
    fun getMessagesForSegment(bookId: Int, pageNumber: Int, segmentIndex: Int): Flow<List<SegmentChatMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: SegmentChatMessage): Long

    @Query("""
        DELETE FROM segment_chat_messages 
        WHERE bookId = :bookId AND pageNumber = :pageNumber AND segmentIndex = :segmentIndex
    """)
    suspend fun clearMessagesForSegment(bookId: Int, pageNumber: Int, segmentIndex: Int)

    @Delete
    suspend fun deleteMessage(message: SegmentChatMessage)

    @Query("""
        SELECT COUNT(*) FROM segment_chat_messages 
        WHERE bookId = :bookId AND pageNumber = :pageNumber AND segmentIndex = :segmentIndex
    """)
    fun getMessageCountForSegment(bookId: Int, pageNumber: Int, segmentIndex: Int): Flow<Int>

    @Query("SELECT * FROM segment_chat_messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<SegmentChatMessage>>
}

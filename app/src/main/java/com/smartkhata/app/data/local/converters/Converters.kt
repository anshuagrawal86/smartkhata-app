package com.smartkhata.app.data.local.converters

import androidx.room.TypeConverter
import com.smartkhata.app.data.model.MediaType
import com.smartkhata.app.data.model.TransactionType

class Converters {
    @TypeConverter
    fun fromTransactionType(value: TransactionType): String = value.name

    @TypeConverter
    fun toTransactionType(value: String): TransactionType {
        return runCatching { TransactionType.valueOf(value) }.getOrDefault(TransactionType.NOTE)
    }

    @TypeConverter
    fun fromMediaType(value: MediaType): String = value.name

    @TypeConverter
    fun toMediaType(value: String): MediaType {
        return runCatching { MediaType.valueOf(value) }.getOrDefault(MediaType.TEXT)
    }
}

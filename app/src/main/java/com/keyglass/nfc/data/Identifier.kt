package com.keyglass.nfc.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A single account identifier, e.g. "EM : email".
 * [code] is the short 2-char label (EM), [account] is the human name (email).
 */
@Entity(tableName = "identifiers")
data class Identifier(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val code: String,
    val account: String,
    val position: Int = 0
)

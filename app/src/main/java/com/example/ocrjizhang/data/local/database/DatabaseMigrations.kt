package com.example.ocrjizhang.data.local.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseMigrations {
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `accounts` (
                    `id` INTEGER NOT NULL,
                    `userId` INTEGER NOT NULL,
                    `name` TEXT NOT NULL,
                    `symbol` TEXT NOT NULL,
                    `balanceFen` INTEGER NOT NULL,
                    `isDefault` INTEGER NOT NULL,
                    `createdAt` INTEGER NOT NULL,
                    `updatedAt` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent(),
            )
        }
    }
}

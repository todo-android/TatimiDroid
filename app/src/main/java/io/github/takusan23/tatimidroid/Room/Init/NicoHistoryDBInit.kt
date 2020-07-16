package io.github.takusan23.tatimidroid.Room.Init

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import io.github.takusan23.tatimidroid.Room.Database.NicoHistoryDB

/**
 * 端末内履歴データベース初期化（いやデータベース吹っ飛ばすって意味じゃなくて初期設定的な）
 * */
class NicoHistoryDBInit(context: Context) {
    val nicoHistoryDB = Room.databaseBuilder(context, NicoHistoryDB::class.java, "NicoHistory.db")
        .addMigrations(object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // SQLite->Room移行。移行後のデータベースを作成する。カラムは移行前と同じ
                database.execSQL(
                    """
                    CREATE TABLE history_tmp (
                    _id INTEGER NOT NULL PRIMARY KEY, 
                    type TEXT NOT NULL,
                    service_id TEXT NOT NULL,
                    user_id TEXT NOT NULL,
                    title TEXT NOT NULL,
                    date INTEGER NOT NULL,
                    description TEXT NOT NULL
                    )
                    """
                )
                // 移行後のデータベースへデータを移す
                database.execSQL(
                    """
                    INSERT INTO history_tmp (_id, type, service_id, user_id, title, date, description)
                    SELECT _id, type, service_id, user_id, title, date, description FROM history
                    """
                )
                // 前あったデータベースを消す
                database.execSQL("DROP TABLE history")
                // 移行後のデータベースの名前を移行前と同じにして移行完了
                database.execSQL("ALTER TABLE history_tmp RENAME TO history")
            }
        }).build()
}
package io.github.takusan23.tatimidroid.Room.Init

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import io.github.takusan23.tatimidroid.Room.Database.NGDB

/**
 * [NGDB]（NGユーザー/コメントデータベース）を生成するだけのクラスです。
 * Migrationsのせいで初期化が長いのでクラスに切り出した。
 * @param context
 * */
class NGDBInit(context: Context) {
    // データベース用意
    val ngDataBase = Room.databaseBuilder(context, NGDB::class.java, "NGList.db")
        .addMigrations(object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // SQLiteから移行
                database.execSQL(
                    """
                    CREATE TABLE ng_list_tmp(
                      _id INTEGER NOT NULL PRIMARY KEY,
                      type TEXT NOT NULL,
                      value TEXT NOT NULL,
                      description TEXT NOT NULL
                    )
                """
                )
                // データを移す
                database.execSQL(
                    """
                   INSERT INTO  ng_list_tmp(_id, type, value, description)
                   SELECT _id, type, value, description FROM ng_list
                """
                )
                // 前あったデータベースを消す
                database.execSQL("DROP TABLE ng_list")
                // 移行後のデータベースの名前を移行前と同じにして移行完了
                database.execSQL("ALTER TABLE ng_list_tmp RENAME TO ng_list")
            }
        }).build()
}
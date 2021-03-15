package io.github.takusan23.tatimidroid.Room.Init

import android.content.Context
import androidx.room.Room
import io.github.takusan23.tatimidroid.Room.Database.NGUploaderUserIdDB

/**
 * NG投稿者IDデータベースをインスタンス化する
 * */
object NGUploaderUserIdDBInit {

    /** NG投稿者 */
    private lateinit var ngUploaderUserIdDB: NGUploaderUserIdDB

    /** しんぐるとん */
    fun getInstance(context: Context): NGUploaderUserIdDB {
        if (!::ngUploaderUserIdDB.isInitialized) {
            ngUploaderUserIdDB = Room.databaseBuilder(context, NGUploaderUserIdDB::class.java, "NGUploaderUserId.db").build()
        }
        return ngUploaderUserIdDB
    }

}
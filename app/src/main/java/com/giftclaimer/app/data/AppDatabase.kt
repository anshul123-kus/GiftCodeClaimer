package com.giftclaimer.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.giftclaimer.app.data.dao.ClaimLogDao
import com.giftclaimer.app.data.dao.ProfileDao
import com.giftclaimer.app.data.dao.SiteDao
import com.giftclaimer.app.data.models.ClaimLog
import com.giftclaimer.app.data.models.Profile
import com.giftclaimer.app.data.models.Site
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [Site::class, Profile::class, ClaimLog::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun siteDao(): SiteDao
    abstract fun profileDao(): ProfileDao
    abstract fun claimLogDao(): ClaimLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "gift_claimer_db"
                )
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        CoroutineScope(Dispatchers.IO).launch {
                            getInstance(context).siteDao().apply {
                                insert(Site(
                                    name = "Jai Club",
                                    url = "https://jaiclub28.com/#/main/RedeemGift",
                                    inputSelector = "input[placeholder='Please enter gift code']",
                                    buttonSelector = "Receive",
                                    popupSelector = ".van-dialog__confirm",
                                    successText = "Successfully received"
                                ))
                                insert(Site(
                                    name = "YaarWin",
                                    url = "https://yaarwin.online/#/main/RedeemGift",
                                    inputSelector = "input[placeholder='Please enter gift code']",
                                    buttonSelector = "Receive",
                                    popupSelector = ".van-dialog__confirm",
                                    successText = "Successfully received"
                                ))
                            }
                        }
                    }
                })
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

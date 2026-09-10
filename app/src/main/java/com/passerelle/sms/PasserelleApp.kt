package com.passerelle.sms

import android.app.Application
import com.passerelle.sms.data.AppDatabase
import com.passerelle.sms.data.SettingsStore
import com.passerelle.sms.data.SmsRepository

class PasserelleApp : Application() {
    lateinit var settings: SettingsStore
        private set
    lateinit var repository: SmsRepository
        private set

    override fun onCreate() {
        super.onCreate()
        settings = SettingsStore(this)
        repository = SmsRepository(AppDatabase.get(this).smsDao())
    }
}

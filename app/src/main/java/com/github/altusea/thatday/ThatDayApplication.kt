package com.github.altusea.thatday

import android.app.Application
import com.github.altusea.thatday.data.EventRepository
import com.github.altusea.thatday.di.ServiceLocator

class ThatDayApplication : Application() {

    val repository: EventRepository by lazy { ServiceLocator.repository(this) }
}

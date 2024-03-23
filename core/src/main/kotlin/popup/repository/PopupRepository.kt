package com.wafflestudio.snu4t.popup.repository

import com.wafflestudio.snu4t.popup.data.Popup
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface PopupRepository : CoroutineCrudRepository<Popup, String>

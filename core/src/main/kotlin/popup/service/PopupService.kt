package com.wafflestudio.snu4t.popup.service

import com.wafflestudio.snu4t.common.client.ClientInfo
import com.wafflestudio.snu4t.popup.data.Popup
import com.wafflestudio.snu4t.popup.repository.PopupRepository
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Service

interface PopupService {
    suspend fun getPopups(clientInfo: ClientInfo): List<Popup>
}

@Service
class PopupServiceImpl(
    private val popupRepository: PopupRepository,
) : PopupService {
    override suspend fun getPopups(clientInfo: ClientInfo): List<Popup> {
        return popupRepository.findAll().toList().sortedBy { it.createdAt }
    }
}

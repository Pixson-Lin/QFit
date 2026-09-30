package com.pixsonlin.qfit

sealed interface PocUiState {
    data object Checking : PocUiState
    data object Unavailable : PocUiState
    data object NeedInstall : PocUiState
    data object RequestingPermission : PocUiState
    data object PermissionDenied : PocUiState
    data object Ready : PocUiState
    data object Writing : PocUiState
    data class Written(val result: PocWriteResult) : PocUiState
    data class Error(val message: String) : PocUiState
}

package me.voltual.vb.ui.home

sealed interface HomeUiState {
    data object Idle : HomeUiState
}
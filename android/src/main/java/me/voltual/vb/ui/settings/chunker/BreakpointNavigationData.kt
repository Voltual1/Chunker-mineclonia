package me.voltual.vb.ui.settings.chunker

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class BreakpointNavigationData(
    val worldId: String,
    val isNew: Boolean = false
) : Parcelable
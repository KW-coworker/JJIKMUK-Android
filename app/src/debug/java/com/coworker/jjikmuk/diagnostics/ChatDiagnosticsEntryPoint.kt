package com.coworker.jjikmuk.diagnostics

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Debug-only entry point for opt-in, metadata-only integration diagnostics. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface ChatDiagnosticsEntryPoint {
    fun sessions(): com.coworker.jjikmuk.feature.auth.session.AuthSessionManager
    fun familyRemote(): com.coworker.jjikmuk.data.repository.HttpFamilyRemote
    fun selectionRemote(): com.coworker.jjikmuk.data.repository.HttpAnalysisSelectionRemote
    fun targets(): com.coworker.jjikmuk.domain.repository.AnalysisTargetRepository
}

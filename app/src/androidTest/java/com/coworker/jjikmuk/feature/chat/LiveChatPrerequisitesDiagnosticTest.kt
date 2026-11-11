package com.coworker.jjikmuk.feature.chat

import android.os.Bundle
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.coworker.jjikmuk.MainActivity
import com.coworker.jjikmuk.diagnostics.ChatDiagnosticsEntryPoint
import com.coworker.jjikmuk.domain.repository.FamilyAccount
import com.coworker.jjikmuk.feature.auth.domain.*
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.first
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test

/** Uses the application's shared session/refresh lock; GET operations only, no chat POST. */
class LiveChatPrerequisitesDiagnosticTest {
    @get:Rule val rule=createAndroidComposeRule<MainActivity>()
    @Test fun inspectActualRequestFailures() = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("inspectChatPrerequisites")=="true")
        val instrument=InstrumentationRegistry.getInstrumentation()
        fun report(text:String) { instrument.sendStatus(0,Bundle().apply { putString("prerequisiteAudit",text) }) }
        val entry=EntryPointAccessors.fromApplication(rule.activity.applicationContext,ChatDiagnosticsEntryPoint::class.java)
        rule.waitUntil(30_000) { entry.sessions().state.value.canEnterHome || entry.sessions().state.value.failure!=null }
        fun session() {
            val state=entry.sessions().state.value
            report("session home=${state.canEnterHome} expired=${state.credentials?.expiresAtMillis?.let { it<=System.currentTimeMillis() }} refreshPending=${state.credentials?.refreshPending} failure=${state.failure?.kind}/${state.failure?.status}/${state.failure?.code}")
        }
        fun result(name:String,result:AuthResult<*>) {
            report(name+"="+when(result) {
                is AuthResult.Success -> "OK"
                is AuthResult.Failure -> "${result.error.kind}/${result.error.status}/${result.error.code}/${result.error.field}"
            })
        }
        session()
        val s=entry.sessions().state.value
        val c=s.credentials ?: return@runBlocking
        val owner=FamilyAccount(c.userId,s.generation)
        result("profile",entry.familyRemote().me(owner))
        result("families",entry.familyRemote().list(owner))
        result("selection",entry.selectionRemote().get(owner))
        entry.targets().refresh(owner)
        val state=withTimeoutOrNull(10_000) { entry.targets().state.first {
            it.ready || it.message?.let { m -> m.contains("못") || m.contains("다시") || m.contains("변경") } == true
        } } ?: entry.targets().state.value
        report("targets ready=${state.ready} profiles=${state.profiles.size} version=${state.serverVersion} message=${state.message}")
        session()
        Unit
    }
}

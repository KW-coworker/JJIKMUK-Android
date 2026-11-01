package com.coworker.jjikmuk.feature.auth.development

import androidx.compose.runtime.Composable
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController

// Release has neither a development button nor a registered development destination.
@Composable
fun DeveloperHomeEntry(onClick: () -> Unit) = Unit
fun NavHostController.openDeveloperHome() = Unit
fun NavGraphBuilder.developerHomeGraph(navController: NavHostController) = Unit

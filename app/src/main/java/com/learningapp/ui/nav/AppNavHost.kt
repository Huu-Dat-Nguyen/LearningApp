package com.learningapp.ui.nav

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.learningapp.ui.home.HomeScreen
import com.learningapp.ui.home.HomeViewModel
import com.learningapp.ui.learn.LearnScreen
import com.learningapp.ui.lesson.LessonScreen
import com.learningapp.ui.profile.ProfileScreen
import com.learningapp.ui.review.ReviewScreen
import com.learningapp.ui.settings.SettingsScreen
import com.learningapp.ui.speak.SpeakScreen
import com.learningapp.ui.theme.Ink
import com.learningapp.ui.theme.appColors
import com.learningapp.ui.theme.Muted
import com.learningapp.ui.theme.ScreenBg
import kotlin.reflect.KClass

private data class Tab(val label: String, val route: Any, val routeClass: KClass<*>)

private val tabs = listOf(
    Tab("Home", HomeRoute, HomeRoute::class),
    Tab("Learn", LearnRoute, LearnRoute::class),
    Tab("Review", ReviewRoute, ReviewRoute::class),
    Tab("Speak", SpeakRoute, SpeakRoute::class),
    Tab("Profile", ProfileRoute, ProfileRoute::class),
)

@Composable
fun AppNavHost() {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val current = backStack?.destination

    fun go(route: Any) = nav.navigate(route) {
        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }

    // Lessons are full screen: no bottom bar.
    val onTab = tabs.any { current?.hasRoute(it.routeClass) == true }

    Scaffold(
        containerColor = ScreenBg,
        bottomBar = {
            if (onTab) BottomBar(selected = { tab -> current?.hasRoute(tab.routeClass) == true }, onSelect = { go(it.route) })
        },
    ) { padding ->
        NavHost(nav, startDestination = HomeRoute, modifier = Modifier.padding(padding)) {
            composable<HomeRoute> {
                val home: HomeViewModel = hiltViewModel()
                val cardsDue by home.cardsDue.collectAsStateWithLifecycle()
                val next by home.nextLesson.collectAsStateWithLifecycle()
                val summary by home.summary.collectAsStateWithLifecycle()
                HomeScreen(
                    onContinueLesson = { next?.let { nav.navigate(LessonRoute(it.id)) } ?: go(LearnRoute) },
                    onReview = { go(ReviewRoute) },
                    onSpeak = { go(SpeakRoute) },
                    cardsDue = cardsDue,
                    nextLessonTitle = next?.title,
                    summary = summary,
                )
            }
            composable<LearnRoute> { LearnScreen(onOpenLesson = { nav.navigate(LessonRoute(it)) }) }
            composable<LessonRoute> { LessonScreen(onClose = { nav.popBackStack() }) }
            composable<ReviewRoute> { ReviewScreen(onDone = { go(HomeRoute) }) }
            composable<SpeakRoute> { SpeakScreen() }
            composable<ProfileRoute> { ProfileScreen(onOpenSettings = { nav.navigate(SettingsRoute) }) }
            composable<SettingsRoute> { SettingsScreen(onBack = { nav.popBackStack() }) }
        }
    }
}

@Composable
private fun BottomBar(selected: (Tab) -> Boolean, onSelect: (Tab) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(ScreenBg)
            .navigationBarsPadding()
            .padding(start = 4.dp, end = 4.dp, top = 6.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        tabs.forEach { tab ->
            val on = selected(tab)
            Column(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onSelect(tab) }
                    .padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Placeholder icon square until real icons are added.
                Box(Modifier.size(22.dp).clip(RoundedCornerShape(7.dp)).background(if (on) Ink else appColors.navIdle))
                Text(
                    tab.label.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (on) Ink else Muted,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

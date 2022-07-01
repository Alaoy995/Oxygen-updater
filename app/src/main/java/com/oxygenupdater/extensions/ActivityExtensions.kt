package com.oxygenupdater.extensions

import android.app.Activity
import android.content.Intent
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IntRange
import androidx.core.app.ActivityOptionsCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.oxygenupdater.R
import com.oxygenupdater.activities.FaqActivity
import com.oxygenupdater.activities.HelpActivity
import com.oxygenupdater.activities.InstallActivity
import com.oxygenupdater.activities.MainActivity
import com.oxygenupdater.activities.MainActivity.Companion.PAGE_SETTINGS
import com.oxygenupdater.activities.MainActivity.Companion.PAGE_UPDATE
import com.oxygenupdater.activities.NewsItemActivity
import com.oxygenupdater.activities.OnboardingActivity
import com.oxygenupdater.activities.SupportActionBarActivity
import com.oxygenupdater.models.NewsItem

/**
 * Starts an activity with a shared element transition
 */
private fun Activity.startActivityWithSharedTransition(
    intent: Intent,
    sharedElement: View,
    transitionName: String = sharedElement.transitionName
) {
    sharedElement.transitionName = transitionName
    intent.putExtra(
        SupportActionBarActivity.INTENT_TRANSITION_NAME,
        transitionName
    )

    val bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(
        this,
        sharedElement,
        transitionName
    ).toBundle()

    startActivity(intent, bundle)
}

fun Activity.startOnboardingActivity(
    @IntRange(
        from = PAGE_UPDATE.toLong(),
        to = PAGE_SETTINGS.toLong()
    ) startPage: Int
) = startActivity(
    Intent(this, OnboardingActivity::class.java)
        .putExtra(MainActivity.INTENT_START_PAGE, startPage)
)

fun Activity.startMainActivity(
    @IntRange(
        from = PAGE_UPDATE.toLong(),
        to = PAGE_SETTINGS.toLong()
    ) startPage: Int
) = startActivity(
    Intent(this, MainActivity::class.java)
        .putExtra(MainActivity.INTENT_START_PAGE, startPage)
)

fun Activity.startHelpActivity(
    sharedElement: View
) = startActivityWithSharedTransition(
    Intent(this, HelpActivity::class.java),
    sharedElement,
    HelpActivity.TRANSITION_NAME
)

fun Activity.startFaqActivity(
    sharedElement: View
) = startActivityWithSharedTransition(
    Intent(this, FaqActivity::class.java),
    sharedElement,
    FaqActivity.TRANSITION_NAME
)

fun Activity.startInstallActivity(
    isDownloaded: Boolean,
    sharedElement: View
) {
    val intent = Intent(this, InstallActivity::class.java)
        .putExtra(InstallActivity.INTENT_SHOW_DOWNLOAD_PAGE, !isDownloaded)

    startActivityWithSharedTransition(
        intent,
        sharedElement
    )
}

fun Activity.startNewsActivity(
    newsItem: NewsItem,
    sharedElement: View
) {
    val intent = Intent(this, NewsItemActivity::class.java)
        .putExtra(NewsItemActivity.INTENT_NEWS_ITEM_ID, newsItem.id)
        .putExtra(NewsItemActivity.INTENT_NEWS_ITEM_IMAGE_URL, newsItem.imageUrl)
        .putExtra(NewsItemActivity.INTENT_NEWS_ITEM_TITLE, newsItem.title)
        .putExtra(NewsItemActivity.INTENT_NEWS_ITEM_SUBTITLE, newsItem.subtitle)

    startActivityWithSharedTransition(
        intent,
        sharedElement
    )
}

/**
 * Allow activity to draw itself full screen
 *
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 */
fun Activity.enableEdgeToEdgeUiSupport() {
    if (packageManager.getActivityInfo(componentName, 0).themeResource == R.style.Theme_OxygenUpdater_DayNight_FullScreen) {
        WindowCompat.setDecorFitsSystemWindows(window, false)

        findViewById<ViewGroup>(android.R.id.content).getChildAt(0)?.apply {
            doOnApplyWindowInsets { view, insets, initialPadding ->
                // initialPadding contains the original padding values after inflation
                view.updatePadding(bottom = initialPadding.bottom + insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom)
            }
        }
    }
}

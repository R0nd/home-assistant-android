package io.homeassistant.companion.android.onboarding

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.wear.activity.ConfirmationActivity
import androidx.wear.remote.interactions.RemoteActivityHelper
import androidx.wear.widget.WearableRecyclerView
import dagger.hilt.android.AndroidEntryPoint
import io.homeassistant.companion.android.R
import io.homeassistant.companion.android.common.R as commonR
import io.homeassistant.companion.android.onboarding.integration.MobileAppIntegrationActivity
import io.homeassistant.companion.android.onboarding.phoneinstall.PhoneInstallActivity
import io.homeassistant.companion.android.util.LoadingView
import javax.inject.Inject
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
@SuppressLint("VisibleForTests") // https://issuetracker.google.com/issues/239451111
class OnboardingActivity : AppCompatActivity(), OnboardingView {

    private lateinit var adapter: ServerListAdapter

    private lateinit var remoteActivityHelper: RemoteActivityHelper

    companion object {
        fun newInstance(context: Context): Intent {
            return Intent(context, OnboardingActivity::class.java)
        }
    }

    private lateinit var loadingView: LoadingView

    private var phoneSignInAvailable = false
    private var phoneInstallOpened = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_onboarding)

        loadingView = findViewById(R.id.loading_view)

        adapter = ServerListAdapter(ArrayList())
        adapter.onManualSetupClicked = {
            if (!phoneSignInAvailable) {
                requestPhoneAppInstall()
            }
        }

        remoteActivityHelper = RemoteActivityHelper(this)

        findViewById<WearableRecyclerView>(R.id.server_list)?.apply {
            layoutManager = LinearLayoutManager(this@OnboardingActivity)
            isEdgeItemsCenteringEnabled = true
            adapter = this@OnboardingActivity.adapter
        }
    }

    override fun onResume() {
        super.onResume()

        loadingView.visibility = View.GONE
    }

    override fun onPause() {
        super.onPause()
    }

    private fun requestPhoneAppInstall() = startActivity(PhoneInstallActivity.newInstance(this))

    override fun startIntegration(serverId: Int) {
        startActivity(MobileAppIntegrationActivity.newInstance(this, serverId))
    }

    override fun showLoading() {
        loadingView.visibility = View.VISIBLE
    }

    override fun showContinueOnPhone() {
        val confirmation = Intent(this, ConfirmationActivity::class.java).apply {
            putExtra(
                ConfirmationActivity.EXTRA_ANIMATION_TYPE,
                ConfirmationActivity.OPEN_ON_PHONE_ANIMATION,
            )
            putExtra(ConfirmationActivity.EXTRA_ANIMATION_DURATION_MILLIS, 2000)
            putExtra(ConfirmationActivity.EXTRA_MESSAGE, getString(commonR.string.continue_on_phone))
        }
        startActivity(confirmation)
        loadingView.visibility = View.GONE
    }

    override fun showError(@StringRes message: Int?) {
        // Show failure message
        val intent = Intent(this, ConfirmationActivity::class.java).apply {
            putExtra(ConfirmationActivity.EXTRA_ANIMATION_TYPE, ConfirmationActivity.FAILURE_ANIMATION)
            putExtra(ConfirmationActivity.EXTRA_MESSAGE, getString(message ?: commonR.string.failed_connection))
        }
        startActivity(intent)
        loadingView.visibility = View.GONE
    }

    override fun onInstanceFound(instance: HomeAssistantInstance) {
        Timber.d("onInstanceFound: ${instance.name}")
        if (!adapter.servers.contains(instance)) {
            adapter.servers.add(instance)
            adapter.notifyDataSetChanged()
            Timber.d("onInstanceFound: added ${instance.name}")
        }
    }

    override fun onInstanceLost(instance: HomeAssistantInstance) {
        if (adapter.servers.contains(instance)) {
            adapter.servers.remove(instance)
            adapter.notifyDataSetChanged()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}

package com.scasp.mobile.ui.common

import android.content.Intent
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.scasp.mobile.R
import com.scasp.mobile.ScaspApp
import com.scasp.mobile.databinding.ViewStatusListBinding
import com.scasp.mobile.ui.login.LoginActivity
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Shared behaviour for signed-in screens: up navigation, session-expiry redirect, list/banner rendering. */
abstract class BaseActivity : AppCompatActivity() {
    protected val app: ScaspApp get() = ScaspApp.from(this)

    override fun onSupportNavigateUp(): Boolean { finish(); return true }

    /** Explicit intent back to the login screen, clearing the back stack. */
    protected fun goToLogin(reason: String?) {
        reason?.let { Toast.makeText(this, it, Toast.LENGTH_LONG).show() }
        startActivity(Intent(this, LoginActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        finish()
    }

    protected fun setUpList(list: ViewStatusListBinding, adapter: RowAdapter, onRefresh: () -> Unit) {
        list.recycler.layoutManager = LinearLayoutManager(this)
        list.recycler.adapter = adapter
        list.swipe.setOnRefreshListener(onRefresh)
    }

    protected fun <T> collect(flow: StateFlow<ListUiState<T>>, list: ViewStatusListBinding, adapter: RowAdapter,
                              emptyText: String, onMessage: () -> Unit, toRow: (T) -> Row) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                flow.collect { s ->
                    if (s.sessionExpired) { goToLogin(getString(R.string.session_expired)); return@collect }
                    list.swipe.isRefreshing = s.loading
                    adapter.submitList(s.items.map(toRow))
                    showBanner(list.banner, UiText.banner(s.origin, s.fetchedAtMillis))
                    list.error.text = s.error; list.error.visibility = if (s.error == null) View.GONE else View.VISIBLE
                    list.empty.text = emptyText; list.empty.visibility = if (s.empty) View.VISIBLE else View.GONE
                    s.message?.let { Toast.makeText(this@BaseActivity, it, Toast.LENGTH_LONG).show(); onMessage() }
                }
            }
        }
    }

    private fun showBanner(v: TextView, text: String?) { v.text = text; v.visibility = if (text == null) View.GONE else View.VISIBLE }
}

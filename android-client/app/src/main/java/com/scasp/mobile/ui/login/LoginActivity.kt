package com.scasp.mobile.ui.login

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.scasp.mobile.ScaspApp
import com.scasp.mobile.databinding.ActivityLoginBinding
import com.scasp.mobile.ui.common.repository
import com.scasp.mobile.ui.home.HomeActivity
import kotlinx.coroutines.launch

/** Launcher screen. Typed text survives rotation automatically (EditText ids + saved instance state). */
class LoginActivity : AppCompatActivity() {
    private lateinit var b: ActivityLoginBinding
    private val vm: LoginViewModel by viewModels { viewModelFactory { initializer { LoginViewModel(repository()) } } }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = ScaspApp.from(this)
        if (app.repository.currentUser != null) { openHome(); return }      // still signed in from last time
        b = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(b.root)
        if (savedInstanceState == null) {                                   // first creation only: don't overwrite edits on rotation
            b.serverUrl.setText(app.settings.baseUrl)
            b.demoFallback.isChecked = app.settings.allowDemoFallback
        }
        b.signIn.setOnClickListener { submit() }
        b.password.setOnEditorActionListener { _, id, _ -> if (id == EditorInfo.IME_ACTION_DONE) { submit(); true } else false }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.state.collect { s ->
                    b.progress.visibility = if (s.loading) View.VISIBLE else View.GONE
                    b.signIn.isEnabled = !s.loading
                    b.error.text = s.error
                    b.error.visibility = if (s.error == null) View.GONE else View.VISIBLE
                    if (s.user != null) openHome()
                }
            }
        }
    }

    private fun submit() {
        val app = ScaspApp.from(this)
        app.settings.allowDemoFallback = b.demoFallback.isChecked
        vm.login(b.serverUrl.text.toString(), b.userId.text.toString(), b.password.text.toString()) { app.settings.baseUrl = it }
    }

    /** Explicit intent: navigation inside the app always names the target Activity class. */
    private fun openHome() {
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }
}

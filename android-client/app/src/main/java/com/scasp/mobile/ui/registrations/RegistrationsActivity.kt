package com.scasp.mobile.ui.registrations

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.scasp.mobile.R
import com.scasp.mobile.databinding.ActivityListBinding
import com.scasp.mobile.ui.common.BaseActivity
import com.scasp.mobile.ui.common.Row
import com.scasp.mobile.ui.common.RowAdapter
import com.scasp.mobile.ui.common.repository

class RegistrationsActivity : BaseActivity() {
    private lateinit var b: ActivityListBinding
    private val vm: RegistrationsViewModel by viewModels { viewModelFactory { initializer { RegistrationsViewModel(repository()) } } }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityListBinding.inflate(layoutInflater)
        setContentView(b.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        b.intro.setText(R.string.registrations_intro)
        val adapter = RowAdapter(onAction = { row ->
            AlertDialog.Builder(this).setMessage(getString(R.string.confirm_withdraw, row.key))
                .setPositiveButton(R.string.withdraw) { _, _ -> vm.withdraw(row.key) }
                .setNegativeButton(android.R.string.cancel, null).show()
        })
        setUpList(b.list, adapter) { vm.refresh() }
        collect(vm.state, b.list, adapter, getString(R.string.empty_registrations), vm::messageShown) { r ->
            Row(r.courseCode, r.courseCode,
                if (r.status == "REGISTERED") getString(R.string.status_registered) else getString(R.string.status_waiting, r.position ?: 0),
                action = getString(R.string.withdraw))
        }
        vm.loadIfNeeded()
    }
}

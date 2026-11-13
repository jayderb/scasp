package com.scasp.mobile.ui.requests

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.activity.viewModels
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.scasp.mobile.R
import com.scasp.mobile.data.model.SERVICE_REQUEST_CATEGORIES
import com.scasp.mobile.databinding.ActivityRequestsBinding
import com.scasp.mobile.ui.common.BaseActivity
import com.scasp.mobile.ui.common.Row
import com.scasp.mobile.ui.common.RowAdapter
import com.scasp.mobile.ui.common.repository

/** Students: submit a request and see their own. Staff: read-only open queue (actions stay in the web app). */
class RequestsActivity : BaseActivity() {
    private lateinit var b: ActivityRequestsBinding
    private val vm: RequestsViewModel by viewModels { viewModelFactory { initializer { RequestsViewModel(repository()) } } }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityRequestsBinding.inflate(layoutInflater)
        setContentView(b.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        b.category.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, SERVICE_REQUEST_CATEGORIES)
        b.priority.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, (1..5).map { "Priority $it" })
        b.form.visibility = if (vm.isStaff) View.GONE else View.VISIBLE
        b.staffNote.visibility = if (vm.isStaff) View.VISIBLE else View.GONE
        b.submit.setOnClickListener {
            vm.submit(b.category.selectedItem as String, b.description.text.toString(), b.priority.selectedItemPosition + 1) {
                b.description.text?.clear()
            }
        }
        val adapter = RowAdapter()
        setUpList(b.list, adapter) { vm.refresh() }
        collect(vm.state, b.list, adapter, getString(R.string.empty_requests), vm::messageShown) { r ->
            Row(r.id, "#${r.id} \u00b7 ${r.category} \u00b7 ${r.status}", "Priority ${r.priority} \u00b7 ${r.description}")
        }
        vm.loadIfNeeded()
    }
}

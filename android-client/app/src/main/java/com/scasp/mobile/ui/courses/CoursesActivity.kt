package com.scasp.mobile.ui.courses

import android.os.Bundle
import android.view.inputmethod.EditorInfo
import androidx.activity.viewModels
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.scasp.mobile.R
import com.scasp.mobile.databinding.ActivityCoursesBinding
import com.scasp.mobile.ui.common.BaseActivity
import com.scasp.mobile.ui.common.Row
import com.scasp.mobile.ui.common.RowAdapter
import com.scasp.mobile.ui.common.repository
import com.scasp.mobile.ui.common.savedState

class CoursesActivity : BaseActivity() {
    private lateinit var b: ActivityCoursesBinding
    private val vm: CoursesViewModel by viewModels { viewModelFactory { initializer { CoursesViewModel(repository(), savedState()) } } }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityCoursesBinding.inflate(layoutInflater)
        setContentView(b.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        val adapter = RowAdapter(onAction = { vm.register(it.key) })
        setUpList(b.list, adapter) { vm.refresh() }
        if (savedInstanceState == null) b.search.setText(vm.query)
        b.searchButton.setOnClickListener { vm.search(b.search.text.toString()) }
        b.search.setOnEditorActionListener { _, id, _ -> if (id == EditorInfo.IME_ACTION_SEARCH) { vm.search(b.search.text.toString()); true } else false }
        collect(vm.state, b.list, adapter, getString(R.string.empty_courses), vm::messageShown) { c ->
            Row(c.code, "${c.code} \u00b7 ${c.title}",
                getString(R.string.course_line, c.enrolled, c.capacity, c.seatsLeft, c.waiting),
                action = if (!vm.canRegister) null else getString(if (c.full) R.string.join_waiting_list else R.string.register))
        }
        vm.loadIfNeeded()
    }
}

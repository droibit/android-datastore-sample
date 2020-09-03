package com.github.droibit.datastore.sample.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import com.github.droibit.datastore.sample.data.SortOrder
import com.github.droibit.datastore.sample.data.TasksRepository
import com.github.droibit.datastore.sample.data.UserPreferencesRepository
import com.github.droibit.datastore.sample.databinding.ActivityTasksBinding

class TasksActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTasksBinding
    private val adapter = TasksAdapter()

    private lateinit var viewModel: TasksViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTasksBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        viewModel = ViewModelProvider(
            this,
            TasksViewModel.Factory(
                TasksRepository, UserPreferencesRepository((this))
            )
        ).get(TasksViewModel::class.java)

        setupRecyclerView()
        setupFilterListeners(viewModel)
        setupSort()

        viewModel.tasksUiModel.observe(this) { tasksUiModel ->
            adapter.submitList(tasksUiModel.tasks)
            updateSort(tasksUiModel.sortOrder)
            binding.showCompletedSwitch.isChecked = tasksUiModel.showCompleted
        }
    }

    private fun setupFilterListeners(viewModel: TasksViewModel) {
        binding.showCompletedSwitch.setOnCheckedChangeListener { _, checked ->
            viewModel.showCompletedTasks(checked)
        }
    }

    private fun setupRecyclerView() {
        val decoration = DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        binding.list.addItemDecoration(decoration)
        binding.list.adapter = adapter
    }

    private fun setupSort() {
        binding.sortDeadline.setOnCheckedChangeListener { _, checked ->
            viewModel.enableSortByDeadline(checked)
        }

        binding.sortPriority.setOnCheckedChangeListener { _, checked ->
            viewModel.enableSortByPriority(checked)
        }
    }

    private fun updateSort(sortOrder: SortOrder) {
        binding.sortDeadline.isChecked =
            sortOrder == SortOrder.BY_DEADLINE || sortOrder == SortOrder.BY_DEADLINE_AND_PRIORITY
        binding.sortPriority.isChecked =
            sortOrder == SortOrder.BY_PRIORITY || sortOrder == SortOrder.BY_DEADLINE_AND_PRIORITY
    }
}
package com.example.ocrjizhang.ui.statistics

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.ocrjizhang.R
import com.example.ocrjizhang.data.model.StatisticsPeriod
import com.example.ocrjizhang.databinding.FragmentStatisticsBinding
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.android.material.chip.ChipGroup
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class StatisticsFragment : Fragment() {

    private var _binding: FragmentStatisticsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: StatisticsViewModel by viewModels()

    private val chartColors by lazy {
        listOf(
            R.color.primary_brand,
            R.color.secondary_brand,
            R.color.warning,
            R.color.negative,
            R.color.positive,
            R.color.primary_brand_dark,
        ).map { colorRes ->
            ContextCompat.getColor(requireContext(), colorRes)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentStatisticsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        configurePeriodSelector()
        configurePieChart()
        configureBarChart()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::render)
            }
        }
    }

    private fun configurePeriodSelector() {
        binding.periodChipGroup.setOnCheckedStateChangeListener { _: ChipGroup, checkedIds: List<Int> ->
            when (checkedIds.firstOrNull()) {
                R.id.dayChip -> viewModel.selectPeriod(StatisticsPeriod.DAY)
                R.id.weekChip -> viewModel.selectPeriod(StatisticsPeriod.WEEK)
                R.id.monthChip -> viewModel.selectPeriod(StatisticsPeriod.MONTH)
            }
        }
    }

    private fun configurePieChart() {
        binding.categoryChart.apply {
            description.isEnabled = false
            setUsePercentValues(false)
            setDrawHoleEnabled(true)
            holeRadius = 62f
            setTransparentCircleAlpha(0)
            setCenterTextSize(14f)
            setCenterTextColor(ContextCompat.getColor(requireContext(), R.color.ink_secondary))
            setEntryLabelColor(ContextCompat.getColor(requireContext(), R.color.ink_primary))
            legend.isEnabled = false
        }
    }

    private fun configureBarChart() {
        binding.trendChart.apply {
            description.isEnabled = false
            legend.apply {
                verticalAlignment = Legend.LegendVerticalAlignment.TOP
                horizontalAlignment = Legend.LegendHorizontalAlignment.RIGHT
                orientation = Legend.LegendOrientation.HORIZONTAL
                setDrawInside(false)
                textColor = ContextCompat.getColor(requireContext(), R.color.ink_secondary)
            }
            axisRight.isEnabled = false
            setDrawGridBackground(false)
            setFitBars(true)
            setNoDataText("")

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                setDrawGridLines(false)
                textColor = ContextCompat.getColor(requireContext(), R.color.ink_secondary)
            }

            axisLeft.apply {
                axisMinimum = 0f
                textColor = ContextCompat.getColor(requireContext(), R.color.ink_secondary)
                gridColor = ContextCompat.getColor(requireContext(), R.color.outline_soft)
            }
        }
    }

    private fun render(state: StatisticsUiState) {
        binding.rangeLabel.text = state.rangeLabel
        binding.incomeValue.text = state.incomeLabel
        binding.expenseValue.text = state.expenseLabel
        binding.surplusValue.text = state.surplusLabel
        binding.overviewEmptyGroup.isVisible = !state.hasTransactions
        binding.emptyTitle.text = state.emptyTitle
        binding.emptyBody.text = state.emptyBody

        if (!binding.dayChip.isChecked && state.selectedPeriod == StatisticsPeriod.DAY) {
            binding.dayChip.isChecked = true
        } else if (!binding.weekChip.isChecked && state.selectedPeriod == StatisticsPeriod.WEEK) {
            binding.weekChip.isChecked = true
        } else if (!binding.monthChip.isChecked && state.selectedPeriod == StatisticsPeriod.MONTH) {
            binding.monthChip.isChecked = true
        }

        renderCategoryChart(state)
        renderTrendChart(state)
    }

    private fun renderCategoryChart(state: StatisticsUiState) {
        val hasCategoryData = state.categoryItems.isNotEmpty()
        binding.categoryChart.isVisible = hasCategoryData
        binding.categoryBreakdownContainer.isVisible = hasCategoryData
        binding.categoryEmptyText.isVisible = !hasCategoryData
        binding.categoryEmptyText.text = state.categoryEmptyText

        binding.categoryBreakdownContainer.removeAllViews()

        if (!hasCategoryData) {
            binding.categoryChart.data = null
            binding.categoryChart.centerText = ""
            binding.categoryChart.highlightValues(null)
            binding.categoryChart.clear()
            return
        }

        val entries = state.categoryItems.map { item ->
            PieEntry(item.amountFen.toFloat() / 100f, item.categoryName)
        }
        val dataSet = PieDataSet(entries, "").apply {
            colors = entries.indices.map { index -> chartColors[index % chartColors.size] }
            sliceSpace = 3f
            valueTextColor = ContextCompat.getColor(requireContext(), R.color.ink_primary)
            valueTextSize = 12f
        }
        binding.categoryChart.apply {
            centerText = when (state.selectedPeriod) {
                StatisticsPeriod.DAY -> "今日支出"
                StatisticsPeriod.WEEK -> "本周支出"
                StatisticsPeriod.MONTH -> "本月支出"
            }
            data = PieData(dataSet)
            invalidate()
        }

        state.categoryItems.forEachIndexed { index, item ->
            val row = layoutInflater.inflate(
                R.layout.item_statistics_breakdown,
                binding.categoryBreakdownContainer,
                false,
            )
            row.findViewById<View>(R.id.colorDot).backgroundTintList =
                ColorStateList.valueOf(chartColors[index % chartColors.size])
            row.findViewById<TextView>(R.id.categoryNameView).text = item.categoryName
            row.findViewById<TextView>(R.id.categoryAmountView).text = item.amountLabel
            row.findViewById<TextView>(R.id.categoryShareView).text = item.shareLabel
            binding.categoryBreakdownContainer.addView(row)
        }
    }

    private fun renderTrendChart(state: StatisticsUiState) {
        val hasTrendData = state.trendItems.any { it.incomeFen > 0L || it.expenseFen > 0L }
        binding.trendChart.isVisible = hasTrendData
        binding.trendEmptyText.isVisible = !hasTrendData
        binding.trendEmptyText.text = state.trendEmptyText

        if (!hasTrendData) {
            binding.trendChart.data = null
            binding.trendChart.highlightValues(null)
            binding.trendChart.clear()
            return
        }

        val labels = state.trendItems.map { it.label }
        val incomeEntries = state.trendItems.mapIndexed { index, item ->
            BarEntry(index.toFloat(), item.incomeFen / 100f)
        }
        val expenseEntries = state.trendItems.mapIndexed { index, item ->
            BarEntry(index.toFloat(), item.expenseFen / 100f)
        }

        val incomeDataSet = BarDataSet(incomeEntries, getString(R.string.statistics_income_legend)).apply {
            color = ContextCompat.getColor(requireContext(), R.color.positive)
        }
        val expenseDataSet = BarDataSet(expenseEntries, getString(R.string.statistics_expense_legend)).apply {
            color = ContextCompat.getColor(requireContext(), R.color.negative)
        }

        val groupSpace = 0.24f
        val barSpace = 0.08f
        val barWidth = 0.3f

        binding.trendChart.apply {
            val barData = BarData(incomeDataSet, expenseDataSet).apply {
                this.barWidth = barWidth
            }
            data = barData
            xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            xAxis.axisMinimum = 0f
            xAxis.axisMaximum = barData.getGroupWidth(groupSpace, barSpace) * labels.size
            groupBars(0f, groupSpace, barSpace)
            invalidate()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

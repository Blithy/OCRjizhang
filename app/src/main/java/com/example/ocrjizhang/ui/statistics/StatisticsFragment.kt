package com.example.ocrjizhang.ui.statistics

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.NumberPicker
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
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.chip.ChipGroup
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.CompositeDateValidator
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import dagger.hilt.android.AndroidEntryPoint
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.launch

@AndroidEntryPoint
class StatisticsFragment : Fragment() {

    private var _binding: FragmentStatisticsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: StatisticsViewModel by viewModels()

    private var latestState: StatisticsUiState = StatisticsUiState()
    private val zoneId: ZoneId = ZoneId.systemDefault()

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
        configureRangeNavigator()
        configureTimeSelectorEntrances()
        configurePieChart()
        configureExpenseBarChart()
        configureAssetLineChart()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::render)
            }
        }
    }

    private fun configurePeriodSelector() {
        binding.periodChipGroup.setOnCheckedStateChangeListener { _: ChipGroup, checkedIds: List<Int> ->
            when (checkedIds.firstOrNull()) {
                R.id.weekChip -> viewModel.selectPeriod(StatisticsPeriod.WEEK)
                R.id.monthChip -> viewModel.selectPeriod(StatisticsPeriod.MONTH)
                R.id.yearChip -> viewModel.selectPeriod(StatisticsPeriod.YEAR)
                R.id.allChip -> viewModel.selectPeriod(StatisticsPeriod.ALL)
                R.id.customChip -> viewModel.selectPeriod(StatisticsPeriod.CUSTOM)
            }
        }
    }

    private fun configureRangeNavigator() {
        binding.previousRangeButton.setOnClickListener { viewModel.moveToPreviousRange() }
        binding.nextRangeButton.setOnClickListener { viewModel.moveToNextRange() }
    }

    private fun configureTimeSelectorEntrances() {
        binding.rangeLabel.setOnClickListener {
            when (latestState.selectedPeriod) {
                StatisticsPeriod.WEEK -> showSingleDatePicker(
                    title = getString(R.string.statistics_pick_week),
                    selection = latestState.rangeStartMillis,
                ) { selectedMillis ->
                    viewModel.selectWeekByDate(selectedMillis)
                }

                StatisticsPeriod.MONTH -> showMonthPickerBottomSheet()
                StatisticsPeriod.YEAR -> showYearPickerBottomSheet()
                StatisticsPeriod.ALL -> Unit
                StatisticsPeriod.CUSTOM -> showDateRangePicker()
            }
        }
        binding.rangeStartLabel.setOnClickListener {
            showSingleDatePicker(
                title = getString(R.string.statistics_pick_range_start),
                selection = latestState.rangeStartMillis,
            ) { selectedMillis ->
                viewModel.selectRangeStart(selectedMillis)
            }
        }
        binding.rangeEndLabel.setOnClickListener {
            showSingleDatePicker(
                title = getString(R.string.statistics_pick_range_end),
                selection = latestState.rangeEndMillis,
            ) { selectedMillis ->
                viewModel.selectRangeEnd(selectedMillis)
            }
        }
    }

    private fun showDateRangePicker() {
        val (start, end) = effectiveRangeSelection()
        val picker = MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText(getString(R.string.statistics_pick_range))
            .setSelection(androidx.core.util.Pair(start, end))
            .setCalendarConstraints(buildCalendarConstraints())
            .build()
        picker.addOnPositiveButtonClickListener { selection ->
            val selectedStart = selection.first ?: return@addOnPositiveButtonClickListener
            val selectedEnd = selection.second ?: selectedStart
            viewModel.selectCustomRange(selectedStart, selectedEnd)
        }
        picker.show(parentFragmentManager, "statistics-range-picker")
    }

    private fun showSingleDatePicker(
        title: String,
        selection: Long,
        onConfirm: (Long) -> Unit,
    ) {
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(title)
            .setSelection(selection)
            .setCalendarConstraints(buildCalendarConstraints())
            .build()
        picker.addOnPositiveButtonClickListener { selected ->
            onConfirm(selected)
        }
        picker.show(parentFragmentManager, "statistics-single-date-picker-$title")
    }

    private fun showYearPickerBottomSheet() {
        val dialog = BottomSheetDialog(requireContext())
        val contentView = layoutInflater.inflate(R.layout.bottom_sheet_statistics_year_picker, null)
        dialog.setContentView(contentView)

        val yearPicker = contentView.findViewById<NumberPicker>(R.id.yearPicker)
        val cancelButton = contentView.findViewById<View>(R.id.yearPickerCancelButton)
        val currentButton = contentView.findViewById<View>(R.id.yearPickerCurrentButton)
        val confirmButton = contentView.findViewById<View>(R.id.yearPickerConfirmButton)

        val currentYear = Instant.ofEpochMilli(System.currentTimeMillis()).atZone(zoneId).year
        val selectedYear = Instant.ofEpochMilli(latestState.rangeStartMillis).atZone(zoneId).year

        yearPicker.minValue = latestState.minYear
        yearPicker.maxValue = latestState.maxYear
        yearPicker.value = selectedYear.coerceIn(latestState.minYear, latestState.maxYear)
        yearPicker.wrapSelectorWheel = false

        cancelButton.setOnClickListener { dialog.dismiss() }
        currentButton.setOnClickListener {
            yearPicker.value = currentYear.coerceIn(latestState.minYear, latestState.maxYear)
        }
        confirmButton.setOnClickListener {
            viewModel.selectYear(yearPicker.value)
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun showMonthPickerBottomSheet() {
        val dialog = BottomSheetDialog(requireContext())
        val contentView = layoutInflater.inflate(R.layout.bottom_sheet_statistics_month_picker, null)
        dialog.setContentView(contentView)

        val yearPicker = contentView.findViewById<NumberPicker>(R.id.yearPicker)
        val monthPicker = contentView.findViewById<NumberPicker>(R.id.monthPicker)
        val cancelButton = contentView.findViewById<View>(R.id.monthPickerCancelButton)
        val currentButton = contentView.findViewById<View>(R.id.monthPickerCurrentButton)
        val confirmButton = contentView.findViewById<View>(R.id.monthPickerConfirmButton)

        val now = Instant.ofEpochMilli(System.currentTimeMillis()).atZone(zoneId)
        val selectedDate = Instant.ofEpochMilli(latestState.rangeStartMillis).atZone(zoneId)

        yearPicker.minValue = latestState.minYear
        yearPicker.maxValue = latestState.maxYear
        yearPicker.value = selectedDate.year.coerceIn(latestState.minYear, latestState.maxYear)
        yearPicker.wrapSelectorWheel = false

        monthPicker.minValue = 1
        monthPicker.maxValue = 12
        monthPicker.displayedValues = Array(12) { index -> "${index + 1}月" }
        monthPicker.value = selectedDate.monthValue
        monthPicker.wrapSelectorWheel = true

        cancelButton.setOnClickListener { dialog.dismiss() }
        currentButton.setOnClickListener {
            yearPicker.value = now.year.coerceIn(latestState.minYear, latestState.maxYear)
            monthPicker.value = now.monthValue
        }
        confirmButton.setOnClickListener {
            viewModel.selectMonth(yearPicker.value, monthPicker.value)
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun buildCalendarConstraints(): CalendarConstraints {
        val start = localDateMillis(latestState.minYear, 1, 1)
        val end = localDateMillis(latestState.maxYear, 12, 31, inclusiveEnd = true)
        val dateValidator = CompositeDateValidator.allOf(
            listOf(
                DateValidatorPointForward.from(start),
                DateValidatorPointBackward.before(end),
            ),
        )
        return CalendarConstraints.Builder()
            .setStart(start)
            .setEnd(end)
            .setOpenAt(latestState.rangeStartMillis.coerceIn(start, end))
            .setValidator(dateValidator)
            .build()
    }

    private fun effectiveRangeSelection(): Pair<Long, Long> {
        val start = if (latestState.rangeStartMillis > 0L) latestState.rangeStartMillis else System.currentTimeMillis()
        val end = if (latestState.rangeEndMillis > 0L) latestState.rangeEndMillis else start
        return start to end
    }

    private fun localDateMillis(year: Int, month: Int, day: Int, inclusiveEnd: Boolean = false): Long {
        val localDate = java.time.LocalDate.of(year, month, day)
        return if (inclusiveEnd) {
            localDate.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli() - 1
        } else {
            localDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
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

    private fun configureExpenseBarChart() {
        binding.expenseTrendChart.apply {
            description.isEnabled = false
            legend.isEnabled = false
            axisRight.isEnabled = false
            setDrawGridBackground(false)
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

    private fun configureAssetLineChart() {
        binding.assetTrendChart.apply {
            description.isEnabled = false
            legend.isEnabled = false
            axisRight.isEnabled = false
            setNoDataText("")
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                setDrawGridLines(false)
                textColor = ContextCompat.getColor(requireContext(), R.color.ink_secondary)
            }
            axisLeft.apply {
                textColor = ContextCompat.getColor(requireContext(), R.color.ink_secondary)
                gridColor = ContextCompat.getColor(requireContext(), R.color.outline_soft)
            }
        }
    }

    private fun render(state: StatisticsUiState) {
        latestState = state
        binding.rangeLabel.text = state.rangeLabel
        binding.rangeStartLabel.text = state.rangeStartLabel
        binding.rangeEndLabel.text = state.rangeEndLabel
        binding.expenseValue.text = state.expenseLabel
        binding.incomeValue.text = state.incomeLabel
        binding.surplusValue.text = state.surplusLabel
        binding.averageExpenseValue.text = state.averageExpenseLabel
        binding.expenseChartAverageText.text = state.expenseChartAverageLabel

        binding.overviewEmptyGroup.isVisible = !state.hasTransactions
        binding.emptyTitle.text = state.emptyTitle
        binding.emptyBody.text = state.emptyBody

        binding.previousRangeButton.isVisible = state.timeDisplayMode != StatisticsTimeDisplayMode.ALL_READ_ONLY
        binding.nextRangeButton.isVisible = state.timeDisplayMode != StatisticsTimeDisplayMode.ALL_READ_ONLY
        binding.previousRangeButton.isEnabled = state.canNavigatePrevious
        binding.nextRangeButton.isEnabled = state.canNavigateNext
        binding.previousRangeButton.alpha = if (state.canNavigatePrevious) 1f else 0.4f
        binding.nextRangeButton.alpha = if (state.canNavigateNext) 1f else 0.4f

        binding.rangeLabel.isVisible = state.timeDisplayMode != StatisticsTimeDisplayMode.RANGE_LABEL
        binding.rangeEndpointGroup.isVisible = state.timeDisplayMode == StatisticsTimeDisplayMode.RANGE_LABEL
        binding.rangeLabel.alpha = if (state.timeDisplayMode == StatisticsTimeDisplayMode.ALL_READ_ONLY) 0.8f else 1f

        val checkedChipId = when (state.selectedPeriod) {
            StatisticsPeriod.WEEK -> R.id.weekChip
            StatisticsPeriod.MONTH -> R.id.monthChip
            StatisticsPeriod.YEAR -> R.id.yearChip
            StatisticsPeriod.ALL -> R.id.allChip
            StatisticsPeriod.CUSTOM -> R.id.customChip
        }
        if (binding.periodChipGroup.checkedChipId != checkedChipId) {
            binding.periodChipGroup.check(checkedChipId)
        }

        renderCategoryChart(state)
        renderExpenseChart(state)
        renderAssetTrendChart(state)
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
                StatisticsPeriod.WEEK -> "本周支出"
                StatisticsPeriod.MONTH -> "本月支出"
                StatisticsPeriod.YEAR -> "今年支出"
                StatisticsPeriod.ALL -> "全部支出"
                StatisticsPeriod.CUSTOM -> "范围支出"
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

    private fun renderExpenseChart(state: StatisticsUiState) {
        val hasTrendData = state.trendItems.any { it.expenseFen > 0L }
        binding.expenseTrendChart.isVisible = hasTrendData
        binding.trendEmptyText.isVisible = !hasTrendData
        binding.trendEmptyText.text = state.trendEmptyText

        if (!hasTrendData) {
            binding.expenseTrendChart.data = null
            binding.expenseTrendChart.highlightValues(null)
            binding.expenseTrendChart.clear()
            return
        }

        val labels = state.trendItems.map { it.label }
        val entries = state.trendItems.mapIndexed { index, item ->
            BarEntry(index.toFloat(), item.expenseFen / 100f)
        }
        val expenseDataSet = BarDataSet(entries, getString(R.string.statistics_expense_legend)).apply {
            color = ContextCompat.getColor(requireContext(), R.color.primary_brand)
            valueTextColor = ContextCompat.getColor(requireContext(), R.color.ink_secondary)
            valueTextSize = 10f
        }
        val barData = BarData(expenseDataSet).apply {
            barWidth = 0.58f
        }
        binding.expenseTrendChart.apply {
            data = barData
            xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            xAxis.axisMinimum = -0.5f
            xAxis.axisMaximum = labels.size - 0.5f
            invalidate()
        }
    }

    private fun renderAssetTrendChart(state: StatisticsUiState) {
        val hasData = state.assetTrendItems.isNotEmpty()
        binding.assetTrendChart.isVisible = hasData
        binding.assetTrendEmptyText.isVisible = !hasData
        binding.assetTrendEmptyText.text = state.assetTrendEmptyText

        if (!hasData) {
            binding.assetTrendChart.data = null
            binding.assetTrendChart.highlightValues(null)
            binding.assetTrendChart.clear()
            return
        }

        val labels = state.assetTrendItems.map { it.label }
        val entries = state.assetTrendItems.mapIndexed { index, item ->
            Entry(index.toFloat(), item.amountFen / 100f)
        }
        val assetDataSet = LineDataSet(entries, getString(R.string.statistics_asset_trend_title)).apply {
            color = ContextCompat.getColor(requireContext(), R.color.primary_brand_dark)
            lineWidth = 2f
            setDrawCircleHole(false)
            setCircleColor(ContextCompat.getColor(requireContext(), R.color.primary_brand_dark))
            circleRadius = 3f
            valueTextColor = ContextCompat.getColor(requireContext(), R.color.ink_secondary)
            valueTextSize = 10f
        }
        binding.assetTrendChart.apply {
            data = LineData(assetDataSet)
            xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            xAxis.axisMinimum = -0.5f
            xAxis.axisMaximum = labels.size - 0.5f
            invalidate()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

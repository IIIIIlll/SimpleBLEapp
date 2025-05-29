package com.example.bleglucose

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.components.XAxis

class GraphActivity : AppCompatActivity() {
    private lateinit var lineChart: LineChart
    private val entries = ArrayList<Entry>()
    private var time = 0f
    private val handler = Handler(Looper.getMainLooper())
    private var running = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_graph)
        lineChart = findViewById(R.id.line_chart)

        setupChart()
        startLiveGraph()
    }

    private fun setupChart() {
        lineChart.description.text = "Live Glucose Readings"
        lineChart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        lineChart.axisRight.isEnabled = false
        lineChart.setTouchEnabled(true)
        lineChart.setPinchZoom(true)
    }

    private fun startLiveGraph() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                if (!running) return
                // For demo: use random data
                val newValue = (70 + Math.random() * 60).toFloat()
                entries.add(Entry(time, newValue))
                time += 1f

                val dataSet = LineDataSet(entries, "Glucose")
                val data = LineData(dataSet)
                lineChart.data = data
                lineChart.notifyDataSetChanged()
                lineChart.invalidate()

                handler.postDelayed(this, 1000)
            }
        }, 1000)
    }

    override fun onDestroy() {
        super.onDestroy()
        running = false
    }
}

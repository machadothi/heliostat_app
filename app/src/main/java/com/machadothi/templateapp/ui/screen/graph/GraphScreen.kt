package com.machadothi.templateapp.ui.screen.graph

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun GraphScreen(
    onGraphSelected: (GraphType) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Button(onClick = {
            onGraphSelected(GraphType.TEMPERATURE)
        }, content = {
            Text(text = "Temperature")
        })
        Button(onClick = {
            onGraphSelected(GraphType.HUMIDITY)
        }, content = {
            Text(text = "Humidity")
        })
    }
}

@Composable
fun oldGraphContent() {
    /*val lineData = uiState.data.map { data ->
                    LineData(x = "${String.format("%1.0f", data.temp)}°C", y = data.temp)
                }.takeLast(10)
                val barData = uiState.data.map { data ->
                    BarData(x = String.format("%1.2f", data.dunno4), y = data.dunno4)
                }.takeLast(10)
                val lineStyle = LineGraphStyle(
                    colors = LineGraphColors(
                        lineColor = Color(0x000FFF),
                        pointColor = Color(0x000FFF),
                        clickHighlightColor = TODO(),
                        crossHairColor = TODO(),
                        xAxisTextColor = TODO(),
                        yAxisTextColor = TODO(),
                        fillType = LineGraphFillType.None
                    ),
                    visibility = LineGraphVisibility(
                        isCrossHairVisible = TODO(),
                        isYAxisLabelVisible = TODO(),
                        isXAxisLabelVisible = TODO(),
                        isGridVisible = TODO()
                    ),
                    yAxisLabelPosition = LabelPosition.LEFT
                )
                LineGraph(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    data = lineData
                )
                BarGraph(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    data = barData,
                )*/
}
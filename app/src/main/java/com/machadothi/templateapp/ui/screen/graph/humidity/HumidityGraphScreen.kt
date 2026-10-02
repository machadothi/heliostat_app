package com.machadothi.templateapp.ui.screen.graph.humidity

import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import ir.ehsannarmani.compose_charts.LineChart
import ir.ehsannarmani.compose_charts.models.AnimationMode
import ir.ehsannarmani.compose_charts.models.DrawStyle
import ir.ehsannarmani.compose_charts.models.HorizontalIndicatorProperties
import ir.ehsannarmani.compose_charts.models.LabelHelperProperties
import ir.ehsannarmani.compose_charts.models.Line

@Composable
fun HumidityGraphScreen(
    viewModel: HumidityGraphViewModel = hiltViewModel()
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        when (val uiState = viewModel.uiState) {
            HumidityGraphUIState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            }

            is HumidityGraphUIState.Loaded -> {
                LineChart(
                    modifier = Modifier.padding(horizontal = 22.dp),
                    data = remember {
                        listOf(
                            Line(
                                label = "Humidity",
                                values = uiState.humidity,
                                color = SolidColor(Color(0xFF23af92)),
                                firstGradientFillColor = Color(0xFF2BC0A1).copy(alpha = .5f),
                                strokeAnimationSpec = tween(2000, easing = EaseInOutCubic),
                                gradientAnimationDelay = 1000,
                                drawStyle = DrawStyle.Stroke(width = 2.dp),
                            )
                        )
                    },
                    labelHelperProperties = LabelHelperProperties(
                        textStyle = TextStyle(color = Color.White)
                    ),
                    indicatorProperties = HorizontalIndicatorProperties(
                        textStyle = TextStyle(color = Color.White)
                    ),

                    animationMode = AnimationMode.Together(delayBuilder = {
                        it * 500L
                    }),
                    minValue = 0.0,
                    maxValue = 100.0,
                    curvedEdges = true,
                )
            }
        }
    }
}
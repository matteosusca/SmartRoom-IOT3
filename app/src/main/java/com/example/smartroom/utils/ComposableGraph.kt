package com.example.smartroom.utils

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun ComposableGraph(
    modifier: Modifier = Modifier,
    values: Graph
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(0.2f)
            .padding(10.dp)
    ) {
        val valuesWidthPerc = 1.toFloat() / values.getSize()
        val minMax = values.getMinMaxValues(0.0)
        val valuesHeightPerc = 1 / (minMax.second - minMax.first)

        for (value in values.getValues()) {
            Column(
                modifier = Modifier.fillMaxWidth(valuesWidthPerc),
                verticalArrangement = Arrangement.Bottom,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier.background(Color.Red)
                        .height(2.dp)
                )
                Box(
                    modifier.fillMaxHeight((valuesHeightPerc * value).toFloat())
                )
            }
        }
    }
}

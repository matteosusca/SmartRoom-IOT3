package com.example.smartroom.utils

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
    values: List<Double>
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(0.2f)
            .padding(10.dp)
            .border(2.dp, Color.Black)
    ) {
        val valuesWidthPerc = 1.toFloat() / values.size
        val min= values.min()
        val max = values.max()
        val valuesHeightPerc = 1 / (max - min)

        for (value in values) {
            Column(
                modifier = Modifier.fillMaxWidth(valuesWidthPerc).border(2.dp, Color.Blue),
                verticalArrangement = Arrangement.Bottom,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .background(Color.Red)
                        .height(2.dp)
                )
                Box(
                    modifier.fillMaxHeight((valuesHeightPerc * value).toFloat())
                )
            }
        }
    }
}

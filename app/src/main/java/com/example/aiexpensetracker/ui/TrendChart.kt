import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aiexpensetracker.viewmodel.MonthlyTrend // 导入刚才定义的类

@Composable
fun TrendChart(
    data: List<MonthlyTrend>,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) return

    val textMeasurer = rememberTextMeasurer()
    val lineColor = MaterialTheme.colorScheme.primary
    val pointColor = MaterialTheme.colorScheme.tertiary
    val labelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "6-Month Spending Trend",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        ) {
            val width = size.width
            val height = size.height
            val spacing = width / (data.size - 1).coerceAtLeast(1) // 点与点的间距

            // 1. 找出最大值，用于计算 Y 轴比例 (留 20% 顶部空间)
            val maxAmount = data.maxOfOrNull { it.totalAmount } ?: 1f
            val yScale = if (maxAmount == 0f) 1f else (height * 0.8f) / maxAmount

            // 2. 准备路径 Path
            val strokePath = Path()
            val fillPath = Path() // 用于底部的渐变填充

            data.forEachIndexed { index, trend ->
                val x = index * spacing
                // Y 轴坐标：Canvas 0 在顶部，所以要用 height 减去
                val y = height - (trend.totalAmount * yScale) - 30f // 留出底部文字空间

                if (index == 0) {
                    strokePath.moveTo(x, y)
                    fillPath.moveTo(x, height) // 填充起始点：左下角
                    fillPath.lineTo(x, y)
                } else {
                    // 使用贝塞尔曲线让线条更平滑 (可选，不想复杂就用 lineTo)
                    val prevX = (index - 1) * spacing
                    val prevTrend = data[index - 1]
                    val prevY = height - (prevTrend.totalAmount * yScale) - 30f

                    val controlX1 = prevX + (x - prevX) / 2
                    val controlX2 = prevX + (x - prevX) / 2

                    strokePath.cubicTo(controlX1, prevY, controlX2, y, x, y)
                    fillPath.cubicTo(controlX1, prevY, controlX2, y, x, y)
                }

                // 3. 画该点的月份文字 (X 轴标签)
                drawText(
                    textMeasurer = textMeasurer,
                    text = trend.monthLabel,
                    topLeft = Offset(x - 20f, height - 20f), // 稍微偏移居中
                    style = TextStyle(color = labelColor, fontSize = 10.sp)
                )

                // 4. 画数据点 (圆点)
                drawCircle(
                    color = pointColor,
                    radius = 4.dp.toPx(),
                    center = Offset(x, y)
                )
            }

            // 闭合填充路径
            fillPath.lineTo(width, height)
            fillPath.close()

            // 5. 绘制渐变填充
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        lineColor.copy(alpha = 0.3f),
                        Color.Transparent
                    ),
                    startY = 0f,
                    endY = height
                )
            )

            // 6. 绘制折线
            drawPath(
                path = strokePath,
                color = lineColor,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )
        }
    }
}
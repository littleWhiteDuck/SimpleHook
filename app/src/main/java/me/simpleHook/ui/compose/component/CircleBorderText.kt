package me.simpleHook.ui.compose.component

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val METHOD_COLOR = Color(red = 192, green = 32, blue = 32)
private val FIELD_COLOR = Color(red = 208, green = 104, blue = 0)
private val INTERFACE_COLOR = Color(red = 80, green = 136, blue = 72)
private val CLASS_COLOR = Color(red = 32, green = 72, blue = 152)

@Composable
fun CircularBorderChar(
    modifier: Modifier = Modifier,
    character: Char,
    color: Color
) {
    BasicText(
        text = character.toString(),
        style = TextStyle(
            textAlign = TextAlign.Center,
            color = color,
            fontFamily = FontFamily.Serif
        ),
        modifier = modifier
            .size(24.dp)
            .aspectRatio(1f)
            .border(width = 1.dp, color = color, shape = CircleShape)
            .padding(4.dp),
        autoSize = TextAutoSize.StepBased(
            maxFontSize = 20.sp
        )
    )
}


@Composable
fun MethodCircularChar() {
    CircularBorderChar(character = 'M', color = METHOD_COLOR)
}


@Composable
fun FieldCircularChar() {
    CircularBorderChar(character = 'F', color = FIELD_COLOR)
}


@Composable
fun InterfaceCircularChar() {
    CircularBorderChar(character = 'I', color = INTERFACE_COLOR)
}


@Composable
fun ClassCircularChar() {
    CircularBorderChar(character = 'C', color = CLASS_COLOR)
}


@Preview
@Composable
private fun CircularBorderCharPreview() {
    MaterialTheme {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            MethodCircularChar()
            FieldCircularChar()
            InterfaceCircularChar()
            ClassCircularChar()
        }
    }
}

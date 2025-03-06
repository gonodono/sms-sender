package com.gonodono.smssender.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.gonodono.smssender.model.Message

@Composable
internal fun ColumnHeaders() {
    val bold = TextStyle.Default.copy(fontWeight = FontWeight.Bold)
    CompositionLocalProvider(LocalTextStyle provides bold) {
        SimpleItem("ID", "Address", "Sent", "Delivery")
    }
}

@Composable
internal fun MessageItem(message: Message) =
    SimpleItem(
        message.id.toString(),
        message.address,
        message.sendStatus.toString(),
        message.deliveryStatus.toString()
    )

@Composable
private fun SimpleItem(vararg texts: String) = Row {
    texts.forEachIndexed { index, text ->
        Text(
            text = text,
            modifier = Modifier.weight(ColumnWeights[index]),
            textAlign = TextAlign.Center,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1
        )
    }
}

private val ColumnWeights = floatArrayOf(2F, 3F, 4F, 4F)
package com.thegreatyamori.saldos.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.thegreatyamori.saldos.MainActivity
import com.thegreatyamori.saldos.domain.Snapshot

class SquareBalanceWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = loadSnapshot(context)
        val hero = snapshot?.hero
        val accent = WidgetColors.accentFor(hero)
        val ring = hero?.let {
            RingBitmap.create(
                context = context,
                ratio = it.ratio,
                color = accent.toArgb(),
                trackColor = WidgetColors.Track.toArgb(),
                sizeDp = 64,
                strokeDp = 7f
            )
        }
        provideContent { SquareContent(snapshot, ring, accent) }
    }
}

@Composable
private fun SquareContent(
    snapshot: Snapshot?,
    ring: android.graphics.Bitmap?,
    accent: Color
) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(WidgetColors.Background)
            .cornerRadius(20.dp)
            .padding(6.dp)
            .clickable(actionStartActivity<MainActivity>()),
        verticalAlignment = Alignment.Vertical.CenterVertically,
        horizontalAlignment = Alignment.Horizontal.CenterHorizontally
    ) {
        if (snapshot == null || ring == null) {
            Text(
                text = "Saldos",
                style = TextStyle(
                    color = ColorProvider(WidgetColors.Text),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            return@Column
        }

        Box(
            modifier = GlanceModifier.size(64.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                provider = ImageProvider(ring),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = GlanceModifier.size(64.dp)
            )
            Text(
                text = snapshot.hero?.value ?: "--",
                style = TextStyle(
                    color = ColorProvider(accent),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = androidx.glance.text.TextAlign.Center
                )
            )
        }

        Spacer(GlanceModifier.height(2.dp))

        Text(
            text = snapshot.hero?.label ?: "Saldos",
            style = TextStyle(color = ColorProvider(WidgetColors.Text), fontSize = 10.sp),
            maxLines = 1
        )
    }
}

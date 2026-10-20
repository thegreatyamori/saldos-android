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
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.thegreatyamori.saldos.MainActivity
import com.thegreatyamori.saldos.domain.Snapshot
import com.thegreatyamori.saldos.util.timeAgo
import com.thegreatyamori.saldos.work.NetworkPolicy

class BalanceWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = loadSnapshot(context)
        val onWifi = NetworkPolicy.isOnWifi(context)
        val hero = snapshot?.hero
        val accent = WidgetColors.accentFor(hero)
        val ring = hero?.let {
            RingBitmap.create(
                context = context,
                ratio = it.ratio,
                color = accent.toArgb(),
                trackColor = WidgetColors.Track.toArgb()
            )
        }
        provideContent { WideContent(snapshot, ring, accent, onWifi) }
    }
}

@Composable
private fun WideContent(
    snapshot: Snapshot?,
    ring: android.graphics.Bitmap?,
    accent: Color,
    onWifi: Boolean
) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(WidgetColors.Background)
            .cornerRadius(22.dp)
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .clickable(actionStartActivity<MainActivity>()),
        verticalAlignment = Alignment.Vertical.CenterVertically
    ) {
        if (snapshot == null || ring == null) {
            Text(
                text = "Configurá Saldos",
                style = TextStyle(
                    color = ColorProvider(WidgetColors.Text),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Text(
                text = "Tocá para abrir",
                style = TextStyle(color = ColorProvider(WidgetColors.Dim), fontSize = 10.sp)
            )
            return@Column
        }

        Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
            Box(
                modifier = GlanceModifier.size(56.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    provider = ImageProvider(ring),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = GlanceModifier.size(56.dp)
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
            Spacer(GlanceModifier.width(10.dp))
            Column {
                Text(
                    text = snapshot.hero?.label ?: "Saldos",
                    style = TextStyle(
                        color = ColorProvider(WidgetColors.Text),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1
                )
                Spacer(GlanceModifier.height(2.dp))
                Text(
                    text = "de ${snapshot.hero?.total ?: "--"} ${snapshot.hero?.unit.orEmpty()}",
                    style = TextStyle(color = ColorProvider(WidgetColors.Dim), fontSize = 10.sp),
                    maxLines = 1
                )
                Spacer(GlanceModifier.height(2.dp))
                Text(
                    text = timeAgo(snapshot.fetchedAt) + if (onWifi) " · WiFi" else "",
                    style = TextStyle(color = ColorProvider(WidgetColors.Dim), fontSize = 9.sp),
                    maxLines = 1
                )
            }
        }
    }
}

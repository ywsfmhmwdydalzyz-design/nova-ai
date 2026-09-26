package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.AiStudioTab
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiStudioTopBar(
    onSettingsClick: () -> Unit,
    onInfoClick: () -> Unit
) {
    Surface(
        color = SurfaceDark,
        tonalElevation = 0.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderDark),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .border(1.5.dp, Brush.linearGradient(listOf(SecondaryCyan, PrimaryNeon)), CircleShape)
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.nova_ai_logo),
                            contentDescription = "شعار نوفا (Nova AI)",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "نوفا (Nova AI)",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = AccentEmerald.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "PRO",
                                    color = AccentEmerald,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }

                        Text(
                            text = "بوابتك الذكية للمحادثة، وتوليد الصور والفيديوهات",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onSettingsClick,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("header_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "الإعدادات والتفضيلات",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = onInfoClick,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("app_info_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "معلومات ودليل نوفا AI",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AiStudioBottomBar(
    currentTab: AiStudioTab,
    onTabSelected: (AiStudioTab) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier
            .windowInsetsPadding(WindowInsets.navigationBars)
            .testTag("bottom_nav_bar"),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        val chatSelected = currentTab == AiStudioTab.CHAT
        val imageSelected = currentTab == AiStudioTab.IMAGE
        val videoSelected = currentTab == AiStudioTab.VIDEO

        NavigationBarItem(
            selected = chatSelected,
            onClick = { onTabSelected(AiStudioTab.CHAT) },
            icon = {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Chat,
                    contentDescription = "المحادثة الذكية"
                )
            },
            label = {
                Text(
                    text = "المحادثة",
                    fontWeight = if (chatSelected) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 11.sp
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.White,
                selectedTextColor = PrimaryNeon,
                indicatorColor = PrimaryNeon,
                unselectedIconColor = TextSecondary,
                unselectedTextColor = TextMuted
            ),
            modifier = Modifier.testTag("nav_chat")
        )

        NavigationBarItem(
            selected = imageSelected,
            onClick = { onTabSelected(AiStudioTab.IMAGE) },
            icon = {
                Icon(
                    imageVector = Icons.Default.Image,
                    contentDescription = "توليد الصور"
                )
            },
            label = {
                Text(
                    text = "توليد الصور",
                    fontWeight = if (imageSelected) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 11.sp
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.White,
                selectedTextColor = SecondaryCyan,
                indicatorColor = SecondaryCyan,
                unselectedIconColor = TextSecondary,
                unselectedTextColor = TextMuted
            ),
            modifier = Modifier.testTag("nav_image")
        )

        NavigationBarItem(
            selected = videoSelected,
            onClick = { onTabSelected(AiStudioTab.VIDEO) },
            icon = {
                Icon(
                    imageVector = Icons.Default.Videocam,
                    contentDescription = "توليد الفيديو"
                )
            },
            label = {
                Text(
                    text = "توليد الفيديو",
                    fontWeight = if (videoSelected) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 11.sp
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.Black,
                selectedTextColor = AccentEmerald,
                indicatorColor = AccentEmerald,
                unselectedIconColor = TextSecondary,
                unselectedTextColor = TextMuted
            ),
            modifier = Modifier.testTag("nav_video")
        )
    }
}

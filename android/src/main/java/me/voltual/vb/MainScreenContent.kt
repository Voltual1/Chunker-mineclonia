package me.voltual.vb

import androidx.compose.foundation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.unit.dp
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.window.core.layout.WindowWidthSizeClass
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.*
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.NavBackStack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject
import me.voltual.vb.KtorClient
import me.voltual.vb.data.UpdateInfo
import me.voltual.vb.data.UpdateSettingsDataStore
import me.voltual.vb.data.UserAgreementDataStore
import me.voltual.vb.core.utils.UpdateCheckResult
import me.voltual.vb.core.utils.UpdateChecker
import me.voltual.vb.core.ui.theme.*
import me.voltual.vb.core.ui.theme.ThemeCustomizeScreen
import me.voltual.vb.core.ui.components.UserAgreementDialog
import me.voltual.vb.core.ui.components.UpdateDialog
import me.voltual.vb.core.ui.animation.*
import me.voltual.vb.ui.*

val topLevelRoutes: Set<NavKey> = setOf(Home)

@Composable
fun PyrolysisApp(
    agreementDataStore: UserAgreementDataStore = koinInject(),
    modifier: Modifier = Modifier,
    platformEntryProvider: @Composable (NavKey, Navigator) -> (@Composable () -> Unit)? = { _, _ -> null }
) {
    val navigationState = rememberNavigationState(
        startRoute = Home,
        topLevelRoutes = topLevelRoutes
    )
    val focusManager = LocalFocusManager.current
    val topAppBarController = remember { TopAppBarController() }
    val navigator = remember(focusManager, topAppBarController, navigationState) {
        Navigator(navigationState, focusManager, topAppBarController)
    }

    CompositionLocalProvider(
        LocalNavigator provides navigator,
        LocalNavigationState provides navigationState,
        LocalTopAppBarController provides topAppBarController,
    ) {
        val snackbarHostState = remember { SnackbarHostState() }

        val userAccepted by agreementDataStore.isUserAgreementAccepted.collectAsState(initial = true)

        var isAgreementDataLoaded by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            delay(150)
            isAgreementDataLoaded = true
        }

        val showAgreementDialog = isAgreementDataLoaded && !(userAccepted)

        BBQTheme() {
            MainScreenContent(
                navigationState = navigationState,
                navigator = navigator,
                snackbarHostState = snackbarHostState,
                showAgreementDialog = showAgreementDialog,
                platformEntryProvider = platformEntryProvider
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun MainScreenContent(
    navigationState: NavigationState,
    navigator: Navigator,
    snackbarHostState: SnackbarHostState,
    showAgreementDialog: Boolean,
    platformEntryProvider: @Composable (NavKey, Navigator) -> (@Composable () -> Unit)?
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val currentRoute = navigationState.currentRoute
    val currentTopLevelRoute = navigationState.topLevelRoute

    val showBackButton = remember(currentRoute) {
        currentRoute != Home 
    }

    val topAppBarController = LocalTopAppBarController.current

    // Adaptive感知：只要设备的宽度超过手机(COMPACT)，无论是折叠屏还是平板，均触发大屏常驻中控模式
    val adaptiveInfo = currentWindowAdaptiveInfo()
    val isExpandedScreen = adaptiveInfo.windowSizeClass.windowWidthSizeClass != WindowWidthSizeClass.COMPACT

    // 抽离统一的侧边栏战术内容
    val innerDrawerContent = @Composable {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(300.dp)
                .roundScreenPadding()
                .background(MaterialTheme.colorScheme.surface) 
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )
        ) {                    
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = "VECTOR // TERMINAL",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Black,
                        letterSpacing = androidx.compose.ui.unit.TextUnit.Unspecified
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

            NavigationDrawerItems(
                navigator = navigator,
                currentTopLevelRoute = currentTopLevelRoute,
                drawerState = if (isExpandedScreen) null else drawerState,
                scope = scope
            )
        }
    }

    // 抽离统一的右侧视窗
    val scaffoldContent = @Composable {
        Scaffold(
            topBar = {
                Column {
                    TopAppBar(
                        title = {
                            Text(
                                text = topAppBarController.customTitle ?: getTitleForDestination(currentRoute),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                )
                            )
                        },
                        navigationIcon = {
                            if (showBackButton) {
                                IconButton(onClick = { navigator.goBack() }) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "返回",
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            } else if (!isExpandedScreen) {
                                // 仅在紧凑屏幕（手机）下显示用于唤出侧边栏的汉堡菜单
                                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                    Icon(
                                        imageVector = Icons.Default.Menu,
                                        contentDescription = "打开菜单",
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        },
                        actions = {
                            topAppBarController.actions.forEach { action ->
                                IconButton(onClick = action.onClick) {
                                    action.icon(action.tint?.invoke() ?: MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            titleContentColor = MaterialTheme.colorScheme.onSurface
                        )
                    )                    
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), thickness = 1.dp)
                }
            },
            snackbarHost = { BBQSnackbarHost(hostState = snackbarHostState) },
            content = { innerPadding ->
                val contentPadding = innerPadding

                val currentBackStack = navigationState.backStacks[currentTopLevelRoute]
                    ?: navigationState.backStacks[navigationState.startRoute]!!

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(contentPadding)
                        .roundScreenPadding()
                ) {
                    BBQNavDisplay(
                        backStack = currentBackStack,
                        onBack = { navigator.goBack() },
                        snackbarHostState = snackbarHostState,
                        modifier = Modifier.fillMaxSize(),
                        platformEntryProvider = { key ->
                            platformEntryProvider(key, navigator)
                        }
                    )

                    if (showAgreementDialog) {
                        UserAgreementDialog(
                            onAgreed = { },
                        )
                    }

                    CheckForUpdates(snackbarHostState)
                }
            }
        )
    }

    // 根据自适应阈值分发给符合 M3 规范的不同 Drawer 容器
    if (isExpandedScreen) {
        PermanentNavigationDrawer(
            drawerContent = {
                // 使用官方 Sheet 包裹，但将色彩重置为透明，以保留战术边框风格
                PermanentDrawerSheet(
                    drawerContainerColor = Color.Transparent,
                    drawerShape = AppShapes.small,
                    drawerTonalElevation = 0.dp
                ) {
                    innerDrawerContent()
                }
            },
            modifier = Modifier.fillMaxSize()
        ) {
            scaffoldContent()
        }
    } else {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = { 
                ModalDrawerSheet(
                    drawerContainerColor = Color.Transparent,
                    drawerShape = AppShapes.small,
                    drawerTonalElevation = 0.dp
                ) {
                    innerDrawerContent()
                }
            },
            gesturesEnabled = true,
            modifier = Modifier.fillMaxSize()
        ) {
            scaffoldContent()
        }
    }
}

@Composable
fun getTitleForDestination(route: NavKey?): String {
    return when (route) {
        Home -> "主页 SYSTEM"
        ThemeCustomize -> "主题定制 THEME"
        UpdateSettings -> "更新设置 UPDATE"
        is TerminalExec -> "终端 EXEC"
        FtpSettings -> "世界中转 FTP"
        LogViewer -> "日志 LOG" 
        CacheSettings -> "缓存设置 CACHE" 
        Export -> "导出 EXPORT" 
        ChunkerSettings -> "转换设置 CHUNKER"
        PackConverterDest -> "材质包转换 PACK"
        DecoderDest -> "存档还原 DECODER"
        is MapPreviewDest -> "地图预览 MAP"
        is StitchDest -> "存档缝合 STITCH"
        else -> "SYSTEM // $route"
    }
}

@Composable
fun CheckForUpdates(snackbarHostState: SnackbarHostState) {
    val coroutineScope = rememberCoroutineScope()
    var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
    val updateSettingsDataStore: UpdateSettingsDataStore = koinInject()

    var showDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val autoCheckUpdates = updateSettingsDataStore.autoCheckUpdates.first()
        if (autoCheckUpdates) {
            UpdateChecker.checkForUpdates { result ->
                when (result) {
                    is UpdateCheckResult.Success -> {
                        updateInfo = result.updateInfo
                        showDialog = true
                    }
                    is UpdateCheckResult.NoUpdate -> {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("当前已是最新版本")
                        }
                    }
                    is UpdateCheckResult.Error -> {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(result.message)
                        }
                    }
                }
            }
        }
    }

    updateInfo?.let { info ->
        if (showDialog) {
            UpdateDialog(updateInfo = info) {
                showDialog = false
                updateInfo = null
            }
        }
    }
}
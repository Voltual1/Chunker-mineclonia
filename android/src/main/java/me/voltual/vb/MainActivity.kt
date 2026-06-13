// Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
// （或任意更新的版本）的条款重新分发和/或修改它。
// 本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.
package me.voltual.vb

import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.util.DisplayMetrics
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import me.voltual.vb.core.database.entity.LogEntry
import me.voltual.vb.core.ui.components.UpdateDialog
import me.voltual.vb.core.ui.components.UserAgreementDialog
import me.voltual.vb.core.ui.theme.*
import me.voltual.vb.core.utils.UpdateCheckResult
import me.voltual.vb.core.utils.UpdateChecker
import me.voltual.vb.data.UpdateInfo
import me.voltual.vb.data.UpdateSettingsDataStore
import me.voltual.vb.data.UserAgreementDataStore
import me.voltual.vb.ui.*
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {
  private val agreementDataStore: UserAgreementDataStore by inject()

  override fun onCreate(savedInstanceState: Bundle?) {
    setTheme(R.style.Theme_BBQ_Main)
    super.onCreate(savedInstanceState)

    if (ThemeColorStore.loadCustomDpiEnabled(this)) {
      applyDpiAndFontScale(this)
    }

    setContent {
      val navigationState =
        rememberNavigationState(startRoute = Home, topLevelRoutes = topLevelRoutes)

      val view = LocalView.current // 获取承载 Compose 的原生 View

      val topAppBarController = remember { TopAppBarController() }

      val navigator =
        remember(navigationState, view) {
          // 传入控制器
          Navigator(navigationState, view, topAppBarController)
        }

      CompositionLocalProvider(
        LocalNavigator provides navigator,
        LocalNavigationState provides navigationState,
        LocalTopAppBarController provides topAppBarController,
      ) {
        val snackbarHostState = remember { SnackbarHostState() }
        val context = LocalContext.current
        val scope = rememberCoroutineScope()

        // 协议状态监听
        val userAccepted by
          agreementDataStore.isUserAgreementAccepted.collectAsState(initial = true)
        var isAgreementDataLoaded by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
          delay(150)
          isAgreementDataLoaded = true
        }

        val showAgreementDialog = isAgreementDataLoaded && !(userAccepted)

        BBQTheme(appDarkTheme = ThemeManager.isAppDarkTheme) {
          MainScreenContent(
            navigationState = navigationState,
            navigator = navigator,
            snackbarHostState = snackbarHostState,
            showAgreementDialog = showAgreementDialog,
            onAgreementDismiss = { finish() },
          )
        }
      }
    }
  }

  init {
    Thread.setDefaultUncaughtExceptionHandler { _, throwable ->
      val crashReport = getCrashReport(throwable)
      CoroutineScope(Dispatchers.IO)
        .launch {
          val logEntry =
            LogEntry(
              type = "CRASH",
              requestBody = "MainActivity 崩溃",
              responseBody = crashReport,
              status = "FAILURE",
            )
          BBQApplication.instance.database.logDao().insert(logEntry)
        }
        .invokeOnCompletion {
          CrashLogActivity.start(BBQApplication.instance, crashReport)
          android.os.Process.killProcess(android.os.Process.myPid())
        }
    }
  }

  private fun getCrashReport(throwable: Throwable): String {
    val stackTrace = throwable.stackTraceToString()
    val deviceInfo =
      """
            设备型号: ${android.os.Build.MODEL}
            Android 版本: ${android.os.Build.VERSION.RELEASE}
            App 版本: ${BuildConfig.VERSION_NAME}
        """
        .trimIndent()
    return """
            崩溃信息: ${throwable.message}

            设备信息:
            $deviceInfo

            堆栈跟踪:
            $stackTrace
        """
      .trimIndent()
  }

  @Suppress("DEPRECATION")
  private fun applyDpiAndFontScale(context: Context) {
    val dpi = ThemeColorStore.loadDpi(context)
    val fontScale = ThemeColorStore.loadFontSize(context)
    val resources = context.resources
    val configuration = Configuration(resources.configuration)
    val metrics = DisplayMetrics()
    val windowManager =
      context.getSystemService(Context.WINDOW_SERVICE) as android.view.WindowManager
    windowManager.defaultDisplay.getMetrics(metrics)
    val newDensityDpi = (dpi * DisplayMetrics.DENSITY_DEFAULT).toInt()
    configuration.densityDpi = newDensityDpi
    configuration.fontScale = fontScale
    metrics.densityDpi = newDensityDpi
    resources.updateConfiguration(configuration, metrics)
  }

  /** 定义所有顶层路由（对应抽屉中独立返回堆栈的页面） */
  val topLevelRoutes: Set<NavKey> = setOf(Home, ThemeCustomize)

  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  fun MainScreenContent(
    navigationState: NavigationState,
    navigator: Navigator,
    snackbarHostState: SnackbarHostState,
    showAgreementDialog: Boolean,
    onAgreementDismiss: () -> Unit,
  ) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val currentRoute = navigationState.currentRoute
    val currentTopLevelRoute = navigationState.topLevelRoute

    val showBackButton = remember(currentRoute) { currentRoute != Home }

    val useDarkTheme = ThemeManager.isAppDarkTheme
    val lightBgUri by
      ThemeColorStore.getDrawerHeaderLightBackgroundUriFlow(context).collectAsState(initial = null)
    val darkBgUri by
      ThemeColorStore.getDrawerHeaderDarkBackgroundUriFlow(context).collectAsState(initial = null)
    val drawerHeaderBackgroundUri = if (useDarkTheme) darkBgUri else lightBgUri

    val isLoggedIn = remember { mutableStateOf(false) }

    val topAppBarController = LocalTopAppBarController.current

    ModalNavigationDrawer(
      drawerState = drawerState,
      drawerContent = {
        Box(modifier = Modifier.width(360.dp)) {
          Column(
            modifier = Modifier.fillMaxHeight().background(MaterialTheme.colorScheme.surfaceVariant)
          ) {
            DrawerHeader(
              modifier = Modifier.fillMaxWidth().height(180.dp),
              backgroundUri = drawerHeaderBackgroundUri,
            )
            NavigationDrawerItems(
              navigator = navigator,
              currentTopLevelRoute = currentTopLevelRoute,
              drawerState = drawerState,
              scope = scope,
            )
          }
        }
      },
      gesturesEnabled = true,
      modifier = Modifier.fillMaxSize(),
    ) {
      Scaffold(
        topBar = {
          TopAppBar(
            title = {
              Text(
                text = getTitleForDestination(currentRoute),
                color = MaterialTheme.colorScheme.onSurface,
              )
            },
            navigationIcon = {
              if (showBackButton) {
                IconButton(onClick = { navigator.goBack() }) {
                  Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                    tint = MaterialTheme.colorScheme.onSurface,
                  )
                }
              } else {
                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                  Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = stringResource(R.string.open_drawer),
                    tint = MaterialTheme.colorScheme.onSurface,
                  )
                }
              }
            },
            actions = {
              // 动态渲染来自子页面的按钮
              topAppBarController.actions.forEach { action ->
                IconButton(onClick = action.onClick) {
                  Icon(
                    imageVector = action.icon,
                    contentDescription = action.description,
                    tint = action.tint?.invoke() ?: LocalContentColor.current,
                  )
                }
              }
            },
            colors =
              TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
              ),
          )
        },
        snackbarHost = { BBQSnackbarHost(hostState = snackbarHostState) },
        content = { innerPadding ->
          val currentBackStack =
            navigationState.backStacks[currentTopLevelRoute]
              ?: navigationState.backStacks[navigationState.startRoute]!!

          Box(modifier = Modifier.padding(innerPadding).roundScreenPadding()) {
            BBQNavDisplay(
              backStack = currentBackStack,
              onBack = { navigator.goBack() },
              snackbarHostState = snackbarHostState,
              modifier = Modifier.fillMaxSize(),
            )

            if (showAgreementDialog) {
              UserAgreementDialog(
                onAgreed = { /* 已在 Dialog 内部处理 */ },
                onDismissRequest = onAgreementDismiss,
              )
            }

            CheckForUpdates(snackbarHostState)
          }
        },
      )
    }
  }

  @Composable
  fun CheckForUpdates(snackbarHostState: SnackbarHostState) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
    var showDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
      val autoCheckUpdates = UpdateSettingsDataStore.autoCheckUpdates.first()
      if (autoCheckUpdates) {
        UpdateChecker.checkForUpdates(context) { result ->
          when (result) {
            is UpdateCheckResult.Success -> {
              updateInfo = result.updateInfo
              showDialog = true
            }
            is UpdateCheckResult.NoUpdate -> {
              coroutineScope.launch { snackbarHostState.showSnackbar("当前已是最新版本") }
            }
            is UpdateCheckResult.Error -> {
              coroutineScope.launch { snackbarHostState.showSnackbar(result.message ?: "检查更新失败") }
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
}

fun restartMainActivity(context: Context) {
  val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
  intent?.let {
    it.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
    val options =
      ActivityOptions.makeCustomAnimation(context, android.R.anim.fade_in, android.R.anim.fade_out)
    context.startActivity(it, options.toBundle())
  }
}

@Composable
fun getTitleForDestination(route: NavKey?): String {
  return when (route) {
    Home -> "主页"
    ThemeCustomize -> "主题定制"
    UpdateSettings -> "更新设置"
    else -> "在~ $route ~里~哦"
  }
}

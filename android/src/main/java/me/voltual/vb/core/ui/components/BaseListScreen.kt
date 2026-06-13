// Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
// （或任意更新的版本）的条款重新分发和/或修改它。
// 本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.

package me.voltual.vb.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun <T> BaseListScreen(
  items: List<T>,
  isLoading: Boolean,
  error: String?,
  currentPage: Int,
  totalPages: Int,
  onRetry: () -> Unit,
  onLoadMore: () -> Unit,
  emptyMessage: String,
  itemContent: @Composable (T) -> Unit,
  autoLoadMode: Boolean = false,
  modifier: Modifier = Modifier,
) {
  val snackbarHostState = remember { SnackbarHostState() }
  val coroutineScope = rememberCoroutineScope()
  val listState = rememberLazyListState()

  if (autoLoadMode) {
    val shouldLoadMore by remember {
      derivedStateOf {
        val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()
        val totalItems = listState.layoutInfo.totalItemsCount
        lastVisibleItem?.let { it.index >= totalItems - 3 } ?: false
      }
    }

    LaunchedEffect(shouldLoadMore) {
      if (shouldLoadMore && !isLoading && currentPage < totalPages) {
        onLoadMore()
      }
    }
  }

  Box(modifier = modifier.fillMaxSize()) {
    when {
      isLoading && currentPage == 1 -> LoadingIndicator()
      error != null -> ErrorState(error, onRetry)
      items.isEmpty() -> EmptyState(emptyMessage)
      else ->
        ContentList(
          items,
          currentPage,
          totalPages,
          onLoadMore,
          itemContent,
          listState,
          autoLoadMode,
        )
    }

    if (isLoading && autoLoadMode && currentPage > 1) {
      CircularProgressIndicator(modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp))
    }
  }

  error?.let {
    LaunchedEffect(error) {
      coroutineScope.launch {
        snackbarHostState.showSnackbar(message = error, duration = SnackbarDuration.Short)
      }
    }
  }
}

@Composable
private fun LoadingIndicator() {
  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    CircularProgressIndicator()
  }
}

@Composable
private fun ErrorState(error: String, onRetry: () -> Unit) {
  Column(
    modifier = Modifier.fillMaxSize().padding(16.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
  ) {
    Text(
      text = "加载失败",
      style = MaterialTheme.typography.titleMedium,
      color = MaterialTheme.colorScheme.error,
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
      text = error,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onErrorContainer,
    )
    Spacer(modifier = Modifier.height(16.dp))
    Button(onClick = onRetry) { Text("重试") }
  }
}

@Composable
private fun EmptyState(message: String) {
  Box(
    modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = message,
      style = MaterialTheme.typography.titleMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

@Composable
private fun <T> ContentList(
  items: List<T>,
  currentPage: Int,
  totalPages: Int,
  onLoadMore: () -> Unit,
  itemContent: @Composable (T) -> Unit,
  listState: LazyListState,
  autoLoadMode: Boolean,
) {
  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp), // 添加内容padding
    state = listState,
  ) {
    items(items) { item -> itemContent(item) }

    if (!autoLoadMode) {
      item {
        if (currentPage < totalPages) {
          LoadMoreButton(onLoadMore)
        }

        PageInfo(currentPage, totalPages)
      }
    } else {
      item { AutoLoadPageInfo(currentPage, totalPages) }
    }
  }
}

@Composable
private fun LoadMoreButton(onClick: () -> Unit) {
  Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
    OutlinedButton(onClick = onClick) { Text("加载更多") }
  }
}

@Composable
private fun PageInfo(currentPage: Int, totalPages: Int) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(8.dp),
    horizontalArrangement = Arrangement.Center,
  ) {
    Text(
      text = "第 $currentPage 页/共 $totalPages 页",
      style = MaterialTheme.typography.labelMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

@Composable
private fun AutoLoadPageInfo(currentPage: Int, totalPages: Int) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
    horizontalArrangement = Arrangement.Center,
  ) {
    if (currentPage < totalPages) {
      Text(
        text = "别急……已经在加载了！",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
      )
    } else {
      Text(
        text = "真的……没有啦！",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

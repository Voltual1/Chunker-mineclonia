<div align="center">
  <img src="icon.png" alt="Vector-Breakthrough" width="128">
  <h1>Vector-Breakthrough (VB)</h1>
  <p><b>全网首个真正安卓原生 Minecraft 世界转换器</b></p>
  <p>直接集成 Chunker 实现</p>
</div>

---

#### 1. 开源协议声明

**本应用是自由软件**，遵循 **GNU Affero General Public License v3.0 (AGPLv3)** 协议发布。这意味着您拥有以下自由：

- **运行** 程序的自由，无论出于何种目的。
- **研究** 程序工作原理并按需修改的自由。
- **重新分发** 副本的自由。
- **改进** 程序并向公众发布改进版的自由。

> ⚠️ **请注意：** AGPLv3 协议具有传染性。任何基于本项目修改、衍生或组合的代码，无论是以何种形式（包括网络服务、独立 App），均必须保持开源并同样使用 AGPLv3 协议。
> 完整的 AGPLv3 协议文本可访问：[http://www.gnu.org/licenses/agpl-3.0.html](http://www.gnu.org/licenses/agpl-3.0.html)

---

### 核心特点

- **真正原生**：直接在 Android 应用层移植并集成 Chunker Java 代码，利用现代 Android 组件（Jetpack Compose + RemoteCoroutineWorker）实现全异步转换。无需依赖 Termux，摆脱复杂的终端套壳和权限环境。
- **专为移动端优化**：针对大世界存档，引入了 WorkManager 分片处理、进度持久化以及严格的后台内存监控，在移动端有限的硬件资源下提供最高的稳定性。
- **代码完全透明**：仓库内包含完整构建所需文件，所有核心逻辑、转换细节均公开可审计，拒绝任何形式的“黑盒”操作。

### 为什么选择 Vector-Breakthrough？

近年来，安卓端 MC 世界转换的需求日益增长，社区也涌现出了许多优秀的解决方案。我们认可并尊重每一位为移动端生态投入热情的开发者。

但我们认为，**“原生”二字不应被稀释。**

Vector-Breakthrough 选择了更艰难但更彻底的底层重构道路。直接重写底层适配带来的好处是显而易见的：我们能够深度介入 Chunker 的生命周期，在**后台持久执行、断点续传、内存控制**上拥有完全的控制力。

**Talk is cheap. Show me the code.** 

我们坚信代码是检验技术的唯一标准。开源世界本就包容多元，如果你更喜欢其他项目的实现风格，我们也由衷地为多一个选择而高兴；而我们，将继续专注于用技术把移动端原生的体验做到极致。

---

### 鸣谢

感谢以下开源项目及贡献者（按字母顺序排列），正是站在巨人的肩膀上，VB 才成为可能：

- [andob/android-awt](https://github.com/andob/android-awt)
- [0pen1/android-sqlite3](https://github.com/0pen1/android-sqlite3)
- [eltanschauung/MC2MT](https://github.com/eltanschauung/MC2MT)
- [GeyserMC/PackConverter](https://github.com/GeyserMC/PackConverter)
- [HiveGamesOSS/Chunker](https://github.com/HiveGamesOSS/Chunker)
- [HiveGamesOSS/leveldb-mcpe-java](https://github.com/HiveGamesOSS/leveldb-mcpe-java)
- [ivancesaridev/json_viewer](https://github.com/ivancesaridev/json_viewer)
- [lizhangqu/retrace](https://github.com/lizhangqu/retrace)
- [oO0oO0oO0o0o00/blocktopograph](https://github.com/oO0oO0oO0o0o00/blocktopograph)
- [openjdk/jdk (ICC profiles)](https://github.com/openjdk/jdk/tree/9333d300aa02831ab78178449f04a4703a0b2082/src/java.desktop/share/classes/sun/java2d/cmm/profiles)
- [rikkahub/rikkahub](https://github.com/rikkahub/rikkahub)
- [rollerozxa/MC2MT](https://github.com/rollerozxa/MC2MT)
- [rust-keylock/rust-keylock-android](https://github.com/rust-keylock/rust-keylock-android)
- [termux/termux-app](https://github.com/termux/termux-app)
- [wolpi/prim-ftpd](https://github.com/wolpi/prim-ftpd)

> 💡 如果您发现了应该鸣谢但此处并未列出的项目，欢迎随时提交 Pull Request 提醒我们！

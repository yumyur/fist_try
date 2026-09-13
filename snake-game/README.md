# Snake（贪吃蛇）— Java Swing 版

一个用 Java 写的经典贪吃蛇小游戏。**零第三方依赖**，只用到 JDK 自带的 Swing / AWT，
单个源文件即可运行。

![Java](https://img.shields.io/badge/Java-11%2B-orange)
![License](https://img.shields.io/badge/license-MIT-blue)

---

## 功能特性

- 经典玩法：吃食物变长、撞墙或咬到自己就结束
- 键盘操作：方向键 或 WASD，空格开始/暂停，P 暂停，R 重开
- 分数越高速度越快（从 130ms/步 逐渐加快到最低 55ms/步）
- 最高分自动保存在 `~/.snake-highscore`，下次打开还在
- 手绘风格渲染：渐变蛇身、会眨的蛇眼、会「呼吸」的食物、棋盘格场地
- 开始 / 暂停 / 结束三种状态都有遮罩提示
- 支持连续转向缓冲（不会因为手速快而丢按键）

## 环境要求

- JDK 11 或更高版本（JDK 17 / 21 都可以）——需要的是 JDK（含 `javac`），JRE 不够
- 任意桌面系统：Windows / macOS / Linux

检查是否安装：

```bash
java -version
javac -version
```

如果提示找不到命令，先安装 JDK，例如 Windows 上：

```powershell
winget install Microsoft.OpenJDK.21
```

## 运行方式

### 方式一：单文件直接运行（最简单，JDK 11+）

```bash
java SnakeGame.java
```

### 方式二：编译后运行

```bash
javac -encoding UTF-8 -d build SnakeGame.java
java -cp build SnakeGame
```

### 方式三：用附带的脚本

| 系统 | 命令 |
| --- | --- |
| Windows（cmd） | `run.bat` |
| Windows（PowerShell） | `.\run.ps1` |
| macOS / Linux | `bash run.sh` |

## 操作说明

| 按键 | 作用 |
| --- | --- |
| `↑` `↓` `←` `→` 或 `W` `A` `S` `D` | 控制方向 |
| `空格` | 开始游戏 / 继续 / 结束后重新开始 |
| `P` | 暂停 / 继续 |
| `R` | 重新开始一局 |

## 计分规则

- 每吃到一个食物 **+1 分**，蛇身 +1 格
- 每得 1 分，每步的时间减少 3ms，最低 55ms
- 最高分跨会话保留，显示在右上角 `BEST`

## 项目结构

```
snake-game/
├── SnakeGame.java   # 全部游戏逻辑与渲染（单文件）
├── run.bat          # Windows 运行脚本
├── run.ps1          # PowerShell 运行脚本
├── run.sh           # macOS / Linux 运行脚本
├── LICENSE          # MIT 许可证
└── README.md
```

代码结构一览（都在 `SnakeGame.java` 里）：

- `Dir` / `State` —— 方向枚举与游戏状态（就绪 / 进行中 / 暂停 / 结束）
- `newGame()` / `placeFood()` —— 初始化与食物随机生成（不会落在蛇身上）
- `handleKey()` / `queueDir()` —— 输入处理与转向缓冲，禁止 180° 掉头
- `step()` —— 每一步的移动、吃食、碰撞判定（撞墙 / 咬到自己）
- `paintComponent()` —— 全部绘制，包括场地、食物、蛇身、HUD 与遮罩

## 自定义

想改难度或画面，直接改 `SnakeGame.java` 顶部的常量即可：

```java
private static final int COLS = 30;              // 横向格子数
private static final int ROWS = 24;              // 纵向格子数
private static final int CELL = 24;              // 每格像素
private static final int BASE_DELAY_MS = 130;    // 初始速度（越小越快）
private static final int MIN_DELAY_MS = 55;      // 最快速度上限
private static final int SPEEDUP_PER_POINT_MS = 3; // 每分加速多少毫秒
```

## English

A classic Snake game in plain Java (Swing), no third-party dependencies, single source file.

```bash
java SnakeGame.java          # JDK 11+ single-file launch
# or
javac -encoding UTF-8 -d build SnakeGame.java && java -cp build SnakeGame
```

**Controls:** arrows or WASD to steer, `SPACE` to start/pause, `P` to pause, `R` to restart.
**Scoring:** +1 per food, the game speeds up as you score, high score is kept in `~/.snake-highscore`.

## License

[MIT](LICENSE)

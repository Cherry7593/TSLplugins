# Config.yml YAML 语法错误修复

## 问题描述
重载插件时出现 YAML 解析错误：
```
org.yaml.snakeyaml.scanner.ScannerException: while scanning for the next token
found character '%' that cannot start any token. (Do not use % for indentation)
 in 'reader', line 290, column 7:
          %prefix%&e使用方法:
```

## 根本原因

在 YAML 中，使用 `|-` 多行字符串时，如果第一行以 `%` 开头，会被解析器误认为是 YAML 指令，导致语法错误。

### 错误的写法
```yaml
usage: |-
  %prefix%&e使用方法:
  &7第一行
  &7第二行
```

**问题**：`%prefix%` 在行首，YAML 解析器认为这是一个指令（如 `%YAML`）

## 解决方案

将多行字符串改为单行，使用 `\n` 表示换行：

```yaml
usage: "%prefix%&e使用方法:\n&7第一行\n&7第二行"
```

**优势**：
- ✅ 避免 YAML 语法错误
- ✅ 明确的字符串边界（使用引号）
- ✅ 仍然支持换行（通过 `\n`）

## 修复的位置

### 1. toss.messages.usage（第 290 行左右）
**修复前**：
```yaml
usage: |-
  %prefix%&e使用方法:
  &7/tsl toss toggle &f- 切换举起功能
  &7/tsl toss velocity <数值> &f- 设置投掷速度
  &7Shift + 右键生物 &f- 举起生物
  &7右键空气 &f- 投掷生物
  &7Shift + 右键空气 &f- 放下所有生物
```

**修复后**：
```yaml
usage: "%prefix%&e使用方法:\n&7/tsl toss toggle &f- 切换举起功能\n&7/tsl toss velocity <数值> &f- 设置投掷速度\n&7Shift + 右键生物 &f- 举起生物\n&7右键空气 &f- 投掷生物\n&7Shift + 右键空气 &f- 放下所有生物"
```

### 2. freeze.messages.usage（第 340 行左右）
**修复前**：
```yaml
usage: |-
  %prefix%&e使用方法:
  &7/tsl freeze <玩家> [时间] &f- 切换冻结状态（自动冻结/解冻）
  &7/tsl freeze list &f- 列出被冻结的玩家
  &8提示: 如果玩家已冻结则解冻，未冻结则冻结
```

**修复后**：
```yaml
usage: "%prefix%&e使用方法:\n&7/tsl freeze <玩家> [时间] &f- 切换冻结状态（自动冻结/解冻）\n&7/tsl freeze list &f- 列出被冻结的玩家\n&8提示: 如果玩家已冻结则解冻，未冻结则冻结"
```

## 验证

修复后，`/tsl reload` 命令应该能正常执行，不再出现配置文件加载错误。

## 注意事项

### YAML 多行字符串的坑

1. **不要在 `|-` 多行字符串的第一行使用 `%`**
   ```yaml
   # ❌ 错误
   message: |-
     %prefix%内容
   
   # ✅ 正确
   message: "%prefix%内容"
   ```

2. **使用引号明确字符串边界**
   ```yaml
   # ✅ 推荐：明确的字符串
   message: "内容"
   
   # ⚠️ 可能有问题：没有引号
   message: 内容
   ```

3. **特殊字符需要引号**
   - 以 `%` 开头
   - 以 `@` 开头
   - 包含 `:` 
   - 包含 `#`
   - 包含 `&` 后跟字母（如 `&a`）

   **解决办法**：给整个字符串加上双引号

## 其他类似问题检查

已检查配置文件中所有 `usage` 字段，确认没有其他类似问题：
- ✅ scale.messages.usage - 单行格式
- ✅ ride.messages.usage - 单行格式
- ✅ toss.messages.usage - 已修复
- ✅ freeze.messages.usage - 已修复

## 修复完成

配置文件语法错误已全部修复，可以正常重载！✅


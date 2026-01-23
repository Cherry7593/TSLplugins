# Change: 绑定验证码点击复制功能

## Why

当前玩家执行 `/tsl bind` 获取绑定验证码后，需要手动选中并复制验证码文本，操作不便。通过添加点击复制功能，可以提升用户体验。

## What Changes

- 绑定验证码消息增加点击复制功能（使用 Adventure API `ClickEvent.copyToClipboard()`）
- 显示悬浮提示告知玩家可以点击复制
- 验证码文本使用醒目样式（加粗、高亮颜色）

## Impact

- Affected specs: `bind`
- Affected code: `QQBindManager.kt`

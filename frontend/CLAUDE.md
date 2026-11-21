\# 项目约束（赛博朋克 UI 改造）



\## 唯一规范

UI 改造的规范文件是 `docs/design/cyberpunk-restyle-playbook.md`。

它与任何其他建议（包括 .claude/skills 里的技能）冲突时，以该手册为准。



\## 事实

\- ant-design-vue 4.2.6：Table/Button/Card/Modal/Tag 的 ComponentToken 是空接口，颜色只能靠全局 token。

\- 主题入口：src/theme/antdTheme.ts，由 src/App.vue 的 <a-config-provider> 注入。

\- 样式变量：src/styles/tokens.css（--cp-\*）。

\- 全局静态 message 不读 ConfigProvider，必须用 App.useApp()。



\## 禁止

\- 禁止改业务逻辑、接口调用、路由、文案、组件 props、事件处理。

\- 禁止用 !important 覆盖 .ant-\* 类名（prefers-reduced-motion 例外）。

\- 禁止新增依赖；禁止一次性修改超过 3 个文件（除非我明确说可以）。

\- 手册与实际代码不一致时：停下来告诉我，不要自行发挥。



\## 每轮完成标准

1\) npm run type-check 与 npm run build-only 通过，贴出完整输出；

2\) 输出「改动文件 / 每处改了什么 / 哪些地方没吃到主题 / 需要我人工确认的点」；

3\) 一个阶段只做一件事，做完停下等我确认。


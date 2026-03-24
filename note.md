# 复现笔记
```aiignore
note / read / update / status / exec
skill / entity / archive
introspect / observe / analytics
growup / evolution / dream
context / overview / briefing
tools / resources / prompts / dynamic skills
```
### TemplateBootstrapService
将所有的md 以及memory迁移到 用户目录下，如果不存在则创建

### McpServerConfig
将读取上下文和追加日志封装为MCP工具

### ExecService
注册指令运行工具，通过client传来的字段确定 执行的命令


```
@Tool 适合暴露动作
SyncResourceSpecification 适合暴露只读上下文
```

### Skill
第一步需要支持读取 MiniClaw 的Skill 目录下的所有skill技能
第二步 可执行技能 和自动把技能变成动态工具


### 当前文件已改动成功，但动态工具/资源要重启服务后才刷新

## 四大核心组件
```aiignore
1.Core memory
2.State & heartbeat
3.Entity graph
4.Skill system

```

## 文件变更追踪 和 使用分析
```aiignore
哪些工具被调用得最多
哪些文件改动最频繁
最近一次活动是什么时候
一天里哪个小时最活跃

MiniClawAnalytics
AnalyticsStoreService 
```

### 短期日志归档
```aiignore
Archive
note → observe/introspect → growup → archive
```


### Dream and Evolution
```aiignore
growup 偏短期蒸馏 --> 最近日志发生了什么
evolution 偏系统演化 --> 系统正在朝什么方向变化
dream 偏意义提炼 --> 这些变化说明了说明，下一步该往哪里走
```


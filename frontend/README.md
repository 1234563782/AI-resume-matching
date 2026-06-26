# AI Recruitment Frontend

独立 Vue 3 + Vite 前端，接口通过 Vite proxy 转发到 Spring Boot 后端 `http://localhost:8080`。

## 启动

先启动后端 Spring Boot，然后在本目录执行：

```bash
npm install
npm run dev
```

浏览器打开：

```text
http://localhost:5173
```

## 功能

- 创建岗位 JD
- 上传候选人简历
- 生成候选人推荐排序
- 点击候选人刷新 ECharts 雷达图
- 清空结果后可再次生成雷达图

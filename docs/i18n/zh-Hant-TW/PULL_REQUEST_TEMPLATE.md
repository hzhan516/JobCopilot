## 變更內容
<!-- 這個 PR 做了什麼？一句話概括。 -->

## 原因
<!-- 為什麼需要這個變更？連結到 Issue 或解釋問題。 -->

## 實作方式
<!-- 技術方案和關鍵決策。 -->

## 測試
<!-- 如何測試的？ -->
- [ ] 單元測試通過 (`cd backend && mvn test`)
- [ ] 架構測試通過 (`cd backend && mvn test -Dtest="*ArchitectureTest*"`)
- [ ] 整合測試通過 (`cd backend && mvn verify -Pintegration-test`)
- [ ] 前端 lint 通過 (`cd frontend && npm run lint`)
- [ ] AI 服務 lint 通過 (`cd ai-service && ruff check .`)
- [ ] 已進行手工測試
- [ ] 邊界情況已覆蓋

## 截圖
<!-- UI 變更時提供 -->

## 檢查清單
- [ ] 已自我審查程式碼
- [ ] 已新增/更新測試
- [ ] 已更新文件（如適用，需提供英語 + 簡體中文 + 繁體中文）
- [ ] 無新增警告
- [ ] 破壞性變更已記錄
- [ ] 包含遷移指南（如有破壞性變更）
- [ ] 遵循六邊形架構規則（domain/app 層無框架程式碼）

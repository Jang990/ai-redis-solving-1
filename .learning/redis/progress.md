# 진행 상태 — Redis

## 현재
- 스테이지: S1  · 상태: escalated중 (SLO RED — k6 naive baseline: 100 VU/30s, p99=173ms vs SLO 100ms, 1265 req/s, 정확성 100%)
- 다음 행동: 학습자가 원인 진단 후 자유 해법으로 p99<100ms green 만들기(brief.md 참조). 해법 미지정. green 시 코치가 vus 올려 한계 확인.
- 현재 부하 방식: k6(도커 일회성, `docker run --rm -i grafana/k6 run - < k6/s1-load.js`) → 호스트 bootRun 서버. 부하 knob=`vus`. scale=1, compose 불변(k6는 compose에 안 넣음 — 일회성 작업).
- 하네스 이력: 학습자 피드백으로 ① 쿼리수 assert(답지) 폐기 → SLO 계약, ② CLAUDE.md에 assert 규칙·자기검문·brief.md(§6-4) 추가, ③ JUnit 내장부하 폐기 → k6 전환(JUnit p99=408ms vs k6 173ms — 측정기 자체 잡음 2.3배 확인). 초기 유출: "DB 접근 줄이기" 방향(후속 고통은 미유출).

## 체크리스트 (제목=고통, 해법·escalation 계획은 curriculum.md에만 — §1-2)
- [x] S0 — 플래시 세일 골격, S0FlashSaleTest green
- [ ] S1 — 조회 폭주 속 SLO 사수 (진행중)
- [ ] S2 — 동시 주문에도 재고 불변식 사수
- [ ] S3 — 유저별 요청 한도 강제
- [ ] S4 — 다단계 발급 불변식 (optional)

## 메모
- 스택: Java/Spring/MySQL  · 도메인: 한정판 플래시 세일 e-commerce
- 컨테이너(고정): mysql:8 + redis:7(vanilla) + app(컨테이너) + nginx LB. 평소 개발은 호스트 bootRun, 다중 고통만 `--scale app=N`.
- (코치 리마인더) curriculum.md 내용·솔루션·다음 스테이지 누설 금지 — §1-2, §4

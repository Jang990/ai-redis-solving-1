# 진행 상태 — Redis

## 현재
- 스테이지: S0  · 상태: 구현중
- 다음 행동: 학습자가 플래시 세일 골격(상품 생성/조회 + 한정 재고 주문)을 구현하고 `S0FlashSaleTest` green 만들기. 코치는 다음 스테이지 누설 금지.
- 현재 부하 방식: 단일스레드 인수테스트, `--scale app=1` (호스트 bootRun)

## 체크리스트
- [ ] S0 — 플래시 세일 골격, S0FlashSaleTest green
- [ ] S1 — 읽기 폭주 캐시 + TTL (stale/stampede)
- [ ] S2 — 동시 구매 oversell, 단일→다중 원자성
- [ ] S3 — 분산 rate limit
- [ ] S4 — 다단계 발급 분산락/멱등 (optional)

## 메모
- 스택: Java/Spring/MySQL  · 도메인: 한정판 플래시 세일 e-commerce
- 컨테이너(고정): mysql:8 + redis:7(vanilla) + app(컨테이너) + nginx LB. 평소 개발은 호스트 bootRun, 다중 고통만 `--scale app=N`.

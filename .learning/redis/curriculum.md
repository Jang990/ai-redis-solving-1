# 커리큘럼: Redis (COACH ONLY — 학습자 열람 금지)

## 관통 명제
Redis는 **프로세스 밖에 공유되는 원자적 인메모리 상태(+TTL)** 다. DB 부하를 흘려보내고,
여러 인스턴스가 *한 진실*을 두고 경쟁할 때 그 조율을 한 곳에서 원자적으로 처리한다.
naive(매 조회 DB / JVM 안의 변수·synchronized / 인스턴스별 카운터)는
"읽기 폭주" 또는 "프로세스 간 경쟁" 앞에서 본인 숫자로 무너진다.

## 도메인
**한정판 플래시 세일 e-commerce.** 상품 상세 조회(읽기 폭주) + 한정 수량 주문(재고 경쟁)
+ 유저별 요청 폭주(어뷰징) + 다단계 발급(쿠폰/예약). 한 도메인이 모든 스테이지를 관통.

## 인프라(고정, §7)
`docker-compose.yml`: mysql:8 + redis:7(vanilla, 단일노드, persistence/eviction 무설정)
+ app(컨테이너, build) + nginx LB. 기본 `--scale app=1`. Redis는 분산 조율 고통(S2 다중·S3·S4)이
본체라 앱을 컨테이너화 + LB. 평소 개발은 호스트 `./gradlew bootRun`로 단일, 다중 고통 재현 시 `--scale app=N`.
**미리 막는 설정 금지**: redis cluster/replica ✗, maxmemory-policy 프리셋 ✗, app 코드 안의 락/캐시 ✗.

---

## S0 (base) — 플래시 세일 골격, Redis 없음
- 시나리오: 상품 생성/상세조회 + 한정 재고 주문. MySQL만. 단일스레드 인수테스트로 행동만 고정.
- 인수 테스트: `S0FlashSaleTest` (상품 stock=3 → 3건 성공 → 4번째 409 → stock==0). 단일스레드라 naive green.
- 심어둘 씨앗: ① `GET /products/{id}`가 매번 DB 조회. ② 주문이 `SELECT stock → if>0 → UPDATE` (check-then-act, 비원자적, 락 없음). ③ 어떤 인메모리 상태도 JVM 로컬.
- 완료: green

## S1 — 읽기 폭주가 MySQL을 녹인다 (캐시 + TTL)
- 시나리오: 플래시 세일 오픈 직전 상품 상세 조회 폭주. 모든 GET이 DB로 직행.
- naive 탈출구: 매 조회 DB 쿼리(S0 씨앗 ①).
- escalation: 동시 조회 부하 → MySQL 커넥션풀 고갈/지연 폭증, p99 폭발을 본인 숫자로 목격.
- 1차 테스트(행동): `k6/s1-load.js` — 100 VU/30s, checks 100% + p99<100ms. SLO는 학습자 본인 머신의 naive 베이스라인(p99=173ms) 대비 설정, 데이터 들고 오면 협상. 쿼리수는 진단 출력으로만(`SHOW GLOBAL STATUS`) — assert 금지(§4).
  [이력: 초안은 "쿼리수 선형 비례 관측" 테스트였으나 §4 답지 자기검문으로 폐기 — 내부 지표 assert는 해법 강제. JUnit 내장부하도 측정 오염(2.3배)으로 폐기 → k6.]
- escalated 테스트(해법 형태 가정 ✗, fix가 뭐든 증상으로만 추궁):
  ① green 후 vus 상승 — 튜닝성 fix의 한계를 본인 숫자로.
  ② fix가 원본과 분리된 사본을 갖게 되는 순간(형태 무관) 행동 계약 추가: "가격/재고 변경 후의 조회는 새 값을 반환한다"(stale을 유저 관점 계약으로).
  ③ 사본 만료/무효화 순간에 부하가 겹쳐도 SLO 유지(stampede를 SLO 위반 증상으로).
- 부하 knob: `k6/s1-load.js`의 `vus`(+②부터 쓰기 혼입). compose 불변. scale=1 유지.
- 재현 레버: **자원**. ③vanilla 기본 한계(매 요청 DB 왕복 + Hikari 기본 풀 + MySQL 기본 설정)로 100VU에서 이미 p99=173ms 재현 확인 → §7-4 캡 불요(부족해지면 그때 검토). 주의: 현재 베이스라인은 서버의 측정 오염 설정을 포함한 숫자 — 학습자가 그걸 걷어내 green이 되면 그것도 정당한 fix, ①로 대응.
- HIDDEN 솔루션: cache-aside + TTL → 무효화(write 시 evict/갱신) → stampede는 락/조기재계산. (누설 금지)
- 그릴링: TTL 길면? 짧으면? stale와 부하의 트레이드오프 어디서 결정? 캐시-DB 불일치 언제 발생?
- 졸업 인식: vus 올려도 SLO 유지 + 갱신 후 일관 계약 green + 만료 폭주에도 SLO 유지. (진단상 쿼리수 디커플 확인은 참고용, 합격 조건 아님.)

## S2 — 동시 구매가 재고를 음수로 만든다 (원자성 → 프로세스 간 원자성)
- 시나리오: 한정 수량에 동시 주문 쇄도. S0 씨앗 ②(check-then-act)가 정체를 드러냄.
- naive 탈출구(단계적): (a) 그냥 둠 → 단일 인스턴스 동시성에서 oversell. (b) JVM `synchronized`/락 → 단일에서 green.
- escalation: ① scale=1에서 동시 구매 → oversell(재고<0 또는 초과 발급) 목격. ② 학습자가 synchronized로 막음 → green. ③ `--scale app=3` → 인메모리 락이 인스턴스마다 따로 놀아 다시 oversell.
- 1차 테스트(행동): `S2OversellTest` — 재고 100에 동시 200 주문 → 성공 주문 정확히 100, 재고 정확히 0, 중복/초과 0.
- escalated 테스트: 같은 테스트를 LB 뒤 `--scale app=3`으로 — JVM 락 버전이 깨짐(성공 주문 > 100).
- 부하 knob: 동시성 + `--scale app=N` (이게 핵심 knob). compose 불변.
- 재현 레버: **논리**(레이스) — 동시성/`--scale`이면 하드웨어 무관하게 터짐. 캡·데이터볼륨 불요.
- HIDDEN 솔루션: Redis 원자 연산(DECR/Lua) 또는 분산락으로 프로세스 밖 단일 진실. (누설 금지)
- 그릴링: synchronized는 왜 2대에서 죽나? DB 비관/낙관락과 Redis 원자연산의 차이·비용? DECR가 음수 되면?
- 졸업 인식: 인스턴스 수와 무관하게 성공=재고, 초과 0.

## S3 — 한 유저가 전부 쓸어간다 (분산 rate limit)
- 시나리오: 봇/어뷰저가 유저당 한도(예: 초당 5건)를 넘겨 주문 시도. 한도를 서버 전체로 강제해야.
- naive 탈출구: 인스턴스 로컬 카운터(맵/Guava)로 유저별 카운트.
- escalation: `--scale app=N` + LB 라운드로빈 → 요청이 인스턴스마다 흩어져 실효 한도 = N×의도. 로컬 카운터로는 절대 못 막음.
- 1차 테스트(행동): `S3RateLimitTest` — 단일 인스턴스에서 한 유저 초당 5 초과분 429.
- escalated 테스트: `--scale app=3` LB 뒤 동일 유저 폭주 → 허용 건수가 5 근처여야 하는데 로컬 카운터면 ~15 통과(실패 목격).
- 부하 knob: `--scale app=N` + 유저별 요청 플러드. compose 불변.
- 재현 레버: **논리**(인스턴스별 상태 분산) — `--scale`이면 하드웨어 무관하게 터짐.
- HIDDEN 솔루션: Redis INCR+EXPIRE / 정밀하면 sliding window(ZSET)·token bucket(Lua 원자). (누설 금지)
- 그릴링: 고정창 경계 버스트 문제? INCR+EXPIRE 원자성 안 지키면 무슨 레이스? 한도 카운트는 어디 살아야 하나?
- 졸업 인식: 인스턴스 수 무관하게 유저 실효 한도 ≈ 의도값.

## S4 — 다단계 발급의 임계구역 (분산락 / 멱등) [optional]
- 시나리오: 쿠폰 발급이 "중복발급 검사 → 재고 차감 → 발급 기록" 다단계. 단일 키 원자연산으로 못 감쌈. + 클라이언트 더블서밋.
- naive 탈출구: S2의 단일키 DECR만으로 처리 시도 → 다단계 사이 레이스로 중복발급/유령차감.
- escalation: `--scale app=3` 동시 + 같은 유저 더블클릭 → 중복 발급 또는 차감-기록 불일치.
- 1차/escalated 테스트: `S4CouponTest` — 동시 다중요청·중복요청에도 유저당 1건, 총발급=재고, 차감과 기록 원자.
- 부하 knob: `--scale app=N` + 중복 요청키.
- 재현 레버: **논리**(다단계 레이스) — 동시성/`--scale`이면 하드웨어 무관하게 터짐.
- HIDDEN 솔루션: 분산락(SET NX PX + 안전 해제 Lua) 또는 멱등키(SET NX). 락 만료/펜싱 토큰 그릴. (누설 금지)
- 그릴링: 락 TTL이 작업보다 짧으면? 해제 시 남의 락 지우는 레이스? 락 vs 멱등키 언제 무엇?
- 졸업 인식: 중복·동시·다인스턴스에도 불변식 유지 + 락 안전해제 이해.

## 졸업 신호
S3(최소) 또는 S4 통과 + synthesis 누적 = 캐시/TTL/원자성/분산조율/만료 트레이드오프 결정 로그 확보.
"언제 Redis를 안 쓰는가"까지 말할 수 있으면 졸업.

# 로또 앱 개발 설계 문서

> 이 문서는 Claude Code로 개발하기 위한 설계 문서입니다.
> 작업 시 **10. 구현 순서**의 단계를 하나씩 진행하고, 각 단계의 완료 기준을 만족하는지 확인한 뒤 다음 단계로 넘어갑니다.

---

## 0. 개요

- **앱**: 로또 6/45 당첨번호 조회 · QR 당첨 확인 · 번호 통계 · 번호 생성 · 내 번호 보관함
- **플랫폼**: Android 전용 (폰 · 폴더블 · 태블릿 대응)
- **UI**: Jetpack Compose + Material 3
- **범위**: 로또 6/45만 포함 (연금복권720+ 제외)
- **원칙**
  - 화면(UI)은 **Room의 Flow만 구독**한다.
  - Firestore는 신규 회차 **증분 동기화**에만 사용한다. 원격 데이터는 반드시 Room을 거쳐 UI에 도달한다.
  - 네트워크 없이도 모든 조회 기능이 동작해야 한다.

---

## 1. 기능 요건

### MVP

| ID | 기능 | 상세 |
| --- | --- | --- |
| F-01 | 최근 당첨번호 조회 | 최신 회차 번호 6개 + 보너스, 추첨일, 1등 당첨금/당첨자 수. 회차 선택으로 과거 회차 조회. 번호 공은 구간별 색상 적용 |
| F-02 | QR 당첨 확인 | 용지 QR 스캔 → 회차와 게임(A~E)별 번호 파싱 → 게임별 등수 표시. 미추첨 회차는 "추첨 전" 표시. 결과를 보관함에 저장 가능 |
| F-03 | 번호별 당첨 횟수 | 1~45 번호별 출현 횟수. 보너스 포함 토글, 최근 N회(전체/100/50/10) 필터 |
| F-04 | 랜덤 번호 생성 | SecureRandom으로 1~5게임 생성. 결과 보관함 저장 가능 |
| F-05 | 내 번호 보관함 | QR/생성/수동 입력 번호 저장. 추첨 후 자동 등수 판정. 삭제/수정 |

### 2차

| ID | 기능 | 상세 |
| --- | --- | --- |
| F-06 | 생성 옵션 | 고정수/제외수, 홀짝 비율, 번호 합계 범위, 연속번호 제한 |
| F-07 | 추첨 결과 알림 | 동기화 완료 시 로컬 알림. 보관함 번호 당첨 시 등수 포함 |
| F-08 | 구매 마감 알림 | 토요일 20:00 판매 마감 전 사용자 지정 시간 알림 |
| F-09 | 실수령액 계산기 | 세후 금액 계산 (5.3 TaxCalculator 참고) |
| F-10 | 홈 위젯 | Glance. 최신 회차 번호, 다음 추첨까지 남은 시간 |
| F-11 | 과거 대조 시뮬레이션 | 입력 번호를 1회차부터 대조해 등수별 횟수 표시 |

---

## 2. 아키텍처

- **패턴**: MVVM + Repository, 단방향 데이터 흐름
- **DI**: Hilt
- **흐름**: `UI(Compose) → ViewModel(StateFlow<UiState>) → Repository → Room / Firestore`
- **모듈**: 단일 `:app` 모듈로 시작, 패키지로 계층 분리

```
app/src/main/java/com/example/lotto
├─ LottoApp.kt                 // @HiltAndroidApp
├─ MainActivity.kt
├─ data
│  ├─ local
│  │  ├─ LottoDatabase.kt
│  │  ├─ entity/Draw.kt, MyTicket.kt
│  │  ├─ dao/DrawDao.kt, TicketDao.kt
│  │  └─ Converters.kt
│  ├─ remote
│  │  └─ FirestoreDrawDataSource.kt
│  └─ repository
│     ├─ DrawRepository.kt
│     └─ TicketRepository.kt
├─ domain
│  ├─ QrParser.kt
│  ├─ RankCalculator.kt
│  ├─ NumberGenerator.kt
│  └─ TaxCalculator.kt
├─ sync
│  ├─ SyncWorker.kt
│  └─ SyncScheduler.kt
├─ ui
│  ├─ LottoAppScaffold.kt      // NavigationSuiteScaffold
│  ├─ navigation/Destinations.kt
│  ├─ home/      HomeScreen.kt, HomeViewModel.kt
│  ├─ scan/      ScanScreen.kt, ScanViewModel.kt
│  ├─ stats/     StatsScreen.kt, StatsViewModel.kt
│  ├─ mytickets/ MyTicketsScreen.kt, MyTicketsViewModel.kt
│  ├─ generate/  GenerateScreen.kt, GenerateViewModel.kt
│  ├─ components/LottoBall.kt, DrawCard.kt, RankBadge.kt
│  └─ theme/
└─ di
   ├─ DatabaseModule.kt
   └─ FirebaseModule.kt
```

### 코딩 규칙

- ViewModel은 `StateFlow<UiState>` 하나를 노출하고, UI 이벤트는 함수로 받는다.
- Composable은 `XxxScreen(viewModel)` → `XxxContent(state, onEvent)`로 분리해 Preview와 테스트가 가능하게 한다.
- 도메인 로직(`domain/`)은 Android 의존성 없이 순수 Kotlin으로 작성한다.
- 문자열은 `strings.xml`에 둔다.

---

## 3. 화면 구성

| 탭 | 화면 | 주요 요소 |
| --- | --- | --- |
| 홈 | HomeScreen | 최신 회차 카드, 회차 선택, 번호 생성 진입 |
| QR | ScanScreen | 스캔 실행 → 결과(게임별 등수) → 보관함 저장 |
| 통계 | StatsScreen | 45개 번호 그리드(횟수에 따라 색 농도), 보너스 토글, N회 필터 |
| 내 번호 | MyTicketsScreen | 회차별 그룹 리스트, 등수 배지, 수동 입력 |

번호 생성(GenerateScreen)은 홈에서 진입하는 하위 화면이다.

---

## 4. 데이터 계층

### 4.1 Entity

```kotlin
@Entity
data class Draw(
    @PrimaryKey val round: Int,
    val date: LocalDate,
    val n1: Int, val n2: Int, val n3: Int,
    val n4: Int, val n5: Int, val n6: Int,   // 오름차순 저장
    val bonus: Int,
    val firstPrize: Long,                     // 1등 1인당 당첨금(원)
    val firstWinners: Int
)

enum class Source { QR, GENERATED, MANUAL }

@Entity(indices = [Index("round")])
data class MyTicket(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val round: Int,
    val numbers: String,      // "3,11,17,24,38,42" (오름차순)
    val source: Source,
    val createdAt: Long,
    val rank: Int? = null     // null = 추첨 전, 0 = 낙첨, 1~5 = 등수
)
```

- `LocalDate`, `Source`는 `Converters`로 변환한다.

### 4.2 DAO

```kotlin
@Dao
interface DrawDao {
    @Query("SELECT * FROM Draw ORDER BY round DESC LIMIT 1")
    fun latest(): Flow<Draw?>

    @Query("SELECT * FROM Draw WHERE round = :round")
    fun byRound(round: Int): Flow<Draw?>

    @Query("SELECT round FROM Draw ORDER BY round DESC")
    fun rounds(): Flow<List<Int>>

    @Query("SELECT MAX(round) FROM Draw")
    suspend fun maxRound(): Int?

    @Query("SELECT * FROM Draw WHERE round = :round")
    suspend fun get(round: Int): Draw?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(draws: List<Draw>)

    @Query("""
        SELECT num, COUNT(*) AS cnt FROM (
            SELECT n1 AS num, round FROM Draw UNION ALL
            SELECT n2, round FROM Draw UNION ALL
            SELECT n3, round FROM Draw UNION ALL
            SELECT n4, round FROM Draw UNION ALL
            SELECT n5, round FROM Draw UNION ALL
            SELECT n6, round FROM Draw UNION ALL
            SELECT bonus, round FROM Draw WHERE :includeBonus = 1
        )
        WHERE round > :fromRound
        GROUP BY num ORDER BY num
    """)
    fun countByNumber(fromRound: Int, includeBonus: Boolean): Flow<List<NumberCount>>
}

data class NumberCount(val num: Int, val cnt: Int)
```

- 번호별 당첨 횟수는 **저장하지 않고 쿼리로 계산**한다.
- N회 필터: `fromRound = maxRound - N` (전체는 0).

```kotlin
@Dao
interface TicketDao {
    @Query("SELECT * FROM MyTicket ORDER BY round DESC, createdAt DESC")
    fun all(): Flow<List<MyTicket>>

    @Query("SELECT * FROM MyTicket WHERE id = :id")
    fun byId(id: Long): Flow<MyTicket?>

    @Query("SELECT * FROM MyTicket WHERE round = :round AND rank IS NULL")
    suspend fun pending(round: Int): List<MyTicket>

    @Insert suspend fun insert(tickets: List<MyTicket>)
    @Update suspend fun update(ticket: MyTicket)
    @Delete suspend fun delete(ticket: MyTicket)
}
```

### 4.3 초기 데이터

- `app/src/main/assets/lotto.db`에 전 회차를 담아 배포한다.
- `Room.databaseBuilder(...).createFromAsset("lotto.db")`로 로드한다.
- `lotto.db`의 스키마는 Room이 생성한 스키마와 정확히 일치해야 한다. `exportSchema = true`로 스키마 JSON을 생성해 두고, 그 스키마로 DB 파일을 만드는 스크립트(`tools/build_asset_db.py`)를 둔다.
- 앱을 출시할 때마다 최신 회차로 다시 생성한다.

---

## 5. 도메인 로직

### 5.1 QrParser

- 동행복권 로또 용지 QR은 URL 형식이다. `v=` 파라미터 뒤에 **회차 4자리**가 오고, 이어서 게임마다 **구분 문자 1자리 + 번호 12자리(2자리 × 6)** 가 반복된다. 끝에 추가 식별 값이 붙을 수 있다.
- 출력: `data class ParsedTicket(val round: Int, val games: List<List<Int>>)`
- 검증: 게임별 번호 6개, 각 1~45, 중복 없음, 게임 수 1~5. 하나라도 어긋나면 `Result.failure`.
- **주의**: 실제 형식은 실물 용지로 반드시 확인한다. 자동/수동/반자동 혼합 용지를 포함한 실제 QR 문자열 샘플을 `src/test/resources/qr_samples.txt`에 모아 테스트한다.

### 5.2 RankCalculator

| 일치 | 등수 |
| --- | --- |
| 6개 | 1 |
| 5개 + 보너스 | 2 |
| 5개 | 3 |
| 4개 | 4 |
| 3개 | 5 |
| 그 외 | 0 (낙첨) |

```kotlin
fun rank(ticket: List<Int>, draw: Draw): Int
```

### 5.3 NumberGenerator / TaxCalculator

- **NumberGenerator**: `(1..45).shuffled(SecureRandom()).take(6).sorted()`
  - 2차(F-06): 조건(고정수, 제외수, 홀짝, 합계, 연속)을 만족할 때까지 재추첨한다. 최대 시도 횟수를 두고, 초과하면 "조건을 만족하는 조합이 없음" 에러를 반환한다.
- **TaxCalculator** (2차, F-09)
  - 당첨금 200만 원 이하: 비과세
  - 200만 원 초과: 과세 대상 = 당첨금 − 1,000원(구입비)
    - 과세 대상 중 3억 원까지: 22%
    - 3억 원 초과분: 33%
  - 세율은 하드코딩하지 말고 상수로 분리한다.

---

## 6. 적응형 레이아웃 (폰 · 폴더블 · 태블릿)

### 6.1 기준

- `currentWindowAdaptiveInfo().windowSizeClass`로 폭 클래스를 판단한다.
  - **Compact**: 폰 세로
  - **Medium**: 폴더블 펼침, 작은 태블릿, 폰 가로
  - **Expanded**: 태블릿
- ViewModel은 화면 크기와 무관하게 하나만 둔다. **UI만 분기**한다.
- 회전이나 폴더블 접기/펼치기 시 상태가 유지되어야 한다 (ViewModel과 `rememberSaveable` 사용).

### 6.2 내비게이션

- `NavigationSuiteScaffold`를 사용한다.
  - Compact: 하단 내비게이션 바
  - Medium / Expanded: 왼쪽 내비게이션 레일

### 6.3 화면별 레이아웃

| 화면 | Compact | Medium / Expanded |
| --- | --- | --- |
| 홈 | 최신 회차 카드 + 회차 선택 드롭다운 | `ListDetailPaneScaffold`: 왼쪽 회차 목록, 오른쪽 회차 상세 |
| 내 번호 | 리스트 → 탭하면 상세 화면으로 이동 | `ListDetailPaneScaffold`: 왼쪽 보관함, 오른쪽 티켓 상세 |
| 통계 | 9열 × 5행 그리드, 아래에 필터 | Expanded: 15열 × 3행 그리드 + 오른쪽 막대 차트 |
| QR 결과 | ModalBottomSheet | Dialog |
| 번호 생성 | 게임 목록 세로 나열 | 2열 그리드 |

- 콘텐츠 최대 폭을 제한해 태블릿에서 카드가 과하게 늘어나지 않게 한다 (예: 단일 패널 최대 600dp).
- Detail 화면의 뒤로 가기는 `BackHandler`로 처리한다. 2단 레이아웃에서는 목록을 닫지 않는다.

### 6.4 방향

- 화면 방향을 고정하지 않는다. Android 16(targetSdk 36)부터는 큰 화면에서 방향 고정 설정이 무시되므로, 모든 화면이 가로/세로 모두에서 깨지지 않아야 한다.
- QR 스캐너도 가로 화면에서 정상 동작하는지 확인한다.

### 6.5 확인 방법

- `@PreviewScreenSizes` 또는 폰/폴더블/태블릿 `@Preview`를 각 Content Composable에 추가한다.
- 에뮬레이터: Pixel(폰), Pixel Fold, Pixel Tablet에서 확인한다.

---

## 7. 동기화

### 7.1 Firestore

- 컬렉션 `draws`, 문서 ID = 회차 번호 문자열(`"1245"`), 필드는 `Draw`와 동일 (`date`는 `"yyyy-MM-dd"` 문자열).
- 보안 규칙: 클라이언트는 **읽기만** 허용한다.

```
rules_version = '2';
service cloud.firestore {
  match /databases/{db}/documents {
    match /draws/{round} {
      allow read: if true;
      allow write: if false;
    }
  }
}
```

### 7.2 수집 (GitHub Actions)

- 스케줄: 매주 토요일 21:30 KST (`cron: '30 12 * * 6'`, UTC 기준), 일요일 09:00 KST에 한 번 더 실행.
- 스크립트(`tools/collect/collect.py`): Firestore의 최대 회차를 확인 → 그 이후 회차를 동행복권에서 수집 → Admin SDK로 기록.
- 서비스 계정 키는 GitHub Secrets(`FIREBASE_SERVICE_ACCOUNT`)에 저장한다. 저장소에 커밋하지 않는다.
- 동행복권은 공식 API가 없으므로 수집 소스가 바뀌면 이 스크립트만 수정한다. 앱 코드는 영향을 받지 않는다.
- Firebase는 Spark(무료) 요금제를 유지한다.

### 7.3 SyncWorker (앱)

1. `drawDao.maxRound()` 확인
2. Firestore에서 `round > maxRound` 문서 조회
3. `@Transaction`으로 Room에 insert
4. 새로 들어온 회차마다 `ticketDao.pending(round)` → `RankCalculator`로 판정 → update
5. 당첨된 티켓(rank 1~5)이 있으면 알림 표시 (2차, F-07)

- 실행 시점
  - 앱 실행 시 `OneTimeWorkRequest` (unique, KEEP)
  - 주기 작업: `PeriodicWorkRequest` 12시간 간격, 네트워크 연결 조건. 토요일 추첨 후에는 최대 12시간 안에 반영된다.
- 실패 시 `Result.retry()` + 지수 백오프.

---

## 8. QR 스캔 구현

- 라이브러리: `com.journeyapps:zxing-android-embedded`
- `rememberLauncherForActivityResult(ScanContract())`로 실행한다.
- `ScanOptions`: QR_CODE 포맷만, 비프음 끄기, 방향 고정 해제.
- 카메라 권한 거부 시 안내 화면과 설정 이동 버튼을 표시한다.
- 스캔 결과 → `QrParser` → 해당 회차 Draw가 Room에 있으면 즉시 판정, 없으면 "추첨 전"으로 표시한다.

---

## 9. 알림 (2차)

- 채널: `draw_result`(추첨 결과), `deadline`(구매 마감)
- Android 13 이상: `POST_NOTIFICATIONS` 권한을 처음 알림 설정을 켤 때 요청한다.
- 구매 마감 알림은 사용자가 지정한 시간에 맞춰 WorkManager로 예약하고, 설정은 DataStore에 저장한다.

---

## 10. 구현 순서

각 단계가 끝나면 빌드와 테스트가 통과해야 한다.

### Step 1. 프로젝트 셋업
- Compose + Material 3, Hilt, Room(KSP), WorkManager, Firebase(Firestore), zxing, material3-adaptive 의존성을 Version Catalog(`libs.versions.toml`)로 추가
- minSdk 26, targetSdk/compileSdk 최신
- **완료 기준**: 빈 화면 앱이 빌드되고 실행된다.

### Step 2. 데이터 계층
- Entity, DAO, Database, Converters, Repository 작성
- `exportSchema = true`
- **완료 기준**: in-memory Room 테스트로 `countByNumber`(보너스 포함/제외, N회 필터)와 `pending` 쿼리가 통과한다.

### Step 3. 도메인 로직
- `QrParser`, `RankCalculator`, `NumberGenerator` 작성
- **완료 기준**: 단위 테스트 통과
  - RankCalculator: 1~5등과 낙첨 경계 케이스
  - NumberGenerator: 6개, 1~45 범위, 중복 없음, 정렬 (1,000회 반복)
  - QrParser: 정상 샘플, 게임 수 1~5, 잘못된 형식 실패

### Step 4. 내비게이션과 적응형 스캐폴드
- `NavigationSuiteScaffold` + 4개 탭, 빈 화면 연결
- **완료 기준**: 폰에서는 하단 바, 태블릿에서는 레일이 표시된다.

### Step 5. 홈 (F-01)
- `LottoBall`, `DrawCard` 컴포넌트, 회차 선택
- Expanded에서 `ListDetailPaneScaffold` 적용
- **완료 기준**: 내장 DB의 최신 회차가 표시되고, 회차를 변경할 수 있다. 폰/태블릿 Preview 확인.

### Step 6. 통계 (F-03)
- 번호 그리드, 보너스 토글, N회 필터
- **완료 기준**: 필터 변경 시 즉시 갱신된다. Compact 9×5, Expanded 15×3 레이아웃.

### Step 7. 번호 생성 (F-04) + 보관함 (F-05)
- 생성 → 저장 → 보관함 목록, 수동 입력, 삭제
- 보관함 Expanded 2단 레이아웃
- **완료 기준**: 생성한 번호가 보관함에 나타나고, 이미 추첨된 회차는 저장 즉시 등수가 표시된다.

### Step 8. QR 스캔 (F-02)
- zxing 연동, 결과 BottomSheet/Dialog, 보관함 저장
- **완료 기준**: 실물 용지 스캔 시 게임별 등수가 표시된다. 가로 화면에서도 동작한다.

### Step 9. 초기 DB 내장
- `tools/build_asset_db.py`로 `assets/lotto.db` 생성, `createFromAsset` 적용
- **완료 기준**: 앱을 새로 설치하면 네트워크 없이 전 회차가 조회된다.

### Step 10. 동기화
- Firestore 데이터 소스, SyncWorker, SyncScheduler
- `tools/collect/collect.py` + `.github/workflows/collect.yml`
- **완료 기준**: Firestore에 새 회차를 추가하면 앱 실행 시 반영되고, 해당 회차의 보관함 티켓 등수가 판정된다. Worker 테스트 통과.

### Step 11. 마무리 (MVP 출시)
- 오프라인 상태 표시(마지막 갱신 시각), 빈 상태/에러 UI
- 스토어 문구 점검(아래 11장)
- **완료 기준**: 폰 · 폴더블 · 태블릿 에뮬레이터에서 전 화면 동작 확인.

### 2차 (F-06 ~ F-11)
생성 옵션 → 알림 → 실수령액 계산기 → 위젯(Glance) → 과거 대조 시뮬레이션 순서로 진행.

---

## 11. 비기능 요건

| 구분 | 요건 |
| --- | --- |
| 오프라인 | 조회 · 통계 · 생성 · 보관함 전 기능이 네트워크 없이 동작. 동기화 실패 시 마지막 갱신 시각 표시 |
| 성능 | 콜드 스타트 후 홈 표시 1초 이내, 통계 쿼리 100ms 이내 |
| 난수 | SecureRandom 사용 |
| 비용 | Firestore Spark 무료 한도(읽기 5만 건/일) 안에서 운영 |
| 스토어 정책 | "당첨 확률 상승", "예측", "적중" 표현 금지. 통계 화면에 "과거 데이터 기반 참고용" 문구 표시. 복권 구매 기능 없음 |
| 권한 | 카메라(스캔 시점), 알림(Android 13+, 알림 켤 때) |
| 접근성 | 번호 공에 contentDescription, 색상만으로 정보를 구분하지 않음 |

---

## 12. 미정 사항

- [ ] 앱 이름 / 패키지명 / 아이콘
- [ ] 광고(AdMob) 탑재 여부
- [ ] 생성 번호 기록을 보관함과 분리할지 여부
- [ ] 동행복권 데이터 수집 소스 확정 (Step 10 전에 결정)

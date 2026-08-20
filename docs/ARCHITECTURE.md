# 프로젝트 구조

코드의 공개 패키지는 `kr.leagueofminecraft`, Fabric 모드 ID와 리소스 네임스페이스는 `league_of_minecraft`로 통일합니다.

```text
src/
├─ main/java/kr/leagueofminecraft/
│  ├─ LeagueOfMinecraftMod.java       # 서버/공통 Fabric 진입점
│  ├─ ModConstants.java               # 호환 모드 ID와 Identifier 생성
│  ├─ registry/ModItems.java          # 챔피언 무기 등록
│  ├─ champion/
│  │  ├─ ChampionDefinition.java      # 공통 챔피언 계약
│  │  ├─ ChampionRegistry.java        # 플레이 가능 챔피언 레지스트리
│  │  ├─ darius/                      # 다리우스 스킬·상태·VFX 서비스
│  │  ├─ yone/                        # 요네 스킬·상태·이동 서비스
│  │  └─ malphite/                    # 말파이트 보호막·강화 평타·돌진 서비스
│  ├─ core/                           # 챔피언 선택, 성장, CC, 매치, 저장
│  ├─ match/                          # 팀 명단, 경기 상태, 기지·부활, 팀 시야와 영구 저장
│  ├─ combat/                         # 물리/마법 피해와 치명타 계산
│  ├─ shop/                           # 상점, LoL 인벤토리, 경제, 아이템·장신구 효과
│  ├─ network/                        # 클라이언트-서버 페이로드
│  └─ mixin/                          # 표시 엔티티와 아이템 보호 훅
├─ client/java/kr/leagueofminecraft/client/
│  └─ LeagueOfMinecraftClient.java    # Z/X/C/V, P/M/B, Alt+숫자 입력과 애니메이션
├─ test/java/kr/leagueofminecraft/     # 전투 수식·쿨타임·입력·팀·귀환 회귀 테스트
└─ main/resources/
   ├─ fabric.mod.json
   ├─ league_of_minecraft.mixins.json
   ├─ assets/league_of_minecraft/      # 모델, 텍스처, 언어, 아이콘
   └─ data/league_of_minecraft/        # 피해 타입 데이터
```

## 의존 방향

- `LeagueOfMinecraftMod`가 레지스트리와 게임 시스템을 초기화합니다.
- `champion`은 `core`, `combat`, `network`, `registry`를 사용합니다.
- `ChampionManager`는 `ChampionRegistry`를 통해 챔피언을 호출하며 구현 클래스를 직접 분기하지 않습니다.
- 새 아이템은 `registry/ModItems`, 새 챔피언은 `champion/<name>`에 추가합니다.
- 새 네트워크 메시지는 `network`에 두고 진입점에서 등록합니다.
- 문자열 네임스페이스를 직접 만들지 말고 `ModConstants.id(path)`를 사용합니다.
- Minecraft 객체가 필요 없는 공식과 상태 컨테이너는 순수 Java 클래스로 분리해 단위 테스트합니다.
- MATCH 팀은 `MatchScoreboardTeams`를 통해 `lom_blue`/`lom_red` 스코어보드 팀과 동기화하며,
  와드 소유권과 적 렌즈 판정은 `TeamWardRules`의 순수 규칙을 사용합니다.
- 와드는 엔티티 태그에 설치자·팀·절대 만료 시각을 저장하며, `TrinketSystem`이 로드된 청크의
  미추적 와드를 주기적으로 복원하므로 별도 와드 JSON과 엔티티 저장본이 중복되지 않습니다.
- 와드 충전 상태는 `PlayerEconomy.AccountSnapshot`에 저장하고 순수 `WardChargeRules`가
  최대 2충전, 90초 재충전, 활성 와드 2개 제한을 계산합니다.
- 킬·어시스트 보상과 10초 기여 판정은 순수 `TakedownRewardRules`를 기준으로 합니다.
- 서포터 대상 판정은 `SupportItemRules`의 순수 규칙을 사용하고, 기사의 맹세 결속 UUID는
  `PlayerEconomy.AccountSnapshot`과 `lol_hyunmin_players.json`을 통해 영구 저장합니다.

## 호환성 규칙

다음 값은 저장 데이터와 리소스팩을 깨뜨릴 수 있으므로 별도 마이그레이션 없이 바꾸지 않습니다.

- Fabric ID: `league_of_minecraft`
- 리소스: `assets/league_of_minecraft`
- 데이터팩: `data/league_of_minecraft`
- 0.14.8 이하 무기 레지스트리만 `darius_skills` 호환 ID로 함께 등록해 기존 월드 로드를 지원합니다.
- 등록 아이템 ID와 피해 타입 ID

Java 패키지명과 Gradle 프로젝트명은 호환 데이터가 아니므로 각각 `kr.leagueofminecraft`, `league-of-minecraft`로 정리했습니다.

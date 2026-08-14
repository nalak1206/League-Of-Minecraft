# 프로젝트 구조

코드의 공개 패키지는 `kr.leagueofminecraft`로 통일합니다. 기존 월드·리소스팩과의 호환성을 위해 Fabric 모드 ID와 리소스 네임스페이스 `darius_skills`는 유지합니다.

```text
src/
├─ main/java/kr/leagueofminecraft/
│  ├─ LeagueOfMinecraftMod.java       # 서버/공통 Fabric 진입점
│  ├─ ModConstants.java               # 호환 모드 ID와 Identifier 생성
│  ├─ registry/ModItems.java          # 챔피언 무기 등록
│  ├─ champion/
│  │  ├─ darius/DariusSkills.java     # 다리우스 P/Q/W/E/R
│  │  └─ yone/                        # 요네 P/Q/W/E/R 및 강철 검
│  ├─ core/                           # 챔피언 선택, 성장, CC, 매치, 저장
│  ├─ combat/                         # 물리/마법 피해와 치명타 계산
│  ├─ shop/                           # 상점 GUI, 경제, 아이템 효과
│  ├─ network/                        # 클라이언트-서버 페이로드
│  └─ mixin/                          # 표시 엔티티와 아이템 보호 훅
├─ client/java/kr/leagueofminecraft/client/
│  └─ LeagueOfMinecraftClient.java    # Z/X/C/V 입력과 클라이언트 애니메이션
└─ main/resources/
   ├─ fabric.mod.json
   ├─ league_of_minecraft.mixins.json
   ├─ assets/darius_skills/            # 모델, 텍스처, 언어, 아이콘
   └─ data/darius_skills/              # 피해 타입 데이터
```

## 의존 방향

- `LeagueOfMinecraftMod`가 레지스트리와 게임 시스템을 초기화합니다.
- `champion`은 `core`, `combat`, `network`, `registry`를 사용합니다.
- 공통 시스템은 특정 챔피언 구현을 가능한 한 직접 참조하지 않습니다.
- 새 아이템은 `registry/ModItems`, 새 챔피언은 `champion/<name>`에 추가합니다.
- 새 네트워크 메시지는 `network`에 두고 진입점에서 등록합니다.
- 문자열 네임스페이스를 직접 만들지 말고 `ModConstants.id(path)`를 사용합니다.

## 호환성 규칙

다음 값은 저장 데이터와 리소스팩을 깨뜨릴 수 있으므로 별도 마이그레이션 없이 바꾸지 않습니다.

- Fabric ID: `darius_skills`
- 리소스: `assets/darius_skills`
- 데이터팩: `data/darius_skills`
- 등록 아이템 ID와 피해 타입 ID

Java 패키지명과 Gradle 프로젝트명은 호환 데이터가 아니므로 각각 `kr.leagueofminecraft`, `league-of-minecraft`로 정리했습니다.

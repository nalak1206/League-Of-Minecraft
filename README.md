# League of Minecraft

Minecraft 26.2 Fabric에서 리그 오브 레전드식 챔피언 전투, 아이템, 상점과 맵 규칙을 구현하는 프로젝트입니다.

현재 플레이 가능 챔피언은 다리우스와 요네입니다. 기본 스킬 키는 `Z/X/C/V = Q/W/E/R`이며 `/lol shop`으로 상점을 엽니다.

## 현재 버전

`0.11.1`

완료된 주요 기능:

- 다리우스 P/Q/W/E/R 및 전용 텍스처
- 요네 P/Q/W/E/R 원본 기반 전면 개편, 검별 VFX와 양손 전용 무기
- 공통 챔피언 선택, 레벨 1~18, 스킬 랭크와 CC 기반
- 월드별 플레이어 진행도·골드·아이템 저장
- 상자 GUI 상점, 시작 아이템, 장화
- 역할군별 대표 전설 아이템 6종, 총 36종 카탈로그
- 전설 아이템 36종 지정 바닐라 외형 적용
- 전사 3종 및 원딜 핵심 5종, 강철심장 고유 효과 1차
- 전사 아이템 충전 공격·주문 검 기본 공격력 계수·칠흑 6중첩 검증
- 전설 아이템 물리·마법 추가 피해 공통 처리기 1차
- 공통 치명타 엔진, 요네 2배 치명타 확률·90% 피해 계수, 무한의 대검 연동
- 야생용 `ADVENTURE`, 롤 맵용 `MATCH` 모드 기반

자세한 현황은 [현재 작업 인수인계](docs/HANDOFF.md), [전설 아이템 명세](docs/ITEMS.md), 빌드 방법은 [개발 가이드](docs/DEVELOPMENT.md), 변경 이력은 [CHANGELOG](CHANGELOG.md)를 확인하세요.

## 빠른 실행

필수 환경:

- Minecraft `26.2`
- Fabric Loader `0.19.3`
- Fabric API `0.157.0+26.2`
- Java `25`

Windows 빌드:

```powershell
./gradlew.bat clean build
```

완성 JAR은 `build/libs/`에 생성됩니다. 커스포지 프로필에서는 버전별 JAR을 쌓지 말고 `mods/league-of-minecraft.jar` 한 파일만 교체합니다.

## 명령어

```text
/lol champion darius|yone
/lol mode adventure|match
/lol level <1~18>
/lol xp <값>
/lol rank q|w|e|r
/lol status
/lol cooldown reset
/lol shop
/lol gold add <값>
/lol gold set <값>
```

## 협업 규칙

기능을 변경할 때 코드만 수정하지 말고 반드시 다음 문서도 함께 갱신합니다.

1. `gradle.properties`의 `mod_version`
2. `CHANGELOG.md`
3. `docs/HANDOFF.md`
4. 명령어나 구조가 바뀌면 `README.md`, `docs/DEVELOPMENT.md`, `docs/ARCHITECTURE.md`

업데이트 크기와 관계없이 작업 단위가 완성되고 빌드 검증을 통과할 때마다 GitHub에 커밋·푸시합니다.

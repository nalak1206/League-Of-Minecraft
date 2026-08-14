# 작업 인수인계

다른 컴퓨터나 작업자는 먼저 이 문서와 `ARCHITECTURE.md`, `ITEMS.md`, `CHANGELOG.md`를 읽습니다.

## 현재 기준

- 버전: `0.13.0`
- 브랜치: `main`
- 원격: `https://github.com/nalak1206/League-Of-Minecraft.git`
- 실행 환경: Minecraft 26.2 / Fabric Loader 0.19.3 / Fabric API 0.157.0+26.2 / Java 25
- 배포 파일: CurseForge `league of minecraft/mods/league-of-minecraft.jar`

## 구현된 기반

- 다리우스와 요네 P/Q/W/E/R, 전용 무기·VFX·SFX
- Z/X/C/V 입력, 액션바 쿨타임, 챔피언 전환 정리
- 공통 CC, 물리/마법 피해, 관통력, 치명타 계산
- 레벨 1~18, 경험치, 스킬 포인트와 스킬 랭크
- 골드, 처치 보상, MATCH 자연 골드, 저장 데이터
- 8페이지 상점 GUI, 시작 아이템·장화·역할군별 전설 아이템
- 6칸 가상 장비 인벤토리와 전설 아이템 36종의 1차 고유 효과

상점 전체 목록과 효과 구현 상태는 `docs/ITEMS.md`를 기준으로 합니다.

## 0.13.0 서비스 개편

- `ChampionDefinition`과 `ChampionRegistry`가 챔피언 공통 생명주기를 담당합니다.
- 다리우스는 상태와 VFX, 요네는 상태와 이동/잠금 서비스를 스킬 본체에서 분리했습니다.
- 아이템 효과는 `CombatEngine`의 공통 챔피언 피해 훅으로 발동합니다.
- 처치 시 원칙의 원형낫이 현재 챔피언의 궁극기 쿨타임을 공통 인터페이스로 환급합니다.

## 다음 작업

1. 공통 전투 계산과 챔피언 전환에 대한 자동 테스트 추가
2. 구원 위치 지정과 기사의 맹세 아군 지정 GUI 추가
3. 다리우스 입력 bootstrap과 요네 VFX를 더 작은 서비스로 추가 분리
4. 신규 챔피언을 `ChampionDefinition` 구현으로 등록

## 주의점

- `darius_skills` 네임스페이스를 바꾸면 기존 아이템·월드·리소스팩이 깨질 수 있습니다.
- 챔피언 무기는 일반 상점 장비와 별개이며 챔피언 전환 시 정리되어야 합니다.
- 피해 공식 수정은 `CombatEngine`과 챔피언별 고정 피해 경로를 모두 확인해야 합니다.
- 저장 필드를 추가할 때 `LolPlayerDataStore`의 저장과 불러오기를 같이 수정합니다.

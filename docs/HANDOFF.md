# 작업 인수인계

다른 컴퓨터나 작업자는 먼저 이 문서와 `ARCHITECTURE.md`, `ITEMS.md`, `CHANGELOG.md`를 읽습니다.

## 현재 기준

- 버전: `0.12.2`
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
- 6칸 가상 장비 인벤토리와 일부 고유 효과

상점 전체 목록과 효과 구현 상태는 `docs/ITEMS.md`를 기준으로 합니다.

## 0.12.2 구조 개편

- Java 패키지를 `kr.leagueofminecraft`로 통일했습니다.
- Fabric 진입점을 `LeagueOfMinecraftMod`로 분리했습니다.
- 챔피언, 공통 코어, 전투, 상점, 네트워크, 레지스트리, 믹스인을 기능별 폴더로 이동했습니다.
- 아이템 등록을 `registry/ModItems`로 모았습니다.
- 식별자는 `ModConstants.id`로 생성합니다.
- 예제 템플릿 코드와 `modid` 리소스를 제거했습니다.
- 믹스인 설정 파일을 `league_of_minecraft.mixins.json`으로 변경했습니다.
- 기존 저장·리소스 호환성을 위해 내부 ID `darius_skills`는 유지합니다.

## 다음 작업

1. `DariusSkills`의 입력 등록과 다리우스 전투 로직을 별도 bootstrap/service 클래스로 추가 분리
2. `YoneSkills`의 E 상태·이동과 VFX 헬퍼 분리
3. `ChampionDefinition` 인터페이스를 도입해 새 챔피언 등록을 데이터 중심으로 변경
4. 상점 고유 효과 미구현 항목을 `ITEMS.md` 순서대로 연결
5. 공통 전투 계산과 챔피언 전환에 대한 자동 테스트 추가

## 주의점

- `darius_skills` 네임스페이스를 바꾸면 기존 아이템·월드·리소스팩이 깨질 수 있습니다.
- 챔피언 무기는 일반 상점 장비와 별개이며 챔피언 전환 시 정리되어야 합니다.
- 피해 공식 수정은 `CombatEngine`과 챔피언별 고정 피해 경로를 모두 확인해야 합니다.
- 저장 필드를 추가할 때 `LolPlayerDataStore`의 저장과 불러오기를 같이 수정합니다.

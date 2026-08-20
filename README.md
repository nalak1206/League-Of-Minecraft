# League of Minecraft

Minecraft 26.2 Fabric에서 리그 오브 레전드식 챔피언 전투, 성장, 골드, 아이템 상점을 구현하는 모드입니다. 현재 플레이 가능한 챔피언은 다리우스와 요네이며 `Z/X/C/V`가 `Q/W/E/R`에 대응합니다.

현재 버전: `0.14.9`

## 빠른 시작

```powershell
$env:JAVA_HOME='C:\path\to\jdk-25'
.\gradlew.bat clean build
```

생성 파일: `build/libs/league-of-minecraft-0.14.9.jar`

기본 챔피언 무기는 핫바 1번에 고정됩니다. `Alt+1/2/3/5/6/7`은 M 인벤토리의 1~6번 장비 칸을 사용하고, `Alt+4`는 Q/W와 E/R 사이 장신구 칸을 사용합니다.

챔피언은 1레벨부터 레벨당 정확히 스킬 포인트 1개를 받습니다. 배우지 않은 Q/W/E/R을
입력하면 스킬이나 무기 변경 없이 `아직 스킬을 배우지 않았습니다!`가 표시됩니다.

주요 명령:

```text
/lol champion darius|yone
/lol mode adventure|match
/lol level <1~18>
/lol xp <값>
/lol rank q|w|e|r
/lol status
/lol cooldown reset
/lol shop
/lol inventory
/lol stats
/lol item use
/lol gold add|set <값>
/lol match team auto|blue|red|none
/lol match base set blue|red
/lol match start|stop|status|spawn
```

MATCH 맵에서는 먼저 각 팀 기지에서 `/lol match base set blue|red`를 실행한 뒤 플레이어가
`/lol match team auto`로 참가합니다. `/lol match start`를 실행하면 참가자가 팀 기지로 이동하며,
사망 후에도 해당 기지에서 부활합니다. 같은 팀끼리는 경기 중 피해를 입힐 수 없습니다.

경기 중 `B`를 누르면 8초 동안 귀환을 정신 집중한 뒤 자신의 팀 기지로 이동합니다. 이동,
피격, 기본 공격, 스킬 입력, 사망 또는 경기 종료 시 귀환이 취소되며 `B`를 다시 눌러 직접
취소할 수도 있습니다.

MATCH에서 투명 와드는 설치 당시 팀 소유가 됩니다. 아군은 아군 와드를 볼 수 있고, 예언자의
렌즈는 반경 12블록 안의 적 팀 와드만 10초 동안 드러냅니다. 야생 ADVENTURE 모드의 렌즈는
기존처럼 주변 생명체를 감지합니다. 적 와드를 공격해 파괴하면 10골드를 획득하며, 와드의
소유 팀과 남은 수명은 서버 재시작·청크 재로딩 후에도 유지됩니다.

구원은 장비 단축키로 사용자 중심 반경 8블록에 시전되어 아군을 회복하고 적에게 피해를
줍니다. 기사의 맹세를 보유한 상태에서 아군 플레이어를 우클릭하면 결속되며, 선택한 대상은
재접속 후에도 유지됩니다. 소유자가 체력 30% 초과·같은 차원·32블록 안에 있으면 결속 대상이
받은 실제 피해의 12%를 소유자에게 이전하고, 결속 대상이 적 챔피언에게 준 피해의 10%만큼
소유자를 회복합니다.

## 문서

- [프로젝트 구조](docs/ARCHITECTURE.md)
- [개발과 배포](docs/DEVELOPMENT.md)
- [작업 인수인계](docs/HANDOFF.md)
- [아이템 구현 현황](docs/ITEMS.md)
- [변경 내역](CHANGELOG.md)

Java 패키지와 Fabric ID, 아이템·리소스 네임스페이스는 모두 `league_of_minecraft`를 사용합니다.
0.14.8 이하에서 저장된 챔피언 무기 ID는 호환 등록 후 접속 시 현재 무기로 자동 교체됩니다.

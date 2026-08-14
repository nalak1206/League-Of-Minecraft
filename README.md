# League of Minecraft

Minecraft 26.2 Fabric에서 리그 오브 레전드식 챔피언 전투, 성장, 골드, 아이템 상점을 구현하는 모드입니다. 현재 플레이 가능한 챔피언은 다리우스와 요네이며 `Z/X/C/V`가 `Q/W/E/R`에 대응합니다.

현재 버전: `0.13.2`

## 빠른 시작

```powershell
$env:JAVA_HOME='C:\path\to\jdk-25'
.\gradlew.bat clean build
```

생성 파일: `build/libs/league-of-minecraft-0.13.2.jar`

기본 챔피언 무기는 핫바 1번에 고정됩니다. `Alt+1/2/3/5/6/7`은 M 인벤토리의 1~6번 장비 칸을 사용하고, `Alt+4`는 Q/W와 E/R 사이 장신구 칸을 사용합니다.

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
```

## 문서

- [프로젝트 구조](docs/ARCHITECTURE.md)
- [개발과 배포](docs/DEVELOPMENT.md)
- [작업 인수인계](docs/HANDOFF.md)
- [아이템 구현 현황](docs/ITEMS.md)
- [변경 내역](CHANGELOG.md)

Java 코드는 `kr.leagueofminecraft` 패키지를 사용합니다. 단, 기존 월드와 리소스팩 호환성을 위해 Fabric ID와 리소스 네임스페이스는 `darius_skills`를 유지합니다.

# 개발·빌드·배포 가이드

## 요구 환경

| 항목 | 버전 |
|---|---:|
| Minecraft | 26.2 |
| Fabric Loader | 0.19.3 |
| Fabric API | 0.157.0+26.2 |
| Java | 25 |

```powershell
git clone https://github.com/nalak1206/League-Of-Minecraft.git
cd League-Of-Minecraft
$env:JAVA_HOME='C:\path\to\jdk-25'
$env:GRADLE_USER_HOME='C:\.gradle-league-of-minecraft'
.\gradlew.bat clean build
```

Windows 사용자 경로에 한글이 있으면 Gradle 테스트 실행기의 클래스패스 파일이 깨질 수 있으므로 `GRADLE_USER_HOME`을 영문 경로로 지정합니다. 테스트만 실행하려면 `.\gradlew.bat test`를 사용합니다.

결과물은 `build/libs/league-of-minecraft-<version>.jar`입니다.

## 기능 추가 위치

- 챔피언: `src/main/java/kr/leagueofminecraft/champion/<champion>`
- 공통 전투 계산: `combat`
- 순수 로직 회귀 테스트: `src/test/java`
- 상태 이상·성장·플레이어 저장: `core`
- 팀·기지·부활·경기 상태: `match`
- 상점과 아이템 효과: `shop`
- 아이템 등록: `registry/ModItems.java`
- 패킷: `network`
- 키 입력: `src/client/java/kr/leagueofminecraft/client`
- 텍스처/모델/언어: `src/main/resources/assets/league_of_minecraft`

새 챔피언 로직을 하나의 거대한 진입점에 추가하지 않습니다. 챔피언별 폴더에 구현하고 `ChampionManager`에는 선택과 라우팅만 연결합니다.

## 업데이트 체크리스트

1. 기능과 회귀 테스트를 완료합니다.
2. `gradle.properties`의 `mod_version`을 올립니다.
3. `CHANGELOG.md`와 `docs/HANDOFF.md`를 갱신합니다.
4. `.\gradlew.bat clean build`를 실행합니다.
5. CurseForge 프로필에는 `mods/league-of-minecraft.jar` 하나만 둡니다.
6. 변경을 커밋하고 `main`에 푸시합니다.

빌드 산출물, `.gradle`, 개인 월드, CurseForge 프로필은 Git에 넣지 않습니다.

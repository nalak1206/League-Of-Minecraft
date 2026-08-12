# 개발·빌드·배포 가이드

## 1. 다른 컴퓨터에서 시작하기

```powershell
git clone https://github.com/nalak1206/League-Of-Minecraft.git
cd League-Of-Minecraft
./gradlew.bat build
```

Java 25가 기본 Java가 아니라면 빌드 전에 `JAVA_HOME`을 Java 25 경로로 설정합니다.

```powershell
$env:JAVA_HOME='C:\path\to\jdk-25'
./gradlew.bat clean build
```

## 2. 실행 환경

| 항목 | 버전 |
|---|---:|
| Minecraft | 26.2 |
| Fabric Loader | 0.19.3 |
| Fabric API | 0.157.0+26.2 |
| Java | 25 |

## 3. 로컬 테스트 배포

빌드 성공 후 `build/libs/*-버전.jar`를 커스포지 프로필의 다음 고정 파일명으로 복사합니다.

```text
<CurseForge>/minecraft/Instances/league of minecraft/mods/league-of-minecraft.jar
```

규칙:

- `league-of-minecraft.jar` 하나만 유지합니다.
- 버전별 JAR을 같은 `mods` 폴더에 함께 두지 않습니다.
- Minecraft가 실행 중일 때 JAR을 교체하지 않습니다.
- Fabric API JAR은 삭제하지 않습니다.

## 4. 버전과 문서 갱신

작업 하나를 마칠 때:

1. 테스트 가능한 단위로 코드를 마무리합니다.
2. `./gradlew.bat clean build`가 성공하는지 확인합니다.
3. `gradle.properties`의 `mod_version`을 올립니다.
4. `CHANGELOG.md`에 실제 구현과 제한 사항을 적습니다.
5. `docs/HANDOFF.md`의 현재 상태와 다음 작업을 갱신합니다.
6. 커스포지 프로필의 단일 JAR을 교체합니다.
7. 큰 업데이트 또는 요청을 받으면 Git 커밋·푸시합니다.

권장 버전 규칙:

- 작은 기능/버그 수정: `0.7.3 -> 0.7.4`
- 큰 시스템 완성: `0.7.x -> 0.8.0`
- 플레이 가능한 1차 정식 통합본: `1.0.0`

## 5. 코드 작성 원칙

- 챔피언 입력은 `ChampionManager`를 거칩니다.
- CC는 포션 효과 대신 가능한 한 `CrowdControl`에 의미를 기록합니다.
- 골드와 구매품은 `PlayerEconomy`, 상점 정의는 `LolShopItem`에 둡니다.
- 월드별 저장 필드를 추가하면 `LolPlayerDataStore`의 저장과 불러오기를 동시에 수정합니다.
- 스킬/아이템 피해는 향후 공통 피해 엔진으로 모을 예정입니다. 새 임시 피해 코드는 제한 사항을 문서에 남깁니다.
- 채팅을 도배하는 디버그 문구를 추가하지 않습니다. 상태 표시는 액션바나 명령 결과를 사용합니다.

## 6. GitHub 게시

원격 저장소:

```text
https://github.com/nalak1206/League-Of-Minecraft.git
```

큰 업데이트나 소유자의 요청 때만 커밋·푸시합니다. 푸시 전 빌드 성공, 변경 파일, 비밀 정보와 대용량 생성물 포함 여부를 확인합니다. `build/`, `.gradle/`, 개인 월드와 커스포지 프로필은 커밋하지 않습니다.

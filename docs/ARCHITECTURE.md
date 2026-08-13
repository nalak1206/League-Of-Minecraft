# 프로젝트 구조

## 진입과 입력

- `DariusSkills.java`: Fabric 서버 초기화, 네트워크 수신, 다리우스 런타임, 서버 틱과 공격 훅
- `DariusSkillsClient.java`: Z/X/C/V 키 입력을 서버로 전송
- `SkillPayload.java`: 스킬 입력 네트워크 페이로드
- `ChampionManager.java`: 챔피언·모드 선택, 공통 명령어, 챔피언별 스킬 라우팅

현재 메인 진입점 이름은 역사적 이유로 `DariusSkills`이며 모드 ID도 `darius_skills`입니다. 기능 안정화 뒤 `lol_hyunmin` 또는 `league_of_minecraft`로 일괄 마이그레이션할 예정입니다. 중간에 ID만 일부 변경하면 아이템·리소스·저장 데이터가 깨질 수 있으므로 금지합니다.

## 챔피언

- `DariusSkills.java`: 다리우스 P/Q/W/E/R, 전용 무기와 VFX
- `YoneSkills.java`: 요네 P/Q/W/E/R, 시전 잠금, Q 폭풍 중첩, E 영체 피해 기록과 R 집결
- `ChampionProgression.java`: 레벨, XP, 스킬 포인트와 Q/W/E/R 랭크

## 전투 공통

- `CrowdControl.java`: 둔화, 속박, 기절, 에어본, 침묵, 무장해제, 시야 차단, 이동기 차단, 제압 상태
- `combat/CombatEngine.java`: 서버 피해 소스, 물리·마법 분류, 무적 프레임과 넉백 정책을 한 경로로 처리
- `combat/CriticalStrikeEngine.java`: 아이템 치명타 확률·피해, 요네 확률 증폭·피해 계수의 공통 판정
- 아직 부족한 핵심: 챔피언 스킬 연결, 고정 피해, 방어력·마법 저항력과 관통력 계산

## 상점과 아이템

- `shop/LolShop.java`: 상점 열기
- `shop/LolShopMenu.java`: 6줄 상자 GUI, 페이지와 구매 클릭 처리
- `shop/LolShopItem.java`: 상품 이름, 가격, 분류, 아이콘과 기본 능력치
- `shop/PlayerEconomy.java`: 골드, 소유 아이템, 플레이어 능력치 반영
- `shop/LegendaryItemEffects.java`: 구현된 전설 아이템 런타임 효과

구매 아이템은 실제 인벤토리를 차지하지 않는 가상 장비입니다. 챔피언 스킬 무기가 1번 슬롯에서 바뀌는 구조와 충돌하지 않기 위한 결정입니다.

## 저장

- `LolPlayerDataStore.java`
- 월드 폴더의 `data/lol_hyunmin_players.json`

저장 대상:

- 선택 챔피언과 게임 모드
- 레벨, XP, 스킬 포인트와 랭크
- 골드와 구매한 가상 아이템

## 리소스

- `assets/darius_skills`: 아이템 모델, 언어, 텍스처
- `data/darius_skills/damage_type`: 다리우스 궁극기 고정 피해 데이터
- `darius_skills.mixins.json`: 디스플레이/아이템 보호 관련 믹스인

## 향후 분리 목표

현재 `DariusSkills.java`에 서버 초기화와 다리우스 구현이 함께 있어 파일이 큽니다. 안정화 과정에서 아래처럼 분리합니다.

```text
LeagueOfMinecraftMod
├─ combat/
├─ champion/darius/
├─ champion/yone/
├─ economy/
├─ item/
└─ match/
```

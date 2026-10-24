# 주행 중 요청 알림과 화면 밖 수신 검토

2026-10-24 공식 문서와 로컬 작업 코드 대조 기록. 제품 결정은 [픽업 시작 방식과 대기 요청](pickup-request-flow.md)을 기준으로 한다. 이 문서는 알림 노출·주행 중 수락·종료 후 수신이 검증됐다는 뜻이 아니다.

## 공식 문서로 확인한 범위

### 화면 밖 수신과 주행 중 수락은 별개다

Pleos는 화면에서 사라진 앱이 백그라운드에 남을 수 있다고 설명한다. 통신 같은 백그라운드 작업은 실행 조건과 정지·재개 정책을 정의해야 하며, 불필요한 상주·타이머·주기 요청을 줄이도록 안내한다. 앱이 화면 밖에 있다는 이유로 항상 종료되는 것은 아니지만, 항상 살아 있음을 보장하는 문서도 아니다.

[앱 구조 및 동작 모델](https://document.pleos.ai/en/docs/pleos-only/vehicle-app-planning-guide/app-structure-screen-model)

### 주행 중 화면은 조건부다

Pleos는 DO 미선언 앱의 주행 중 사용을 제한한다. DO 선언 앱도 CarUxRestrictions와 ActiveRestrictions에 따라 기능을 제한해야 한다. Mobility Services의 일부 기능은 제한적으로 허용될 수 있지만, 문서에 마중 요청 수락이 허용된다고 명시돼 있지는 않다. DO 선언만으로 심사 통과나 실차 사용 허용을 주장하지 않는다.

- [지역별 주행 규제 정책](https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/driving-restiction-policy)
- [주행 규제 개발 가이드](https://document.pleos.ai/docs/pleos-only/vehicle-app-developer-guide/driving-restriction-development-guide)
- [앱 카테고리](https://document.pleos.ai/en/docs/pleos-only/vehicle-app-planning-guide/app-category)

### 시스템 알림 UI가 있다는 것과 우리 버튼이 허용된다는 것은 다르다

Pleos 디자인 가이드에는 시스템 알림과 Action 구성이 있다. 다만 이 가이드는 마중이 사용할 수 있는 알림 분류·권한·주행 중 버튼 동작을 보증하지 않는다.

AAOS 일반 문서는 일반 앱의 헤드업 알림 분류를 제한하고, MessagingStyle에서는 지정된 동작 외 추가 버튼을 표시하지 않는다고 설명한다. 메시지 알림의 기본 재생·음소거 버튼을 임의의 픽업 수락 버튼과 동일하게 취급하면 안 된다. Pleos의 실제 동작과 배포 정책은 별도 확인 대상이다.

- [Pleos 시스템 알림](https://document.pleos.ai/en/docs/connect/guide/docs-design/components/notification/system-notification)
- [AAOS 알림 제약](https://developer.android.com/training/cars/platforms/automotive-os/notifications)

## 현재 코드와의 차이

2026-10-24 로컬 작업 브랜치 기준이다. main 반영 여부와 별개다.

- 차량 앱은 DO를 선언하지 않는다. 현재 DrivingAccess는 제한 없음이 확인된 상태에서만 조작을 허용한다.
- MainActivity의 화면 갱신·조회 타이머는 onStop에서 멈춘다.
- PickupSharingService는 이미 승인된 ACTIVE·ARRIVED 픽업의 공유를 유지한다. 픽업이 없는 상태에서 새 요청을 기다리는 서비스가 아니다.
- 이 서비스는 START_NOT_STICKY다. 강제 중지·프로세스 종료·시동 재시작 뒤 자동 수신을 보장하지 않는다.
- 현재 에뮬레이터의 패키지 조회에서 Google Play services 패키지를 찾지 못했다. 일반 휴대전화의 FCM 구성을 그대로 사용할 수 있다고 가정하지 않는다. 실차·다른 이미지의 지원 여부는 별도다.

## 구현 확정 전 시험

1. 앱 화면이 없는 상태에서 예제 요청을 수신하는 경로부터 검증한다. 실사용 서버 요청과 로컬 시험 입력은 구분한다.
2. 다른 앱이 앞에 있는 동안 일반 알림과 헤드업 표시 여부를 각각 확인한다.
3. 실제 P → D → P 제한 전환에서 내용·버튼 표시와 실행 가능 여부를 확인한다. 허용되지 않은 카테고리 위장, 오버레이·강제 화면 전환으로 우회하지 않는다.
4. 수락 즉시 위치를 보내지 않고, 요청 유효성·권한·현재 픽업 충돌과 경로 반영 결과를 서버와 SDK에서 확인한다.
5. 알림 권한 거절, 네트워크 끊김, 만료·취소 뒤 늦은 수락, 화면 복귀, 프로세스 종료를 구분해 시험한다.
6. 에뮬레이터 통과와 Pleos 배포 심사·실차 허용 여부를 구분한다. 공개 문서로 확정할 수 없는 헤드업·동작 분류는 제공 측 확인 대상으로 남긴다.


# 픽업 상태와 데이터 초안

2026-10-22. 다음 구현을 위한 계약 초안이다. 현재 서버·API·인증은 구현하지 않았다.

## 상태를 나누는 이유

‘준비 완료’와 ‘차량 도착’은 같은 상태가 아니다. 운전자가 아직 이동 중이어도 탑승자는 준비를 끝낼 수 있다.
픽업 진행, 탑승자 준비, 장소 변경, 연결 상태를 각각 관리한다.

| 구분 | 상태 |
| --- | --- |
| 픽업 진행 | INVITING → WAITING_APPROVAL → ACTIVE → ARRIVED → COMPLETED |
| 종료 분기 | CANCELLED, EXPIRED |
| 탑승자 준비 | WAITING, PREPARING, READY |
| 장소 변경 | 제안 없음 / 제안 중 / 경로 반영 중 / 실패 |
| 차량 정보 | 정상 수신 / 갱신 지연 / 연결 끊김 |

완료·취소·만료 이후에는 늦게 도착한 위치나 준비 상태 변경으로 픽업이 다시 활성화되지 않아야 한다.

## 필요한 데이터

| 대상 | 주요 필드 | 주의점 |
| --- | --- | --- |
| 픽업 | pickupId, status, createdAt, endedAt, version | 추측하기 어려운 식별자. 식별자를 아는 것만으로 조회 권한을 주지 않음 |
| 참여 | participantId, displayName, approvalStatus, readiness | 이름은 본인 인증 수단이 아님 |
| 차량·휴대전화 연결 | pairingId, expiresAt, usedAt | 운전자용 연결과 탑승자 초대를 분리. 재사용·무차별 대입 제한 필요 |
| 초대 | inviteId, expiresAt, revokedAt, acceptedParticipantId | 토큰 원문을 로그·분석 도구에 남기지 않음 |
| 확정 장소 | placeVersion, label, latitude, longitude, provider, poiId | 다른 지도 사업자 ID를 호환된다고 가정하지 않음 |
| 변경 제안 | proposalId, requestedPlace, basePlaceVersion, status | 제안과 확정 장소를 별도로 보관 |
| 차량 정보 | routeId, placeVersion, latitude, longitude, durationSeconds, sourceTime, receivedAt | SDK 경로 식별값이 비어 있거나 바뀌는 상황 처리 필요 |
| 전달 확인 | eventId, sequence, receivedByCarAt | 서버 저장 성공과 차량 반영을 구분 |

실제 구현에서 필요성이 없는 차량 식별 정보·이동 경로 전체·탑승자 휴대전화 위치는 수집하지 않는 방향이다.
보관 기간은 미정이다. 종료 시 조회 차단과 보관 데이터 삭제·정리 정책을 따로 정해야 한다.

## 권한 구분

- 차량 측: 해당 픽업의 소유 세션만 참여 승인·차량 정보 전송·장소 확정·종료를 수행한다.
- 운전자 휴대전화: 허용된 초대 준비 권한만 갖게 한다. 차량 측 소유 권한 전체를 그대로 넘기지 않는다.
- 탑승자 웹: 승인받은 픽업의 공개 정보만 조회하고 자신의 준비 상태와 장소 제안만 바꾼다.
- 실제 인증 방식은 아직 선택하지 않았다. 초안의 화면 상태나 표시 이름을 서버 권한 판단에 사용하지 않는다.

## 다음 서버 작업의 완료 기준

1. 승인 전·다른 픽업·종료된 픽업의 위치 조회를 서버가 거절한다.
2. 초대 만료·철회·재사용 제한을 서버에서 처리한다.
3. 중복·역순 이벤트와 재접속에도 현재 상태가 되돌아가지 않는다.
4. 장소 변경 결과가 늦게 오더라도 proposalId·placeVersion·routeId를 대조해 이전 응답이 새 약속을 덮지 않게 한다.
5. 데이터가 오래됐는지 판단할 기준을 실제 SDK 수신 간격과 연결 상태에 맞춰 정한다. API 필드가 있다는 사실만으로 갱신 주기를 가정하지 않는다.
6. 완료·취소 직후 전송 중단과 조회 차단을 모두 확인한다.

REST 경로명, 실시간 전송 방식, 인증 토큰 형식, DB 구조는 이 요구를 기준으로 다음 단계에서 정한다.

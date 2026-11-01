# 멘토링 UI 변경과 복원

- 비교스캔 API 준비 전 `ScannerAvailability.compareEnabled=false`로 선택 UI 및 비교 목록 탭을 숨긴다. 재실행 시 기존 비교 모드 상태도 일반 모드로 바꾼다. 비교 화면/기능 코드는 유지한다.
- **복원 필수:** API 연결과 실서버 검증을 마친 후 compareEnabled를 true로 바꾸고 일반/비교 선택 및 비교 목록 동작을 확인한다. 임시 숨김 상태를 기능 삭제로 판단하지 않는다.
- 하단바는 공통 JjikmukBottomNavigationBar에서 Home/Product/Chat/MY만 표시한다. Diet enum은 이전 상태 호환을 위해 남아 있으나 메뉴에 노출하지 않는다.
- 권한 안내는 체크 없는 일괄 요청 UI다. 안내를 완료/보류한 기록을 저장한다. 로그인 세션이 있거나 카메라가 이미 허용된 경우에도 안내를 건너뛴다. 실제 권한 상태는 카메라 등 각 기능의 런타임 검사로 확인한다.

# AI Usage Log

## 2025-02-14
- Patch 1: Spring Boot config and minimal models
  - Added Spring Boot build config
  - Added domain models
  - Added validation annotations
- Patch 2: Holder and account endpoints
  - Added shared in-memory store
  - Added account endpoints
  - Added holder account listing
- Patch 3: Transactions
  - Added transaction request DTO and controller
  - Added transaction storage and balance updates
- Patch 4: Transaction validation
  - Added cross-field validation annotation
  - Removed controller-level field checks
- Patch 5: Transaction null recipient/sender
  - Allowed null sender/recipient IDs for deposits/withdrawals
- Patch 6: Tightened transaction validation
  - Enforced sender/recipient presence rules by type
- Patch 7: SQLite persistence
  - Added JDBC + SQLite dependencies and schema
  - Replaced in-memory store with JDBC store

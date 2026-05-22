# BidMart Wallet Service

`bidmart-wallet-service` adalah microservice untuk domain saldo dan wallet lifecycle pada BidMart. Service ini dipisahkan dari monolith `Bidmart` dengan pendekatan strangler pattern agar migrasi bisa bertahap tanpa mematikan sistem lama.

## Fungsi Utama

- Menyimpan saldo tersedia (`availableBalance`) dan saldo tertahan (`heldBalance`) per user.
- Menjalankan operasi top-up dan withdraw.
- Menjalankan hold fund untuk proses bidding.
- Menjalankan release hold saat bid kalah atau auction batal.
- Menjalankan capture hold saat pemenang auction ditetapkan.
- Menyediakan riwayat transaksi dan jejak audit dasar.

## Data Ownership

Service ini memiliki ownership untuk:

- Wallet state per `userId`.
- Hold state per `holdId` (`HELD`, `RELEASED`, `CAPTURED`).
- Auction-aware hold state untuk integrasi bidding internal.
- Ledger transaksi wallet.

Service lain tidak boleh mengakses database wallet secara langsung. Interaksi harus melalui API contract.

## API Contract

### Public Wallet Endpoints

- `GET /wallets/{userId}/balance`
- `POST /wallets/{userId}/top-up`
- `POST /wallets/{userId}/withdraw`
- `POST /wallets/{userId}/holds`
- `POST /wallets/{userId}/holds/{holdId}/release`
- `POST /wallets/{userId}/holds/{holdId}/capture`
- `GET /wallets/{userId}/transactions`

Public hold endpoints tetap menggunakan `holdId` dan mempertahankan response body yang sudah ada.

### Internal Bidding Endpoints

Endpoint ini dipakai oleh `bidmart-bidding-command-service`:

- `POST /wallet/internal/hold`
- `POST /wallet/internal/release`
- `POST /wallet/internal/capture`

Request body:

```json
{
  "userId": "UUID",
  "auctionId": "UUID",
  "amount": 25000
}
```

Success response:

- `204 No Content`

Untuk `release` dan `capture`, `amount` harus sama dengan active hold amount untuk pasangan `userId + auctionId`. Perbandingan nominal dilakukan sebagai nilai `BigDecimal`, bukan string formatting.

Error response:

```json
{
  "code": "ERROR_CODE",
  "message": "Human readable message"
}
```

Important error codes:

| Code | HTTP status |
|---|---:|
| `INVALID_AMOUNT` | 400 |
| `INSUFFICIENT_BALANCE` | 409 |
| `INSUFFICIENT_HELD_BALANCE` | 409 |
| `HOLD_OWNERSHIP_MISMATCH` | 403 |
| `ACTIVE_HOLD_NOT_FOUND` | 404 |
| `HOLD_AMOUNT_MISMATCH` | 409 |
| `INVALID_HOLD_STATE` | 409 |
| `INVALID_REQUEST` | 400 |

## Rencana Idempotency

Idempotency dibutuhkan untuk mencegah duplikasi command saat retry jaringan:

1. **Hold fund**
   - Client public hold dapat mengirim `idempotencyKey` unik per command hold.
   - Jika key sama dikirim ulang, service mengembalikan hasil hold yang sama tanpa memotong saldo ulang.
2. **Release hold**
   - `Idempotency-Key` pada public release memastikan hold yang sudah dilepas tidak diproses dua kali.
3. **Capture hold**
   - `Idempotency-Key` pada public capture memastikan hold yang sama tidak dicapture berulang.

Internal `/wallet/internal/*` saat ini memakai lookup active hold berdasarkan `userId + auctionId`. Penyimpanan masih in-memory/bootstrap-level. Produksi harus pindah ke penyimpanan persisten, misalnya Redis dan database transaction boundary.

## Dependency ke Service Lain

- **bidmart-bidding-command-service**: memanggil endpoint internal hold/release/capture.
- **bidmart-gateway**: facade API eksternal selama strangler pattern berjalan.
- **bidmart-auth-service**: event user registration dapat dipakai untuk wallet provisioning otomatis.

Saat integrasi lokal, pastikan `bidmart-bidding-command-service` mengarah ke URL wallet service:

```properties
WALLET_SERVICE_BASE_URL=http://localhost:8084
```

## Run Lokal

```bash
./gradlew bootRun
```

Default port: `8084` melalui konfigurasi:

```properties
server.port=${PORT:8084}
```

## Test

```bash
./gradlew test
```

## Manual Verification Lokal

Contoh PowerShell berikut memakai UUID tetap agar flow mudah diulang. Jalankan wallet service di `http://localhost:8084` lebih dulu.

```powershell
$userId = "11111111-1111-1111-1111-111111111111"
$auctionId = "22222222-2222-2222-2222-222222222222"
$baseUrl = "http://localhost:8084"

Invoke-RestMethod `
  -Method Post `
  -Uri "$baseUrl/wallets/$userId/top-up" `
  -ContentType "application/json" `
  -Body '{"amount":100000,"idempotencyKey":"manual-topup-1"}'

Invoke-WebRequest `
  -Method Post `
  -Uri "$baseUrl/wallet/internal/hold" `
  -ContentType "application/json" `
  -Body "{`"userId`":`"$userId`",`"auctionId`":`"$auctionId`",`"amount`":25000}"

Invoke-WebRequest `
  -Method Post `
  -Uri "$baseUrl/wallet/internal/release" `
  -ContentType "application/json" `
  -Body "{`"userId`":`"$userId`",`"auctionId`":`"$auctionId`",`"amount`":25000}"

Invoke-WebRequest `
  -Method Post `
  -Uri "$baseUrl/wallet/internal/hold" `
  -ContentType "application/json" `
  -Body "{`"userId`":`"$userId`",`"auctionId`":`"$auctionId`",`"amount`":25000}"

Invoke-WebRequest `
  -Method Post `
  -Uri "$baseUrl/wallet/internal/capture" `
  -ContentType "application/json" `
  -Body "{`"userId`":`"$userId`",`"auctionId`":`"$auctionId`",`"amount`":25000}"
```

## Coupling yang Masih Harus Diputus

- Dual source saldo dari monolith lama (`app_user` vs `wallet`) belum sepenuhnya dipensiunkan.
- Kontrak event (`auction-won`, `auction-lost`, `auction-cancelled`) belum final.
- Audit trail masih basic transaction log, belum append-only immutable store.

## Profiling and Logging

Profiling dan logging membantu memastikan operasi wallet tetap benar, mudah diobservasi, dan responsif pada flow normal maupun retry. Area utama yang perlu diamati:

- Top up wallet.
- Withdraw wallet.
- Hold funds untuk bid/internal flow.
- Release funds.
- Capture funds.
- Auction credit.
- Retrieval riwayat transaksi.

`WalletTransactionService` mencatat log info setelah transaksi wallet berhasil disimpan. Log ini berisi `userId`, transaction type, dan reference. Log sengaja tidak menyertakan token, password, secret, raw request headers, full object dumps, database URL, atau transaction amount.

Skenario profiling/performance yang disarankan:

- Repeated top up requests.
- Repeated bid/hold requests, termasuk retry dengan idempotency key yang sama.
- Release/capture retry dengan idempotency key yang sama.
- Retrieval transaction history untuk user dengan jumlah transaksi bertambah.
- Mixed wallet flow: top up, hold, release, hold, capture, dan auction credit.

Metrik yang disarankan:

- Response time.
- Throughput.
- Error rate.
- JVM memory usage.
- Database latency atau query time jika tersedia.
- Konsistensi jumlah transaksi setelah retry.

Tools yang bisa dipakai:

- `./gradlew clean test` atau `.\gradlew.bat clean test` untuk regression check.
- Application logs untuk bukti transaction recording.
- Spring Boot Actuator jika enabled.
- JMeter untuk HTTP load testing.
- Java Flight Recorder atau IDE profiler untuk profiling JVM.

Frontend smoke check ringan:

- Login.
- Top up wallet.
- Perform bid.
- Check wallet balance.
- Inspect wallet service logs untuk event transaction recording.

Limitasi saat ini:

- Bukti profiling masih lightweight, belum full benchmark.
- Logging membantu observability, tetapi bukan distributed tracing.
- Idempotency cache masih in-memory dan cocok untuk satu instance service saja sampai di-upgrade.
- Observability production-grade dapat membutuhkan persistent idempotency record, trace ID, dan cross-service correlation.

## Status Migrasi

- OK: Scaffold service wallet berdiri di repo terpisah.
- OK: Endpoint public wallet tersedia.
- OK: Endpoint internal bidding compatibility tersedia.
- Note: Belum ada persistence DB production-grade.
- Note: Belum ada distributed lock/outbox untuk exactly-once semantics.

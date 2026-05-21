# BidMart Wallet Service

`bidmart-wallet-service` adalah microservice untuk domain saldo dan wallet lifecycle pada BidMart. Service ini dipisahkan dari monolith `Bidmart` dengan pendekatan strangler pattern agar migrasi bisa bertahap tanpa mematikan sistem lama.

## Domain Tanggung Jawab

- Menyimpan saldo tersedia (`availableBalance`) dan saldo tertahan (`heldBalance`) per user.
- Menjalankan operasi top-up dan withdraw.
- Menjalankan hold fund untuk proses bidding.
- Menjalankan release hold saat bid kalah atau auction batal.
- Menjalankan capture hold saat pemenang auction ditetapkan.
- Menyediakan riwayat transaksi dan jejak audit dasar.

## Endpoint Publik (via Gateway)

- `GET /wallet/balance`
- `POST /wallet/topup`
- `GET /wallet/transactions`

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

Lihat `.env.example`.

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
cp .env.example .env
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

## Status Migrasi

- OK: Scaffold service wallet berdiri di repo terpisah.
- OK: Endpoint public wallet tersedia.
- OK: Endpoint internal bidding compatibility tersedia.
- Note: Belum ada persistence DB production-grade.
- Note: Belum ada distributed lock/outbox untuk exactly-once semantics.

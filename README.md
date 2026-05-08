# BidMart Wallet Service

`bidmart-wallet-service` adalah microservice untuk domain saldo dan wallet lifecycle pada BidMart. Service ini dipisahkan dari monolith `Bidmart` (branch sumber: `feat/auction-query-rollout`) dengan pendekatan strangler pattern agar migrasi bisa bertahap tanpa mematikan sistem lama.

## Fungsi Utama

- Menyimpan saldo tersedia (`availableBalance`) dan saldo tertahan (`heldBalance`) per user.
- Menjalankan operasi top-up dan withdraw.
- Menjalankan hold fund untuk proses bidding.
- Menjalankan release hold saat bid kalah/auction batal.
- Menjalankan capture hold saat pemenang auction ditetapkan.
- Menyediakan riwayat transaksi dan jejak audit dasar.

## Data Ownership

Service ini memiliki ownership untuk:

- Wallet state per `userId`.
- Hold state per `holdId` (`HELD`, `RELEASED`, `CAPTURED`).
- Ledger transaksi wallet.

Service lain **dilarang** mengakses database wallet secara langsung. Interaksi harus via API contract.

## API Contract (Minimal)

- `GET /wallets/{userId}/balance`
- `POST /wallets/{userId}/top-up`
- `POST /wallets/{userId}/withdraw`
- `POST /wallets/{userId}/holds`
- `POST /wallets/{userId}/holds/{holdId}/release`
- `POST /wallets/{userId}/holds/{holdId}/capture`
- `GET /wallets/{userId}/transactions`

## Rencana Idempotency

Idempotency dibutuhkan untuk mencegah duplikasi command saat retry jaringan:

1. **Hold fund**
   - Client (bidding command service) wajib mengirim `idempotencyKey` unik per command hold.
   - Jika key sama dikirim ulang, service mengembalikan hasil hold yang sama tanpa memotong saldo ulang.
2. **Release hold**
   - `Idempotency-Key` pada release memastikan hold yang sudah dilepas tidak diproses dua kali.
3. **Capture hold**
   - `Idempotency-Key` pada capture memastikan hold yang sama tidak dicapture berulang.

> Implementasi saat ini masih in-memory cache untuk bootstrap. Produksi harus pindah ke penyimpanan persisten (mis. Redis + DB transaction boundary).

## Dependency ke Service Lain

- **bidmart-bidding-command-service**: memanggil endpoint hold/release/capture.
- **bidmart-gateway**: façade API eksternal selama strangler pattern berjalan.
- **bidmart-auth-service**: event user registration dapat dipakai untuk wallet provisioning otomatis.

## Run Lokal

```bash
./gradlew bootRun
```

Default port: `8084`.

## Test

```bash
./gradlew test
```

## Coupling yang Masih Harus Diputus

- Dual source saldo dari monolith lama (`app_user` vs `wallet`) belum sepenuhnya dipensiunkan.
- Kontrak event (auction-won, auction-lost, auction-cancelled) belum final.
- Audit trail masih basic transaction log, belum append-only immutable store.

## Status Migrasi

- ✅ Scaffold service wallet berdiri di repo terpisah.
- ✅ Endpoint minimum tersedia.
- ⚠️ Belum ada persistence DB production-grade.
- ⚠️ Belum ada distributed lock/outbox untuk exactly-once semantics.

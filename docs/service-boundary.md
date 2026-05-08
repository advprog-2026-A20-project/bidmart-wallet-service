# Service Boundary - bidmart-wallet-service

## Ruang Lingkup Domain

Service ini hanya menangani domain wallet:

- saldo user;
- hold/release/capture dana;
- histori transaksi wallet.

## Yang Termasuk

- Wallet aggregate (`availableBalance`, `heldBalance`).
- Hold aggregate (`holdId`, `userId`, `amount`, `status`).
- Wallet transaction ledger.

## Yang Tidak Termasuk

- Validasi bisnis listing/auction.
- Penentuan winner bidding.
- Otentikasi/otorisasi user.
- Notification dispatch.

## API Boundary

### Public-ish

- `GET /wallets/{userId}/balance`
- `POST /wallets/{userId}/top-up`
- `POST /wallets/{userId}/withdraw`
- `GET /wallets/{userId}/transactions`

### Internal antar-service

- `POST /wallets/{userId}/holds`
- `POST /wallets/{userId}/holds/{holdId}/release`
- `POST /wallets/{userId}/holds/{holdId}/capture`

## Idempotency & Consistency Notes

- Hold/release/capture wajib idempotent karena command service bisa retry.
- Pada fase bootstrap ini idempotency disimpan in-memory (sementara).
- TODO produksi: simpan idempotency record persisten + TTL + unique index.
- TODO produksi: outbox/inbox pattern agar sinkron dengan event auction.

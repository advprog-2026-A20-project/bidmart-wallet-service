# Wallet Service Boundary

## Tanggung Jawab

- Menjadi source of truth saldo dan hold.
- Menyediakan operasi idempotent untuk hold, release, dan capture.
- Menyimpan ledger transaksi wallet.

## Kontrak Minimal

```http
GET /api/wallet/balance
POST /api/wallet/topup
GET /api/wallet/transactions
POST /internal/wallet/holds
POST /internal/wallet/releases
POST /internal/wallet/captures
```

Setiap command internal wajib membawa `idempotencyKey`.

## Risiko yang Harus Ditutup

- Double hold saat retry.
- Wallet hold berhasil tetapi bid gagal.
- Bid berhasil tetapi release previous leader gagal.
- Dual source saldo antara user dan wallet table.

# BidMart Wallet Service

Repository ini adalah target bounded context untuk wallet dan payment reservation BidMart. Pada fase ini repo masih berupa scaffold agar source of truth saldo dapat dirapikan sebelum logic dipindahkan dari gateway.

## Service Boundary

Wallet service akan menangani:

- Balance account.
- Top up.
- Hold dana untuk bid.
- Release hold saat bidder kalah atau auction unsold.
- Capture hold saat winner ditentukan.
- Ledger transaksi wallet.

Wallet service tidak boleh bergantung pada entity auction atau user JPA dari service lain. Semua operasi internal harus memakai ID dan idempotency key.

## Run Lokal

```bash
./gradlew bootRun
```

Default port:

```text
8084
```

## Test

```bash
./gradlew test
```

## Dependency Service Lain

- Bidding command service memanggil hold/release/capture.
- Auth service mem-publish `UserRegistered` agar wallet account dapat dibuat.
- Gateway meneruskan endpoint wallet publik.

## Catatan Migrasi

Monolith lama masih memiliki dua model saldo: `app_user.availableBalance/heldBalance` dan `wallet.balance`. Ini harus disatukan sebelum wallet service menjadi source of truth.

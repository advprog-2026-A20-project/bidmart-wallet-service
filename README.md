# BidMart Wallet Service

`bidmart-wallet-service` adalah source of truth saldo wallet BidMart.

## Domain Tanggung Jawab

- saldo tersedia (`availableBalance`)
- saldo tertahan (`heldBalance`)
- transaksi wallet
- hold/release/capture fund untuk lifecycle bidding

## Endpoint Publik (via Gateway)

- `GET /wallet/balance`
- `POST /wallet/topup`
- `GET /wallet/transactions`

## Endpoint Internal

- `POST /wallet/internal/hold`
- `POST /wallet/internal/release`
- `POST /wallet/internal/capture`

## Endpoint Structured Wallet API

- `GET /wallets/{userId}/balance`
- `POST /wallets/{userId}/top-up`
- `POST /wallets/{userId}/withdraw`
- `POST /wallets/{userId}/holds`
- `POST /wallets/{userId}/holds/{holdId}/release`
- `POST /wallets/{userId}/holds/{holdId}/capture`
- `GET /wallets/{userId}/transactions`

## Environment

Lihat `.env.example`.

Variabel utama:

- `PORT` (default `8085`)
- `JWT_SECRET`, `JWT_EXP_SECONDS`
- `CORS_ALLOWED_ORIGINS`

## Local Run

```bash
cp .env.example .env
./gradlew bootRun
```

## Test

```bash
./gradlew test
```

## Docker

```bash
docker build -t bidmart-wallet-service .
docker run --env-file .env -p 8085:8085 bidmart-wallet-service
```

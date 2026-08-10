# 🐳 Hướng dẫn chạy EcoMart Backend bằng Docker

Dự án được container hóa sẵn với **2 service** trong [`compose.yaml`](../compose.yaml):

| Service | Container | Mô tả |
|---|---|---|
| `postgres` | `ecomart-postgres` | Database PostgreSQL 16, dữ liệu lưu trong volume `postgres_data` |
| `app` | `ecomart-backend` | Spring Boot app (build từ [`Dockerfile`](../Dockerfile)), chạy port `8081` |

```
┌─────────────────────────────┐
│         Docker Compose       │
│                              │
│  ┌──────────┐     ┌────────┐ │
│  │   app    │────▶│ postgres│ │
│  │ :8081    │  JDBC  :5432  │ │
│  └──────────┘     └────────┘ │
└─────────────────────────────┘
```

---

## ✅ Yêu cầu

- **Docker Desktop** (Windows/Mac) hoặc Docker Engine + Docker Compose plugin (Linux).
- Kiểm tra nhanh:

```bash
docker --version
docker compose version
```

---

## 🚀 Chạy nhanh (chỉ 1 lệnh)

```bash
docker compose up -d --build
```

Lệnh này sẽ:

1. Build image `app` từ `Dockerfile` (lần đầu hơi lâu vì phải tải dependencies).
2. Khởi động PostgreSQL → chờ `postgres` **healthy** → mới khởi động app.
3. Map port: `8081` (app) và `5432` (PostgreSQL).

Sau khi chạy, truy cập:

| Gì | URL |
|---|---|
| 🌐 Ứng dụng (REST API) | <http://localhost:8081> |
| 📄 Swagger UI (tài liệu API tương tác) | <http://localhost:8081/swagger-ui.html> |
| 📜 OpenAPI Spec (JSON) | <http://localhost:8081/v3/api-docs> |

> 🔑 Tài khoản admin mặc định: `admin@ecomart.com` / `Admin123!` *(seed tự động khi khởi động)*.

---

## 🧰 Các lệnh Docker hữu ích

```bash
# Xem trạng thái các container
docker compose ps

# Xem log (theo dõi liên tục)
docker compose logs -f app
docker compose logs -f postgres

# Dừng app + postgres (giữ nguyên dữ liệu trong volume)
docker compose down

# Dừng và XÓA luôn cả volume dữ liệu (⚠️ mất toàn bộ data)
docker compose down -v

# Rebuild lại image app sau khi sửa code
docker compose up -d --build app

# Restart lại app
docker compose restart app

# Vào shell của container postgres
docker compose exec postgres psql -U postgres -d ecomart_db
```

---

## ⚙️ Cấu hình qua file `.env`

Toàn bộ cấu hình có thể override bằng file `.env` đặt ngay tại thư mục gốc dự án (file này đã được `.gitignore` loại trừ, an toàn khi commit). Copy mẫu sau và sửa theo nhu cầu:

```env
# ── Database ──
POSTGRES_DB=ecomart_db
POSTGRES_USER=postgres
POSTGRES_PASSWORD=postgrespassword
POSTGRES_PORT=5432

# ── App ──
APP_PORT=8081

# ── JWT ──
# ⚠️ Nên đổi secret dài (>= 32 bytes) trước khi deploy thật!
APP_JWT_SECRET=9a4f2c8d7e1b5a3f6c0d9e8f7a6b5c4d3e2f1a0b9c8d7e6f5a4b3c2d1e0f9a8b
APP_JWT_EXPIRATION_MS=86400000
```

Sau khi đổi, chạy lại:

```bash
docker compose up -d
```

---

## 🔁 Cách hoạt động của profile "docker"

- Khi chạy bằng Docker Compose, biến `SPRING_PROFILES_ACTIVE=docker` được set sẵn trong `compose.yaml`.
- Spring Boot sẽ nạp thêm file [`application-docker.yml`](../src/main/resources/application-docker.yml), chuyển datasource từ **H2 in-memory** sang **PostgreSQL** (host `postgres`, đúng tên container).
- Khi chạy trực tiếp ngoài Docker (IDE / `mvnw spring-boot:run`), mặc định vẫn dùng **H2 in-memory** — không cần Docker.

---

## 🩺 Xử lý sự cố thường gặp

| Vấn đề | Cách xử lý |
|---|---|
| **Lỗi port `8081`/`5432` đã bị chiếm** | Đổi `APP_PORT` / `POSTGRES_PORT` trong `.env`, rồi `docker compose up -d` |
| **App crash vì không kết nối được DB** | Kiểm tra: `docker compose logs app` → đảm bảo `postgres` healthy trước (`docker compose ps`) |
| **Thay đổi code không phản ánh khi up lại** | `docker compose up -d --build app` (hoặc `docker compose build --no-cache app`) |
| **Lần build đầu quá lâu** | Bình thường — Maven phải tải dependencies. Các lần sau nhanh nhờ layer cache |
| **Muốn xóa dữ liệu DB, chạy lại từ đầu** | `docker compose down -v && docker compose up -d --build` |
| **App chạy nhưng Swagger trả 404** | Đợi app khởi động xong (healthcheck chờ ~45s): `docker compose ps` xem trạng thái `healthy` |

---

## 🏗️ Các file Docker trong dự án

| File | Vai trò |
|---|---|
| [`Dockerfile`](../Dockerfile) | Multi-stage build: stage 1 dùng Maven build JAR (có layer cache), stage 2 dùng JRE gọn nhẹ, chạy non-root user |
| [`compose.yaml`](../compose.yaml) | Định nghĩa 2 service `postgres` + `app`, healthcheck, volume, network |
| [`.dockerignore`](../.dockerignore) | Loại trừ `target/`, `.git`, `.env`, IDE files... khỏi build context (image nhẹ & an toàn) |
| [`application-docker.yml`](../src/main/resources/application-docker.yml) | Profile Spring Boot kết nối PostgreSQL khi chạy trong container |

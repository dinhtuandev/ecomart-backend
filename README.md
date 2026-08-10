# 🛒 EcoMart Backend (`ecomart-backend`)

**RESTful API Backend cho nền tảng Thương mại điện tử B2C EcoMart** — xây dựng với Spring Boot 3, xác thực Stateless JWT, chạy được ngay trên H2 in-memory trong môi trường dev và sẵn sàng chuyển sang PostgreSQL khi lên production.

---

## 📖 Mục lục

1. [✨ Tính năng cốt lõi](#-tính-năng-cốt-lõi)
2. [🧰 Tech Stack](#-tech-stack)
3. [📋 Yêu cầu & Cài đặt](#-yêu-cầu--cài-đặt)
4. [⚙️ Cấu hình môi trường](#️-cấu-hình-môi-trường)
5. [🚀 Chạy ứng dụng](#-chạy-ứng-dụng)
6. [📚 Tài liệu API](#-tài-liệu-api)
7. [🧪 Kiểm thử](#-kiểm-thử)
8. [🐳 Chạy với Docker](#-chạy-với-docker)
9. [🏗️ Cấu trúc thư mục](#️-cấu-trúc-thư-mục)

---

## ✨ Tính năng cốt lõi

- 🔐 **Xác thực & Phân quyền JWT** — Đăng ký, đăng nhập, quên/đặt lại mật khẩu, quản lý profile & địa chỉ giao hàng; phân quyền `CUSTOMER` / `ADMIN` bằng Spring Security.
- 📦 **Product Catalog** — Danh mục, thương hiệu, tìm kiếm sản phẩm theo từ khóa, lọc theo danh mục/thương hiệu/khoảng giá, sắp xếp & phân trang.
- 🛒 **Giỏ hàng & Đặt hàng COD** — Thêm/sửa/xóa sản phẩm trong giỏ, đặt hàng giao dịch với **Pessimistic Lock** chống oversell, hủy đơn & hoàn tồn kho.
- ⭐ **Đánh giá sản phẩm** — Chỉ Customer đã mua đơn `COMPLETED` mới được review; admin ẩn/hiện đánh giá.
- 👑 **Admin Dashboard** — Quản trị danh mục, thương hiệu, sản phẩm, tồn kho, đơn hàng, người dùng; thống kê doanh thu theo khoảng thời gian.
- 📊 **API Docs tự động** — Swagger UI / OpenAPI 3 tích hợp sẵn, không cần cấu hình thêm.

---

## 🧰 Tech Stack

| Thành phần | Công nghệ |
|---|---|
| ☕ Ngôn ngữ | Java 17+ (đang chạy trên JDK 21) |
| 🌱 Framework | Spring Boot 3.3.5 |
| 💾 ORM / Persistence | Spring Data JPA (Hibernate 6) |
| 🗄️ Database | **H2** (in-memory, mặc định dev) · **PostgreSQL** (production, qua `compose.yaml`) |
| 🔐 Bảo mật / Auth | Spring Security · **JJWT 0.12.6** (Stateless Bearer Token) |
| 📄 API Docs | springdoc-openapi 2.6.0 (Swagger UI) |
| ✅ Validation | Spring Boot Starter Validation (Bean Validation) |
| 🧩 Codegen | Lombok |
| 🐳 Container | Docker Compose (`compose.yaml` — PostgreSQL) |
| 📦 Build tool | Maven (Wrapper `./mvnw`) |

---

## 📋 Yêu cầu & Cài đặt

### 🛠️ Prerequisites

- **JDK 17 hoặc 21** (kiểm tra: `java -version`)
- **Maven 3.9+** *(tùy chọn — dự án có sẵn Maven Wrapper `mvnw`)*
- **Docker Desktop** *(chỉ bắt buộc khi muốn chạy PostgreSQL thật)*

### 📥 Installation

```powershell
# Clone repository
git clone https://github.com/your-org/ecomart-backend.git
cd ecomart-backend

# Cài đặt dependency (build thử để verify)
./mvnw.cmd clean compile
```

> ⚠️ **Mẹo**: Bản mặc định đã tắt Docker Compose và dùng **H2 in-memory**, nên bạn **không cần** khởi động Docker Desktop để chạy dev.

---

## ⚙️ Cấu hình môi trường

Spring Boot ánh xạ các biến môi trường (env vars) sang property trong [`application.yml`](src/main/resources/application.yml). Tạo file `.env` hoặc set biến trực tiếp theo mẫu sau:

```env
# ── Server ──
SERVER_PORT=8081

# ── Database (mặc định dùng H2 in-memory) ──
SPRING_DATASOURCE_URL=jdbc:h2:mem:ecomartdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL
SPRING_DATASOURCE_USERNAME=sa
SPRING_DATASOURCE_PASSWORD=

# ── Đổi sang PostgreSQL khi deploy (bỏ comment) ──
# SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/ecomart_db
# SPRING_DATASOURCE_USERNAME=postgres
# SPRING_DATASOURCE_PASSWORD=postgrespassword

# ── JWT ──
# ⚠️ Thay bằng secret dài (>= 32 bytes) khi lên production!
APP_JWT_SECRET=your-32-char-min-secret-key-here
APP_JWT_EXPIRATION_MS=86400000

# ── Docker Compose (bật = true nếu muốn auto-start PostgreSQL) ──
SPRING_DOCKER_COMPOSE_ENABLED=false
```

---

## 🚀 Chạy ứng dụng

### Cách 1: IntelliJ IDEA (khuyên dùng)

1. Mở dự án và chờ Maven load xong dependencies.
2. Mở `src/main/java/com/ecomart/EcomartBackendApplication.java`.
3. Nhấn nút **Run ▶️** cạnh hàm `main`.

### Cách 2: Command Line

```powershell
# Development mode
./mvnw.cmd spring-boot:run

# Hoặc chạy package đã build
./mvnw.cmd package
java -jar target/ecomart-backend-0.0.1-SNAPSHOT.jar
```

Sau khi chạy, ứng dụng listen tại **`http://localhost:8081`**.

---

## 📚 Tài liệu API

- 🟢 **Swagger UI** (tài liệu tương tác, thử API trực tiếp):  
  <http://localhost:8081/swagger-ui.html>
- 📄 **OpenAPI Spec (JSON)**:  
  <http://localhost:8081/v3/api-docs>
- 🗄️ **H2 Console**: <http://localhost:8081/h2-console> *(JDBC URL: `jdbc:h2:mem:ecomartdb`, User: `sa`, mật khẩu trống)*

### 🔥 Endpoint quan trọng

| Method | Endpoint | Mô tả |
|---|---|---|
| `POST` | `/api/v1/auth/register` | Đăng ký tài khoản Customer |
| `POST` | `/api/v1/auth/login` | Đăng nhập → nhận **Bearer Access Token** |
| `GET` | `/api/v1/products` | Danh sách sản phẩm công khai (có filter, sort, phân trang) |
| `GET` | `/api/v1/cart` | Xem giỏ hàng *(cần token)* |
| `POST` | `/api/v1/orders` | Đặt hàng COD *(cần token)* |
| `GET` | `/api/v1/admin/dashboard` | Thống kê tổng quan *(cần quyền `ADMIN`)* |

> 🔑 **Tài khoản mặc định**: Admin `admin@ecomart.com` / `Admin123!` *(được seed tự động khi khởi chạy)*. Các endpoint admin dùng tiền tố `/api/v1/admin/**` và yêu cầu role `ADMIN`.

---

## 🧪 Kiểm thử

```powershell
# Chạy toàn bộ unit/integration tests
./mvnw.cmd test

# Chạy và xem chi tiết test + coverage report
./mvnw.cmd verify
```

---

## 🐳 Chạy với Docker

> 📖 Hướng dẫn chi tiết: xem [docs/DOCKER.md](docs/DOCKER.md)

```bash
# Build image + khởi động PostgreSQL & ứng dụng
# (lần đầu build lâu do phải tải Maven dependencies)
docker compose up -d --build

# Xem log theo dõi
# docker compose logs -f app

# Dừng toàn bộ (giữ dữ liệu DB trong volume)
# docker compose down
```

- 🌐 Ứng dụng: <http://localhost:8081> · 📄 Swagger: <http://localhost:8081/swagger-ui.html>
- Khi chạy bằng Docker, ứng dụng dùng **PostgreSQL** (profile `docker`), dữ liệu lưu trong volume `postgres_data`.
- Chạy trực tiếp ngoài Docker (IDE / `./mvnw.cmd spring-boot:run`) vẫn dùng **H2 in-memory** như trước, không cần Docker.

---

## 🏗️ Cấu trúc thư mục

```text
ecomart-backend/
├── src/main/java/com/ecomart/
│   ├── config/          # Security, CORS, OpenAPI
│   ├── security/        # JWT provider, filter, principal
│   ├── dto/             # Request/Response DTOs
│   └── exception/       # Global exception handler
├── src/main/resources/
│   ├── application.yml          # Cấu hình chính (DB, JWT, docs)
│   └── application-docker.yml   # Profile "docker" (PostgreSQL)
├── Dockerfile           # Build image ứng dụng (multi-stage)
├── .dockerignore        # Loại trừ file không cần thiết khi build
├── compose.yaml         # PostgreSQL + app (Docker Compose)
├── docs/DOCKER.md       # Hướng dẫn chạy bằng Docker
└── pom.xml              # Maven configuration
```

# ============================================================
#  Stage 1 — Build: compile ứng dụng thành file JAR
# ============================================================
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copy pom.xml trước và tải dependencies riêng để tận dụng
# Docker layer cache (bước này chỉ chạy lại khi pom.xml thay đổi)
COPY pom.xml .
RUN mvn -B dependency:go-offline

# Copy source code và build JAR (bỏ test để build nhanh hơn)
COPY src ./src
RUN mvn -B clean package -DskipTests

# ============================================================
#  Stage 2 — Run: image runtime gọn nhẹ
# ============================================================
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Chạy với user không phải root (security best practice)
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# Copy JAR đã build từ stage 1
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]

FROM eclipse-temurin:17-jdk-jammy
# 컨테이너 기본 시간대는 UTC다. 이미지 레벨에서 고정해야 compose·docker run 등
# 기동 경로가 달라져도 JVM 기본 시간대가 서비스 시간대(KST)로 유지된다.
ENV TZ=Asia/Seoul
WORKDIR /app
COPY aics-api/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
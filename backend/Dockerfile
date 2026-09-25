FROM eclipse-temurin:21-jdk
WORKDIR /backend
COPY . .
RUN javac -cp ".:mysql-connector-j-26.7.0.jar" *.java
EXPOSE 8080
CMD java -cp ".:mysql-connector-j-26.7.0.jar" server

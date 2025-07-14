# Use a base image that includes the full JDK and Gradle
FROM gradle:8.8-jdk21-jammy
WORKDIR /app

# Copy the entire project context into the container
COPY . .

# Make the Gradle wrapper executable
RUN chmod +x ./gradlew
RUN chmod +x ./entrypoint.sh

# Expose the application port and the remote debug port
EXPOSE ${MODULO_SERVICE_PORT}
EXPOSE ${MODULO_SERVICE_DEBUG_PORT}

# Define nosso script como o ponto de entrada do contêiner
ENTRYPOINT ["./entrypoint.sh"]
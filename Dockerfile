## This Dockerfile can be used in 2 ways:
## 1. Build the jar file inside the container and run using the minimal runtime image.
## 2. Copy the jar file built outside docker and run using the minimal runtime image.
##
## The first option is suitable for running in a CI pipeline and the second
## option is suitable for testing locally.
##
## 



## Test network connectivity. Used in build_in_container to get a better
## error message then the default one...
FROM alpine as test_network
RUN ping -c 1 8.8.8.8 || ( echo "Docker cannot reach the internet from inside the container. Try running 'sudo systemctl restart docker'." && exit 1 )



## Build the jar file from source inside the docker container. This is suitable
## for running in a CI pipeline.
FROM maven:3-eclipse-temurin-21 as build
WORKDIR /app

# By copying something from the test_network stage it will automatically
# run that stage, ie. the network test inside it.
COPY --from=test_network /bin/ping /bin/ping

# Copy Maven settings and pom.xml
COPY maven-settings.xml /root/.m2/
COPY pom.xml .

# Tell Maven to download all dependencies now so the remaining build
# can proceed without internet access. This also ensures quick failure
# if there are any issues with the dependencies and it allows us to 
# cache the dependencies on the next build.
RUN mvn -s /root/.m2/maven-settings.xml -B dependency:go-offline

# Copy source code and build the application
COPY src ./src
RUN mvn -s /root/.m2/maven-settings.xml package



## Create a minimal runtime image which can be used to run the jar file regardless
## of where it was built. 
## NOTE: This target should not be used directly, see use_local_build or 
## use_container_build below.
FROM eclipse-temurin:21-jre as minimal_runtime
WORKDIR /app

# Install curl for healthcheck
RUN apt-get update && \
	apt-get install -y curl && \
	apt-get clean && \
	rm -rf /var/lib/apt/lists/*

# Create a non-root user to run the application
RUN addgroup --system --gid 1001 appuser && \
	adduser --system --uid 1001 --ingroup appuser appuser

# Make sure the log dir exists
RUN mkdir -p /app/logs

# Now make sure the non-root user has perms for the /app dir
RUN	chown -R appuser:appuser /app

# Switch to non-root user
USER appuser

# Expose the application port
EXPOSE 8080

# Set entry point. NOTE: that the app.jar file doesn't exist yet, it will be
# copied in from the build_in_container stage or from outside the container.
ENTRYPOINT ["java", "-jar", "app.jar"]

# Health check
HEALTHCHECK --interval=30s --timeout=3s --retries=3 \
	CMD curl -f http://localhost:8080/actuator/health || exit 1 





# Build the jar file inside the container and run using the minimal runtime image.
FROM minimal_runtime as in_container
COPY --from=build /app/target/*.jar app.jar



# Copy the jar file built outside docker and run using the minimal runtime image.
FROM minimal_runtime as copy_local
COPY target/*.jar app.jar

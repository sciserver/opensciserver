all: java

java:
	cd components/java && ./gradlew :racm:modelJar && ./gradlew build

clean:
	cd components/java && ./gradlew clean
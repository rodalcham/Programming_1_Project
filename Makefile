.DEFAULT_GOAL := run

install:
	mvn install -DskipTests

compile:
	mvn compile

run:
	mvn exec:java -Dexec.mainClass="WebServer"

build:
	mvn package

clean:
	mvn clean

rebuild: clean compile run
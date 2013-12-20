RESTful Library Service
======================

$ mvn clean package

# To launch Library A instance
$ java -jar target/library-0.0.1-SNAPSHOT.jar server config/library_a_config.yml 

# To launch Library B instance
$ java -jar target/library-0.0.1-SNAPSHOT.jar server config/library_b_config.yml 

java -jar target/procurement-service-0.0.1-SNAPSHOT.jar server config/procurement-service_config.yml 

# How to run this Java process forever
$ nohup ./bin/library_a.sh 0<&- &> /tmp/app-a.log &
$ nohup ./bin/library_b.sh 0<&- &> /tmp/app-b.log &

nohup ./bin/run.sh 0<&- &> /tmp/.log &

nohup ./bin/run.sh 0<&- &> /tmp/procurement.log &

Service endpoint: 
http://localhost:8001/library/v1
http://localhost:8002/library/v1

Admin: http://localhost:8081/


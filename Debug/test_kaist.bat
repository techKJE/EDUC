start "SV40 Demo - MMS HTTP Server" java -jar MMSServer.jar
start "SV40 Demo - MMS MNS Dummy" java -jar MNSDummy.jar
start "SV40 Demo - EDUS File Server" miniweb/miniweb.exe -p 8811 -r miniweb/htdocs
timeout 10
# EDUSMockup Host shall have global ip access.
start "SV40 Demo - EDUS MMS Mockup Server"  java -jar EDUSMockup.jar -port 8902 -mms www.mms-kaist.com:8088 -mrn urn:mrn:smart-navi:service:sv40
timeout 5
start "SV40 Demo - EDUC"  java -jar EDUC.jar -mms www.mms-kaist.com:8088

package re.kr.enav.sv40.educ.util;

public interface SV40EDUErrCodable {
	String getErrCode();
	String getMessage(String... args);
}

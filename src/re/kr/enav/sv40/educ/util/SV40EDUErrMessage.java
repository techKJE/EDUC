package re.kr.enav.sv40.educ.util;

public class SV40EDUErrMessage {
	public static String get(SV40EDUErrCodable errCodable, String... args) {
		return "[" + errCodable.getErrCode() + "] " + errCodable.getMessage(args); 
	}
}

package re.kr.enav.sv40.educ.util;

public enum SV40EDUErrCode implements SV40EDUErrCodable {
	ERR_001("ERR_001","%1을(를) 찾을 수 없습니다."),
	ERR_002("ERR_002","%1에 연결할 수 없습니다."),
	ERR_003("ERR_003","%1에 연결할 수 없습니다. (e-Nav센터에 문의)"),
	ERR_004("ERR_004","%1는 필수입력요소입니다."),
	ERR_005("ERR_005","%1는 zip파일형식이 아닙니다"),
	ERR_006("ERR_006","%1에 접근할 수 없습니다. (e-Nav센터에 문의)");
	
	private String errCode;
	private String errMsg;
	 
	public String getErrCode() {
		return this.errCode;
	}
	
   public String getMessage(String... args) {
      return SV40EDUErrCodeUtil.parseMessage(this.errMsg, args);
   }

   SV40EDUErrCode(String errCode, String msg) {
      this.errCode = errCode;
      this.errMsg = msg;
   }
}

package eu.trentorise.smartcampus.mobility.util;

public class ConnectorException extends Exception {
	private static final long serialVersionUID = 3510890602135252304L;

	private int code;
	
	public ConnectorException() {
	}

	public ConnectorException(String message, int code) {
		super(message);
		this.code = code;
	}

	public int getCode() {
		return code;
	}

	public void setCode(int code) {
		this.code = code;
	}
	
	

}

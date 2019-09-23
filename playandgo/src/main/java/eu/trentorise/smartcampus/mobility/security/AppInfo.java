package eu.trentorise.smartcampus.mobility.security;

import java.io.Serializable;
import java.util.Map;


public class AppInfo implements Serializable {

	private static final long serialVersionUID = -2583888256140211744L;
	
	private String appId;
    private String password;
    private String messagingAppId;
    private String gcmSenderApiKey;
    private String gcmSenderId;
    private String gameId;

    private Map<String,Object> extra;

	public String getAppId() {
		return appId;
	}

	public void setAppId(String appId) {
		this.appId = appId;
	}

	public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

	public String getMessagingAppId() {
		return messagingAppId;
	}

	public void setMessagingAppId(String messagingAppId) {
		this.messagingAppId = messagingAppId;
	}

	public String getGcmSenderApiKey() {
		return gcmSenderApiKey;
	}

	public void setGcmSenderApiKey(String gcmSenderApiKey) {
		this.gcmSenderApiKey = gcmSenderApiKey;
	}

	public String getGcmSenderId() {
		return gcmSenderId;
	}

	public void setGcmSenderId(String gcmSenderId) {
		this.gcmSenderId = gcmSenderId;
	}

	public String getGameId() {
		return gameId;
	}

	public void setGameId(String gameId) {
		this.gameId = gameId;
	}


	public Map<String, Object> getExtra() {
		return extra;
	}

	public void setExtra(Map<String, Object> extra) {
		this.extra = extra;
	}

	
	@Override
    public String toString() {
    	return appId + "=" + password;
    }
	

}

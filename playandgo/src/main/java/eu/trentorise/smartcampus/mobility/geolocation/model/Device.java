package eu.trentorise.smartcampus.mobility.geolocation.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown=true)
public class Device {
	private boolean available;

	private String cordova;

	private String manufacturer;

	private String model;

	private String platform;

	private String uuid;

	private String version;

	public boolean getAvailable() {
		return this.available;
	}

	public String getCordova() {
		return this.cordova;
	}

	public String getManufacturer() {
		return this.manufacturer;
	}

	public String getModel() {
		return this.model;
	}

	public String getPlatform() {
		return this.platform;
	}

	public String getUuid() {
		return this.uuid;
	}

	public String getVersion() {
		return this.version;
	}

	public void setAvailable(boolean available) {
		this.available = available;
	}

	public void setCordova(String cordova) {
		this.cordova = cordova;
	}

	public void setManufacturer(String manufacturer) {
		this.manufacturer = manufacturer;
	}

	public void setModel(String model) {
		this.model = model;
	}

	public void setPlatform(String platform) {
		this.platform = platform;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public void setVersion(String version) {
		this.version = version;
	}
}

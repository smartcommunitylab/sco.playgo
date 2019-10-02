package eu.trentorise.smartcampus.mobility.gamificationweb.model;

public class BadgeConcept {

	private String name;
	private String url;

	public String getName() {
		return name;
	}

	public String getUrl() {
		return url;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public BadgeConcept() {
		super();
	}

	public BadgeConcept(String name) {
		super();
		this.name = name;
	}	
	
	public BadgeConcept(String name, String url) {
		super();
		this.name = name;
		this.url = url;
	}

}

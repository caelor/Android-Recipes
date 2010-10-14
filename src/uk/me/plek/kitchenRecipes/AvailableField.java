package uk.me.plek.kitchenRecipes;

public class AvailableField {

	protected final String _name;
	protected final String _relativeUri;
	protected final String _type;
	
	public AvailableField(String name, String uri, String type) {
		super();
		
		_name = name;
		_relativeUri = uri;
		_type = type;
	}
	
	public String getName() {
		return _name;
	}
	
	public String getUri() {
		return _relativeUri;
	}
	
	public String getType() {
		return _type;
	}
}

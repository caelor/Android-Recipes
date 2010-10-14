package uk.me.plek.kitchenRecipes;

public class ActiveFilter {
	protected final String _field;
	protected final String _value;
	protected final String _deletedUri;
	protected final boolean _isAddNewFilter;
	
	public ActiveFilter(String field, String value, String deletedUri) {
		super();
		
		_field = field;
		_value = value;
		_deletedUri = deletedUri;
		_isAddNewFilter = false;
	}
	
	public ActiveFilter() {
		super();
		
		_field = null;
		_value = null;
		_deletedUri = null;
		_isAddNewFilter = true;
	}
	
	public String getField() { return _field; }
	public String getValue() { return _value; }
	public String getDeletedUri() { return _deletedUri; }
	public boolean isAddNewFilter() { return _isAddNewFilter; }
}

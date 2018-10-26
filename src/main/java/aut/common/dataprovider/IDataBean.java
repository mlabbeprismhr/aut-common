package aut.common.dataprovider;

public interface IDataBean {
	public String getValue();
	
	public String getName();
	
	public <T> Class<T> getClazz();
	
}

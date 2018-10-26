package aut.common.dataprovider;

import java.util.List;
import java.util.Map;

public interface IDataProvider {

	<T> List<T> getDataList(Class<T> clazz, String currentEnv) throws Exception;
	
	public <T> Map<String, Object> getData(Class<T> clazz, String id, String currentEnv) throws Exception;
	
}

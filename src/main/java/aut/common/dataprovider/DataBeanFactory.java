package aut.common.dataprovider;


public class DataBeanFactory {

	@SuppressWarnings("unchecked")
	public static <T> T createLocalYamlDataBean(IDataBean iDataBean, String currentEnv) throws Exception {
		String id = iDataBean.getValue();
		Class<?> clazz = iDataBean.getClazz();
		YamlDataProvider iDataProvider = new YamlDataProvider();
		return (T) iDataProvider.getDataBean(clazz, id, currentEnv);
	}
	
	@SuppressWarnings("unchecked")
	public static <T> T createLocalJSONDataBean(IDataBean iDataBean, String app, String currentEnv) throws Exception {
		Class<?> clazz = iDataBean.getClazz();
		JSONDataProvider jsonDataProvider = new JSONDataProvider(app, currentEnv);
		return (T) jsonDataProvider.getJSONDataBean(clazz, currentEnv); 
	}

}

package aut.common.dataprovider;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.BeanUtilsBean;
import org.yaml.snakeyaml.Yaml;


public class YamlDataProvider implements IDataProvider {

	//private final String beansPackagePath = "web.prismhr.data.beans";

	public <T> T getDataBean(Class<T> clazz, String id, String currentEnv) throws Exception {
		Map<String, Object> data = getData(clazz, id, currentEnv);
		try {
			return populateBean(data, clazz, currentEnv);
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	public <T> Map<String, Object> getData(Class<T> clazz, String id, String currentEnv) {
		Iterable<Object> allObjects = getData(clazz, currentEnv);
		while (allObjects.iterator().hasNext()) {
			Object object = allObjects.iterator().next();
			Map<String, Object> map = (Map<String, Object>) object;
			setRandomName(map);
			
			if (null == id) return map;
			
			if (map.get("id").equals(id)) return map;
		}
		return null;
	}

	public synchronized <T> Iterable<Object> getData(Class<T> clazz, String currentEnv) {
		String className = getRelativePath(clazz);
		//String currentEnv = TestProperties.testProperties.getString(TestProperties.TEST_ENV);		
		String yamlFilePath = "testdata/" + currentEnv.toUpperCase() + "/" + className + ".yaml";
		InputStream inStream = ClassLoader.getSystemResourceAsStream(yamlFilePath);
		Iterable<Object> allObjects = new Yaml().loadAll(inStream);
		return allObjects;
	}

	private void setRandomName(Map<String, Object> data) {
		for (Entry<String, Object> item : data.entrySet()) {
			String value = item.getValue().toString();
			if (value.contains("[R]")) {
				String newValue = value.replace("[R]", String.valueOf(System.currentTimeMillis()));
				item.setValue(newValue);
			}
		}
	}

	@Override
	public <T> List<T> getDataList(Class<T> clazz, String currentEnv) throws Exception {
		Map<String, Object> allObjects = getData(clazz, null, currentEnv);
		List<T> listBean = new ArrayList<T>();
		for (Entry<String, Object> entry : allObjects.entrySet()) {
			Map<?, ?> map = (Map<?, ?>) entry;
			try {
				listBean.add(populateBean(map, clazz, currentEnv));
			} catch (InstantiationException | IllegalAccessException
					| InvocationTargetException | ClassNotFoundException
					| NoSuchMethodException | SecurityException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	private <T> T populateBean(Map<?, ?> map, Class<T> clazz, String currentEnv) throws Exception {
		T bean = clazz.newInstance();
		BeanUtils.populate(bean, (Map<String, ? extends Object>) map);
		Field[] fields = clazz.getDeclaredFields();
		for (Field field : fields) {
			String type = field.getType().getName();
			if (field.getType().isPrimitive() || field.getType().toString().contains("String")) {
				continue;
			}
			//if (type.contains(beansPackagePath)) {
			if (type.contains(".data.beans")) {
				String fieldName = field.getName();
				String fieldKey = fieldName + "_id";
				
				if (!map.containsKey(fieldKey)) continue;
				
				String fieldValue = map.get(fieldKey).toString();
				
				if (fieldValue.isEmpty()) continue;
				
				T subBean = (T) getDataBean(Class.forName(type), fieldValue, currentEnv);
				BeanUtilsBean.getInstance().setProperty(bean, fieldName, subBean);
			} else {
				String fieldKey = field.getName() + "_enum";
				
				if (!map.containsKey(fieldKey)) continue;
				
				String fieldValue = map.get(fieldKey).toString();
				
				if (fieldValue.isEmpty()) continue;
				
				@SuppressWarnings("rawtypes")
				Class cls = Class.forName(type);
				Object o = Enum.valueOf(cls, fieldValue);
				BeanUtilsBean.getInstance().setProperty(bean, field.getName(), o);
			}
		}
		return bean;
	}

	private static String getRelativePath(@SuppressWarnings("rawtypes") Class clazz) {
		String className = clazz.getName();
		className = className.replaceAll(".*\\.data\\.beans\\.", "");//"web.prismhr.data.beans.", "");
		className = className.replaceAll(".*\\.data\\.testdata\\.", "");//web.prismhr.data.testdata.", "");
		className = className.toLowerCase().replace("bean", "");
		className = className.replace(".", "/");
		return className;
	}
}

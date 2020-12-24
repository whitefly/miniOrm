package mybatis;

import java.lang.reflect.Proxy;

public class MapperFactory {

    /**
     * 返回代理对象
     *
     * @param clazz
     * @param <T>
     * @return
     */
    public static <T> T getBean(Class<T> clazz) {
        MapperHandler mapperHandler = new MapperHandler();
        return (T) Proxy.newProxyInstance(MapperHandler.class.getClassLoader(), new Class[]{clazz}, mapperHandler);
    }
}

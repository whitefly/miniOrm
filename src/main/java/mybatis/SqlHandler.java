package mybatis;

import java.lang.reflect.Method;

public interface SqlHandler {

    Object handleSelect(Method method, Object[] params);

    Boolean handleInsert(Method method, Object[] params);

    Integer handleUpdate(Method method, Object[] params);

    Integer handleDelete(Method method, Object[] params);
}

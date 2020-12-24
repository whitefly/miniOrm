package mybatis;

import Utils.DbUtil;
import Utils.ParseUtil;
import jdk.nashorn.internal.runtime.regexp.joni.exception.ValueException;
import mybatis.an.Delete;
import mybatis.an.Insert;
import mybatis.an.Select;
import mybatis.an.Update;

import java.lang.reflect.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class MapperHandler implements InvocationHandler, SqlHandler {


    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (method.isAnnotationPresent(Select.class)) {
            return handleSelect(method, args);
        } else if (method.isAnnotationPresent(Insert.class)) {
            return handleInsert(method, args);
        } else if (method.isAnnotationPresent(Update.class)) {
            return handleUpdate(method, args);
        } else if (method.isAnnotationPresent(Delete.class)) {
            return handleDelete(method, args);
        } else {
            throw new ValueException("缺少必要的sql注释");
        }
    }

    @Override
    public Object handleSelect(Method method, Object[] params) {
        //获取注解
        String sql = method.getAnnotation(Select.class).value();
        sql = checkSql(sql);
        Class<?> returnType = method.getReturnType();

        //判断返回值是否为List
        Class<?> doType;
        if (List.class.isAssignableFrom(returnType)) {
            doType = exactTypeFromMethod(method);
            return multiQuery(sql, params, doType, method);
        } else if (!Collection.class.isAssignableFrom(returnType)) {
            doType = returnType;
            return singleQuery(sql, params, doType, method);
        }
        return null;
    }

    @Override
    public Boolean handleInsert(Method method, Object[] params) {
        String sql = method.getAnnotation(Insert.class).value();
        sql = checkSql(sql);
        // TODO: 2020/12/24 实现传入List,然后进行批量插入
        return singleInsert(sql, params, method);
    }

    @Override
    public Integer handleUpdate(Method method, Object[] params) {
        String sql = method.getAnnotation(Insert.class).value();
        sql = checkSql(sql);
        return singleDelete(sql, params, method);
    }


    @Override
    public Integer handleDelete(Method method, Object[] params) {
        String sql = method.getAnnotation(Delete.class).value();
        sql = checkSql(sql);
        return singleDelete(sql, params, method);
    }


    private Object multiQuery(String sql, Object[] params, Class<?> clazz, Method m) {
        try (Connection connection = DbUtil.getConnection()) {
            ParseUtil.ParseResult parseResult = ParseUtil.preparedSql(sql, params, m);
            PreparedStatement ps = connection.prepareStatement(parseResult.getPreparedSql());

            finishPreparedStatement(parseResult, ps);
            ResultSet resultSet = ps.executeQuery();

            //调用转换层
            return objectAdapter(resultSet, clazz);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return null;
    }

    private Integer singleDelete(String sql, Object[] params, Method m) {
        try (Connection connection = DbUtil.getConnection()) {
            ParseUtil.ParseResult parseResult = ParseUtil.preparedSql(sql, params, m);
            PreparedStatement ps = connection.prepareStatement(parseResult.getPreparedSql());

            finishPreparedStatement(parseResult, ps);
            return ps.executeUpdate();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return -1;
    }


    private Object singleQuery(String sql, Object[] params, Class<?> clazz, Method m) {
        List o = (List) multiQuery(sql, params, clazz, m);
        if (o != null && !o.isEmpty()) return o.get(0);
        else return null;
    }

    private Boolean singleInsert(String sql, Object[] params, Method m) {
        try (Connection connection = DbUtil.getConnection()) {
            ParseUtil.ParseResult parseResult = ParseUtil.preparedSql(sql, params, m);
            // TODO: 2020/12/24 将自增策略设置为可选变量
            PreparedStatement ps = connection.prepareStatement(parseResult.getPreparedSql(), Statement.RETURN_GENERATED_KEYS);
            finishPreparedStatement(parseResult, ps);
            int i = ps.executeUpdate();
            if (i == 1) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        int generatedKey = rs.getInt(1);
                        //给传入的变量赋值,这里默认第一个参数就是DO,没有注解,所以暂时用固定变量名id来代替
                        // TODO: 2020/12/24 这里需要重构
                        Object obj = params[0];
                        try {
                            Field id = obj.getClass().getDeclaredField("id");
                            id.setAccessible(true);
                            id.set(obj, generatedKey);
                        } catch (NoSuchFieldException e) {
                            e.printStackTrace();
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        }
                    }
                }
                return true;
            }
            return false;
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return false;
    }

    private void finishPreparedStatement(ParseUtil.ParseResult parseResult, PreparedStatement preparedStatement) throws SQLException {
        //填充参数
        List<ParseUtil.Placeholder> placeholders = parseResult.getPlaceholders();
        int parameterCount = preparedStatement.getParameterMetaData().getParameterCount();
        for (int i = 0; i < parameterCount; i++) {
            preparedStatement.setObject(i + 1, placeholders.get(i).getValue());
        }
    }

    private static String checkSql(String sql) {
        if (sql != null && sql.length() == 0) throw new ValueException("sql不能为空:" + sql);
        return sql.trim();
    }

    private <T> List<T> objectAdapter(ResultSet rs, Class<?> outputClass) {
        ArrayList<T> result = new ArrayList<>();
        try {
            ResultSetMetaData metaData = rs.getMetaData();
            while (rs.next()) {
                T bean = (T) outputClass.newInstance();
                //开始给bean赋值
                for (int i = 0; i < metaData.getColumnCount(); i++) {
                    String columnName = metaData.getColumnName(i + 1);
                    Object columnValue = rs.getObject(i + 1);

                    //要求属性名和列名一一对应,这里没有转换功能;
                    Field field = outputClass.getDeclaredField(columnName);
                    field.setAccessible(true);
                    if (columnValue != null) {
                        field.set(bean, columnValue);
                    }
                }
                result.add(bean);
            }
            return result;
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
        return null;
    }

    private Class<?> exactTypeFromMethod(Method m) {
        Type genericReturnType = m.getGenericReturnType();
        if (!ParameterizedType.class.isAssignableFrom(genericReturnType.getClass())) {
            throw new ValueException("函数缺少必要的DO泛型返回值");
        }
        Type[] typeArguments = ((ParameterizedType) m.getGenericReturnType()).getActualTypeArguments();
        return (Class<?>) typeArguments[0];
    }
}

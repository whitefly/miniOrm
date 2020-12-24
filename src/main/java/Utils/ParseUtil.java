package Utils;

import jdk.nashorn.internal.runtime.regexp.joni.exception.ValueException;
import lombok.Data;
import DO.User;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ParseUtil {
    static Pattern placeholderRe = Pattern.compile("#\\{(\\S+?)\\}");

    @Data
    public static class Placeholder {
        String initExpress;
        String varName;
        String varField;
        Object value;
    }

    @Data
    public static class ParseResult {
        String initSql; //注解中的原始sql
        String preparedSql; //使用?替换后的sql
        String executeSql; //真正用于执行的sql
        List<Placeholder> placeholders; //每一个站位符所代表的值;
    }


    public static ParseResult preparedSql(String sql, Object[] params, Method m) {
        ParseResult parseResult = new ParseResult();
        parseResult.setInitSql(sql);

        //替换为?
        Matcher matcher1 = placeholderRe.matcher(sql);
        parseResult.setPreparedSql(matcher1.replaceAll("?"));

        //创建占位符对象
        List<Placeholder> placeholders = new ArrayList<>();
        Matcher matcher2 = placeholderRe.matcher(sql);
        while (matcher2.find()) {
            Placeholder p = new Placeholder();
            p.initExpress = matcher2.group();
            String[] split = matcher2.group(1).trim().split("\\.");
            p.varName = split[0];
            if (split.length == 2) p.varField = split[1];
            else if (split.length >= 3) {
                throw new ValueException("占位变量格式错误:" + p.initExpress);
            }
            placeholders.add(p);
        }
        parseResult.setPlaceholders(placeholders);

        //赋值
        for (Placeholder placeholder : placeholders) {
            try {
                setPlaceholderValue(placeholder, params, m);
            } catch (NoSuchMethodException | NoSuchFieldException | IllegalAccessException e) {
                e.printStackTrace();
                throw new ValueException("参数赋值错误");
            }
        }

        //生成完整sql显示给用户
        parseResult.setExecuteSql(generateExecuteSql(parseResult));
        if (Boolean.parseBoolean(DbUtil.prop.getProperty("showSql", "false"))) {
            System.out.println(parseResult.getExecuteSql());
        }
        return parseResult;
    }

    public static String generateExecuteSql(ParseResult parseResult) {
        String rnt = parseResult.getInitSql();
        for (Placeholder ph : parseResult.getPlaceholders()) {
            rnt = rnt.replace(ph.getInitExpress(), ph.getValue().toString());
        }
        return rnt;
    }


    private static void setPlaceholderValue(Placeholder placeholder, Object[] params, Method m) throws NoSuchMethodException, NoSuchFieldException, IllegalAccessException {
        Parameter[] parameters = m.getParameters();
        //单个参数
        for (int i = 0; i < parameters.length; i++) {
            Object obj = params[i];
            if (parameters[i].getName().equals(placeholder.getVarName())) {
                if (placeholder.getVarField() == null) {
                    //不含标点,直接赋值
                    placeholder.value = obj;
                } else {
                    Field field = obj.getClass().getDeclaredField(placeholder.getVarField());
                    field.setAccessible(true);
                    placeholder.value = field.get(obj);
                }
                return;
            }
        }
    }

    public static void demo() throws NoSuchMethodException {
        String sql = "INSERT INTO users (name, password) VALUES (#{user.name}, #{user.password})";
        User u = new User();
        u.setName("伊素婉");
        u.setPassword("123456");
        Object[] objects = {u};
        Method insert = ParseUtil.class.getMethod("insert", User.class);
        ParseResult parseResult = preparedSql(sql, objects, insert);

        System.out.println(parseResult);
    }


    public static void main(String[] args) throws NoSuchMethodException, NoSuchFieldException {
        demo();
    }

}

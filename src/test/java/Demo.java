import DO.User;
import mybatis.MapperFactory;
import mybatis.MyMapper;

import java.util.List;

public class Demo {

    public static void main(String[] args) {
        MyMapper mapper = MapperFactory.getBean(MyMapper.class);

        //测试:插入单个,并显示自增主键;
        User user2 = new User();
        user2.setName("许三多");
        user2.setPassword("1234567");
        System.out.println(mapper.insert(user2));
        System.out.println(user2);

        //测试:查询全部
//        List<User> users = mapper.findUser();
//        users.forEach(System.out::println);

        //测试:查询单个
//        User user1 = mapper.findUserById(2);
//        System.out.println(user1);


        //删除单个
//        Integer delete = mapper.delete(5);
//        System.out.println(delete);
    }
}

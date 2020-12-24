package mybatis;

import DO.User;
import mybatis.an.Delete;
import mybatis.an.Insert;
import mybatis.an.Select;

import java.util.List;

public interface MyMapper {

    @Select(value = "select * from user")
    public List<User> findUser();

    @Select(value = "select * from user where id= #{id}")
    public User findUserById(Integer id);

    @Insert(value = "INSERT INTO user (name, password) VALUES (#{user.name}, #{user.password})")
    public Boolean insert(User user);

    @Delete(value = "delete from user where id=#{id}")
    public Integer delete(Integer id);
}

### 一、简介
>`hoaven-libMybatis`项目(https://github.com/hoaven/hoaven-libMybatis)的插件，旨在减少写mapper的工作，mapper会在编译时自动生成。
### 二、使用
>需要在通用dao中指定model定义的包位置、编译时生成mapper的包位置等。
```java
@EnableAutoGenMapper(modelPackageName = {"com.hoaven.aliadmin.model","com.hoaven.aliadmin.enum"}, 
mapperPackageName = "com.hoaven.aliadmin.mapper",
superMapperClassName = GenericMapper.className)
@Repository
public class AliAdminBaseDao extends GenericDao {
    @Resource
    SqlSession aliAdminSqlSession;

    @Override
    protected SqlSession getSession() {
        return aliAdminSqlSession;
    }

    @Override
    public String getBasePackage() {
        return "com.hoaven.aliadmin.mapper";
    }

}
```
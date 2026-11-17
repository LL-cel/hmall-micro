# 购物车服务

启动类：`com.hmall.CartServiceApplication`。业务包：`com.hmall.cart`。
服务名 cart-service，端口 8082，数据库 hm-cart。

在 hmall-micro 下执行：

```powershell
mvn -pl cart-service -am install -DskipTests
java -jar cart-service/target/cart-service.jar
```

默认启用 dev，保留原 application-dev.yaml、application-local.yaml 数据库环境配置。
可通过 `--spring.profiles.active=local` 切换环境。Knife4j 文档地址：http://localhost:8082/doc.html。

迁移 CartController、ICartService、CartServiceImpl、CartMapper 及 XML、Cart、CartFormDTO、CartVO。
所有业务 package、import、Mapper namespace 和文档扫描包已同步修改。
保留原 5 个接口：POST/PUT/GET/DELETE /carts，以及 DELETE /carts/{id}。
批量删除仍按照原实现按商品 ID 删除，单条删除按购物车条目 ID 删除。

CartServiceImpl 通过构造器注入 ItemClient，查询购物车后批量调用 item-service 的 GET /items 接口，
按商品 ID 回填 CartVO 的 newPrice、status 和 stock。空购物车不调用商品服务；
商品接口未返回的条目保留原 VO 默认值。远程调用异常向上抛出，不静默返回过期数据。
联调时需保证 Nacos 可用，并且 item-service 已注册到相同的服务发现环境。
原 UserContext.getUser() 调用均替换为 1L，不迁移 JWT 与登录拦截器。

请先创建 hm-cart 数据库，并从原 hmall 库导入 cart 表的实际结构及所需数据。
源码中无建表脚本，服务不会自动创建库表；含连字符的数据库名在 SQL 中需使用反引号。
本次验证覆盖编译、打包、应用启动和文档接口注册；数据库 CRUD、容量上限及数据隔离尚未联调。

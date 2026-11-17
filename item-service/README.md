# 商品服务

从单体 hm-service 迁移商品管理、搜索及库存扣减功能，接口路径、字段和业务逻辑保持原样。
启动类为 `com.hmall.ItemServiceApplication`，业务包为 `com.hmall.item`。
公共代码仍使用 `com.hmall.common`；Knife4j 通过 hm-common 的传递依赖提供。

## 启动

在 hmall-micro 目录执行：

```powershell
mvn -pl item-service -am package -DskipTests
java -jar item-service/target/item-service.jar
```

默认启用 dev 环境；application-dev.yaml、application-local.yaml 保留原数据库地址和密码配置。
可通过 `--spring.profiles.active=local` 切换环境，也可用 `--hm.db.host=localhost --hm.db.pw=你的密码` 覆盖配置。
服务名 item-service，端口 8081，文档地址 http://localhost:8081/doc.html。

## 数据库准备

连接库名已改为 `hm-item`，不会自动创建数据库或表。
先在目标 MySQL 创建该数据库（SQL 中使用反引号包裹含连字符的库名），再将原 hmall 库中的 item 表结构及数据导入新库。
当前源码中未发现 SQL 建表脚本，请使用原数据库导出的实际表结构，避免推测字段、索引及默认值。

## 迁移范围

- ItemController：商品分页、批量查询、详情、新增、更新、状态变更、删除及库存扣减。
- SearchController：`GET /search/list`，保留现有搜索条件与上架状态过滤。
- IItemService、ItemServiceImpl、ItemMapper 及 XML：保留 MyBatis-Plus 和库存扣减事务。
- Item、ItemDTO、ItemPageQuery，以及库存扣减使用的 OrderDetailDTO。
- 保留数据库环境、MyBatis-Plus、日志及 Knife4j 配置，更新 Mapper namespace 和文档扫描包。

原登录拦截器已放行商品和搜索接口，未迁移用户认证、JWT、Redis及其他业务模块。
单体工程保持不变；原订单和购物车中的本地商品服务调用尚未改为跨服务调用。

## 验证记录

- Maven reactor 编译、打包成功，生成 target/item-service.jar。
- 默认 dev 配置启动成功，监听 8081；验证后已停止进程。
- Knife4j 页面 HTTP 200，Swagger 文档包含预期 6 个路径、9 个接口操作。
- package、import、Mapper XML、Knife4j 扫描包未残留旧业务包路径。
- 本次未连接 hm-item 数据库验证 CRUD、搜索结果、库存扣减及事务回滚；需完成数据迁移后联调。
- 公共模块存在原有废弃 API 和未检查操作编译提示，未改动公共 Java 代码。

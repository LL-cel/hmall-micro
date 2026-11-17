# trade-service

独立订单服务，继承 `com.hmall:hmall-micro:1.0.0`。包名 `com.hmall.trade`，端口 `8085`，数据库 `hm-trade`。

## 迁移范围与接口契约

迁移单体 OrderController、Order/OrderDetail/OrderLogistics 的实体、Mapper/XML、Service/实现，以及 OrderFormDTO、OrderVO。保持原接口和字段：

| 方法 | 路径 | 功能 |
| --- | --- | --- |
| POST | /orders | 创建订单；请求含 addressId、paymentType、details，返回订单 ID |
| GET | /orders/{id} | 查询订单 |
| PUT | /orders/{orderId} | 标记订单已支付 |

用户 ID 暂时固定为 `1L`。未安装单体登录拦截器。支付单及余额支付属于支付模块，本次未迁移；原单体代码不变。单体原本未保存下单表单中的 addressId，本次保留原行为。

## 远程调用契约

`hm-api` 为共享契约模块，trade-service 通过该模块获得 OpenFeign 和 LoadBalancer 依赖，启动类通过 `@EnableFeignClients(basePackages = "com.hmall.api")` 扫描客户端。

| 客户端 | 方法 | HTTP 请求 |
| --- | --- | --- |
| com.hmall.api.item.ItemClient | queryItemByIds | GET /items?ids=11&ids=12 |
| com.hmall.api.item.ItemClient | deductStock | PUT /items/stock/deduct；JSON 数组 [{"itemId":11,"num":2}] |
| com.hmall.api.cart.CartClient | removeByItemIds | DELETE /carts?ids=11&ids=12 |

购物车参数为商品 ID，删除当前用户对应的商品条目，并非删除该用户全部购物车数据。复用现有 CartController 接口，当前 cart-service 同样固定用户为 1L。

OrderDetailDTO 统一位于 `com.hmall.api.item.dto`。已删除 item-service 的重复 DTO，并更新其 Controller、Service、ServiceImpl、Mapper 的引用；cart-service 没有该重复 DTO。

## 构建与启动

在 hmall-micro 目录执行：

```shell
mvn -pl trade-service -am package
java -jar trade-service/target/trade-service.jar
```

默认 dev 配置沿用单体 MySQL 配置（localhost:3306，root/mysql）。需预先创建 hm-trade，并准备与单体一致的 order、order_detail、order_logistics 表。本次不创建或更改数据库。

Nacos 默认地址 `192.168.150.88:8848`，默认注册地址 `192.168.150.1`，与现有微服务保持一致。可使用环境变量 `NACOS_SERVER_ADDR`、`NACOS_USERNAME`、`NACOS_PASSWORD`、`NACOS_DISCOVERY_IP` 覆盖。换机器时应设置本机可达的注册地址。

启动真实下单链路需要 MySQL、Nacos、item-service、cart-service。修改共享契约后，应重新构建并重启相关服务。若运行中的 JAR 被 Windows 占用，需停止对应服务后再打包。

## 验证与限制

- 交易服务及共享模块打包成功；商品、购物车及相关模块编译通过。
- 5 项自动化测试通过：Feign 查询/扣库存/删除购物车的 HTTP 契约及失败传播；下单金额、用户、明细映射、缺失商品和库存调用异常。HTTP 使用本地测试服务器，数据库写入使用 mock。
- 启动检查：8085 启动成功，/v2/api-docs 返回 HTTP 200，包含三个订单路由；成功连接 Nacos 并注册。
- 未执行真实下单和数据库写入测试，也未验证库存并发、跨服务故障补偿和重复提交。

保留原调用顺序：查商品、保存订单/明细、清理购物车、扣减库存。本地 @Transactional 仅保护订单库，无法回滚已成功的远程购物车/库存操作，网络超时也可能导致远程结果不确定。本次没有引入分布式事务、幂等或补偿机制，不能视为已完成生产级跨服务一致性处理。
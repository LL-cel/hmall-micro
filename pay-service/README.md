# pay-service

支付微服务，继承 `com.hmall:hmall-micro:1.0.0`，包名 `com.hmall.pay`，端口 `8086`，数据库 `hm-pay`。

## 迁移范围

迁移 PayController、IPayOrderService、PayOrderServiceImpl、PayOrderMapper/XML、PayOrder 实体、PayApplyDTO、PayOrderFormDTO、PayOrderVO，以及 PayType/PayStatus/PayChannel 枚举。保留单体余额支付、支付单复用和渠道切换逻辑。支付用户 ID 暂时固定为 `1L`。单体源码未修改。

| 方法 | 路径 | 功能 |
| --- | --- | --- |
| POST | /pay-orders | 生成支付单，仅支持 payType=5（余额支付），返回字符串形式的支付单 ID |
| POST | /pay-orders/{id} | 使用余额支付，请求体含 pw，路径 ID 覆盖请求体 ID |

## 远程契约

hm-api 新增以下共享客户端；pay-service 依赖 hm-api，获得 OpenFeign 和 LoadBalancer。启动类配置 `@EnableFeignClients(basePackages = "com.hmall.api")`。

| 客户端 | 方法 | HTTP 请求 |
| --- | --- | --- |
| com.hmall.api.user.UserClient | deductMoney | PUT /users/money/deduct?pw=...&amount=... |
| com.hmall.api.trade.TradeClient | markOrderPaySuccess | PUT /orders/{orderId} |

复用现有 UserController 和 OrderController 的接口。远程契约只有 String、Integer、Long 参数，无需共享 DTO，也无需修改 user-service/trade-service 的 DTO。支付 DTO/VO 仅用于支付模块，保留在 com.hmall.pay 下。

支付顺序：读取支付单并检查状态 → 调 user-service 扣减支付单存储的金额 → 更新本地支付单 → 调 trade-service 标记业务订单已支付。

## 配置与启动

在 hmall-micro 目录执行：

```shell
mvn -pl pay-service -am package
java -jar pay-service/target/pay-service.jar
```

默认 dev 配置沿用单体 MySQL 配置：localhost:3306、root/mysql。需要提前准备 hm-pay 数据库及与单体一致的 pay_order 表。本次不创建或修改数据库。

Nacos 默认地址为 `192.168.150.88:8848`，注册地址为 `192.168.150.1`。支持通过 `NACOS_SERVER_ADDR`、`NACOS_USERNAME`、`NACOS_PASSWORD`、`NACOS_DISCOVERY_IP` 环境变量覆盖。换机器时需要设置可被其他服务访问的本机地址。

实际支付依赖 MySQL、Nacos、user-service、trade-service 同时可用。未安装单体登录拦截器，固定用户方案仅用于当前拆分阶段。

## 验证与限制

- 支付服务及依赖编译打包成功。
- 6 项自动化测试通过：HTTP 路由、参数编码、远程错误传播，以及正常支付调用顺序、已支付拦截、扣款失败、订单更新失败、本地状态竞争失败。使用本地 HTTP 测试服务器和 mock，未访问真实余额或支付数据。
- 用户、交易模块兼容性编译及交易模块原有测试通过。
- 支付服务已在 8086 启动，成功注册至 Nacos（192.168.150.1:8086）；/v2/api-docs 返回 HTTP 200 并包含两个支付路由。运行检查完成后已停止验证进程。
- 未执行真实支付、落库校验、并发扣款和故障补偿测试。

本地 @Transactional 只能回滚 hm-pay 的事务，不能回滚已成功的用户余额扣款和订单状态更新。远程超时结果可能不确定；原流程先扣款再更新支付状态，不能保证并发/重试时仅扣一次。此次迁移未引入分布式事务、扣款幂等或补偿机制。
# user-service

用户微服务，继承 `com.hmall:hmall-micro:1.0.0`，端口 `8084`。

## 迁移范围

- `POST /users/login`：用户登录，保留 BCrypt 校验和 RSA JWT 签名。
- `PUT /users/money/deduct?pw=...&amount=...`：余额扣减。
- `GET /addresses`：当前用户地址列表。
- `GET /addresses/{addressId}`：查询并检查地址归属。
- 用户、地址实体与 Mapper，登录 DTO/VO、地址 DTO、用户状态枚举、JWT 配置和密钥文件。

当前地址查询和余额扣减中的用户 ID 均固定为 `1L`，不安装原单体登录拦截器。此为临时开发方案，接入真实身份认证后需恢复用户上下文。

## 构建与运行

在 hmall-micro 目录执行：

```shell
mvn -pl user-service -am package -DskipTests
java -jar user-service/target/user-service.jar
```

默认使用 dev 配置，MySQL 为 localhost:3306，数据库为 `hm-user`，账号密码沿用单体 dev 配置。数据库需事先准备与单体一致的 `user` 和 `address` 表及数据；本次迁移不执行数据库变更。

Nacos 默认地址为 `192.168.150.88:8848`，可通过 `NACOS_SERVER_ADDR`、`NACOS_USERNAME` 和 `NACOS_PASSWORD` 环境变量覆盖。

除要求的业务依赖外，保留 `spring-security-crypto` 和 `spring-security-rsa`，用于现有登录逻辑。
# UnionBanPlugin

UnionBanPlugin 是一个 Bukkit 插件，使用 MySQL 数据库在多个服务器之间云封禁玩家。

## 安装

1. 从 [GitHub Release](https://github.com/Minecraft-Public-Welfare-Alliance/UnionBan-Bukkit/releases) 下载最新版本的插件。
2. 将下载的 JAR 文件放入 Bukkit 服务器的 `plugins` 文件夹。
3. 重启启动服务器或使用PlugMan启用插件。
4. 通过编辑位于 `plugins/MPWAUnionBan` 文件夹中的 `config.yml` 文件来配置插件。
5. 在配置中输入数据库详细信息，将插件连接到您的 MySQL 数据库。
6. 使用PlugMan重载插件

## 使用方法

- `/uban <playerID> <Reason(hacking, stealing, destroying, other)> <Reason_Text> <isOnline>` - 从联盟中禁止一个玩家。

## 配置

插件使用位于 `plugins/MPWAUnionBan` 文件夹中的 `config.yml` 文件进行配置。您可以修改以下设置：

- `database`：数据库连接详细信息（主机、端口、数据库名称、用户名、密码）
- `banCheckInterval`：刷新封禁列表的时间间隔（以秒为单位）
- `from`：标识封禁玩家的服务器

## 贡献者

- [BaimoQilin](https://github.com/Zhou-Shilin) (Zhou-Shilin, from [Hypiworld](https://baimoqilin.top/hypiworld))

## TODO

- [x] 支持使用`/uban`指令联合封禁玩家
- [x] 支持在配置文件中配置服务器名称
- [ ] 支持在腐竹试图将封禁玩家加入whitelist时给予提醒
- [ ] 支持在配置文件中配置按类别封禁玩家
- [ ] 支持导入banlist

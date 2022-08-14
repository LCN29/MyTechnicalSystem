# Git 常用操作

## 修改仓库的用户名和邮箱

> 1. 修改全局的的用户名和邮箱

* 方式 1
```
git config --global user.name "用户名"

git config --global user.email "邮箱"
```

* 方式 2

在 ${user_home} 新建一个 .gitconfig 文件， 在文件里面添加

```
[user]
	email = 用户邮箱
	name = 用户名
```

> 2. 只修改单独一个仓库的用户名

进入的仓库所在的目录

```
git config user.name "用户名"

git config user.email "用户邮箱"
```

> 3. 代码临时保存

```
git stash save "描述"

git stash pop 

```

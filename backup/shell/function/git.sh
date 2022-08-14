#!/bin/bash


gitShell() {

	case "$1" in

		'name')
			# 设置 git 本地仓库仓库的用户名
			setLocalGitWareHouseName $2;
		;;

		'create-branch')
			# 创建分支
			createBranch $2;
		;;

		'push')
			# 提交修改
			gitPush $2;
		;;

		'create-branch')
			# 创建分支
			createBranch $2;
		;;

		'delete-branch')
			if  [ -n "$2" ] ;then
				# 只删除自己的分支
				if [[ $2 =~ "can" ]]; then

					echo "开始删除分支 $2 ....";
					git checkout master;
					git pull;
					# 删除本地分支
					git branch -D $2;
					# 删除远程分支
					git push origin --delete $2;
					git pull; 
			        echo "删除分支 $2 成功";

				else 
          			echo "需要删除的分支不包含 can 关键字";    
				fi
			else 
		        echo "没有输入要删除的分支名, 不做任何处理";
		    fi
		;;
		'merge-revert')
			git reset --hard HEAD~;
		;;
		'stash-save')
			if  [ ! -n "$2" ] ;then
				git stash;
			else
				git stash save $2;
			fi
		;;
		*)
			echo "unknow command";
		;;
	esac;  
}

# 设置本地仓库的用户名
setLocalGitWareHouseName() {

		gitName='';

		# 没有输入用户名
		if  [ ! -n "$1" ] ;then
			# 从 $configPath 路径下读取 $GitNameKey 的配置
			gitName=`sed "/^$GitNameKey=/!d;s/.*=//" $configPath`;
		else
			# 读取输入的用户名
			gitName=$2;
		fi


		if [ -z "$gitName" ]; then
			echo -e "设置的用户名为空, 跳过配置, 请检查 $configPath 下的 \033[31m$GitNameKey\033[0m 属性是否配置了或者在脚本后面追加用户名";
			return;
		fi

		git config user.name $gitName;
		echo -e "Git 本地仓库用户名设置为 \033[34m$gitName\033[0m 成功";
}

# 创建新的分支
createBranch() {

	if [ -z "$1" ]; then
 		echo -e "新建分支, 但是分支名为空, 请输入分支名";
		return;
	fi

	git checkout master;
	git pull;
	git checkout -b $1;
	git push --set-upstream origin $1;
}

gitPush() {

	git add .;
}